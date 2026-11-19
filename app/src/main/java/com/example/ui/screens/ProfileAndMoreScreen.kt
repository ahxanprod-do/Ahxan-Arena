package com.example.ui.screens

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import com.example.data.auth.AuthHelper
import com.example.data.model.BroadcastNotification
import com.example.data.model.GeneralConfig
import com.example.data.model.StaffRole
import com.example.data.model.UserNotification
import com.example.data.model.UserProfile
import com.example.notifications.ArenaNotificationHelper
import com.example.ui.components.ArenaTopHeader
import com.example.ui.components.CoinBadge
import com.example.ui.components.NotificationDrawerSheet
import com.example.ui.components.SubScreenTopBar
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.ArenaBlue
import com.example.ui.theme.ArenaCharcoal
import com.example.ui.theme.ArenaGreen
import com.example.ui.theme.ArenaPurple
import com.example.ui.theme.CardDarkText
import com.example.ui.theme.CardMutedText
import com.example.ui.theme.CoinYellow
import com.example.ui.theme.HeaderNavy
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.TextSecondary

@Composable
fun ProfileAndMoreScreen(
    user: UserProfile?,
    staffRole: StaffRole?,
    generalConfig: GeneralConfig,
    isAdminAccess: Boolean,
    notifications: List<UserNotification> = emptyList(),
    broadcasts: List<BroadcastNotification> = emptyList(),
    onSendTestPush: () -> Unit,
    onTogglePush: (Boolean) -> Unit = {},
    onMarkNotificationRead: (String) -> Unit = {},
    onNavigate: (String) -> Unit,
    onOpenAdminCenter: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }

    var showNotifications by remember { mutableStateOf(false) }
    var infoDialogTitle by remember { mutableStateOf<String?>(null) }
    var infoDialogBody by remember { mutableStateOf("") }
    var pushEnabled by remember(user?.pushEnabled) { mutableStateOf(user?.pushEnabled ?: true) }
    var hasNotifPerm by remember { mutableStateOf(ArenaNotificationHelper.hasNotificationPermission(context)) }

    val permLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasNotifPerm = granted
    }

    val unreadCount = notifications.count { !it.read } + broadcasts.size

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFECEFF3))
    ) {
        ArenaTopHeader(
            coinBalance = user?.totalBalance ?: 0.0,
            unreadCount = unreadCount,
            onOpenNotifications = { showNotifications = true },
            onOpenWallet = { onNavigate("wallet") }
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Hero Profile Banner + Floating Avatar & 3-Col Stats Card (Screenshot 155400)
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(68.dp)
                                .background(HeaderNavy)
                        )
                        Box(
                            modifier = Modifier
                                .padding(top = 18.dp)
                                .size(92.dp)
                                .clip(CircleShape)
                                .background(CoinYellow)
                                .border(4.dp, HeaderNavy, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "Player Avatar",
                                tint = HeaderNavy,
                                modifier = Modifier.size(54.dp)
                            )
                        }
                    }

                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = user?.displayName?.ifBlank { "Player" } ?: "Player",
                        color = CardDarkText,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (isAdminAccess) (staffRole?.roleLabel ?: "Admin Verified") else "Verified Player",
                        color = ArenaGreen,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    Spacer(Modifier.height(14.dp))

                    Surface(
                        color = Color.White,
                        shape = RoundedCornerShape(14.dp),
                        shadowElevation = 2.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            MeStatColumn(
                                value = "${user?.matchesPlayed ?: 0}",
                                label = "Matches Played",
                                showCoin = false,
                                modifier = Modifier.weight(1f)
                            )
                            VerticalDivider(
                                modifier = Modifier.height(36.dp),
                                color = Color(0xFFE2E8F0)
                            )
                            MeStatColumn(
                                value = "${user?.totalKills ?: 0}",
                                label = "Total Kills",
                                showCoin = false,
                                modifier = Modifier.weight(1f)
                            )
                            VerticalDivider(
                                modifier = Modifier.height(36.dp),
                                color = Color(0xFFE2E8F0)
                            )
                            MeStatColumn(
                                value = "${(user?.totalWinnings ?: 0.0).toLong()}",
                                label = "PlayCoin Won",
                                showCoin = true,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // Admin Command Center Highlight Card for Admin / Staff
            if (isAdminAccess) {
                item {
                    Surface(
                        color = Color(0xFFFFFBEB),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.5.dp, AmberWarning),
                        shadowElevation = 2.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onOpenAdminCenter() }
                            .testTag("open_admin_command_center_btn")
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.AdminPanelSettings,
                                contentDescription = null,
                                tint = Color(0xFFD97706),
                                modifier = Modifier.size(26.dp)
                            )
                            Spacer(Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Admin Command Center",
                                    color = CardDarkText,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                                Text(
                                    text = "Manage Matches, Banners, Room IDs & PayZapi",
                                    color = Color(0xFF92400E),
                                    fontSize = 11.sp
                                )
                            }
                            Icon(
                                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = null,
                                tint = Color(0xFFD97706)
                            )
                        }
                    }
                }
            }

            // Clean White Menu Rows (Screenshots 155400 & 155402)
            item {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MeMenuRow(
                        title = "My Profile",
                        icon = Icons.Default.AccountCircle,
                        onClick = { onNavigate("edit_profile") },
                        modifier = Modifier.testTag("menu_my_profile")
                    )
                    MeMenuRow(
                        title = "My Wallet",
                        icon = Icons.Default.AccountBalanceWallet,
                        onClick = { onNavigate("wallet") }
                    )
                    MeMenuRow(
                        title = "My Statistics",
                        icon = Icons.Default.BarChart,
                        onClick = { onNavigate("my_matches") }
                    )
                    MeMenuRow(
                        title = "Top Players",
                        icon = Icons.Default.EmojiEvents,
                        onClick = { onNavigate("my_matches") }
                    )
                    MeMenuRow(
                        title = "Notifications",
                        icon = Icons.Default.Notifications,
                        onClick = { showNotifications = true }
                    )
                    MeMenuRow(
                        title = "Refer & Earn",
                        icon = Icons.Default.CardGiftcard,
                        onClick = { onNavigate("refer") },
                        modifier = Modifier.testTag("profile_refer_btn")
                    )
                    MeMenuRow(
                        title = "Contact Us",
                        icon = Icons.Default.SupportAgent,
                        onClick = { onNavigate("support") },
                        modifier = Modifier.testTag("profile_support_btn")
                    )

                    // Importance Notice Switch Row
                    Surface(
                        color = Color.White,
                        shape = RoundedCornerShape(10.dp),
                        shadowElevation = 1.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.NotificationsActive,
                                    contentDescription = null,
                                    tint = ArenaBlue,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(Modifier.width(14.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Importance Notice",
                                        color = CardMutedText,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = if (hasNotifPerm) "Mobile shutter alerts enabled" else "Tap to grant notification permission",
                                        color = Color(0xFF94A3B8),
                                        fontSize = 11.sp
                                    )
                                }
                                Switch(
                                    checked = pushEnabled,
                                    onCheckedChange = { checked ->
                                        pushEnabled = checked
                                        onTogglePush(checked)
                                        if (checked && !hasNotifPerm && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                            permLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                        }
                                    },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = ArenaBlue,
                                        checkedTrackColor = ArenaBlue.copy(alpha = 0.3f)
                                    )
                                )
                            }
                            if (pushEnabled) {
                                OutlinedButton(
                                    onClick = {
                                        if (!hasNotifPerm && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                            permLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                        } else {
                                            onSendTestPush()
                                        }
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(1.dp, ArenaBlue.copy(alpha = 0.45f)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 6.dp, bottom = 4.dp)
                                        .testTag("test_shutter_notification_btn")
                                ) {
                                    Text("Test Notification on My Phone", color = ArenaBlue, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    MeMenuRow(
                        title = "FAQ",
                        icon = Icons.AutoMirrored.Filled.HelpOutline,
                        onClick = {
                            infoDialogTitle = "Frequently Asked Questions"
                            infoDialogBody = "1. How do I join a match?\nAdd coins via In-App QR in My Wallet, pick an upcoming match, choose your slot number, and confirm your IGN & UID.\n\n2. When do I get Room ID & Password?\n15 minutes before match start inside Match Details and My Matches.\n\n3. How are winnings redeemed?\nGo to Wallet > Redeem Coins via UPI or Binance BEP-20 USDT."
                        }
                    )
                    MeMenuRow(
                        title = "About Us",
                        icon = Icons.Default.Info,
                        onClick = {
                            infoDialogTitle = "About Ahxan Arena"
                            infoDialogBody = "Ahxan Arena is a competitive esports tournament platform hosting daily BGMI, Free Fire Max, CODM, and Valorant scrims with verified slot management and instant wallet payouts."
                        }
                    )
                    MeMenuRow(
                        title = "Privacy Policy",
                        icon = Icons.Default.Lock,
                        onClick = {
                            infoDialogTitle = "Privacy Policy"
                            infoDialogBody = "We store only your profile identity (Name, IGN, Game UID, Phone, Email) and tournament ledger securely in Google Firebase Firestore. We never sell player information to third parties."
                        }
                    )
                    MeMenuRow(
                        title = "Terms & Conditions",
                        icon = Icons.Default.VerifiedUser,
                        onClick = {
                            infoDialogTitle = "Terms & Conditions"
                            infoDialogBody = generalConfig.rulesText.ifBlank {
                                "Fair play is mandatory. Hacking, teaming, or emulator spoofing leads to permanent account suspension."
                            }
                        }
                    )
                    MeMenuRow(
                        title = "Share App",
                        icon = Icons.Default.Share,
                        onClick = {
                            val code = user?.referralCode.orEmpty()
                            val shareText = "Join me on Ahxan Arena Esports Tournaments! Use my referral code $code to get bonus coins."
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, shareText)
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Share Ahxan Arena"))
                        }
                    )
                    MeMenuRow(
                        title = "Logout",
                        icon = Icons.Default.PowerSettingsNew,
                        onClick = { AuthHelper.signOut(credentialManager, scope) },
                        modifier = Modifier.testTag("sign_out_btn")
                    )
                }
            }

            item {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "App Version : v1.0",
                        color = CardMutedText,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }

    if (showNotifications) {
        NotificationDrawerSheet(
            notifications = notifications,
            broadcasts = broadcasts,
            onMarkNotificationRead = onMarkNotificationRead,
            onNavigate = onNavigate,
            onDismiss = { showNotifications = false }
        )
    }

    if (infoDialogTitle != null) {
        AlertDialog(
            onDismissRequest = { infoDialogTitle = null },
            containerColor = ObsidianSurface,
            title = { Text(infoDialogTitle!!, style = MaterialTheme.typography.titleLarge, color = CoinYellow) },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text(infoDialogBody, color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
                    if (infoDialogTitle == "About Us" && (generalConfig.telegramUrl.isNotBlank() || generalConfig.discordUrl.isNotBlank())) {
                        Spacer(Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (generalConfig.telegramUrl.isNotBlank()) {
                                OutlinedButton(onClick = { runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(generalConfig.telegramUrl))) } }) {
                                    Text("Telegram", color = ArenaBlue)
                                }
                            }
                            if (generalConfig.discordUrl.isNotBlank()) {
                                OutlinedButton(onClick = { runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(generalConfig.discordUrl))) } }) {
                                    Text("Discord", color = ArenaPurple)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { infoDialogTitle = null }) {
                    Text("Close", color = CoinYellow)
                }
            }
        )
    }
}

