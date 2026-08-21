package dev.hypercarrier.patcher.ipc

import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.IBinder
import android.os.PersistableBundle
import android.util.Log
import dev.hypercarrier.patcher.BuildConfig
import dev.hypercarrier.patcher.IPrivilegedCarrierService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku

/**
 * High-level bridge to manage Shizuku IPC connection, lifecycle, permissions, and privileged service calls.
 */
object ShizukuBridge {

    private const val TAG = "ShizukuBridge"
    const val REQUEST_CODE_SHIZUKU = 1001

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private val _isShizukuInstalled = MutableStateFlow(false)
    val isShizukuInstalled: StateFlow<Boolean> = _isShizukuInstalled.asStateFlow()

    private val _isShizukuRunning = MutableStateFlow(false)
    val isShizukuRunning: StateFlow<Boolean> = _isShizukuRunning.asStateFlow()

    private val _hasPermission = MutableStateFlow(false)
    val hasPermission: StateFlow<Boolean> = _hasPermission.asStateFlow()

    private val _isServiceConnected = MutableStateFlow(false)
    val isServiceConnected: StateFlow<Boolean> = _isServiceConnected.asStateFlow()

    private val _serviceError = MutableStateFlow<String?>(null)
    val serviceError: StateFlow<String?> = _serviceError.asStateFlow()

    private var privilegedService: IPrivilegedCarrierService? = null

    private val binderReceivedListener = Shizuku.OnBinderReceivedListener {
        Log.i(TAG, "Shizuku binder received")
        checkState()
    }

    private val binderDeadListener = Shizuku.OnBinderDeadListener {
        Log.w(TAG, "Shizuku binder died")
        _isShizukuRunning.value = false
        _isServiceConnected.value = false
        privilegedService = null
    }

    private val requestPermissionResultListener = Shizuku.OnRequestPermissionResultListener { requestCode, grantResult ->
        if (requestCode == REQUEST_CODE_SHIZUKU) {
            val granted = grantResult == PackageManager.PERMISSION_GRANTED
            Log.i(TAG, "Shizuku permission result: granted=$granted")
            _hasPermission.value = granted
            if (granted) {
                bindService()
            }
        }
    }

