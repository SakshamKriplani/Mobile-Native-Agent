package com.mobilenative.agent.ui.main

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.mobilenative.agent.accessibility.AccessibilityBridge
import com.mobilenative.agent.ui.overlay.FloatingOverlayService
import com.mobilenative.agent.ui.theme.AccentGreen
import com.mobilenative.agent.ui.theme.CardBackground
import com.mobilenative.agent.ui.theme.MobileNativeAgentTheme
import com.mobilenative.agent.ui.theme.PrimaryBlue
import com.mobilenative.agent.ui.theme.SurfaceDark
import com.mobilenative.agent.ui.theme.TextPrimary
import com.mobilenative.agent.ui.theme.TextSecondary
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var accessibilityBridge: AccessibilityBridge

    private val requestNotificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            // Update state
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MobileNativeAgentTheme {
                MainDashboardScreen(
                    accessibilityBridge = accessibilityBridge,
                    onOpenOverlaySettings = { openOverlaySettings() },
                    onOpenAccessibilitySettings = { openAccessibilitySettings() },
                    onRequestNotificationPermission = { requestNotificationPermission() },
                    onStartOverlay = { startOverlayService() },
                    onStopOverlay = { stopOverlayService() }
                )
            }
        }
    }

    private fun openOverlaySettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivity(intent)
        }
    }

    private fun openAccessibilitySettings() {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        startActivity(intent)
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun startOverlayService() {
        val intent = Intent(this, FloatingOverlayService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }

    private fun stopOverlayService() {
        val intent = Intent(this, FloatingOverlayService::class.java)
        stopService(intent)
    }
}

@Composable
fun MainDashboardScreen(
    accessibilityBridge: AccessibilityBridge,
    onOpenOverlaySettings: () -> Unit,
    onOpenAccessibilitySettings: () -> Unit,
    onRequestNotificationPermission: () -> Unit,
    onStartOverlay: () -> Unit,
    onStopOverlay: () -> Unit
) {
    val context = LocalContext.ContextEffect()
    var hasOverlayPermission by remember { mutableStateOf(false) }
    var hasNotificationPermission by remember { mutableStateOf(false) }
    val isAccessibilityConnected by accessibilityBridge.isServiceConnected.collectAsState()

    // Refresh permission statuses on resume/mount
    LaunchedEffect(Unit) {
        hasOverlayPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(context)
        } else true

        hasNotificationPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else true
    }

    Scaffold(
        containerColor = SurfaceDark
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(20.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Mobile-Native Agent",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            Text(
                text = "Level 1: System Baseline & Floating Pill",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Permission Status Cards
            PermissionStatusCard(
                title = "1. Display Over Other Apps",
                description = "Required to show the floating agent pill and confirmation card over WhatsApp and shopping apps.",
                isGranted = hasOverlayPermission,
                icon = Icons.Default.Layers,
                onGrantClick = onOpenOverlaySettings
            )

            Spacer(modifier = Modifier.height(14.dp))

            PermissionStatusCard(
                title = "2. Accessibility Service",
                description = "Required for reading chat messages and automating on-screen actions with user confirmation.",
                isGranted = isAccessibilityConnected,
                icon = Icons.Default.Security,
                onGrantClick = onOpenAccessibilitySettings
            )

            Spacer(modifier = Modifier.height(14.dp))

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                PermissionStatusCard(
                    title = "3. Foreground Notification",
                    description = "Ensures Samsung OS does not sleep or kill the agent while running in the background.",
                    isGranted = hasNotificationPermission,
                    icon = Icons.Default.Notifications,
                    onGrantClick = onRequestNotificationPermission
                )
                Spacer(modifier = Modifier.height(14.dp))
            }

            Spacer(modifier = Modifier.height(20.dp))

            val allReady = hasOverlayPermission && isAccessibilityConnected

            // Start / Stop Service Control Card
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = CardBackground),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Agent Overlay Controller",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (allReady) {
                            "All permissions granted! Launch the floating pill to start."
                        } else {
                            "Please grant the permissions above to enable the floating pill."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = onStartOverlay,
                            enabled = allReady,
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Launch Pill")
                        }

                        OutlinedButton(
                            onClick = onStopOverlay,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Stop Pill")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PermissionStatusCard(
    title: String,
    description: String,
    isGranted: Boolean,
    icon: ImageVector,
    onGrantClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(if (isGranted) AccentGreen.copy(alpha = 0.15f) else Color(0xFFF59E0B).copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isGranted) AccentGreen else Color(0xFFF59E0B),
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    fontSize = 12.sp
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            if (isGranted) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Granted",
                    tint = AccentGreen,
                    modifier = Modifier.size(24.dp)
                )
            } else {
                Button(
                    onClick = onGrantClick,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Grant", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun LocalContext.ContextEffect(): Context {
    return LocalContext.current
}
