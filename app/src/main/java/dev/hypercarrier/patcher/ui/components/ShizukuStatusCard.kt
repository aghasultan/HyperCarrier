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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import dev.hypercarrier.patcher.ui.theme.SignalExcellent
import dev.hypercarrier.patcher.ui.theme.SignalModerate
import dev.hypercarrier.patcher.ui.theme.SignalPoor

@Composable
fun ShizukuStatusCard(
    isShizukuRunning: Boolean,
    hasPermission: Boolean,
    isServiceConnected: Boolean,
    errorMessage: String?,
    onRequestPermission: () -> Unit,
    onRetryConnect: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isReady = isShizukuRunning && hasPermission && isServiceConnected

    val statusColor = when {
        isReady -> SignalExcellent
        isShizukuRunning && !hasPermission -> SignalModerate
        else -> SignalPoor
    }

    val statusText = when {
        isReady -> "Privileged IPC Active (UID 2000 / Shell)"
        isShizukuRunning && !hasPermission -> "Shizuku Running (Permission Required)"
        isShizukuRunning && !isServiceConnected -> "Shizuku Running (Connecting Service...)"
        else -> "Shizuku Daemon Not Running"
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        ),
        border = BorderStroke(1.dp, statusColor.copy(alpha = 0.4f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = statusColor.copy(alpha = 0.15f),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = if (isReady) Icons.Default.CheckCircle else Icons.Default.Security,
                            contentDescription = null,
                            tint = statusColor,
                            modifier = Modifier
                                .padding(8.dp)
                                .size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = "Shizuku Injection Engine",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = CircleShape,
                                color = statusColor,
                                modifier = Modifier.size(7.dp)
                            ) {}
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = statusText,
                                style = MaterialTheme.typography.bodyMedium,
                                color = statusColor
                            )
                        }
                    }
                }
            }

            AnimatedVisibility(visible = !isReady) {
                Column(modifier = Modifier.padding(top = 14.dp)) {
                    Text(
                        text = if (!isShizukuRunning) {
                            "Start the Shizuku app or start Shizuku via Wireless Debugging / ADB to enable persistent disk-level CarrierConfig injection."
                        } else if (!hasPermission) {
                            "Grant Shizuku permission to allow HyperCarrier to execute CarrierConfigManager.overrideConfig as Shell UID 2000."
                        } else {
                            "Connecting to Privileged Carrier Service..."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (isShizukuRunning && !hasPermission) {
                            Button(
                                onClick = onRequestPermission,
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Icon(Icons.Default.Security, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Authorize Shizuku")
                            }
                        } else if (!isShizukuRunning) {
                            Button(
                                onClick = onRetryConnect,
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                            ) {
                                Icon(Icons.Default.Terminal, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Check Shizuku Connection")
                            }
                        }
                    }
                }
            }

            if (errorMessage != null && !isReady) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = errorMessage,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
