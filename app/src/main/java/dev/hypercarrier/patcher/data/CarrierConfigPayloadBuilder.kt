package dev.hypercarrier.patcher.data

import android.os.PersistableBundle

/**
 * Android 17 / Pixel 9 CarrierConfig Payload Builder with Extreme Turbo Carrier Aggregation
 * and exhaustive system settings toggle unlock matrix.
 */
class CarrierConfigPayloadBuilder {

    companion object {
        // --- 5G NR SA/NSA & VoNR Keys ---
        const val KEY_CARRIER_NR_AVAILABILITIES_INT_ARRAY = "carrier_nr_availabilities_int_array"
        const val KEY_CARRIER_NR_AVAILABILITY_INT = "carrier_nr_availability_int"
        const val KEY_VONR_ENABLED_BOOL = "vonr_enabled_bool"
        const val KEY_VONR_SETTING_VISIBILITY_BOOL = "vonr_setting_visibility_bool"
        const val KEY_NR_TIMERS_RESET_ON_VOICE_QOS_BOOL = "nr_timers_reset_on_voice_qos_bool"
        const val KEY_5G_ICON_CONFIGURATION_STRING = "5g_icon_configuration_string"
        const val KEY_5G_ICON_DISPLAY_GRACE_PERIOD_SEC_INT = "5g_icon_display_grace_period_sec_int"
        const val KEY_5G_ICON_DISPLAY_SECONDARY_GRACE_PERIOD_SEC_INT = "5g_icon_display_secondary_grace_period_sec_int"
        const val KEY_UNMETERED_NR_NSA_BOOL = "unmetered_nr_nsa_bool"
        const val KEY_UNMETERED_NR_SA_BOOL = "unmetered_nr_sa_bool"
        const val KEY_BANDWIDTH_STRING_ARRAY = "bandwidth_string_array"

        // --- System Settings Visibility & Menu Unlocks ---
        const val KEY_HIDE_PREFERRED_NETWORK_TYPE_BOOL = "hide_preferred_network_type_bool"
        const val KEY_HIDE_CARRIER_NETWORK_SETTINGS_BOOL = "hide_carrier_network_settings_bool"
        const val KEY_HIDE_ENHANCED_4G_LTE_BOOL = "hide_enhanced_4g_lte_bool"
        const val KEY_HIDE_ENABLE_2G_BOOL = "hide_enable_2g_bool"
        const val KEY_WORLD_MODE_ENABLED_BOOL = "world_mode_enabled_bool"
        const val KEY_CARRIER_SETTINGS_ENABLE_BOOL = "carrier_settings_enable_bool"
        const val KEY_SHOW_4G_FOR_LTE_DATA_ICON_BOOL = "show_4g_for_lte_data_icon_bool"
        const val KEY_SUPPORT_TDSCDMA_BOOL = "support_tdscdma_bool"

        // --- Carrier Aggregation & Zero-Delay Data Keys ---
        const val KEY_CARRIER_DATA_CALL_APN_DELAY_DEFAULT_LONG = "carrier_data_call_apn_delay_default_long"
        const val KEY_CARRIER_DATA_CALL_APN_DELAY_SUBSEQUENT_LONG = "carrier_data_call_apn_delay_subsequent_long"
        const val KEY_DATA_LIMIT_THRESHOLD_BOOL = "data_limit_threshold_bool"
        const val KEY_DATA_SWITCH_VALIDATION_MIN_GAP_LONG = "data_switch_validation_min_gap_long"
        const val KEY_SUPPORT_TETHERING_MAC_RANDOMIZATION_BOOL = "support_tethering_mac_randomization_bool"

        // --- VoLTE & IMS Core Keys ---
        const val KEY_CARRIER_VOLTE_AVAILABLE_BOOL = "carrier_volte_available_bool"
        const val KEY_CARRIER_VOLTE_PROVISIONED_BOOL = "carrier_volte_provisioned_bool"
        const val KEY_CARRIER_VOLTE_PROVISIONING_REQUIRED_BOOL = "carrier_volte_provisioning_required_bool"
        const val KEY_CARRIER_VOLTE_TTY_SUPPORTED_BOOL = "carrier_volte_tty_supported_bool"
        const val KEY_EDITABLE_ENHANCED_4G_LTE_BOOL = "editable_enhanced_4g_lte_bool"
        const val KEY_ENHANCED_4G_LTE_ON_BY_DEFAULT_BOOL = "enhanced_4g_lte_on_by_default_bool"
        const val KEY_CARRIER_CONFIG_APPLIED_BOOL = "carrier_config_applied_bool"
        const val KEY_SHOW_IMS_REGISTRATION_STATUS_BOOL = "show_ims_registration_status_bool"
        const val KEY_CARRIER_SUPPORTS_CALLER_ID_VERTICAL_SERVICE_CODES_BOOL = "carrier_supports_caller_id_vertical_service_codes_bool"
        const val KEY_CARRIER_ALLOW_TURNOFF_IMS_BOOL = "carrier_allow_turnoff_ims_bool"
        const val KEY_CARRIER_VOLTE_OVERRIDE_WFC_PROVISIONING_BOOL = "carrier_volte_override_wfc_provisioning_bool"
        const val KEY_CARRIER_SUPPORTS_SS_OVER_UT_BOOL = "carrier_supports_ss_over_ut_bool"
        const val KEY_CARRIER_UT_PROVISIONED_BOOL = "carrier_ut_provisioned_bool"
        const val KEY_CARRIER_UT_PROVISIONING_REQUIRED_BOOL = "carrier_ut_provisioning_required_bool"
        const val KEY_CARRIER_PROMOTE_WFC_ON_CALL_FAIL_BOOL = "carrier_promote_wfc_on_call_fail_bool"
        const val KEY_CARRIER_IMS_GBA_REQUIRED_BOOL = "carrier_ims_gba_required_bool"

        // --- VoWiFi / WFC & Cross-SIM Keys ---
        const val KEY_CARRIER_WFC_IMS_AVAILABLE_BOOL = "carrier_wfc_ims_available_bool"
        const val KEY_CARRIER_DEFAULT_WFC_IMS_ENABLED_BOOL = "carrier_default_wfc_ims_enabled_bool"
        const val KEY_CARRIER_DEFAULT_WFC_IMS_ROAMING_ENABLED_BOOL = "carrier_default_wfc_ims_roaming_enabled_bool"
        const val KEY_EDITABLE_WFC_MODE_BOOL = "editable_wfc_mode_bool"
        const val KEY_EDITABLE_WFC_ROAMING_MODE_BOOL = "editable_wfc_roaming_mode_bool"
        const val KEY_CARRIER_DEFAULT_WFC_IMS_MODE_INT = "carrier_default_wfc_ims_mode_int"
        const val KEY_CARRIER_DEFAULT_WFC_IMS_ROAMING_MODE_INT = "carrier_default_wfc_ims_roaming_mode_int"
        const val KEY_CARRIER_CROSS_SIM_IMS_AVAILABLE_BOOL = "carrier_cross_sim_ims_available_bool"
        const val KEY_EMERGENCY_NOTIFICATION_NAME_STRING = "emergency_notification_name_string"
        const val KEY_WFC_EMERGENCY_ADDRESS_CARRIER_APP_STRING = "wfc_emergency_address_carrier_app_string"
        const val KEY_WFC_DATA_SPN_FORMAT_IDX_INT = "wfc_data_spn_format_idx_int"
        const val KEY_CARRIER_WFC_SUPPORTS_WIFI_ONLY_BOOL = "carrier_wfc_supports_wifi_only_bool"

        // --- Network Thresholds & Signal Enhancement Keys ---
        const val KEY_5G_NR_SSRSRP_THRESHOLDS_INT_ARRAY = "5g_nr_ssrsrp_thresholds_int_array"
        const val KEY_LTE_RSRP_THRESHOLDS_INT_ARRAY = "lte_rsrp_thresholds_int_array"
        const val KEY_5G_NR_SSRSRQ_THRESHOLDS_INT_ARRAY = "5g_nr_ssrsrq_thresholds_int_array"
        const val KEY_LTE_RSRQ_THRESHOLDS_INT_ARRAY = "lte_rsrq_thresholds_int_array"
        const val KEY_5G_NR_SSSINR_THRESHOLDS_INT_ARRAY = "5g_nr_sssinr_thresholds_int_array"
        const val KEY_LTE_RSSNR_THRESHOLDS_INT_ARRAY = "lte_rssnr_thresholds_int_array"
        const val KEY_USE_RSRP_FOR_LTE_SIGNAL_BAR_BOOL = "use_rsrp_for_lte_signal_bar_bool"
        const val KEY_PARAMETERS_USED_FOR_LTE_SIGNAL_BAR_INT = "parameters_used_for_lte_signal_bar_int"

        // Default optimized threshold arrays for maximum sensitivity
        val DEFAULT_5G_NR_RSRP_THRESHOLDS = intArrayOf(-140, -115, -105, -95)
        val DEFAULT_LTE_RSRP_THRESHOLDS = intArrayOf(-140, -115, -105, -95)
        val DEFAULT_5G_NR_RSRQ_THRESHOLDS = intArrayOf(-43, -20, -16, -10)
        val DEFAULT_LTE_RSRQ_THRESHOLDS = intArrayOf(-34, -20, -15, -10)
        val DEFAULT_5G_NR_SINR_THRESHOLDS = intArrayOf(-23, -5, 5, 20)
        val DEFAULT_LTE_RSSNR_THRESHOLDS = intArrayOf(-20, 0, 10, 30)

        // 5G Icon configuration string for instant icon display
        const val DEFAULT_5G_ICON_CONFIG =
            "connected_mmwave:5G_PLUS,connected:5G,not_restricted_rrc_idle:5G,not_restricted_rrc_con:5G"

        /**
         * Creates a comprehensive, fully unlocked CarrierConfig bundle with Turbo Aggregation
         * and all Android Settings toggles unhidden.
         */
        fun buildUniversalUltraUnlockBundle(): PersistableBundle {
            return CarrierConfigPayloadBuilder()
                .enable5gNr(
                    enableSa = true,
                    enableNsa = true,
                    nrAvailability = 3,
                    iconConfig = DEFAULT_5G_ICON_CONFIG,
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
                    defaultMode = 1, // 1 = Wi-Fi Preferred, 2 = Cellular Preferred
                    crossSimAvailable = true
                )
                .enableSignalEnhancements(
                    nrRsrpThresholds = DEFAULT_5G_NR_RSRP_THRESHOLDS,
                    lteRsrpThresholds = DEFAULT_LTE_RSRP_THRESHOLDS,
                    useRsrpForLteBars = true
                )
                .setGeneralOverrides()
                .build()
        }
    }

