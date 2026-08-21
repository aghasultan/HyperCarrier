package dev.hypercarrier.patcher.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PhoneInTalk
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VpnLock
import androidx.compose.material.icons.filled.WifiCalling3
import androidx.compose.material.icons.filled.WifiTethering
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.hypercarrier.patcher.data.ImsCapabilityState
import dev.hypercarrier.patcher.ui.theme.SignalExcellent
import dev.hypercarrier.patcher.ui.theme.SignalModerate
import dev.hypercarrier.patcher.ui.theme.SignalPoor

/**
 * Hyper-Elite Interactive IMS Engine & Capabilities Control Card.
 * Allows 1-tap real-time switching, direct modem provisioning, and live status inspection.
 */
@Composable
fun ImsStatusCard(
    imsCapabilities: ImsCapabilityState,
    onToggleVoLte: (Boolean) -> Unit,
    onToggleVoWifi: (Boolean) -> Unit,
    onToggleVoNr: (Boolean) -> Unit,
    onToggleViLte: (Boolean) -> Unit,
    onSetVoWifiMode: (Int) -> Unit,
    onForceReRegister: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(true) }
    var selectedWfcMode by remember { mutableStateOf(1) } // 1 = Wi-Fi Preferred, 2 = Cellular Preferred

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        ),
        border = BorderStroke(
            1.dp,
            if (imsCapabilities.isImsRegistered) SignalExcellent.copy(alpha = 0.4f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row: Title + Registration Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                        modifier = Modifier.size(38.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhoneInTalk,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .padding(8.dp)
                                .size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Text(
                            text = "IMS Engine & Voice Core",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (imsCapabilities.isImsRegistered) "Transport: ${imsCapabilities.registrationTransport}" else "Carrier Provisioning Ready",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (imsCapabilities.isImsRegistered) SignalExcellent.copy(alpha = 0.15f) else SignalModerate.copy(alpha = 0.15f),
                    border = BorderStroke(
                        1.dp,
                        if (imsCapabilities.isImsRegistered) SignalExcellent.copy(alpha = 0.5f) else SignalModerate.copy(alpha = 0.5f)
                    )
                ) {
                    Text(
                        text = if (imsCapabilities.isImsRegistered) "REGISTERED" else "READY TO PROVISION",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (imsCapabilities.isImsRegistered) SignalExcellent else SignalModerate,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Quick Status Feature Badges Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                ImsFeaturePill(
                    label = "VoLTE",
                    isActive = imsCapabilities.isVoLteAvailable,
                    modifier = Modifier.weight(1f)
                )
                ImsFeaturePill(
                    label = "VoWiFi",
                    isActive = imsCapabilities.isVoWifiAvailable,
                    modifier = Modifier.weight(1f)
                )
                ImsFeaturePill(
                    label = "VoNR 5G",
                    isActive = imsCapabilities.isVoNrAvailable,
                    modifier = Modifier.weight(1f)
                )
                ImsFeaturePill(
                    label = "UT / XCAP",
                    isActive = imsCapabilities.isUtAvailable,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Interactive Toggles Section
            Text(
                text = "1-Tap Engine Controls",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(8.dp))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // 1. VoLTE Switch
                ImsToggleRow(
                    icon = Icons.Default.PhoneInTalk,
                    title = "Voice over LTE (VoLTE / 4G Calling)",
                    subtitle = "Direct modem-level HD voice without 3G/2G drop",
                    checked = imsCapabilities.isVoLteAvailable,
                    onCheckedChange = onToggleVoLte
                )

                // 2. VoWiFi Switch
                ImsToggleRow(
                    icon = Icons.Default.WifiCalling3,
                    title = "Wi-Fi Calling (VoWiFi / WFC)",
                    subtitle = "Carrier voice & SMS over any Wi-Fi hotspot",
                    checked = imsCapabilities.isVoWifiAvailable,
                    onCheckedChange = onToggleVoWifi
                )

                // 3. VoNR Switch
                ImsToggleRow(
                    icon = Icons.Default.WifiTethering,
                    title = "Voice over New Radio (5G VoNR)",
                    subtitle = "Ultra-HD voice directly on pure 5G Standalone",
                    checked = imsCapabilities.isVoNrAvailable,
                    onCheckedChange = onToggleVoNr
                )

                // 4. ViLTE Video Switch
                ImsToggleRow(
                    icon = Icons.Default.Videocam,
                    title = "Carrier Video Calling (ViLTE)",
                    subtitle = "Hardware-accelerated modem video calling",
                    checked = imsCapabilities.isVideoAvailable,
                    onCheckedChange = onToggleViLte
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Wi-Fi Calling Mode Selector
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "WFC Mode:",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold
                )

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(
                        selected = selectedWfcMode == 1,
                        onClick = {
                            selectedWfcMode = 1
                            onSetVoWifiMode(1)
                        },
                        label = { Text("Wi-Fi Preferred", fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.primary
                        )
                    )

                    FilterChip(
                        selected = selectedWfcMode == 2,
                        onClick = {
                            selectedWfcMode = 2
                            onSetVoWifiMode(2)
                        },
                        label = { Text("Cellular Preferred", fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Force Re-Register Button
            OutlinedButton(
                onClick = onForceReRegister,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Force Modem IMS Re-Registration", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
fun ImsToggleRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
        border = BorderStroke(
            1.dp,
            if (checked) SignalExcellent.copy(alpha = 0.3f) else Color.Transparent
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (checked) SignalExcellent else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = if (checked) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                    )
                }
            }

            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = SignalExcellent,
                    uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                    uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )
        }
    }
}

@Composable
fun ImsFeaturePill(
    label: String,
    isActive: Boolean,
    modifier: Modifier = Modifier
) {
    val pillBg = if (isActive) SignalExcellent.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
    val pillColor = if (isActive) SignalExcellent else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = pillBg,
        border = BorderStroke(1.dp, if (isActive) SignalExcellent.copy(alpha = 0.4f) else Color.Transparent)
    ) {
        Row(
            modifier = Modifier.padding(vertical = 6.dp, horizontal = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = if (isActive) Icons.Default.Check else Icons.Default.Close,
                contentDescription = null,
                tint = pillColor,
                modifier = Modifier.size(12.dp)
            )
            Spacer(modifier = Modifier.width(3.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = pillColor,
                fontSize = 11.sp
            )
        }
    }
}
