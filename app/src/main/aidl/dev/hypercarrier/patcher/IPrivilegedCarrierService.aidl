package dev.hypercarrier.patcher;

import android.os.PersistableBundle;

/**
 * Privileged carrier configuration and telephony service running under UID 2000 (Shell) via Shizuku.
 * Provides root-free persistent CarrierConfig overrides directly written to disk,
 * granular 1-tap IMS Engine toggles, low-level network mode enforcement, and radio power cycling.
 */
interface IPrivilegedCarrierService {
    /**
     * Injects a persistent CarrierConfig override bundle for the given subscription ID.
     */
    void applyPersistentConfig(int subId, in PersistableBundle bundle);

    /**
     * Clears all CarrierConfig overrides for the given subscription ID, restoring OEM defaults.
     */
    void clearConfigOverride(int subId);

    /**
     * Retrieves the active CarrierConfig bundle for the given subscription ID.
     */
    PersistableBundle getCarrierConfig(int subId);

    /**
     * Granular 1-Tap Toggle: Voice over LTE (VoLTE / 4G Calling).
     */
    void setVoLteEnabled(int subId, boolean enable);

    /**
     * Granular 1-Tap Toggle: Voice over Wi-Fi (VoWiFi / Wi-Fi Calling).
     */
    void setVoWifiEnabled(int subId, boolean enable);

    /**
     * Granular 1-Tap Toggle: Voice over New Radio (5G VoNR).
     */
    void setVoNrEnabled(int subId, boolean enable);

    /**
     * Granular 1-Tap Toggle: Carrier Video Calling (ViLTE).
     */
    void setViLteEnabled(int subId, boolean enable);

    /**
     * Granular Setting: Wi-Fi Calling Mode (1 = Wi-Fi Preferred, 2 = Cellular Preferred).
     */
    void setVoWifiMode(int subId, int mode);

    /**
     * Forces immediate IMS deregistration and re-registration trigger on modem.
     */
    void forceReRegisterIms(int subId);

    /**
     * Directly provisions IMS capabilities (VoLTE, VoWiFi, VoNR, Video) via ITelephony/IImsConfig.
     */
    void setImsProvisioning(int subId, boolean enableVoLte, boolean enableVoWifi, boolean enableVoNr);

    /**
     * Queries real-time IMS registration state for the given subscription ID.
     * Returns: 0 = Unregistered, 1 = Registered (VoLTE/VoWiFi/VoNR), -1 = Unknown/Error.
     */
    int getImsRegistrationState(int subId);

    /**
     * Enforces allowed network types mask (e.g. 5G SA/NSA, LTE, etc.).
     */
    void setAllowedNetworkTypes(int subId, long mask);

    /**
     * Enforces specific high-level network mode:
     * 1 = 5G SA Only (Ultra-Fast)
     * 2 = 5G NSA + LTE-CA Turbo (Full Performance)
     * 3 = LTE-A Only (Battery Saving)
     */
    void setNetworkMode(int subId, int mode);

    /**
     * Soft cycles radio power (toggle off -> sleep -> toggle on) to flush dead cell locks.
     */
    void cycleRadioPower(int subId);

    /**
     * Flushes local device DNS resolver cache.
     */
    void flushDnsCache();

    /**
     * Checks if the privileged service is active and running under UID 2000 (Shell).
     */
    boolean isPrivileged();

    /**
     * Requests termination of the privileged user service process.
     */
    void destroy();
}