    private val bundle = PersistableBundle()

    /**
     * Configures 5G NR NSA & SA parameters and unlocks 5G in settings.
     */
    fun enable5gNr(
        enableSa: Boolean = true,
        enableNsa: Boolean = true,
        nrAvailability: Int = 3,
        iconConfig: String = DEFAULT_5G_ICON_CONFIG,
        gracePeriodSec: Int = 0
    ): CarrierConfigPayloadBuilder {
        val availabilities = mutableListOf<Int>()
        if (enableNsa) availabilities.add(1) // CARRIER_NR_AVAILABILITY_NSA
        if (enableSa) availabilities.add(2)  // CARRIER_NR_AVAILABILITY_SA

        bundle.putIntArray(KEY_CARRIER_NR_AVAILABILITIES_INT_ARRAY, availabilities.toIntArray())
        bundle.putInt(KEY_CARRIER_NR_AVAILABILITY_INT, nrAvailability)
        bundle.putString(KEY_5G_ICON_CONFIGURATION_STRING, iconConfig)
        bundle.putInt(KEY_5G_ICON_DISPLAY_GRACE_PERIOD_SEC_INT, gracePeriodSec)
        bundle.putInt(KEY_5G_ICON_DISPLAY_SECONDARY_GRACE_PERIOD_SEC_INT, gracePeriodSec)
        bundle.putBoolean(KEY_UNMETERED_NR_NSA_BOOL, true)
        bundle.putBoolean(KEY_UNMETERED_NR_SA_BOOL, true)
        bundle.putBoolean("4g_only_bool", false)
        return this
    }

