package com.example.ui.screens

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import com.example.data.auth.AuthHelper
import com.example.data.model.GeneralConfig
import com.example.data.model.StaffRole
import com.example.data.model.UserProfile
import com.example.notifications.ArenaNotificationHelper
import com.example.ui.components.CyberGlassCard
import com.example.ui.components.NeonPrimaryButton
import com.example.ui.components.StatusPill
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CrimsonDanger
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.EmeraldProfit
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun ProfileAndMoreScreen(
    user: UserProfile?,
    staffRole: StaffRole?,
    generalConfig: GeneralConfig,
    isAdminAccess: Boolean,
    busy: Boolean,
    onSaveProfile: (String, String, String, String, Boolean) -> Unit,
    onSendTestPush: () -> Unit,
    onNavigate: (String) -> Unit,
    onOpenAdminCenter: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }

    var displayName by remember(user?.displayName) { mutableStateOf(user?.displayName.orEmpty()) }
    var inGameName by remember(user?.inGameName) { mutableStateOf(user?.inGameName.orEmpty()) }
    var gameUid by remember(user?.gameUid) { mutableStateOf(user?.gameUid.orEmpty()) }
    var phoneNumber by remember(user?.phoneNumber) { mutableStateOf(user?.phoneNumber.orEmpty()) }
    var pushEnabled by remember(user?.pushEnabled) { mutableStateOf(user?.pushEnabled ?: true) }
    var hasNotifPerm by remember { mutableStateOf(ArenaNotificationHelper.hasNotificationPermission(context)) }

    val permLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasNotifPerm = granted
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("PROFILE & COMMAND", style = MaterialTheme.typography.headlineMedium, color = NeonCyan)
                StatusPill(
                    label = if (isAdminAccess) (staffRole?.roleLabel ?: "SUPER ADMIN") else "PLAYER",
                    color = if (isAdminAccess) AmberWarning else NeonCyan
                )
            }
        }

        // Admin Command Center Entry
        if (isAdminAccess) {
            item {
                CyberGlassCard(glowColor = AmberWarning, onClick = onOpenAdminCenter, modifier = Modifier.testTag("open_admin_command_center_btn")) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.AdminPanelSettings, contentDescription = null, tint = AmberWarning, modifier = Modifier.size(32.dp))
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text("Admin Command Center", style = MaterialTheme.typography.titleLarge, color = AmberWarning)
                                Text("12 Enterprise Modules • PayZapi • Push Studio • RBAC", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                            }
                        }
                        StatusPill("OPEN", AmberWarning)
                    }
                }
            }
        }

        // Quick Hub Navigation
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                CyberGlassCard(glowColor = ElectricViolet, onClick = { onNavigate("refer") }, modifier = Modifier.weight(1f).testTag("profile_refer_btn")) {
                    Icon(Icons.Default.CardGiftcard, contentDescription = null, tint = ElectricViolet)
                    Spacer(Modifier.height(6.dp))
                    Text("Refer & Earn", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                    Text("Code: ${user?.referralCode.orEmpty()}", style = MaterialTheme.typography.labelSmall, color = NeonCyan)
                }
                CyberGlassCard(glowColor = EmeraldProfit, onClick = { onNavigate("support") }, modifier = Modifier.weight(1f).testTag("profile_support_btn")) {
                    Icon(Icons.Default.SupportAgent, contentDescription = null, tint = EmeraldProfit)
                    Spacer(Modifier.height(6.dp))
                    Text("24/7 Support", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                    Text("Tickets & Hacker Desk", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                }
            }
        }

        // Push Notification Shutter Controls
        item {
            CyberGlassCard(glowColor = NeonCyan) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = NeonCyan)
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text("Mobile Shutter Push Alerts", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                            Text(if (hasNotifPerm) "Permission Granted" else "Tap Enable to allow shutter alerts", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                        }
                    }
                    Switch(checked = pushEnabled, onCheckedChange = { pushEnabled = it })
                }
                Spacer(Modifier.height(10.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (!hasNotifPerm && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        Button(
                            onClick = { permLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) },
                            colors = ButtonDefaults.buttonColors(containerColor = AmberWarning, contentColor = ObsidianBg),
                            modifier = Modifier.weight(1f)
                        ) { Text("Grant Permission") }
                    }
                    OutlinedButton(
                        onClick = {
                            if (!hasNotifPerm && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                permLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                onSendTestPush()
                            }
                        },
                        modifier = Modifier.weight(1f).testTag("test_shutter_notification_btn")
                    ) {
                        Text("Test Notification on My Phone", color = NeonCyan)
                    }
                }
            }
        }

        // Player Profile Editor
        item {
            CyberGlassCard(glowColor = ElectricViolet) {
                Text("Player Identity & Esports ID", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
                Text(user?.email.orEmpty(), style = MaterialTheme.typography.labelSmall, color = TextMuted)
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(value = displayName, onValueChange = { displayName = it }, label = { Text("Display Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = inGameName, onValueChange = { inGameName = it }, label = { Text("Default In-Game Name (IGN)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = gameUid, onValueChange = { gameUid = it }, label = { Text("Default Game UID") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = phoneNumber,
                    onValueChange = { phoneNumber = it.filter { ch -> ch.isDigit() || ch == '+' || ch == ' ' }.take(16) },
                    label = { Text("Phone Number") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth().testTag("profile_phone_input")
                )
                Spacer(Modifier.height(12.dp))
                NeonPrimaryButton(
                    text = if (busy) "Saving..." else "Save Profile",
                    enabled = !busy,
                    onClick = { onSaveProfile(displayName, inGameName, gameUid, phoneNumber, pushEnabled) },
                    modifier = Modifier.fillMaxWidth().testTag("save_profile_btn")
                )
            }
        }

        // Rules & Community Links
        item {
            CyberGlassCard(glowColor = TextMuted) {
                Text("Arena Rules & Community", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                Spacer(Modifier.height(6.dp))
                Text(generalConfig.rulesText, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(generalConfig.telegramUrl))) } }) {
                        Text("Telegram", color = NeonCyan)
                    }
                    OutlinedButton(onClick = { runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(generalConfig.discordUrl))) } }) {
                        Text("Discord", color = ElectricViolet)
                    }
                }
            }
        }

        // Sign Out
        item {
            Button(
                onClick = { AuthHelper.signOut(credentialManager, scope) },
                colors = ButtonDefaults.buttonColors(containerColor = CrimsonDanger.copy(alpha = 0.2f), contentColor = CrimsonDanger),
                modifier = Modifier.fillMaxWidth().height(50.dp).testTag("sign_out_btn")
            ) {
                Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Sign Out")
            }
        }
    }
}
