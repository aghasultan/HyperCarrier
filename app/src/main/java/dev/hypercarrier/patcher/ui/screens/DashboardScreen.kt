package dev.hypercarrier.patcher.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.PhoneInTalk
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.hypercarrier.patcher.data.CarrierPreset
import dev.hypercarrier.patcher.data.CarrierPresets
import dev.hypercarrier.patcher.data.InjectionResult
import dev.hypercarrier.patcher.ui.MainViewModel
import dev.hypercarrier.patcher.ui.components.ImsStatusCard
import dev.hypercarrier.patcher.ui.components.ShizukuStatusCard
import dev.hypercarrier.patcher.ui.components.SignalStrengthCard
import dev.hypercarrier.patcher.ui.components.SimSelectorSection
import dev.hypercarrier.patcher.ui.theme.SignalExcellent

@Composable
fun DashboardScreen(
    viewModel: MainViewModel,
    onNavigateToEditor: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val subscriptions by viewModel.subscriptions.collectAsState()
    val selectedSub by viewModel.selectedSubscription.collectAsState()
    val isShizukuRunning by viewModel.isShizukuRunning.collectAsState()
    val hasPermission by viewModel.hasShizukuPermission.collectAsState()
    val isServiceConnected by viewModel.isServiceConnected.collectAsState()
    val serviceError by viewModel.serviceError.collectAsState()

    val signalMetrics by viewModel.signalMetrics.collectAsState()
    val imsCapabilities by viewModel.imsCapabilities.collectAsState()
    val injectionResult by viewModel.injectionResult.collectAsState()
    val selectedModeId by viewModel.selectedNetworkMode.collectAsState()
    val isRadioGuardActive by viewModel.isRadioGuardActive.collectAsState()

    var showClearDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Shizuku Privilege Banner
        item {
            ShizukuStatusCard(
                isShizukuRunning = isShizukuRunning,
                hasPermission = hasPermission,
                isServiceConnected = isServiceConnected,
                errorMessage = serviceError,
                onRequestPermission = { viewModel.requestShizukuPermission() },
                onRetryConnect = { viewModel.retryShizukuConnection() }
            )
        }

        // Injection Result Banner
        item {
            AnimatedVisibility(visible = injectionResult !is InjectionResult.Idle) {
                InjectionResultBanner(
                    result = injectionResult,
                    onDismiss = { viewModel.dismissInjectionResult() }
                )
            }
        }

        // SIM Selector
        item {
            SimSelectorSection(
                subscriptions = subscriptions,
                selectedSubId = selectedSub?.subscriptionId ?: -1,
                onSelectSubscription = { viewModel.selectSubscription(it) }
            )
        }

        // Hyper-Elite Hero RF Radar & 1-Tap Control Center
        item {
            HeroRfRadarCard(
                carrierName = selectedSub?.carrierName ?: selectedSub?.displayName ?: "Baseband Initialized",
                networkType = signalMetrics.networkType,
                rsrpDbm = signalMetrics.rsrpDbm,
                is5g = signalMetrics.is5gConnected,
                isImsRegistered = imsCapabilities.isImsRegistered,
                onFlushRadio = { viewModel.triggerRadioFlush() },
                onResetIms = { viewModel.forceReRegisterIms() },
                onCopyTelemetry = {
                    val report = viewModel.exportTelemetryReport()
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                    clipboard?.setPrimaryClip(ClipData.newPlainText("HyperCarrier Telemetry", report))
                    Toast.makeText(context, "RF Telemetry copied to clipboard", Toast.LENGTH_SHORT).show()
                }
            )
        }

        // Signal & RF Diagnostics Card
        item {
            SignalStrengthCard(metrics = signalMetrics)
        }

        // Network Mode Enforcer Card (5G SA / 5G NSA+LTE-CA / LTE-A)
        item {
            NetworkModeEnforcerCard(
                selectedModeId = selectedModeId,
                onSelectMode = { viewModel.setNetworkMode(it) }
            )
        }

        // Radio Turbo Flush & Auto-Healer Card
        item {
            RadioTurboGuardCard(
                isRadioGuardActive = isRadioGuardActive,
                onToggleGuard = { viewModel.toggleRadioGuard(it) },
                onFlushRadio = { viewModel.triggerRadioFlush() }
            )
        }

        // Interactive IMS Capabilities & 1-Tap Control Card
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

        // One-Tap Presets Header with Quick Scroll Row
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Carrier Presets Matrix",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Turbo Aggregation Ready",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Quick horizontal preset chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val quickPresets = listOf(
                        CarrierPresets.ZONG_PAKISTAN,
                        CarrierPresets.JAZZ_PAKISTAN,
                        CarrierPresets.TELENOR_PAKISTAN,
                        CarrierPresets.UFONE_PAKISTAN,
                        CarrierPresets.GLOBAL_ULTRA_UNLOCK
                    )
                    quickPresets.forEach { preset ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                            modifier = Modifier.clickable { viewModel.applyPreset(preset) }
                        ) {
                            Text(
                                text = preset.name,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }
        }

        // Presets List
        items(CarrierPresets.ALL_PRESETS) { preset ->
            CarrierPresetItem(
                preset = preset,
                isRecommended = selectedSub?.let {
                    preset.targetMcc == it.mcc && (preset.targetMnc == it.mnc || preset.targetMnc == "*")
                } ?: false,
                onApply = { viewModel.applyPreset(preset) }
            )
        }

        // Quick Actions Section
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "System Administration",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = { viewModel.applyPreset(CarrierPresets.GLOBAL_ULTRA_UNLOCK) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Default.FlashOn, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Apply Ultra Unlock")
                    }

                    OutlinedButton(
                        onClick = { showClearDialog = true },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f))
                    ) {
                        Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Restore Defaults")
                    }
                }
            }
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Restore OEM / Carrier Defaults?") },
            text = {
                Text("This will wipe all persistent CarrierConfig disk overrides for the current SIM slot and restore factory carrier policies.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearOverrides()
                        showClearDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Clear Overrides")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

/**
 * Hyper-Elite Hero RF Radar Card.
 * Displays animated concentric radar rings, rotating sweep line, and 1-tap quick action controls.
 */
@Composable
fun HeroRfRadarCard(
    carrierName: String,
    networkType: String,
    rsrpDbm: Int,
    is5g: Boolean,
    isImsRegistered: Boolean,
    onFlushRadio: () -> Unit,
    onResetIms: () -> Unit,
    onCopyTelemetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "RadarTransition")
    val sweepAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "RadarSweep"
    )
    val pulseRadius by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "RadarPulse"
    )

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
        ),
        border = BorderStroke(
            1.dp,
            if (isImsRegistered) SignalExcellent.copy(alpha = 0.5f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Radar Visual Header
            Box(
                modifier = Modifier
                    .size(150.dp)
                    .padding(6.dp),
                contentAlignment = Alignment.Center
            ) {
                val primaryColor = MaterialTheme.colorScheme.primary
                val accentColor = if (isImsRegistered) SignalExcellent else MaterialTheme.colorScheme.tertiary

                Canvas(modifier = Modifier.fillMaxSize()) {
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val maxRadius = size.minDimension / 2f

                    // Fixed concentric rings
                    for (i in 1..3) {
                        drawCircle(
                            color = primaryColor.copy(alpha = 0.12f * i),
                            radius = maxRadius * (i / 3f),
                            center = center,
                            style = Stroke(width = 1.5.dp.toPx())
                        )
                    }

                    // Pulsing expanding ring
                    drawCircle(
                        color = accentColor.copy(alpha = (1f - pulseRadius).coerceIn(0f, 0.7f)),
                        radius = maxRadius * pulseRadius,
                        center = center,
                        style = Stroke(width = 2.dp.toPx())
                    )

                    // Crosshairs
                    drawLine(
                        color = primaryColor.copy(alpha = 0.2f),
                        start = Offset(center.x, 0f),
                        end = Offset(center.x, size.height),
                        strokeWidth = 1.dp.toPx()
                    )
                    drawLine(
                        color = primaryColor.copy(alpha = 0.2f),
                        start = Offset(0f, center.y),
                        end = Offset(size.width, center.y),
                        strokeWidth = 1.dp.toPx()
                    )

                    // Rotating Radar Sweep
                    val sweepRad = Math.toRadians(sweepAngle.toDouble())
                    val endX = center.x + maxRadius * kotlin.math.cos(sweepRad).toFloat()
                    val endY = center.y + maxRadius * kotlin.math.sin(sweepRad).toFloat()
                    drawLine(
                        color = accentColor.copy(alpha = 0.85f),
                        start = center,
                        end = Offset(endX, endY),
                        strokeWidth = 2.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }

                // Center Telemetry Pill
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                    border = BorderStroke(1.5.dp, if (isImsRegistered) SignalExcellent else primaryColor),
                    modifier = Modifier.size(72.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = if (rsrpDbm != -999) "$rsrpDbm" else "--",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            color = if (isImsRegistered) SignalExcellent else MaterialTheme.colorScheme.onSurface,
                            fontSize = 17.sp
                        )
                        Text(
                            text = "dBm",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 9.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Carrier & Baseband State
            Text(
                text = carrierName.ifBlank { "Modem Baseband Ready" },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(2.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (is5g) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Text(
                        text = networkType.ifBlank { "LTE/5G" },
                        style = MaterialTheme.typography.labelSmall,
                        color = if (is5g) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSecondaryContainer,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (isImsRegistered) SignalExcellent.copy(alpha = 0.18f) else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)
                ) {
                    Text(
                        text = if (isImsRegistered) "IMS SIP ONLINE" else "IMS UNREGISTERED",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isImsRegistered) SignalExcellent else MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 1-Tap Quick Action Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onFlushRadio,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Turbo Flush", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = onResetIms,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                ) {
                    Icon(Icons.Default.PhoneInTalk, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Reset IMS", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = onCopyTelemetry,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Telemetry", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun NetworkModeEnforcerCard(
    selectedModeId: Int,
    onSelectMode: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Speed,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Modem Network Mode Enforcer",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                MainViewModel.NETWORK_MODES.forEach { mode ->
                    val isSelected = mode.id == selectedModeId
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectMode(mode.id) },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                        border = BorderStroke(if (isSelected) 1.5.dp else 1.dp, if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = mode.title,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = mode.subtitle,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 12.sp
                                )
                            }
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RadioTurboGuardCard(
    isRadioGuardActive: Boolean,
    onToggleGuard: (Boolean) -> Unit,
    onFlushRadio: () -> Unit,
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
                        imageVector = Icons.Default.Build,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Auto-Healer & Radio Guard",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Auto-reconnects dropped IMS/5G cells",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Switch(
                    checked = isRadioGuardActive,
                    onCheckedChange = onToggleGuard,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.primary,
                        checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                    )
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = onFlushRadio,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "1-Tap Radio Turbo Flush",
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun CarrierPresetItem(
    preset: CarrierPreset,
    isRecommended: Boolean,
    onApply: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isRecommended) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            }
        ),
        border = BorderStroke(
            if (isRecommended) 1.5.dp else 1.dp,
            if (isRecommended) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
        )
    ) {
        Row(
            modifier = Modifier
                .padding(14.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = preset.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (isRecommended) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.primary
                        ) {
                            Text(
                                text = "MATCHED SIM",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(3.dp))

                Text(
                    text = preset.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 16.sp
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            ElevatedButton(
                onClick = onApply,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.elevatedButtonColors(
                    containerColor = if (isRecommended) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
                )
            ) {
                Text(
                    text = "Apply",
                    fontWeight = FontWeight.SemiBold,
                    color = if (isRecommended) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun InjectionResultBanner(
    result: InjectionResult,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (bgColor, contentColor, text) = when (result) {
        is InjectionResult.Success -> Triple(
            SignalExcellent.copy(alpha = 0.15f),
            SignalExcellent,
            result.message
        )
        is InjectionResult.Error -> Triple(
            MaterialTheme.colorScheme.errorContainer,
            MaterialTheme.colorScheme.onErrorContainer,
            result.message
        )
        is InjectionResult.InProgress -> Triple(
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.onPrimaryContainer,
            "Injecting persistent overrides to disk..."
        )
        is InjectionResult.Idle -> Triple(Color.Transparent, Color.Transparent, "")
    }

    if (result is InjectionResult.Idle) return

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = bgColor,
        border = BorderStroke(1.dp, contentColor.copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (result is InjectionResult.InProgress) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = contentColor
                    )
                } else {
                    Icon(
                        imageVector = if (result is InjectionResult.Success) Icons.Default.Check else Icons.Default.Close,
                        contentDescription = null,
                        tint = contentColor,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = contentColor,
                    fontWeight = FontWeight.Medium
                )
            }

            if (result !is InjectionResult.InProgress) {
                IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Dismiss",
                        tint = contentColor,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