    /**
     * Enables Extreme Carrier Aggregation (LTE-CA + NR-DC) with 0ms setup latency and 4Gbps bandwidth caps.
     */
    fun enableTurboAggregation(): CarrierConfigPayloadBuilder {
        bundle.putStringArray(
            KEY_BANDWIDTH_STRING_ARRAY,
            arrayOf("5G:4000000,200000", "LTE:1000000,150000")
        )
        bundle.putLong(KEY_CARRIER_DATA_CALL_APN_DELAY_DEFAULT_LONG, 0L)
        bundle.putLong(KEY_CARRIER_DATA_CALL_APN_DELAY_SUBSEQUENT_LONG, 0L)
        bundle.putBoolean(KEY_DATA_LIMIT_THRESHOLD_BOOL, false)
        bundle.putLong(KEY_DATA_SWITCH_VALIDATION_MIN_GAP_LONG, 0L)
        bundle.putBoolean(KEY_SUPPORT_TETHERING_MAC_RANDOMIZATION_BOOL, true)
        return this
    }

    /**
     * Configures Voice over New Radio (VoNR) and exposes VoNR toggle in Android Settings.
     */
    fun enableVoNr(
        enabled: Boolean = true,
        settingVisibility: Boolean = true,
        resetTimersOnVoiceQos: Boolean = true
    ): CarrierConfigPayloadBuilder {
        bundle.putBoolean(KEY_VONR_ENABLED_BOOL, enabled)
        bundle.putBoolean(KEY_VONR_SETTING_VISIBILITY_BOOL, settingVisibility)
        bundle.putBoolean(KEY_NR_TIMERS_RESET_ON_VOICE_QOS_BOOL, resetTimersOnVoiceQos)
        return this
    }

