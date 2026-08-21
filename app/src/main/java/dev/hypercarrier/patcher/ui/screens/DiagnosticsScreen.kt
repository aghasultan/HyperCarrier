package dev.hypercarrier.patcher.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.CellTower
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.hypercarrier.patcher.data.BenchmarkResult
import dev.hypercarrier.patcher.data.CarrierAggregationInfo
import dev.hypercarrier.patcher.data.CarrierConfigItem
import dev.hypercarrier.patcher.data.CellComponentInfo
import dev.hypercarrier.patcher.ui.MainViewModel
import dev.hypercarrier.patcher.ui.components.ImsStatusCard
import dev.hypercarrier.patcher.ui.components.SignalStrengthCard
import dev.hypercarrier.patcher.ui.theme.SignalExcellent
import dev.hypercarrier.patcher.ui.theme.SignalModerate
import dev.hypercarrier.patcher.ui.theme.SignalPoor

@Composable
fun DiagnosticsScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val selectedSub by viewModel.selectedSubscription.collectAsState()
    val signalMetrics by viewModel.signalMetrics.collectAsState()
    val imsCapabilities by viewModel.imsCapabilities.collectAsState()
    val carrierAggregation by viewModel.carrierAggregation.collectAsState()
    val benchmarkResult by viewModel.benchmarkResult.collectAsState()
    val configItems by viewModel.activeConfigItems.collectAsState()
    val isLoadingConfig by viewModel.isLoadingConfig.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("All") }

    val filteredItems = remember(configItems, searchQuery, selectedFilter) {
        configItems.filter { item ->
            val matchesSearch = searchQuery.isBlank() ||
                    item.key.contains(searchQuery, ignoreCase = true) ||
                    item.value.contains(searchQuery, ignoreCase = true)

            val matchesFilter = when (selectedFilter) {
                "Overridden" -> item.isOverridden
                "5G NR" -> item.key.contains("nr", ignoreCase = true) || item.key.contains("5g", ignoreCase = true)
                "VoLTE" -> item.key.contains("volte", ignoreCase = true) || item.key.contains("enhanced_4g", ignoreCase = true)
                "VoWiFi" -> item.key.contains("wfc", ignoreCase = true)
                "Thresholds" -> item.key.contains("thresholds", ignoreCase = true) || item.key.contains("signal", ignoreCase = true)
                "Turbo CA" -> item.key.contains("bandwidth", ignoreCase = true) || item.key.contains("delay", ignoreCase = true)
                else -> true
            }

            matchesSearch && matchesFilter
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 36.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Real-time RF Signal Gauge
        item {
            SignalStrengthCard(metrics = signalMetrics)
        }

        // Carrier Aggregation (LTE-CA + NR-DC) RF Lab Card
        item {
            CarrierAggregationLabCard(caInfo = carrierAggregation)
        }

        // 1-Tap Ultra Latency & Jitter Benchmark Card
        item {
            LatencyBenchmarkCard(
                result = benchmarkResult,
                onRunCloudflare = { viewModel.runBenchmark("1.1.1.1", "Cloudflare Anycast (1.1.1.1)") },
                onRunGoogle = { viewModel.runBenchmark("8.8.8.8", "Google Anycast (8.8.8.8)") }
            )
        }

        // Live IMS Capabilities Matrix & 1-Tap Control Card
        item {
            ImsStatusCard(
                imsCapabilities = imsCapabilities,
                onToggleVoLte = { viewModel.toggleVoLte(it) },
                onToggleVoWifi = { viewModel.toggleVoWifi(it) },
                onToggleVoNr = { viewModel.toggleVoNr(it) },
                onToggleViLte = { viewModel.toggleViLte(it) },
                onSetVoWifiMode = { viewModel.setVoWifiMode(it) },
                onForceReRegister = { viewModel.forceReRegisterIms() }
            )
        }

        // Detailed RF Parameters Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Radio Physical Layer (PHY) Telemetry",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        DiagnosticParamRow(label = "Physical Cell ID (PCI)", value = if (signalMetrics.pci != -1) "${signalMetrics.pci}" else "N/A")
                        DiagnosticParamRow(label = "Cell ID (NCI/CI)", value = if (signalMetrics.cellId != -1L) "${signalMetrics.cellId}" else "N/A")
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        DiagnosticParamRow(label = "Operating Band", value = signalMetrics.band)
                        DiagnosticParamRow(label = "CQI Index", value = if (signalMetrics.cqi != -1) "${signalMetrics.cqi}" else "N/A")
                    }
                }
            }
        }

        // Active CarrierConfig Inspector Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "CarrierConfig Snapshot Inspector",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "${configItems.size} active system parameters loaded",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(onClick = { selectedSub?.let { viewModel.loadActiveCarrierConfig(it.subscriptionId) } }) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Reload",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        // Search bar
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Filter config keys (e.g. vonr, wfc, rsrp, bandwidth)...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp)
            )
        }

        // Filter chips
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("All", "Overridden", "Turbo CA", "5G NR", "VoLTE", "VoWiFi", "Thresholds").forEach { filter ->
                    val isSelected = selectedFilter == filter
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedFilter = filter },
                        label = { Text(filter, fontSize = 11.sp) },
                        leadingIcon = if (isSelected) {
                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
                        } else null,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    )
                }
            }
        }

        // Config items list or loading indicator
        if (isLoadingConfig) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        } else if (filteredItems.isEmpty()) {
            item {
                Text(
                    text = "No matching configuration keys found.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp)
                )
            }
        } else {
            items(filteredItems) { item ->
                CarrierConfigItemRow(item = item)
            }
        }
    }
}

