package dev.hypercarrier.patcher.data

import android.os.PersistableBundle

/**
 * Pre-baked single-tap carrier presets with Extreme Turbo Carrier Aggregation.
 */
object CarrierPresets {

    /**
     * Jazz Pakistan (PMCL - MCC 410, MNC 01)
     * VoLTE enabled, WFC mode 1 (Wi-Fi Preferred), NSA/SA NR enabled, Extreme Turbo Aggregation.
     */
    val JAZZ_PAKISTAN = CarrierPreset(
        id = "jazz_pk",
        name = "Jazz Pakistan",
        country = "Pakistan",
        targetMcc = "410",
        targetMnc = "01",
        description = "VoLTE & VoWiFi (Wi-Fi Preferred) + 5G NSA/SA + Extreme Turbo CA with instant provisioning.",
        is5gSaSupported = true,
        isVoNrSupported = true,
        isVoLteSupported = true,
        isVoWifiSupported = true,
        payloadBuilder = { _ ->
            CarrierConfigPayloadBuilder()
                .enable5gNr(
                    enableSa = true,
                    enableNsa = true,
                    nrAvailability = 3,
                    iconConfig = CarrierConfigPayloadBuilder.DEFAULT_5G_ICON_CONFIG,
                    gracePeriodSec = 0
                )
                .enableVoNr(enabled = true, settingVisibility = true)
                .enableTurboAggregation()
                .enableVoLte(
                    available = true,
                    editable = true,
                    onByDefault = true,
                    supportsCallerId = true,
                    allowTurnOff = true
                )
                .enableVoWifi(
                    available = true,
                    defaultEnabled = true,
                    roamingEnabled = true,
                    editable = true,
                    defaultMode = 1, // Wi-Fi Preferred
                    crossSimAvailable = true
                )
                .enableSignalEnhancements()
                .setGeneralOverrides()
                .putString("carrier_name_override_string", "Jazz")
                .build()
        }
    )

    /**
     * Zong Pakistan (CMPak - MCC 410, MNC 04)
     * VoLTE active, VoNR active, aggressive 5G SA prioritization + Extreme Turbo CA.
     */
    val ZONG_PAKISTAN = CarrierPreset(
        id = "zong_pk",
        name = "Zong Pakistan",
        country = "Pakistan",
        targetMcc = "410",
        targetMnc = "04",
        description = "Aggressive 5G SA/NSA prioritization + VoNR active + 4Gbps Turbo Aggregation profile.",
        is5gSaSupported = true,
        isVoNrSupported = true,
        isVoLteSupported = true,
        isVoWifiSupported = true,
        payloadBuilder = { _ ->
            CarrierConfigPayloadBuilder()
                .enable5gNr(
                    enableSa = true,
                    enableNsa = true,
                    nrAvailability = 3,
                    iconConfig = CarrierConfigPayloadBuilder.DEFAULT_5G_ICON_CONFIG,
                    gracePeriodSec = 0
                )
                .enableVoNr(enabled = true, settingVisibility = true)
                .enableTurboAggregation()
                .enableVoLte(
                    available = true,
                    editable = true,
                    onByDefault = true,
                    supportsCallerId = true,
                    allowTurnOff = true
                )
                .enableVoWifi(
                    available = true,
                    defaultEnabled = true,
                    roamingEnabled = true,
                    editable = true,
                    defaultMode = 1,
                    crossSimAvailable = true
                )
                .enableSignalEnhancements()
                .setGeneralOverrides()
                .putString("carrier_name_override_string", "Zong")
                .build()
        }
    )