@Composable
private fun MeStatColumn(
    value: String,
    label: String,
    showCoin: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (showCoin) {
                CoinBadge(size = 18.dp)
                Spacer(Modifier.width(5.dp))
            }
            Text(
                text = value,
                color = ArenaBlue,
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = label,
            color = CardMutedText,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun MeMenuRow(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = Color.White,
        shape = RoundedCornerShape(10.dp),
        shadowElevation = 1.dp,
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = ArenaBlue,
                modifier = Modifier.size(22.dp)
            )
            Spacer(Modifier.width(14.dp))
            Text(
                text = title,
                color = CardMutedText,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = CardMutedText
            )
        }
    }
}

/**
 * Dedicated Edit Profile Screen matching Screenshots 155421 & 155422.
 */
@Composable
fun EditProfileScreen(
    user: UserProfile?,
    busy: Boolean,
    onSaveProfile: (String, String, String, String, Boolean) -> Unit,
    onBack: () -> Unit
) {
    var displayName by remember(user?.displayName) { mutableStateOf(user?.displayName.orEmpty()) }
    var inGameName by remember(user?.inGameName) { mutableStateOf(user?.inGameName.orEmpty()) }
    var gameUid by remember(user?.gameUid) { mutableStateOf(user?.gameUid.orEmpty()) }
    var phoneNumber by remember(user?.phoneNumber) {
        mutableStateOf(
            user?.phoneNumber.orEmpty().removePrefix("+91").trim()
        )
    }
    val pushEnabled = user?.pushEnabled ?: true

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = Color.White,
        unfocusedTextColor = Color.White,
        focusedBorderColor = Color(0xFF94A3B8),
        unfocusedBorderColor = Color(0xFF64748B),
        focusedLabelColor = Color(0xFFCBD5E1),
        unfocusedLabelColor = Color(0xFF94A3B8),
        disabledTextColor = Color(0xFFCBD5E1),
        disabledBorderColor = Color(0xFF475569),
        disabledLabelColor = Color(0xFF94A3B8)
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(HeaderNavy)
    ) {
        SubScreenTopBar(
            title = "My Profile",
            containerColor = HeaderNavy,
            onBack = onBack
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // Top Dark Navy Avatar + 3-Col Stats
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(HeaderNavy)
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(88.dp)
                        .clip(CircleShape)
                        .background(CoinYellow),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Profile Avatar",
                        tint = HeaderNavy,
                        modifier = Modifier.size(54.dp)
                    )
                }

                Spacer(Modifier.height(18.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "${user?.matchesPlayed ?: 0}",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Spacer(Modifier.height(3.dp))
                        Text("Matches Played", color = Color(0xFFCBD5E1), fontSize = 11.sp)
                    }
                    VerticalDivider(modifier = Modifier.height(32.dp), color = Color(0xFF475569))
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "${user?.totalKills ?: 0}",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Spacer(Modifier.height(3.dp))
                        Text("Total Kills", color = Color(0xFFCBD5E1), fontSize = 11.sp)
                    }
                    VerticalDivider(modifier = Modifier.height(32.dp), color = Color(0xFF475569))
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CoinBadge(size = 18.dp)
                            Spacer(Modifier.width(5.dp))
                            Text(
                                text = "${(user?.totalWinnings ?: 0.0).toLong()}",
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                        Spacer(Modifier.height(3.dp))
                        Text("PlayCoin Won", color = Color(0xFFCBD5E1), fontSize = 11.sp)
                    }
                }
            }

            // Bottom Dark Charcoal Rounded Edit Form (matching Screenshots 155421 / 155422)
            Surface(
                color = ArenaCharcoal,
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 22.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Edit Profile",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    OutlinedTextField(
                        value = displayName,
                        onValueChange = { displayName = it },
                        label = { Text("Player Name") },
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp),
                        colors = fieldColors,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = inGameName,
                        onValueChange = { inGameName = it },
                        label = { Text("In-Game Name (IGN)") },
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp),
                        colors = fieldColors,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = gameUid,
                        onValueChange = { gameUid = it },
                        label = { Text("Game UID") },
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp),
                        colors = fieldColors,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .width(72.dp)
                                .height(56.dp)
                                .padding(top = 6.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .border(1.dp, Color(0xFF64748B), RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("+91", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                        }

                        OutlinedTextField(
                            value = phoneNumber,
                            onValueChange = { phoneNumber = it.filter { ch -> ch.isDigit() || ch == '+' }.take(15) },
                            label = { Text("Phone Number") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            shape = RoundedCornerShape(8.dp),
                            colors = fieldColors,
                            modifier = Modifier.weight(1f).testTag("profile_phone_input")
                        )
                    }

                    OutlinedTextField(
                        value = user?.email.orEmpty(),
                        onValueChange = {},
                        enabled = false,
                        label = { Text("Email") },
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp),
                        colors = fieldColors,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(6.dp))

                    Button(
                        onClick = {
                            val cleanPhone = if (phoneNumber.isNotBlank() && !phoneNumber.startsWith("+")) {
                                phoneNumber
                            } else phoneNumber
                            onSaveProfile(displayName, inGameName, gameUid, cleanPhone, pushEnabled)
                        },
                        enabled = !busy,
                        colors = ButtonDefaults.buttonColors(containerColor = ArenaPurple, contentColor = Color.White),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("save_profile_btn")
                    ) {
                        Text(
                            text = if (busy) "SAVING..." else "SAVE",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                    Spacer(Modifier.height(24.dp))
                }
            }
        }
    }
}
