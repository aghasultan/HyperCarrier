# Proguard rules for HyperCarrier Hyper-Elite Patcher

# Keep AIDL generated interfaces and stubs
-keep class dev.hypercarrier.patcher.IPrivilegedCarrierService { *; }
-keep class dev.hypercarrier.patcher.IPrivilegedCarrierService$Stub { *; }
-keep class dev.hypercarrier.patcher.ipc.PrivilegedCarrierService { *; }

# Keep Broker Instrumentation & Engine
-keep class dev.hypercarrier.patcher.ipc.HyperInstrumentation { *; }
-keep class dev.hypercarrier.patcher.ipc.HyperCarrierEngine { *; }

# Keep Shizuku UserService and reflection entry points
-keep class dev.rikka.shizuku.** { *; }
-keep interface dev.rikka.shizuku.** { *; }
-keep class moe.shizuku.** { *; }

# Keep Hidden API Bypass
-keep class org.lsposed.hiddenapibypass.** { *; }

# Keep reflection targets on Android Framework
-keepclassmembers class android.app.IActivityManager { *; }
-keepclassmembers class android.app.IActivityManager$Stub { *; }
-keepclassmembers class android.app.UiAutomationConnection { *; }
-keepclassmembers class android.os.ServiceManager { *; }

# Keep reflection targets on Android Telephony and CarrierConfigManager
-keepclassmembers class android.telephony.CarrierConfigManager {
    public void overrideConfig(int, android.os.PersistableBundle, boolean);
    public void overrideConfig(int, android.os.PersistableBundle);
}

-keepclassmembers class android.telephony.TelephonyManager {
    public void setAllowedNetworkTypesForReason(int, long);
    public boolean isImsRegistered(int);
}

-keepclassmembers class com.android.internal.telephony.ICarrierConfigLoader { *; }
-keepclassmembers class com.android.internal.telephony.ICarrierConfigLoader$Stub { *; }
-keepclassmembers class com.android.internal.telephony.ITelephony { *; }
-keepclassmembers class com.android.internal.telephony.ITelephony$Stub { *; }
-keepclassmembers class com.android.internal.telephony.ISub { *; }
-keepclassmembers class com.android.internal.telephony.ISub$Stub { *; }

# Keep data models
-keep class dev.hypercarrier.patcher.data.** { *; }
