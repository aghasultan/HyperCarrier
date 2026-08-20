# HyperCarrier Engine: Hyper-Elite Edition for Google Pixel 9 (Android 17)

HyperCarrier is the ultimate, production-grade connectivity and carrier configuration engine designed specifically for Google Pixel devices (optimized for Tensor G4 / Pixel 9 series on Android 17 / Baklava).

It merges and strictly supersedes the architectures of:
1. **[ikirby/pixelcarriersettings](https://github.com/ikirby/pixelcarriersettings)**: Reboot-persistent disk-level overrides using hidden `CarrierConfigManager.overrideConfig(subId, bundle, persistent = true)`.
2. **[kyujin-cho/pixel-volte-patch](https://github.com/kyujin-cho/pixel-volte-patch)**: Exhaustive carrier configuration key definitions (VoLTE, VoWiFi, VoNR, 5G SA/NSA, IMS registration, QS tiles, and Telephony callback monitoring).

---

## 🌟 Hyper-Elite Features & Innovations

### 1. Zero-Reboot-Amnesia (Persistent Disk Overrides)
Standard ADB `setprop` or ephemeral CarrierConfig injections are wiped whenever the device reboots or mobile data cycles. HyperCarrier routes injection through `PrivilegedCarrierService` running under the `com.android.shell` process via Shizuku (UID 2000). This invokes:
```java
CarrierConfigManager.overrideConfig(subId, bundle, true /* persistent */)
```
Android writes these overrides directly to flash storage (`/data/user_de/0/com.android.phone/files/carrierconfig-*.xml`). Once injected, the patch **survives device reboots** and developer options resets without requiring root or an active ADB connection.

### 2. Extreme Turbo Aggregation & Zero-Delay Profile
- **4 Gbps 5G / 1 Gbps LTE Bandwidth Allocations**: Injected via `bandwidth_string_array = ["5G:4000000,200000", "LTE:1000000,150000"]`.
- **0ms APN Connection Delay**: Zero data setup latency (`carrier_data_call_apn_delay_default_long = 0L`).
- **Instant Dual-SIM Data Switching**: Eliminates delay gaps (`data_switch_validation_min_gap_long = 0L`).
- **Throttling Threshold Removal**: Disables carrier-side soft data caps (`data_limit_threshold_bool = false`).

### 3. Modem Network Mode Enforcer & Radio Turbo Flush
- **5G SA Only (Ultra-Fast)**: Forces pure 5G Standalone core, eliminating LTE anchor latency.
- **5G NSA + LTE-CA Turbo (Full Power)**: Aggregates all LTE and 5G bands simultaneously.
- **LTE-A Only (Extreme Battery)**: Locks onto 4G+ multi-carrier aggregation without NR power draw.
- **1-Tap Radio Turbo Flush**: Cycles radio power via low-level `setRadioPower` to instantly clear dead cell locks and lock onto the fastest carrier component.

### 4. Autonomous Radio Guard & Auto-Healer
- Background watchdog service (`RadioGuardService`) that monitors IMS registration and cellular signal health.
- Automatically re-applies persistent carrier overrides, soft-resets the radio, and flushes DNS if IMS unregisters or cell drops to 2G/3G in poor coverage.

### 5. RF Lab & Real-Time Latency Benchmark
- **Carrier Aggregation (CA) Lab**: Live Primary Serving Cell (PCELL) and Secondary Serving Cells (SCELL 1..3) aggregation inspector showing total aggregated bandwidth (e.g. 60 MHz LTE-A / 140 MHz 5G EN-DC), 4x4 MIMO, and 256-QAM active tags.
- **1-Tap Ultra Latency Benchmark**: Non-blocking coroutine Anycast tester measuring Min RTT, Average Latency, Jitter variance, and Packet Loss % against Cloudflare (1.1.1.1) and Google (8.8.8.8).

---

## 🇵🇰 Pre-Baked Carrier Profiles & Presets

- **Jazz Pakistan (MCC 410, MNC 01):** VoLTE enabled, WFC mode 1 (Wi-Fi Preferred), NSA/SA NR enabled, Extreme Turbo CA.
- **Zong Pakistan (MCC 410, MNC 04):** VoLTE active, VoNR active, aggressive 5G SA prioritization + 4Gbps Turbo profile.
- **Telenor PK (MCC 410, MNC 06):** VoLTE + WFC override, Enhanced 4G LTE default ON + Extreme LTE-A Aggregation.
- **Ufone PK (MCC 410, MNC 03):** 4G Calling + IMS core force registration + 0ms APN low-latency profile.
- **Global Ultra-Unlock:** Universal wildcard matching all SIM slots with maximum aggregation and sensitivity.

---

## 📱 Architecture Overview

```text
HyperCarrier/
├── app/
│   ├── build.gradle.kts
│   ├── src/
│   │   └── main/
│   │       ├── aidl/
│   │       │   └── dev/hypercarrier/patcher/IPrivilegedCarrierService.aidl
│   │       ├── java/dev/hypercarrier/patcher/
│   │       │   ├── data/
│   │       │   │   ├── CarrierPresets.kt
│   │       │   │   ├── CarrierConfigPayloadBuilder.kt
│   │       │   │   └── Models.kt
│   │       │   ├── ipc/
│   │       │   │   ├── PrivilegedCarrierService.kt
│   │       │   │   └── ShizukuBridge.kt
│   │       │   ├── telephony/
│   │       │   │   ├── TelephonyDiagnosticsManager.kt
│   │       │   │   ├── NetworkBenchmarkManager.kt
│   │       │   │   └── SubscriptionHelper.kt
│   │       │   ├── service/
│   │       │   │   ├── ImsStatusTileService.kt
│   │       │   │   ├── VoNRToggleTileService.kt
│   │       │   │   ├── RadioFlushTileService.kt
│   │       │   │   └── RadioGuardService.kt
│   │       │   ├── ui/
│   │       │   │   ├── MainActivity.kt
│   │       │   │   ├── MainViewModel.kt
│   │       │   │   ├── theme/
│   │       │   │   └── screens/
│   │       │   │       ├── DashboardScreen.kt
│   │       │   │       ├── ConfigEditorScreen.kt
│   │       │   │       └── DiagnosticsScreen.kt
│   │       └── AndroidManifest.xml
│   └── proguard-rules.pro
├── build.gradle.kts
├── settings.gradle.kts
└── gradle/libs.versions.toml
```

---

## ⚡ Setup & Usage Instructions

1. **Install Shizuku** on your Pixel device:
   - Start Shizuku via **Wireless Debugging** (Settings > Developer options > Wireless debugging) or via ADB (`adb shell sh /sdcard/Android/data/moe.shizuku.privileged.api/start.sh`).
2. **Build and Install HyperCarrier**:
   ```bash
   ./gradlew assembleDebug
   adb install app/build/outputs/apk/debug/app-debug.apk
   ```
3. **Authorize Shizuku Permission**:
   - Launch HyperCarrier and tap **Authorize Shizuku** on the top status pill.
4. **Apply Profile**:
   - Select your target SIM card (SIM 1, SIM 2, or eSIM).
   - Tap **Apply** on your operator card (e.g. Jazz, Zong, Telenor, Ufone) or **Apply Ultra Unlock**.
   - Your changes are immediately written to flash and will persist across reboots.
5. **Quick Settings Tiles**:
   - Add **IMS Status**, **VoNR 5G Voice**, and **Radio Flush** tiles to your Quick Settings shade for live monitoring and instant toggling.
