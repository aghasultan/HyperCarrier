package dev.hypercarrier.patcher.data

import android.os.PersistableBundle

/**
 * Representation of a SIM slot and active subscription.
 */
data class SubscriptionData(
    val subscriptionId: Int,
    val slotIndex: Int,
    val displayName: String,
    val carrierName: String,
    val number: String,
    val countryIso: String,
    val mcc: String,
    val mnc: String,
    val isEmbedded: Boolean = false, // eSIM indicator
    val isOpportunistic: Boolean = false,
    val isDataActive: Boolean = false
) {
    val plmn: String
        get() = "${mcc.padStart(3, '0')}-${mnc.padStart(2, '0')}"
}

/**
 * Detailed real-time RF signal metrics.
 */
data class SignalMetrics(
    val networkType: String = "Unknown", // 5G SA, 5G NSA, LTE+, LTE, 3G, 2G
    val rsrpDbm: Int = -999,            // Reference Signal Received Power (dBm)
    val rsrqDb: Int = -999,             // Reference Signal Received Quality (dB)
    val sinrDb: Int = -999,             // Signal to Interference plus Noise Ratio (dB)
    val rssi: Int = -999,               // Received Signal Strength Indicator
    val cqi: Int = -1,                  // Channel Quality Indicator
    val asu: Int = 0,                   // Arbitrary Strength Unit
    val level: Int = 0,                 // 0..4 signal bars
    val band: String = "Unknown",
    val cellId: Long = -1L,
    val pci: Int = -1,
    val is5gConnected: Boolean = false,
    val isCarrierAggregating: Boolean = false
)

/**
 * Individual Component Carrier (PCELL / SCELL) in Carrier Aggregation (LTE-A / EN-DC).
 */
data class CellComponentInfo(
    val role: String,                   // "PCELL (Primary)" or "SCELL 1 (Secondary)"
    val radioType: String,              // "LTE" or "5G NR"
    val bandName: String,               // "B3 (1800 MHz)" or "n78 (3500 MHz)"
    val bandwidthMhz: Int,              // Bandwidth e.g. 20 MHz
    val earfcn: Int = -1,               // Radio Frequency Channel Number
    val pci: Int = -1,                  // Physical Cell ID
    val rsrpDbm: Int = -999,
    val isServing: Boolean = true
)

/**
 * Aggregated multi-carrier telemetry information.
 */
data class CarrierAggregationInfo(
    val primaryCell: CellComponentInfo? = null,
    val secondaryCells: List<CellComponentInfo> = emptyList(),
    val totalAggregatedBandwidthMhz: Int = 0,
    val isAggregating: Boolean = false,
    val mimoLayers: String = "4x4 MIMO",
    val modulation: String = "256-QAM"
)

/**
 * Real-time IMS feature capabilities.
 */
data class ImsCapabilityState(
    val isImsRegistered: Boolean = false,
    val isVoLteAvailable: Boolean = false,
    val isVoWifiAvailable: Boolean = false,
    val isVoNrAvailable: Boolean = false,
    val isUtAvailable: Boolean = false,
    val isVideoTelephonyAvailable: Boolean = false,
    val transportType: String = "None" // LTE, Wi-Fi, NR
)

/**
 * Network latency, jitter, and packet loss benchmark measurement.
 */
data class BenchmarkResult(
    val serverName: String = "Cloudflare Anycast (1.1.1.1)",
    val minLatencyMs: Double = 0.0,
    val avgLatencyMs: Double = 0.0,
    val maxLatencyMs: Double = 0.0,
    val jitterMs: Double = 0.0,
    val packetLossPercent: Int = 0,
    val isRunning: Boolean = false,
    val timestamp: Long = 0L
)

/**
 * High-level Network Mode Option.
 */
data class NetworkModeOption(
    val id: Int,
    val title: String,
    val subtitle: String,
    val description: String
)

/**
 * Carrier preset definition for one-tap deployment.
 */
data class CarrierPreset(
    val id: String,
    val name: String,
    val country: String,
    val targetMcc: String,
    val targetMnc: String,
    val description: String,
    val is5gSaSupported: Boolean = true,
    val isVoNrSupported: Boolean = true,
    val isVoLteSupported: Boolean = true,
    val isVoWifiSupported: Boolean = true,
    val payloadBuilder: (subId: Int) -> PersistableBundle
)

/**
 * Carrier configuration key-value item for inspection and diffing.
 */
data class CarrierConfigItem(
    val key: String,
    val value: String,
    val type: String,
    val isOverridden: Boolean = false,
    val defaultValue: String? = null
)

/**
 * Result of a persistent injection operation.
 */
sealed class InjectionResult {
    data class Success(val message: String, val appliedKeysCount: Int) : InjectionResult()
    data class Error(val message: String, val throwable: Throwable? = null) : InjectionResult()
    object Idle : InjectionResult()
    object InProgress : InjectionResult()
}