    /**
     * Telenor Pakistan (MCC 410, MNC 06)
     * VoLTE + WFC override, Enhanced 4G LTE default ON + Extreme Turbo CA.
     */
    val TELENOR_PAKISTAN = CarrierPreset(
        id = "telenor_pk",
        name = "Telenor Pakistan",
        country = "Pakistan",
        targetMcc = "410",
        targetMnc = "06",
        description = "VoLTE + WFC override + Enhanced 4G LTE default ON + Extreme LTE-A Aggregation.",
        is5gSaSupported = true,
        isVoNrSupported = true,
        isVoLteSupported = true,
        isVoWifiSupported = true,
        payloadBuilder = { _ ->
            CarrierConfigPayloadBuilder()
                .enable5gNr(
                    enableSa = true,
                    enableNsa = true,
                    nrAvailability = 3,
                    iconConfig = CarrierConfigPayloadBuilder.DEFAULT_5G_ICON_CONFIG,
                    gracePeriodSec = 0
                )
                .enableVoNr(enabled = true, settingVisibility = true)
                .enableTurboAggregation()
                .enableVoLte(
                    available = true,
                    editable = true,
                    onByDefault = true,
                    supportsCallerId = true,
                    allowTurnOff = true
                )
                .enableVoWifi(
                    available = true,
                    defaultEnabled = true,
                    roamingEnabled = true,
                    editable = true,
                    defaultMode = 1,
                    crossSimAvailable = true
                )
                .enableSignalEnhancements()
                .setGeneralOverrides()
                .putString("carrier_name_override_string", "Telenor PK")
                .build()
        }
    )

    /**
     * Ufone 4G (PTML - MCC 410, MNC 03)
     * 4G Calling + IMS core force registration + Extreme Turbo CA.
     */
    val UFONE_PAKISTAN = CarrierPreset(
        id = "ufone_pk",
        name = "Ufone 4G",
        country = "Pakistan",
        targetMcc = "410",
        targetMnc = "03",
        description = "4G Calling (VoLTE) + IMS core force registration + 0ms APN low-latency profile.",
        is5gSaSupported = true,
        isVoNrSupported = true,
        isVoLteSupported = true,
        isVoWifiSupported = true,
        payloadBuilder = { _ ->
            CarrierConfigPayloadBuilder()
                .enable5gNr(
                    enableSa = true,
                    enableNsa = true,
                    nrAvailability = 3,
                    iconConfig = CarrierConfigPayloadBuilder.DEFAULT_5G_ICON_CONFIG,
                    gracePeriodSec = 0
                )
                .enableVoNr(enabled = true, settingVisibility = true)
                .enableTurboAggregation()
                .enableVoLte(
                    available = true,
                    editable = true,
                    onByDefault = true,
                    supportsCallerId = true,
                    allowTurnOff = true
                )
                .enableVoWifi(
                    available = true,
                    defaultEnabled = true,
                    roamingEnabled = true,
                    editable = true,
                    defaultMode = 1,
                    crossSimAvailable = true
                )
                .enableSignalEnhancements()
                .setGeneralOverrides()
                .putString("carrier_name_override_string", "Ufone")
                .build()
        }
    )

    /**
     * Global Ultra-Unlock (Universal wildcard matching any SIM)
     */
    val GLOBAL_ULTRA_UNLOCK = CarrierPreset(
        id = "global_unlock",
        name = "Global Ultra-Unlock",
        country = "Universal",
        targetMcc = "*",
        targetMnc = "*",
        description = "All-in-one universal override: VoLTE, VoWiFi, VoNR, 5G SA/NSA, Extreme Turbo CA, high-sensitivity signal bars.",
        is5gSaSupported = true,
        isVoNrSupported = true,
        isVoLteSupported = true,
        isVoWifiSupported = true,
        payloadBuilder = { _ ->
            CarrierConfigPayloadBuilder.buildUniversalUltraUnlockBundle()
        }
    )

    val ALL_PRESETS = listOf(
        GLOBAL_ULTRA_UNLOCK,
        JAZZ_PAKISTAN,
        ZONG_PAKISTAN,
        TELENOR_PAKISTAN,
        UFONE_PAKISTAN
    )

    /**
     * Finds the most relevant preset based on SIM MCC and MNC, or falls back to Global Ultra-Unlock.
     */
    fun findBestPreset(mcc: String?, mnc: String?): CarrierPreset {
        if (mcc.isNullOrBlank() || mnc.isNullOrBlank()) return GLOBAL_ULTRA_UNLOCK
        val cleanMcc = mcc.trim()
        val cleanMnc = mnc.trim().padStart(2, '0')

        return ALL_PRESETS.firstOrNull { preset ->
            preset.targetMcc == cleanMcc && (preset.targetMnc == cleanMnc || preset.targetMnc == "*")
        } ?: GLOBAL_ULTRA_UNLOCK
    }
}