    private val userServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            Log.i(TAG, "PrivilegedCarrierService connected")
            if (service != null) {
                privilegedService = IPrivilegedCarrierService.Stub.asInterface(service)
                _isServiceConnected.value = true
                _serviceError.value = null
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            Log.w(TAG, "PrivilegedCarrierService disconnected")
            privilegedService = null
            _isServiceConnected.value = false
        }
    }

    private val userServiceArgs = Shizuku.UserServiceArgs(
        ComponentName(BuildConfig.APPLICATION_ID, PrivilegedCarrierService::class.java.name)
    )
        .daemon(false)
        .processNameSuffix("privileged_carrier_service")
        .debuggable(BuildConfig.DEBUG)
        .version(BuildConfig.VERSION_CODE)

    /**
     * Initializes listeners for Shizuku binder and permission updates.
     */
    fun init(context: Context) {
        try {
            Shizuku.addBinderReceivedListenerSticky(binderReceivedListener)
            Shizuku.addBinderDeadListener(binderDeadListener)
            Shizuku.addRequestPermissionResultListener(requestPermissionResultListener)
            checkState()
        } catch (t: Throwable) {
            Log.e(TAG, "Failed to initialize ShizukuBridge", t)
            _serviceError.value = "Shizuku init error: ${t.message}"
        }
    }

    /**
     * Refreshes Shizuku availability, permission, and connection state.
     */
    fun checkState() {
        val pingSuccess = try {
            Shizuku.pingBinder()
        } catch (t: Throwable) {
            false
        }

        _isShizukuRunning.value = pingSuccess

        if (pingSuccess) {
            val permissionGranted = try {
                if (Shizuku.getVersion() < 11) {
                    false
                } else {
                    Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
                }
            } catch (t: Throwable) {
                false
            }
            _hasPermission.value = permissionGranted

            if (permissionGranted && !_isServiceConnected.value) {
                bindService()
            }
        } else {
            _hasPermission.value = false
            _isServiceConnected.value = false
        }
    }

    /**
     * Requests Shizuku permission from the user.
     */
    fun requestPermission() {
        if (!Shizuku.pingBinder()) {
            _serviceError.value = "Shizuku is not running. Please start Shizuku first."
            return
        }

        if (Shizuku.getVersion() < 11) {
            _serviceError.value = "Shizuku version is too old. Please update Shizuku."
            return
        }

        try {
            Shizuku.requestPermission(REQUEST_CODE_SHIZUKU)
        } catch (t: Throwable) {
            Log.e(TAG, "Failed to request Shizuku permission", t)
            _serviceError.value = "Request permission failed: ${t.message}"
        }
    }

    /**
     * Binds to the privileged carrier user service via Shizuku.
     */
    fun bindService() {
        if (!_hasPermission.value || !_isShizukuRunning.value) {
            Log.w(TAG, "Cannot bind service: permission not granted or Shizuku not running")
            return
        }

        try {
            Log.i(TAG, "Binding to PrivilegedCarrierService...")
            Shizuku.bindUserService(userServiceArgs, userServiceConnection)
        } catch (t: Throwable) {
            Log.e(TAG, "Failed to bind privileged user service", t)
            _serviceError.value = "Service bind failed: ${t.message}"
        }
    }

    /**
     * Unbinds the privileged carrier service.
     */
    fun unbindService() {
        try {
            Shizuku.unbindUserService(userServiceArgs, userServiceConnection, true)
            privilegedService = null
            _isServiceConnected.value = false
        } catch (t: Throwable) {
            Log.w(TAG, "Error unbinding service: ${t.message}")
        }
    }

    /**
     * Injects a persistent CarrierConfig override bundle for the given subscription ID.
     */
    suspend fun applyPersistentConfig(subId: Int, bundle: PersistableBundle): Result<Unit> = withContext(Dispatchers.IO) {
        val service = privilegedService ?: return@withContext Result.failure(
            IllegalStateException("Privileged service is not connected. Please authorize Shizuku.")
        )

        try {
            service.applyPersistentConfig(subId, bundle)
            Result.success(Unit)
        } catch (t: Throwable) {
            Log.e(TAG, "Failed to apply persistent config: ${t.message}", t)
            Result.failure(t)
        }
    }

    /**
     * Clears all CarrierConfig overrides for the given subscription ID, restoring defaults.
     */
    suspend fun clearConfigOverride(subId: Int): Result<Unit> = withContext(Dispatchers.IO) {
        val service = privilegedService ?: return@withContext Result.failure(
            IllegalStateException("Privileged service is not connected. Please authorize Shizuku.")
        )

        try {
            service.clearConfigOverride(subId)
            Result.success(Unit)
        } catch (t: Throwable) {
            Log.e(TAG, "Failed to clear config: ${t.message}", t)
            Result.failure(t)
        }
    }

    /**
     * Directly provisions IMS capabilities (VoLTE, VoWiFi, VoNR, Video) via privileged service.
     */
    suspend fun setImsProvisioning(subId: Int, enableVoLte: Boolean = true, enableVoWifi: Boolean = true, enableVoNr: Boolean = true): Result<Unit> = withContext(Dispatchers.IO) {
        val service = privilegedService ?: return@withContext Result.failure(
            IllegalStateException("Privileged service is not connected. Please authorize Shizuku.")
        )

        try {
            service.setImsProvisioning(subId, enableVoLte, enableVoWifi, enableVoNr)
            Result.success(Unit)
        } catch (t: Throwable) {
            Log.e(TAG, "Failed to set IMS provisioning: ${t.message}", t)
            Result.failure(t)
        }
    }

    /**
     * Granular 1-Tap Toggle: Voice over LTE (VoLTE / 4G Calling).
     */
    suspend fun setVoLteEnabled(subId: Int, enable: Boolean): Result<Unit> = withContext(Dispatchers.IO) {
        val service = privilegedService ?: return@withContext Result.failure(
            IllegalStateException("Privileged service is not connected. Please authorize Shizuku.")
        )

        try {
            service.setVoLteEnabled(subId, enable)
            Result.success(Unit)
        } catch (t: Throwable) {
            Log.e(TAG, "Failed to set VoLTE: ${t.message}", t)
            Result.failure(t)
        }
    }

    /**
     * Granular 1-Tap Toggle: Voice over Wi-Fi (VoWiFi / Wi-Fi Calling).
     */
    suspend fun setVoWifiEnabled(subId: Int, enable: Boolean): Result<Unit> = withContext(Dispatchers.IO) {
        val service = privilegedService ?: return@withContext Result.failure(
            IllegalStateException("Privileged service is not connected. Please authorize Shizuku.")
        )

        try {
            service.setVoWifiEnabled(subId, enable)
            Result.success(Unit)
        } catch (t: Throwable) {
            Log.e(TAG, "Failed to set VoWiFi: ${t.message}", t)
            Result.failure(t)
        }
    }

    /**
     * Granular 1-Tap Toggle: Voice over New Radio (5G VoNR).
     */
    suspend fun setVoNrEnabled(subId: Int, enable: Boolean): Result<Unit> = withContext(Dispatchers.IO) {
        val service = privilegedService ?: return@withContext Result.failure(
            IllegalStateException("Privileged service is not connected. Please authorize Shizuku.")
        )

        try {
            service.setVoNrEnabled(subId, enable)
            Result.success(Unit)
        } catch (t: Throwable) {
            Log.e(TAG, "Failed to set VoNR: ${t.message}", t)
            Result.failure(t)
        }
    }

    /**
     * Granular 1-Tap Toggle: Carrier Video Calling (ViLTE).
     */
    suspend fun setViLteEnabled(subId: Int, enable: Boolean): Result<Unit> = withContext(Dispatchers.IO) {
        val service = privilegedService ?: return@withContext Result.failure(
            IllegalStateException("Privileged service is not connected. Please authorize Shizuku.")
        )

        try {
            service.setViLteEnabled(subId, enable)
            Result.success(Unit)
        } catch (t: Throwable) {
            Log.e(TAG, "Failed to set ViLTE: ${t.message}", t)
            Result.failure(t)
        }
    }

    /**
     * Granular Setting: Wi-Fi Calling Mode (1 = Wi-Fi Preferred, 2 = Cellular Preferred).
     */
    suspend fun setVoWifiMode(subId: Int, mode: Int): Result<Unit> = withContext(Dispatchers.IO) {
        val service = privilegedService ?: return@withContext Result.failure(
            IllegalStateException("Privileged service is not connected. Please authorize Shizuku.")
        )

        try {
            service.setVoWifiMode(subId, mode)
            Result.success(Unit)
        } catch (t: Throwable) {
            Log.e(TAG, "Failed to set VoWiFi mode: ${t.message}", t)
            Result.failure(t)
        }
    }

    /**
     * Forces immediate IMS deregistration and re-registration trigger on modem.
     */
    suspend fun forceReRegisterIms(subId: Int): Result<Unit> = withContext(Dispatchers.IO) {
        val service = privilegedService ?: return@withContext Result.failure(
            IllegalStateException("Privileged service is not connected. Please authorize Shizuku.")
        )

        try {
            service.forceReRegisterIms(subId)
            Result.success(Unit)
        } catch (t: Throwable) {
            Log.e(TAG, "Failed to re-register IMS: ${t.message}", t)
            Result.failure(t)
        }
    }

    /**
     * Fetches current active CarrierConfig bundle for the given subscription ID.
     */
    suspend fun getCarrierConfig(subId: Int): Result<PersistableBundle> = withContext(Dispatchers.IO) {
        val service = privilegedService ?: return@withContext Result.failure(
            IllegalStateException("Privileged service is not connected.")
        )

        try {
            val bundle = service.getCarrierConfig(subId)
            Result.success(bundle ?: PersistableBundle())
        } catch (t: Throwable) {
            Log.e(TAG, "Failed to get carrier config: ${t.message}", t)
            Result.failure(t)
        }
    }

    /**
     * Enforces allowed network types mask (e.g. 5G SA/NSA, LTE).
     */
    suspend fun setAllowedNetworkTypes(subId: Int, mask: Long): Result<Unit> = withContext(Dispatchers.IO) {
        val service = privilegedService ?: return@withContext Result.failure(
            IllegalStateException("Privileged service is not connected.")
        )

        try {
            service.setAllowedNetworkTypes(subId, mask)
            Result.success(Unit)
        } catch (t: Throwable) {
            Log.e(TAG, "Failed to set allowed network types: ${t.message}", t)
            Result.failure(t)
        }
    }

    /**
     * Enforces high-level network mode:
     * 1 = 5G SA Only (Ultra-Fast)
     * 2 = 5G NSA + LTE-CA Turbo (Full Performance)
     * 3 = LTE-A Only (Battery Saving)
     */
    suspend fun setNetworkMode(subId: Int, mode: Int): Result<Unit> = withContext(Dispatchers.IO) {
        val service = privilegedService ?: return@withContext Result.failure(
            IllegalStateException("Privileged service is not connected.")
        )

        try {
            service.setNetworkMode(subId, mode)
            Result.success(Unit)
        } catch (t: Throwable) {
            Log.e(TAG, "Failed to set network mode: ${t.message}", t)
            Result.failure(t)
        }
    }

    /**
     * Soft cycles radio power to clear dead cell locks and force CA re-negotiation.
     */
    suspend fun cycleRadioPower(subId: Int): Result<Unit> = withContext(Dispatchers.IO) {
        val service = privilegedService ?: return@withContext Result.failure(
            IllegalStateException("Privileged service is not connected.")
        )

        try {
            service.cycleRadioPower(subId)
            Result.success(Unit)
        } catch (t: Throwable) {
            Log.e(TAG, "Failed to cycle radio power: ${t.message}", t)
            Result.failure(t)
        }
    }

    /**
     * Flushes local DNS resolver cache.
     */
    suspend fun flushDnsCache(): Result<Unit> = withContext(Dispatchers.IO) {
        val service = privilegedService ?: return@withContext Result.failure(
            IllegalStateException("Privileged service is not connected.")
        )

        try {
            service.flushDnsCache()
            Result.success(Unit)
        } catch (t: Throwable) {
            Log.e(TAG, "Failed to flush DNS cache: ${t.message}", t)
            Result.failure(t)
        }
    }

    /**
     * Queries real-time IMS registration state via privileged service.
     */
    suspend fun getImsRegistrationState(subId: Int): Int = withContext(Dispatchers.IO) {
        val service = privilegedService ?: return@withContext -1
        try {
            service.getImsRegistrationState(subId)
        } catch (t: Throwable) {
            -1
        }
    }
}
