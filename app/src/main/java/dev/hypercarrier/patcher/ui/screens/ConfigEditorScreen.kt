package dev.hypercarrier.patcher.ui.screens

import android.os.PersistableBundle
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CellTower
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.PhoneInTalk
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.WifiCalling3
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.hypercarrier.patcher.data.CarrierConfigPayloadBuilder
import dev.hypercarrier.patcher.ui.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfigEditorScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val selectedSub by viewModel.selectedSubscription.collectAsState()
    val isServiceConnected by viewModel.isServiceConnected.collectAsState()

    // 5G & VoNR state
    var enable5gSa by remember { mutableStateOf(true) }
    var enable5gNsa by remember { mutableStateOf(true) }
    var enableVoNr by remember { mutableStateOf(true) }
    var showVoNrSetting by remember { mutableStateOf(true) }
    var unmetered5g by remember { mutableStateOf(true) }

    // Turbo Aggregation & Latency state
    var enableTurboCa by remember { mutableStateOf(true) }
    var zeroApnDelay by remember { mutableStateOf(true) }
    var instantDualSimSwitch by remember { mutableStateOf(true) }

    // VoLTE & IMS state
    var enableVoLte by remember { mutableStateOf(true) }
    var editableEnhanced4g by remember { mutableStateOf(true) }
    var enhanced4gOnByDefault by remember { mutableStateOf(true) }
    var showImsStatus by remember { mutableStateOf(true) }
    var allowTurnOffIms by remember { mutableStateOf(true) }
    var supportCallerIdVsc by remember { mutableStateOf(true) }

    // VoWiFi & Cross SIM state
    var enableVoWifi by remember { mutableStateOf(true) }
    var wfcMode by remember { mutableStateOf(1) } // 1 = Wi-Fi Preferred, 2 = Cellular Preferred
    var enableCrossSimIms by remember { mutableStateOf(true) }
    var suppressEmergencyBanner by remember { mutableStateOf(true) }

    // RF Sensitivity state
    var aggressiveSensitivity by remember { mutableStateOf(true) }
    var useRsrpForSignalBar by remember { mutableStateOf(true) }

    // Custom Key Dialog / Fields
    var showAddCustomKey by remember { mutableStateOf(false) }
    var customKeyName by remember { mutableStateOf("") }
    var customKeyValue by remember { mutableStateOf("") }
    var customKeyType by remember { mutableStateOf("Boolean") }
    val customKeys = remember { mutableMapOf<String, Any>() }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 36.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Granular CarrierConfig Payload",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Target: ${selectedSub?.carrierName ?: "No SIM"} (Slot ${selectedSub?.let { it.slotIndex + 1 } ?: "?"})",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Section 0: Extreme Turbo Aggregation & Latency
        item {
            ConfigSectionCard(
                title = "Extreme Turbo Aggregation & Latency",
                icon = Icons.Default.Speed
            ) {
                ConfigToggleItem(
                    title = "Extreme Turbo CA (LTE-CA / EN-DC)",
                    subtitle = "4 Gbps 5G / 1 Gbps LTE bandwidth allocations in bandwidth_string_array",
                    checked = enableTurboCa,
                    onCheckedChange = { enableTurboCa = it }
                )
                ConfigToggleItem(
                    title = "0ms Zero-Delay APN Connection",
                    subtitle = "Sets carrier_data_call_apn_delay_default_long = 0L",
                    checked = zeroApnDelay,
                    onCheckedChange = { zeroApnDelay = it }
                )
                ConfigToggleItem(
                    title = "Instant Dual-SIM Data Switching",
                    subtitle = "Sets data_switch_validation_min_gap_long = 0L",
                    checked = instantDualSimSwitch,
                    onCheckedChange = { instantDualSimSwitch = it }
                )
            }
        }

        // Section 1: 5G NR SA/NSA & VoNR
        item {
            ConfigSectionCard(
                title = "5G NR & VoNR Core",
                icon = Icons.Default.NetworkCheck
            ) {
                ConfigToggleItem(
                    title = "5G Standalone (SA)",
                    subtitle = "Enables Pure 5G Core (carrier_nr_availabilities_int_array [SA])",
                    checked = enable5gSa,
                    onCheckedChange = { enable5gSa = it }
                )
                ConfigToggleItem(
                    title = "5G Non-Standalone (NSA)",
                    subtitle = "Enables LTE-Anchored 5G (carrier_nr_availabilities_int_array [NSA])",
                    checked = enable5gNsa,
                    onCheckedChange = { enable5gNsa = it }
                )
                ConfigToggleItem(
                    title = "Voice over New Radio (VoNR)",
                    subtitle = "Enables ultra HD voice calls directly over 5G SA core",
                    checked = enableVoNr,
                    onCheckedChange = { enableVoNr = it }
                )
                ConfigToggleItem(
                    title = "VoNR User Setting Visibility",
                    subtitle = "Exposes VoNR toggle in Android Settings > SIMs menu",
                    checked = showVoNrSetting,
                    onCheckedChange = { showVoNrSetting = it }
                )
                ConfigToggleItem(
                    title = "Unmetered 5G SA/NSA Data",
                    subtitle = "Sets unmetered_nr_sa_bool & unmetered_nr_nsa_bool = true",
                    checked = unmetered5g,
                    onCheckedChange = { unmetered5g = it }
                )
            }
        }

        // Section 2: VoLTE & IMS Engine
        item {
            ConfigSectionCard(
                title = "VoLTE & IMS Engine",
                icon = Icons.Default.PhoneInTalk
            ) {
                ConfigToggleItem(
                    title = "VoLTE Available",
                    subtitle = "Forces carrier_volte_available_bool = true",
                    checked = enableVoLte,
                    onCheckedChange = { enableVoLte = it }
                )
                ConfigToggleItem(
                    title = "Enhanced 4G LTE Editable",
                    subtitle = "Unlocks greyed-out 4G Calling toggle in Settings",
                    checked = editableEnhanced4g,
                    onCheckedChange = { editableEnhanced4g = it }
                )
                ConfigToggleItem(
                    title = "Enhanced 4G LTE Default ON",
                    subtitle = "Automatically enables 4G Calling on SIM discovery",
                    checked = enhanced4gOnByDefault,
                    onCheckedChange = { enhanced4gOnByDefault = it }
                )
                ConfigToggleItem(
                    title = "Show IMS Registration Status",
                    subtitle = "Displays IMS status in Settings > About Phone > SIM status",
                    checked = showImsStatus,
                    onCheckedChange = { showImsStatus = it }
                )
                ConfigToggleItem(
                    title = "Allow Turn Off IMS",
                    subtitle = "Allows manual IMS enablement/disablement",
                    checked = allowTurnOffIms,
                    onCheckedChange = { allowTurnOffIms = it }
                )
                ConfigToggleItem(
                    title = "Caller ID Vertical Service Codes",
                    subtitle = "Supports *31# and carrier caller ID dialing codes",
                    checked = supportCallerIdVsc,
                    onCheckedChange = { supportCallerIdVsc = it }
                )
            }
        }

        // Section 3: VoWiFi / WFC & Cross-SIM
        item {
            ConfigSectionCard(
                title = "Wi-Fi Calling (VoWiFi) & Cross-SIM",
                icon = Icons.Default.WifiCalling3
            ) {
                ConfigToggleItem(
                    title = "Wi-Fi Calling (WFC) Available",
                    subtitle = "carrier_wfc_ims_available_bool = true",
                    checked = enableVoWifi,
                    onCheckedChange = { enableVoWifi = it }
                )
                ConfigToggleItem(
                    title = "Wi-Fi Preferred Calling Mode",
                    subtitle = if (wfcMode == 1) "Mode 1 (Wi-Fi Preferred)" else "Mode 2 (Cellular Preferred)",
                    checked = wfcMode == 1,
                    onCheckedChange = { wfcMode = if (it) 1 else 2 }
                )
                ConfigToggleItem(
                    title = "Cross-SIM IMS Backup Calling",
                    subtitle = "Routes calls from SIM 1 over SIM 2 mobile data when no coverage",
                    checked = enableCrossSimIms,
                    onCheckedChange = { enableCrossSimIms = it }
                )
                ConfigToggleItem(
                    title = "Suppress Emergency 911 Address Popups",
                    subtitle = "Clears carrier emergency address notification requirements",
                    checked = suppressEmergencyBanner,
                    onCheckedChange = { suppressEmergencyBanner = it }
                )
            }
        }

        // Section 4: RF Sensitivity & Signal Enhancements
        item {
            ConfigSectionCard(
                title = "RF Sensitivity & Signal Metering",
                icon = Icons.Default.CellTower
            ) {
                ConfigToggleItem(
                    title = "Aggressive RSRP Thresholds",
                    subtitle = "SS-RSRP [-140, -115, -105, -95] prevents premature signal drop",
                    checked = aggressiveSensitivity,
                    onCheckedChange = { aggressiveSensitivity = it }
                )
                ConfigToggleItem(
                    title = "Use RSRP for LTE Signal Bar",
                    subtitle = "Renders precise signal bars based on RSRP power",
                    checked = useRsrpForSignalBar,
                    onCheckedChange = { useRsrpForSignalBar = it }
                )
            }
        }

        // Section 5: Custom Key Injection
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Custom CarrierConfig Keys",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        TextButton(onClick = { showAddCustomKey = !showAddCustomKey }) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add Key")
                        }
                    }

                    AnimatedVisibility(visible = showAddCustomKey) {
                        Column(
                            modifier = Modifier.padding(top = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = customKeyName,
                                onValueChange = { customKeyName = it },
                                label = { Text("Key Name (e.g. vonr_enabled_bool)") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )

                            OutlinedTextField(
                                value = customKeyValue,
                                onValueChange = { customKeyValue = it },
                                label = { Text("Value (e.g. true, 1, or string)") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )

                            Button(
                                onClick = {
                                    if (customKeyName.isNotBlank()) {
                                        when {
                                            customKeyValue.equals("true", ignoreCase = true) -> customKeys[customKeyName] = true
                                            customKeyValue.equals("false", ignoreCase = true) -> customKeys[customKeyName] = false
                                            customKeyValue.toIntOrNull() != null -> customKeys[customKeyName] = customKeyValue.toInt()
                                            else -> customKeys[customKeyName] = customKeyValue
                                        }
                                        customKeyName = ""
                                        customKeyValue = ""
                                        showAddCustomKey = false
                                    }
                                },
                                modifier = Modifier.align(Alignment.End)
                            ) {
                                Text("Save Key")
                            }
                        }
                    }

                    if (customKeys.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        customKeys.forEach { (k, v) ->
                            Text(
                                text = "$k = $v",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // Action: Persistent Flash Injection Button
        item {
            Button(
                onClick = {
                    val builder = CarrierConfigPayloadBuilder()
                        .enable5gNr(enableSa = enable5gSa, enableNsa = enable5gNsa)
                        .enableVoNr(enabled = enableVoNr, settingVisibility = showVoNrSetting)
                        .enableVoLte(
                            available = enableVoLte,
                            editable = editableEnhanced4g,
                            onByDefault = enhanced4gOnByDefault,
                            supportsCallerId = supportCallerIdVsc,
                            allowTurnOff = allowTurnOffIms
                        )
                        .enableVoWifi(
                            available = enableVoWifi,
                            defaultMode = wfcMode,
                            crossSimAvailable = enableCrossSimIms
                        )
                        .enableSignalEnhancements(useRsrpForLteBars = useRsrpForSignalBar)
                        .setGeneralOverrides()

                    if (enableTurboCa) {
                        builder.enableTurboAggregation()
                    }
                    if (zeroApnDelay) {
                        builder.putLong(CarrierConfigPayloadBuilder.KEY_CARRIER_DATA_CALL_APN_DELAY_DEFAULT_LONG, 0L)
                        builder.putLong(CarrierConfigPayloadBuilder.KEY_CARRIER_DATA_CALL_APN_DELAY_SUBSEQUENT_LONG, 0L)
                    }
                    if (instantDualSimSwitch) {
                        builder.putLong(CarrierConfigPayloadBuilder.KEY_DATA_SWITCH_VALIDATION_MIN_GAP_LONG, 0L)
                    }

                    customKeys.forEach { (k, v) ->
                        when (v) {
                            is Boolean -> builder.putBoolean(k, v)
                            is Int -> builder.putInt(k, v)
                            is Long -> builder.putLong(k, v)
                            is String -> builder.putString(k, v)
                        }
                    }

                    viewModel.applyCustomBundle(builder.build())
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Default.FlashOn, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Inject to Flash (Persistent Overrides)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun ConfigSectionCard(
    title: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
            content()
        }
    }
}

@Composable
fun ConfigToggleItem(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 16.sp
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.primary,
                checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
            )
        )
    }
}
