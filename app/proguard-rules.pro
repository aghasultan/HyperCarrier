# Proguard rules for HyperCarrier

# Keep AIDL generated interfaces and stubs
-keep class dev.hypercarrier.patcher.IPrivilegedCarrierService { *; }
-keep class dev.hypercarrier.patcher.IPrivilegedCarrierService$Stub { *; }
-keep class dev.hypercarrier.patcher.ipc.PrivilegedCarrierService { *; }

# Keep Shizuku UserService and reflection entry points
-keep class dev.rikka.shizuku.** { *; }
-keep interface dev.rikka.shizuku.** { *; }

# Keep reflection targets on Android Telephony and CarrierConfigManager
-keepclassmembers class android.telephony.CarrierConfigManager {
    public void overrideConfig(int, android.os.PersistableBundle, boolean);
    public void overrideConfig(int, android.os.PersistableBundle);
}

-keepclassmembers class android.telephony.TelephonyManager {
    public void setAllowedNetworkTypesForReason(int, long);
    public boolean isImsRegistered(int);
}

-keepclassmembers class com.android.internal.telephony.ICarrierConfigLoader {
    public void overrideConfig(int, android.os.PersistableBundle, boolean);
}

-keepclassmembers class com.android.internal.telephony.ITelephony {
    *;
}