    /**
     * Configures Voice over LTE (VoLTE) and IMS Engine, and unhides toggles in Android Settings.
     */
    fun enableVoLte(
        available: Boolean = true,
        editable: Boolean = true,
        onByDefault: Boolean = true,
        supportsCallerId: Boolean = true,
        allowTurnOff: Boolean = true
    ): CarrierConfigPayloadBuilder {
        bundle.putBoolean(KEY_CARRIER_VOLTE_AVAILABLE_BOOL, available)
        bundle.putBoolean(KEY_EDITABLE_ENHANCED_4G_LTE_BOOL, editable)
        bundle.putBoolean(KEY_HIDE_ENHANCED_4G_LTE_BOOL, false)
        bundle.putBoolean(KEY_ENHANCED_4G_LTE_ON_BY_DEFAULT_BOOL, onByDefault)
        bundle.putBoolean(KEY_CARRIER_VOLTE_TTY_SUPPORTED_BOOL, true)
        bundle.putBoolean(KEY_CARRIER_CONFIG_APPLIED_BOOL, true)
        bundle.putBoolean(KEY_SHOW_IMS_REGISTRATION_STATUS_BOOL, true)
        bundle.putBoolean(KEY_CARRIER_SUPPORTS_CALLER_ID_VERTICAL_SERVICE_CODES_BOOL, supportsCallerId)
        bundle.putBoolean(KEY_CARRIER_ALLOW_TURNOFF_IMS_BOOL, allowTurnOff)

        // Force provisioning and eliminate provisioning barriers
        bundle.putBoolean(KEY_CARRIER_VOLTE_OVERRIDE_WFC_PROVISIONING_BOOL, true)
        bundle.putBoolean(KEY_CARRIER_VOLTE_PROVISIONED_BOOL, true)
        bundle.putBoolean(KEY_CARRIER_VOLTE_PROVISIONING_REQUIRED_BOOL, false)
        bundle.putBoolean(KEY_CARRIER_SUPPORTS_SS_OVER_UT_BOOL, true)
        bundle.putBoolean(KEY_CARRIER_UT_PROVISIONED_BOOL, true)
        bundle.putBoolean(KEY_CARRIER_UT_PROVISIONING_REQUIRED_BOOL, false)
        bundle.putBoolean(KEY_CARRIER_IMS_GBA_REQUIRED_BOOL, false)
        bundle.putBoolean(KEY_CARRIER_PROMOTE_WFC_ON_CALL_FAIL_BOOL, true)
        return this
    }

