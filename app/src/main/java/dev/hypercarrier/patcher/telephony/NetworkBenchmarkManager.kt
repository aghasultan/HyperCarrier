package dev.hypercarrier.patcher.telephony

import android.util.Log
import dev.hypercarrier.patcher.data.BenchmarkResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import java.net.Socket
import kotlin.math.abs

/**
 * Real-time Anycast latency, jitter, and packet loss benchmark engine.
 */
class NetworkBenchmarkManager {

    companion object {
        private const val TAG = "NetworkBenchmark"
        private const val DEFAULT_HOST = "1.1.1.1" // Cloudflare Anycast DNS
        private const val DEFAULT_PORT = 53
        private const val PING_COUNT = 8
        private const val TIMEOUT_MS = 1500
    }

    private val _benchmarkState = MutableStateFlow(BenchmarkResult())
    val benchmarkState: StateFlow<BenchmarkResult> = _benchmarkState.asStateFlow()

    /**
     * Executes a fast, low-overhead TCP socket benchmark against Tier-1 Anycast servers.
     */
    suspend fun runBenchmark(host: String = DEFAULT_HOST, serverLabel: String = "Cloudflare Anycast (1.1.1.1)") = withContext(Dispatchers.IO) {
        _benchmarkState.value = BenchmarkResult(
            serverName = serverLabel,
            isRunning = true,
            timestamp = System.currentTimeMillis()
        )

        val rtts = mutableListOf<Double>()
        var droppedPackets = 0

        for (i in 1..PING_COUNT) {
            val startTime = System.nanoTime()
            var socket: Socket? = null
            try {
                socket = Socket()
                socket.connect(InetSocketAddress(host, DEFAULT_PORT), TIMEOUT_MS)
                val durationMs = (System.nanoTime() - startTime) / 1_000_000.0
                rtts.add(durationMs)
                Log.d(TAG, "Ping #$i to $host: ${String.format("%.2f", durationMs)} ms")
            } catch (t: Throwable) {
                droppedPackets++
                Log.w(TAG, "Ping #$i dropped: ${t.message}")
            } finally {
                try {
                    socket?.close()
                } catch (_: Throwable) {}
            }

            // Small delay between probes
            kotlinx.coroutines.delay(100)
        }

        if (rtts.isEmpty()) {
            _benchmarkState.value = BenchmarkResult(
                serverName = serverLabel,
                minLatencyMs = 0.0,
                avgLatencyMs = 0.0,
                maxLatencyMs = 0.0,
                jitterMs = 0.0,
                packetLossPercent = 100,
                isRunning = false,
                timestamp = System.currentTimeMillis()
            )
            return@withContext
        }

        val minMs = rtts.minOrNull() ?: 0.0
        val maxMs = rtts.maxOrNull() ?: 0.0
        val avgMs = rtts.average()

        // Calculate jitter (RFC 1889 variance)
        var totalJitter = 0.0
        for (j in 0 until rtts.size - 1) {
            totalJitter += abs(rtts[j + 1] - rtts[j])
        }
        val jitterMs = if (rtts.size > 1) totalJitter / (rtts.size - 1) else 0.0
        val packetLoss = ((droppedPackets.toDouble() / PING_COUNT) * 100).toInt()

        _benchmarkState.value = BenchmarkResult(
            serverName = serverLabel,
            minLatencyMs = minMs,
            avgLatencyMs = avgMs,
            maxLatencyMs = maxMs,
            jitterMs = jitterMs,
            packetLossPercent = packetLoss,
            isRunning = false,
            timestamp = System.currentTimeMillis()
        )
    }
}