@Composable
fun CarrierAggregationLabCard(
    caInfo: CarrierAggregationInfo,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
        border = BorderStroke(1.dp, if (caInfo.isAggregating) SignalExcellent.copy(alpha = 0.4f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CellTower,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Carrier Aggregation (CA) RF Lab",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (caInfo.isAggregating) SignalExcellent.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        text = if (caInfo.isAggregating) "CA ACTIVE (4G+/5G)" else "SINGLE CARRIER",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (caInfo.isAggregating) SignalExcellent else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Total Aggregated Bandwidth Banner
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "${caInfo.totalAggregatedBandwidthMhz} MHz",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Total Aggregated RF Channel",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                    ) {
                        Text(
                            text = caInfo.mimoLayers,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                    ) {
                        Text(
                            text = caInfo.modulation,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Component Carriers List
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                caInfo.primaryCell?.let { pcell ->
                    ComponentCarrierRow(cell = pcell)
                }
                caInfo.secondaryCells.forEach { scell ->
                    ComponentCarrierRow(cell = scell)
                }
            }
        }
    }
}

@Composable
fun ComponentCarrierRow(
    cell: CellComponentInfo,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "${cell.role} - ${cell.bandName}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "EARFCN/NR-ARFCN: ${if (cell.earfcn != -1) cell.earfcn else "N/A"} | PCI: ${if (cell.pci != -1) cell.pci else "N/A"}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 10.sp
                )
            }

            Surface(
                shape = RoundedCornerShape(6.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
            ) {
                Text(
                    text = "${cell.bandwidthMhz} MHz",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
    }
}

@Composable
fun LatencyBenchmarkCard(
    result: BenchmarkResult,
    onRunCloudflare: () -> Unit,
    onRunGoogle: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Speed,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Real-Time Latency & Jitter Benchmark",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                if (result.isRunning) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Metrics Display Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                MetricItem(
                    label = "Min Latency",
                    value = if (result.minLatencyMs > 0) "${String.format("%.1f", result.minLatencyMs)} ms" else "--"
                )
                MetricItem(
                    label = "Average RTT",
                    value = if (result.avgLatencyMs > 0) "${String.format("%.1f", result.avgLatencyMs)} ms" else "--"
                )
                MetricItem(
                    label = "Jitter Variance",
                    value = if (result.jitterMs > 0) "${String.format("%.1f", result.jitterMs)} ms" else "--"
                )
                MetricItem(
                    label = "Packet Loss",
                    value = if (result.timestamp > 0) "${result.packetLossPercent}%" else "--"
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Benchmark Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onRunCloudflare,
                    enabled = !result.isRunning,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Cloudflare (1.1.1.1)")
                }

                Button(
                    onClick = onRunGoogle,
                    enabled = !result.isRunning,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Google (8.8.8.8)")
                }
            }
        }
    }
}

@Composable
fun MetricItem(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
fun DiagnosticParamRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun CarrierConfigItemRow(
    item: CarrierConfigItem,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (item.isOverridden) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
            }
        ),
        border = BorderStroke(
            1.dp,
            if (item.isOverridden) MaterialTheme.colorScheme.primary.copy(alpha = 0.4f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = item.key,
                    style = MaterialTheme.typography.bodyMedium,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = if (item.isOverridden) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (item.isOverridden) SignalExcellent.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        text = if (item.isOverridden) "TURBO ACTIVE" else item.type,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (item.isOverridden) SignalExcellent else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = item.value,
                style = MaterialTheme.typography.bodyMedium,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 16.sp
            )
        }
    }
}