    /**
     * Configures Wi-Fi Calling (VoWiFi / WFC) and Cross-SIM Calling, unhiding toggles in Settings.
     */
    fun enableVoWifi(
        available: Boolean = true,
        defaultEnabled: Boolean = true,
        roamingEnabled: Boolean = true,
        editable: Boolean = true,
        defaultMode: Int = 1, // 1 = Wi-Fi Preferred, 2 = Cellular Preferred
        defaultRoamingMode: Int = 1,
        crossSimAvailable: Boolean = true
    ): CarrierConfigPayloadBuilder {
        bundle.putBoolean(KEY_CARRIER_WFC_IMS_AVAILABLE_BOOL, available)
        bundle.putBoolean(KEY_CARRIER_DEFAULT_WFC_IMS_ENABLED_BOOL, defaultEnabled)
        bundle.putBoolean(KEY_CARRIER_DEFAULT_WFC_IMS_ROAMING_ENABLED_BOOL, roamingEnabled)
        bundle.putBoolean(KEY_EDITABLE_WFC_MODE_BOOL, editable)
        bundle.putBoolean(KEY_EDITABLE_WFC_ROAMING_MODE_BOOL, editable)
        bundle.putInt(KEY_CARRIER_DEFAULT_WFC_IMS_MODE_INT, defaultMode)
        bundle.putInt(KEY_CARRIER_DEFAULT_WFC_IMS_ROAMING_MODE_INT, defaultRoamingMode)
        bundle.putBoolean(KEY_CARRIER_CROSS_SIM_IMS_AVAILABLE_BOOL, crossSimAvailable)
        bundle.putBoolean(KEY_CARRIER_WFC_SUPPORTS_WIFI_ONLY_BOOL, true)
        bundle.putString(KEY_EMERGENCY_NOTIFICATION_NAME_STRING, "")
        bundle.putString(KEY_WFC_EMERGENCY_ADDRESS_CARRIER_APP_STRING, "")
        bundle.putInt(KEY_WFC_DATA_SPN_FORMAT_IDX_INT, 0)
        bundle.putInt("wfc_spn_format_idx_int", 0)
        bundle.putInt("wfc_flight_mode_spn_format_idx_int", 0)
        bundle.putBoolean("use_wfc_home_network_mode_in_roaming_network_bool", false)
        bundle.putBoolean("use_otasp_for_provisioning_bool", false)
        bundle.putInt("call_waiting_service_class_int", 1)
        return this
    }

