package dev.hypercarrier.patcher.ui.components

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
import androidx.compose.material.icons.filled.WifiCalling3
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.hypercarrier.patcher.data.ImsCapabilityState
import dev.hypercarrier.patcher.ui.theme.SignalExcellent
import dev.hypercarrier.patcher.ui.theme.SignalPoor

@Composable
fun ImsStatusCard(
    imsCapabilities: ImsCapabilityState,
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
                        imageVector = Icons.Default.PhoneInTalk,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "IMS Engine Capabilities",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (imsCapabilities.isImsRegistered) SignalExcellent.copy(alpha = 0.15f) else SignalPoor.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, if (imsCapabilities.isImsRegistered) SignalExcellent.copy(alpha = 0.5f) else SignalPoor.copy(alpha = 0.5f))
                ) {
                    Text(
                        text = if (imsCapabilities.isImsRegistered) "REGISTERED" else "UNREGISTERED",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (imsCapabilities.isImsRegistered) SignalExcellent else SignalPoor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
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
                    label = "VoNR (5G)",
                    isActive = imsCapabilities.isVoNrAvailable,
                    modifier = Modifier.weight(1f)
                )
                ImsFeaturePill(
                    label = "UT / XCAP",
                    isActive = imsCapabilities.isUtAvailable,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun ImsFeaturePill(
    label: String,
    isActive: Boolean,
    modifier: Modifier = Modifier
) {
    val pillBg = if (isActive) SignalExcellent.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    val pillColor = if (isActive) SignalExcellent else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = pillBg,
        border = BorderStroke(1.dp, if (isActive) SignalExcellent.copy(alpha = 0.3f) else Color.Transparent)
    ) {
        Row(
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = if (isActive) Icons.Default.Check else Icons.Default.Close,
                contentDescription = null,
                tint = pillColor,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = pillColor
            )
        }
    }
}
