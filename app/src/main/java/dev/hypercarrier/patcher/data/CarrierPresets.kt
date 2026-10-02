package dev.hypercarrier.patcher.data

import android.os.PersistableBundle

/**
 * Pre-baked single-tap carrier presets with Extreme Turbo Carrier Aggregation,
 * 5G SA/NSA prioritization, VoNR voice calling, and VoLTE/VoWiFi provisioning.
 */
object CarrierPresets {

    // --- Pakistani Operators ---

    val ZONG_PAKISTAN = CarrierPreset(
        id = "zong_pk",
        name = "Zong Pakistan (CMPak)",
        country = "Pakistan",
        targetMcc = "410",
        targetMnc = "04",
        description = "Aggressive 5G SA/NSA prioritization (n78, n41, n1) + VoNR active + VoLTE/VoWiFi + 4Gbps Turbo Aggregation (B1+B3+n78).",
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
                .putBoolean("carrier_name_override_bool", true)
                .putString("carrier_name_string", "Zong")
                .putString("ims.ims_user_agent_string", "HyperCarrier/1.0 (Pixel 9 Tensor G4; VoLTE/VoWiFi)")
                .build()
        }
    )

    val JAZZ_PAKISTAN = CarrierPreset(
        id = "jazz_pk",
        name = "Jazz (PMCL)",
        country = "Pakistan",
        targetMcc = "410",
        targetMnc = "01",
        description = "VoLTE & VoWiFi (Wi-Fi Preferred) + 5G NSA/SA + B1+B3+B8 CA Turbo Aggregation with instant provisioning.",
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
                .putBoolean("carrier_name_override_bool", true)
                .putString("carrier_name_string", "Jazz")
                .build()
        }
    )

    val TELENOR_PAKISTAN = CarrierPreset(
        id = "telenor_pk",
        name = "Telenor Pakistan",
        country = "Pakistan",
        targetMcc = "410",
        targetMnc = "06",
        description = "VoLTE + WFC override + Enhanced 4G LTE default ON + B3+B5+B8 LTE-A Aggregation.",
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
                .putBoolean("carrier_name_override_bool", true)
                .putString("carrier_name_string", "Telenor PK")
                .build()
        }
    )

    val UFONE_PAKISTAN = CarrierPreset(
        id = "ufone_pk",
        name = "Ufone 4G (PTML)",
        country = "Pakistan",
        targetMcc = "410",
        targetMnc = "03",
        description = "4G Calling (VoLTE) + IMS core force registration + 0ms APN low-latency profile + B3+B8 CA.",
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
                .putBoolean("carrier_name_override_bool", true)
                .putString("carrier_name_string", "Ufone")
                .build()
        }
    )

    // --- Global Operators ---

    val VERIZON_US = CarrierPreset(
        id = "verizon_us",
        name = "Verizon Wireless",
        country = "United States",
        targetMcc = "311",
        targetMnc = "480",
        description = "5G Ultra Wideband (C-Band n77 + mmWave) + VoLTE + VoWiFi + Aggressive SA Core.",
        is5gSaSupported = true,
        isVoNrSupported = true,
        isVoLteSupported = true,
        isVoWifiSupported = true,
        payloadBuilder = { _ ->
            CarrierConfigPayloadBuilder()
                .enable5gNr(enableSa = true, enableNsa = true, nrAvailability = 3)
                .enableVoNr(enabled = true)
                .enableTurboAggregation()
                .enableVoLte()
                .enableVoWifi()
                .enableSignalEnhancements()
                .setGeneralOverrides()
                .putBoolean("carrier_name_override_bool", true)
                .putString("carrier_name_string", "Verizon")
                .build()
        }
    )

    val TMOBILE_US = CarrierPreset(
        id = "tmobile_us",
        name = "T-Mobile US",
        country = "United States",
        targetMcc = "310",
        targetMnc = "260",
        description = "Ultra Capacity 5G SA (n41, n71, n25) + Pure VoNR Voice + High-sensitivity signal gauge.",
        is5gSaSupported = true,
        isVoNrSupported = true,
        isVoLteSupported = true,
        isVoWifiSupported = true,
        payloadBuilder = { _ ->
            CarrierConfigPayloadBuilder()
                .enable5gNr(enableSa = true, enableNsa = true, nrAvailability = 3)
                .enableVoNr(enabled = true)
                .enableTurboAggregation()
                .enableVoLte()
                .enableVoWifi()
                .enableSignalEnhancements()
                .setGeneralOverrides()
                .putBoolean("carrier_name_override_bool", true)
                .putString("carrier_name_string", "T-Mobile")
                .build()
        }
    )

    val ATT_US = CarrierPreset(
        id = "att_us",
        name = "AT&T",
        country = "United States",
        targetMcc = "310",
        targetMnc = "410",
        description = "5G+ (n77) + Advanced VoLTE HD Voice + Wi-Fi Calling + 4G+ Turbo Aggregation.",
        is5gSaSupported = true,
        isVoNrSupported = true,
        isVoLteSupported = true,
        isVoWifiSupported = true,
        payloadBuilder = { _ ->
            CarrierConfigPayloadBuilder()
                .enable5gNr(enableSa = true, enableNsa = true, nrAvailability = 3)
                .enableVoNr(enabled = true)
                .enableTurboAggregation()
                .enableVoLte()
                .enableVoWifi()
                .enableSignalEnhancements()
                .setGeneralOverrides()
                .putBoolean("carrier_name_override_bool", true)
                .putString("carrier_name_string", "AT&T")
                .build()
        }
    )

    val EE_UK = CarrierPreset(
        id = "ee_uk",
        name = "EE UK",
        country = "United Kingdom",
        targetMcc = "234",
        targetMnc = "30",
        description = "5G SA/NSA + 4G Calling + Wi-Fi Calling + Carrier Aggregation unlock.",
        is5gSaSupported = true,
        isVoNrSupported = true,
        isVoLteSupported = true,
        isVoWifiSupported = true,
        payloadBuilder = { _ ->
            CarrierConfigPayloadBuilder()
                .enable5gNr(enableSa = true, enableNsa = true, nrAvailability = 3)
                .enableVoNr(enabled = true)
                .enableTurboAggregation()
                .enableVoLte()
                .enableVoWifi()
                .enableSignalEnhancements()
                .setGeneralOverrides()
                .build()
        }
    )

    val JIO_5G_IN = CarrierPreset(
        id = "jio_in",
        name = "Jio True 5G",
        country = "India",
        targetMcc = "405",
        targetMnc = "854",
        description = "Pure Standalone 5G (n28 + n78) + VoNR 5G Voice + 4G VoLTE + Cross-SIM Calling.",
        is5gSaSupported = true,
        isVoNrSupported = true,
        isVoLteSupported = true,
        isVoWifiSupported = true,
        payloadBuilder = { _ ->
            CarrierConfigPayloadBuilder()
                .enable5gNr(enableSa = true, enableNsa = false, nrAvailability = 2) // Force Pure 5G SA
                .enableVoNr(enabled = true)
                .enableTurboAggregation()
                .enableVoLte()
                .enableVoWifi()
                .enableSignalEnhancements()
                .setGeneralOverrides()
                .putBoolean("carrier_name_override_bool", true)
                .putString("carrier_name_string", "Jio True 5G")
                .build()
        }
    )

    val AIRTEL_5G_IN = CarrierPreset(
        id = "airtel_in",
        name = "Airtel 5G Plus",
        country = "India",
        targetMcc = "404",
        targetMnc = "45",
        description = "5G Plus NSA/SA + VoLTE + VoWiFi + Ultra-responsive data pipe.",
        is5gSaSupported = true,
        isVoNrSupported = true,
        isVoLteSupported = true,
        isVoWifiSupported = true,
        payloadBuilder = { _ ->
            CarrierConfigPayloadBuilder()
                .enable5gNr(enableSa = true, enableNsa = true, nrAvailability = 3)
                .enableVoNr(enabled = true)
                .enableTurboAggregation()
                .enableVoLte()
                .enableVoWifi()
                .enableSignalEnhancements()
                .setGeneralOverrides()
                .putBoolean("carrier_name_override_bool", true)
                .putString("carrier_name_string", "Airtel 5G Plus")
                .build()
        }
    )

    /**
     * Global Ultra-Unlock (Universal wildcard matching any SIM anywhere in the world)
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
        ZONG_PAKISTAN,
        JAZZ_PAKISTAN,
        TELENOR_PAKISTAN,
        UFONE_PAKISTAN,
        VERIZON_US,
        TMOBILE_US,
        ATT_US,
        EE_UK,
        JIO_5G_IN,
        AIRTEL_5G_IN
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