    /**
     * Configures RF sensitivity thresholds for 5G and LTE.
     */
    fun enableSignalEnhancements(
        nrRsrpThresholds: IntArray = DEFAULT_5G_NR_RSRP_THRESHOLDS,
        lteRsrpThresholds: IntArray = DEFAULT_LTE_RSRP_THRESHOLDS,
        nrRsrqThresholds: IntArray = DEFAULT_5G_NR_RSRQ_THRESHOLDS,
        lteRsrqThresholds: IntArray = DEFAULT_LTE_RSRQ_THRESHOLDS,
        nrSinrThresholds: IntArray = DEFAULT_5G_NR_SINR_THRESHOLDS,
        lteRssnrThresholds: IntArray = DEFAULT_LTE_RSSNR_THRESHOLDS,
        useRsrpForLteBars: Boolean = true
    ): CarrierConfigPayloadBuilder {
        bundle.putIntArray(KEY_5G_NR_SSRSRP_THRESHOLDS_INT_ARRAY, nrRsrpThresholds)
        bundle.putIntArray(KEY_LTE_RSRP_THRESHOLDS_INT_ARRAY, lteRsrpThresholds)
        bundle.putIntArray(KEY_5G_NR_SSRSRQ_THRESHOLDS_INT_ARRAY, nrRsrqThresholds)
        bundle.putIntArray(KEY_LTE_RSRQ_THRESHOLDS_INT_ARRAY, lteRsrqThresholds)
        bundle.putIntArray(KEY_5G_NR_SSSINR_THRESHOLDS_INT_ARRAY, nrSinrThresholds)
        bundle.putIntArray(KEY_LTE_RSSNR_THRESHOLDS_INT_ARRAY, lteRssnrThresholds)
        bundle.putBoolean(KEY_USE_RSRP_FOR_LTE_SIGNAL_BAR_BOOL, useRsrpForLteBars)
        bundle.putInt(KEY_PARAMETERS_USED_FOR_LTE_SIGNAL_BAR_INT, 1) // 1 = RSRP
        return this
    }

    /**
     * Enables world mode, unlocks network selection menus, unhides Preferred Network Type, and enables 2G toggle.
     */
    fun setGeneralOverrides(): CarrierConfigPayloadBuilder {
        bundle.putBoolean(KEY_WORLD_MODE_ENABLED_BOOL, true)
        bundle.putBoolean(KEY_HIDE_PREFERRED_NETWORK_TYPE_BOOL, false)
        bundle.putBoolean(KEY_HIDE_CARRIER_NETWORK_SETTINGS_BOOL, false)
        bundle.putBoolean(KEY_HIDE_ENABLE_2G_BOOL, false)
        bundle.putBoolean(KEY_CARRIER_SETTINGS_ENABLE_BOOL, true)
        bundle.putBoolean(KEY_SHOW_4G_FOR_LTE_DATA_ICON_BOOL, false) // Show 4G+ / LTE-A
        bundle.putBoolean(KEY_SUPPORT_TDSCDMA_BOOL, false)
        return this
    }

    /**
     * Adds custom raw Boolean override.
     */
    fun putBoolean(key: String, value: Boolean): CarrierConfigPayloadBuilder {
        bundle.putBoolean(key, value)
        return this
    }

    /**
     * Adds custom raw Int override.
     */
    fun putInt(key: String, value: Int): CarrierConfigPayloadBuilder {
        bundle.putInt(key, value)
        return this
    }

    /**
     * Adds custom raw Long override.
     */
    fun putLong(key: String, value: Long): CarrierConfigPayloadBuilder {
        bundle.putLong(key, value)
        return this
    }

    /**
     * Adds custom raw String override.
     */
    fun putString(key: String, value: String): CarrierConfigPayloadBuilder {
        bundle.putString(key, value)
        return this
    }

    /**
     * Adds custom raw IntArray override.
     */
    fun putIntArray(key: String, value: IntArray): CarrierConfigPayloadBuilder {
        bundle.putIntArray(key, value)
        return this
    }

    /**
     * Adds custom raw StringArray override.
     */
    fun putStringArray(key: String, value: Array<String>): CarrierConfigPayloadBuilder {
        bundle.putStringArray(key, value)
        return this
    }

    /**
     * Builds and returns the final PersistableBundle ready for persistent injection.
     */
    fun build(): PersistableBundle {
        return PersistableBundle(bundle)
    }
}
