package com.example.ui.screens.admin

import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.model.AuditLog
import com.example.data.model.CmsBanner
import com.example.data.model.GeneralConfig
import com.example.data.model.StaffRole
import com.example.data.model.UserProfile
import com.example.ui.components.CyberGlassCard
import com.example.ui.components.NeonPrimaryButton
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatusPill
import com.example.ui.components.bannerDrawableForKey
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CrimsonDanger
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.EmeraldProfit
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.utils.ArenaFormatters
import com.example.utils.QrCodeUtils
import com.example.viewmodel.AdminViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminUsersCrmModule(
    users: List<UserProfile>,
    busy: Boolean,
    adminVm: AdminViewModel
) {
    var query by remember { mutableStateOf("") }
    var selectedUser by remember { mutableStateOf<UserProfile?>(null) }

    val filtered = remember(users, query) {
        if (query.isBlank()) users else users.filter {
            it.displayName.contains(query, true) ||
                it.email.contains(query, true) ||
                it.userId.contains(query, true) ||
                it.referralCode.contains(query, true)
        }
    }

    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            SectionHeader("User 360° CRM & Ban Control", "${users.size} players")
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text("Search name, email, UID, or referral code") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }
        items(filtered, key = { it.userId }) { u ->
            CyberGlassCard(glowColor = if (u.banned) CrimsonDanger else NeonCyan, onClick = { selectedUser = u }) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(u.displayName, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                        Text("${u.email} • ${u.referralCode}", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                        Text("Bal: ${ArenaFormatters.formatInr(u.totalBalance)} • Matches: ${u.matchesPlayed}", style = MaterialTheme.typography.labelSmall, color = EmeraldProfit)
                    }
                    StatusPill(if (u.banned) "BANNED" else u.role.uppercase(), if (u.banned) CrimsonDanger else NeonCyan)
                }
            }
        }
    }

    selectedUser?.let { u ->
        var bucket by remember(u.userId) { mutableStateOf("deposit") }
        var isCredit by remember(u.userId) { mutableStateOf(true) }
        var amount by remember(u.userId) { mutableStateOf("") }
        var reason by remember(u.userId) { mutableStateOf("") }
        var banReason by remember(u.userId) { mutableStateOf(u.banReason.ifBlank { "Fair-play violation" }) }

        ModalBottomSheet(onDismissRequest = { selectedUser = null }, containerColor = ObsidianSurface) {
            Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp)) {
                Text("User 360° • ${u.displayName}", style = MaterialTheme.typography.headlineSmall, color = NeonCyan)
                Text("UID: ${u.userId}\nEmail: ${u.email}\nIGN: ${u.inGameName} | Game UID: ${u.gameUid}", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                Spacer(Modifier.height(8.dp))
                Text("Deposit: ${ArenaFormatters.formatInr(u.depositBalance)} | Winning: ${ArenaFormatters.formatInr(u.winningBalance)} | Bonus: ${ArenaFormatters.formatInr(u.bonusBalance)}", style = MaterialTheme.typography.labelLarge, color = EmeraldProfit)

                Spacer(Modifier.height(12.dp))
                Text("Manual Wallet Credit / Debit", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(selected = isCredit, onClick = { isCredit = true }, label = { Text("Credit (+)") })
                    FilterChip(selected = !isCredit, onClick = { isCredit = false }, label = { Text("Debit (-)") })
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("deposit", "winning", "bonus").forEach { b ->
                        FilterChip(selected = bucket == b, onClick = { bucket = b }, label = { Text(b) })
                    }
                }
                OutlinedTextField(value = amount, onValueChange = { amount = it }, label = { Text("Amount ₹") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(value = reason, onValueChange = { reason = it }, label = { Text("Mandatory Audit Reason") }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                NeonPrimaryButton(
                    text = if (busy) "Applying..." else "Execute Wallet Adjustment",
                    enabled = !busy && (amount.toDoubleOrNull() ?: 0.0) > 0.0 && reason.isNotBlank(),
                    onClick = {
                        adminVm.adjustUserWallet(u.userId, bucket, isCredit, amount.toDoubleOrNull() ?: 0.0, reason)
                        selectedUser = null
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(14.dp))
                OutlinedTextField(value = banReason, onValueChange = { banReason = it }, label = { Text("Ban / Unban Reason") }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = {
                        adminVm.toggleBanUser(u.userId, !u.banned, banReason)
                        selectedUser = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = if (u.banned) EmeraldProfit else CrimsonDanger, contentColor = if (u.banned) ObsidianBg else TextPrimary),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (u.banned) "Unban Player" else "Ban Player Account")
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminStaffRolesModule(
    roles: List<StaffRole>,
    isSuperAdmin: Boolean,
    busy: Boolean,
    adminVm: AdminViewModel
) {
    var editingRole by remember { mutableStateOf<StaffRole?>(null) }

    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            SectionHeader("Sub-Admin & Staff RBAC", if (isSuperAdmin) "Super Admin Access" else "Read-only for staff")
            if (isSuperAdmin) {
                Spacer(Modifier.height(8.dp))
                NeonPrimaryButton(
                    text = "Add Staff Role",
                    icon = Icons.Default.Add,
                    onClick = { editingRole = StaffRole() },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        items(roles, key = { it.userId }) { r ->
            CyberGlassCard(glowColor = ElectricViolet) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("${r.name.ifBlank { r.email }} (${r.roleLabel})", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                        Text("UID: ${r.userId} • ${r.email}", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                    }
                    if (isSuperAdmin) {
                        Row {
                            OutlinedButton(onClick = { editingRole = r }) { Text("Edit", color = NeonCyan) }
                            IconButton(onClick = { adminVm.removeStaffRole(r.userId) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Remove Role", tint = CrimsonDanger)
                            }
                        }
                    }
                }
            }
        }
    }

    editingRole?.let { init ->
        var uid by remember(init.userId) { mutableStateOf(init.userId) }
        var email by remember(init.userId) { mutableStateOf(init.email) }
        var name by remember(init.userId) { mutableStateOf(init.name) }
        var label by remember(init.userId) { mutableStateOf(init.roleLabel) }
        var pMatches by remember(init.userId) { mutableStateOf(init.manageMatches) }
        var pRooms by remember(init.userId) { mutableStateOf(init.publishRooms) }
        var pDeposits by remember(init.userId) { mutableStateOf(init.verifyDeposits) }
        var pWithdraws by remember(init.userId) { mutableStateOf(init.approveWithdrawals) }
        var pCoupons by remember(init.userId) { mutableStateOf(init.manageCoupons) }
        var pTickets by remember(init.userId) { mutableStateOf(init.replyTickets) }
        var pPush by remember(init.userId) { mutableStateOf(init.sendPush) }
        var pUsers by remember(init.userId) { mutableStateOf(init.manageUsers) }
        var pSettings by remember(init.userId) { mutableStateOf(init.systemSettings) }

        ModalBottomSheet(onDismissRequest = { editingRole = null }, containerColor = ObsidianSurface) {
            Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp)) {
                Text("Configure Staff Permissions", style = MaterialTheme.typography.headlineSmall, color = ElectricViolet)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = uid, onValueChange = { uid = it }, label = { Text("Firebase User UID") }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Staff Email") }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") }, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = label, onValueChange = { label = it }, label = { Text("Role Label") }, modifier = Modifier.weight(1f))
                }
                Spacer(Modifier.height(8.dp))
                listOf(
                    "Manage Matches" to (pMatches to { v: Boolean -> pMatches = v }),
                    "Publish Room IDs" to (pRooms to { v: Boolean -> pRooms = v }),
                    "Verify PayZapi Deposits" to (pDeposits to { v: Boolean -> pDeposits = v }),
                    "Approve Withdrawals" to (pWithdraws to { v: Boolean -> pWithdraws = v }),
                    "Manage Coupons" to (pCoupons to { v: Boolean -> pCoupons = v }),
                    "Reply Support Tickets" to (pTickets to { v: Boolean -> pTickets = v }),
                    "Send Push Broadcasts" to (pPush to { v: Boolean -> pPush = v }),
                    "Manage Users & Bans" to (pUsers to { v: Boolean -> pUsers = v }),
                    "CMS & System Settings" to (pSettings to { v: Boolean -> pSettings = v })
                ).forEach { (title, pair) ->
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(title, color = TextPrimary)
                        Switch(checked = pair.first, onCheckedChange = pair.second)
                    }
                }
                Spacer(Modifier.height(10.dp))
                NeonPrimaryButton(
                    text = if (busy) "Saving..." else "Save Staff Role",
                    enabled = !busy && uid.isNotBlank() && email.isNotBlank(),
                    onClick = {
                        adminVm.saveStaffRole(
                            StaffRole(
                                userId = uid,
                                email = email,
                                name = name,
                                roleLabel = label,
                                manageMatches = pMatches,
                                publishRooms = pRooms,
                                verifyDeposits = pDeposits,
                                approveWithdrawals = pWithdraws,
                                manageCoupons = pCoupons,
                                replyTickets = pTickets,
                                sendPush = pPush,
                                manageUsers = pUsers,
                                systemSettings = pSettings
                            )
                        )
                        editingRole = null
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminCmsEngagementModule(
    generalConfig: GeneralConfig,
    banners: List<CmsBanner>,
    busy: Boolean,
    adminVm: AdminViewModel
) {
    val context = LocalContext.current
    var ticker by remember(generalConfig) { mutableStateOf(generalConfig.tickerText) }
    var maintMode by remember(generalConfig) { mutableStateOf(generalConfig.maintenanceMode) }
    var maintMsg by remember(generalConfig) { mutableStateOf(generalConfig.maintenanceMessage) }
    var maintEta by remember(generalConfig) { mutableStateOf(generalConfig.maintenanceEta) }
    var checkinEnabled by remember(generalConfig) { mutableStateOf(generalConfig.checkinEnabled) }
    var checkinCsv by remember(generalConfig) { mutableStateOf(generalConfig.checkinRewards.joinToString(",")) }
    var bonusPct by remember(generalConfig) { mutableStateOf(generalConfig.bonusUsablePercent.toString()) }
    var rulesText by remember(generalConfig) { mutableStateOf(generalConfig.rulesText) }
    var tgUrl by remember(generalConfig) { mutableStateOf(generalConfig.telegramUrl) }
    var dcUrl by remember(generalConfig) { mutableStateOf(generalConfig.discordUrl) }
    var matchesHeroUrl by remember(generalConfig) { mutableStateOf(generalConfig.matchesHeroBannerUrl) }
    var matchesHeroKey by remember(generalConfig) { mutableStateOf(generalConfig.matchesHeroBannerKey) }
    var matchesHeroTitle by remember(generalConfig) { mutableStateOf(generalConfig.matchesHeroTitle) }
    var matchesHeroSub by remember(generalConfig) { mutableStateOf(generalConfig.matchesHeroSubtitle) }
    var editingBanner by remember { mutableStateOf<CmsBanner?>(null) }

    val heroPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    BitmapFactory.decodeStream(stream)
                }
            }.getOrNull()?.let { bmp ->
                matchesHeroUrl = QrCodeUtils.compressBitmapToBase64(bmp, maxWidth = 720, quality = 65)
            }
        }
    }

    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { SectionHeader("CMS, Engagement & Maintenance") }
        item {
            CyberGlassCard(glowColor = ElectricViolet) {
                SectionHeader("Matches Top Hero Image", "Shown right above all tournaments")
                Spacer(Modifier.height(8.dp))
                val previewBitmap = remember(matchesHeroUrl) {
                    val trimmed = matchesHeroUrl.trim()
                    if (trimmed.isNotEmpty() && !trimmed.startsWith("http", ignoreCase = true)) {
                        QrCodeUtils.decodeBase64Image(trimmed)
                    } else null
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .border(1.dp, NeonCyan.copy(alpha = 0.45f), RoundedCornerShape(14.dp))
                ) {
                    val trimmed = matchesHeroUrl.trim()
                    when {
                        trimmed.startsWith("http", ignoreCase = true) -> {
                            AsyncImage(model = trimmed, contentDescription = "Matches Banner Preview", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                        }
                        previewBitmap != null -> {
                            Image(bitmap = previewBitmap.asImageBitmap(), contentDescription = "Matches Banner Preview", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                        }
                        else -> {
                            Image(painter = painterResource(id = bannerDrawableForKey(matchesHeroKey)), contentDescription = "Matches Banner Preview", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(
                        onClick = { heroPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                        modifier = Modifier.weight(1f).testTag("admin_pick_matches_hero_btn")
                    ) {
                        Icon(Icons.Default.Image, contentDescription = null, tint = NeonCyan)
                        Spacer(Modifier.width(6.dp))
                        Text("Pick from Gallery", color = NeonCyan)
                    }
                    if (matchesHeroUrl.isNotBlank()) {
                        OutlinedButton(onClick = { matchesHeroUrl = "" }) {
                            Text("Reset Preset", color = AmberWarning)
                        }
                    }
                }
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    value = if (matchesHeroUrl.startsWith("http", ignoreCase = true) || matchesHeroUrl.isBlank()) matchesHeroUrl else "[Custom Gallery Image Uploaded]",
                    onValueChange = { matchesHeroUrl = it },
                    label = { Text("Or Paste Image URL (https://...)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(6.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(listOf("bgmi", "freefire", "codm", "hero")) { k ->
                        FilterChip(
                            selected = matchesHeroUrl.isBlank() && matchesHeroKey == k,
                            onClick = {
                                matchesHeroUrl = ""
                                matchesHeroKey = k
                            },
                            label = { Text(k.uppercase()) }
                        )
                    }
                }
                OutlinedTextField(value = matchesHeroTitle, onValueChange = { matchesHeroTitle = it }, label = { Text("Matches Banner Title") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(value = matchesHeroSub, onValueChange = { matchesHeroSub = it }, label = { Text("Matches Banner Subtitle") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            }
        }
        item {
            CyberGlassCard(glowColor = if (maintMode) CrimsonDanger else NeonCyan) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Global Maintenance Mode", style = MaterialTheme.typography.titleMedium, color = if (maintMode) CrimsonDanger else TextPrimary)
                    Switch(checked = maintMode, onCheckedChange = { maintMode = it })
                }
                if (maintMode) {
                    OutlinedTextField(value = maintMsg, onValueChange = { maintMsg = it }, label = { Text("Maintenance Banner") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = maintEta, onValueChange = { maintEta = it }, label = { Text("ETA") }, modifier = Modifier.fillMaxWidth())
                }
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(value = ticker, onValueChange = { ticker = it }, label = { Text("Home Notice Ticker") }, modifier = Modifier.fillMaxWidth())
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Daily Check-In Streak Enabled", color = TextPrimary)
                    Switch(checked = checkinEnabled, onCheckedChange = { checkinEnabled = it })
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = checkinCsv, onValueChange = { checkinCsv = it }, label = { Text("Day 1-7 Rewards CSV") }, modifier = Modifier.weight(1.4f))
                    OutlinedTextField(value = bonusPct, onValueChange = { bonusPct = it }, label = { Text("Bonus Use %") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                }
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(value = tgUrl, onValueChange = { tgUrl = it }, label = { Text("Telegram URL") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = dcUrl, onValueChange = { dcUrl = it }, label = { Text("Discord URL") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = rulesText, onValueChange = { rulesText = it }, label = { Text("Global Rules Text") }, minLines = 2, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(10.dp))
                NeonPrimaryButton(
                    text = if (busy) "Saving..." else "Save CMS & App Controls",
                    enabled = !busy,
                    onClick = {
                        val parsedRewards = checkinCsv.split(",").mapNotNull { it.trim().toLongOrNull() }.ifEmpty { listOf(5L, 8L, 10L, 12L, 15L, 20L, 50L) }
                        adminVm.saveGeneralConfig(
                            generalConfig.copy(
                                tickerText = ticker,
                                maintenanceMode = maintMode,
                                maintenanceMessage = maintMsg,
                                maintenanceEta = maintEta,
                                checkinEnabled = checkinEnabled,
                                checkinRewards = parsedRewards,
                                bonusUsablePercent = bonusPct.toLongOrNull() ?: 10L,
                                telegramUrl = tgUrl,
                                discordUrl = dcUrl,
                                rulesText = rulesText,
                                matchesHeroBannerUrl = matchesHeroUrl.trim(),
                                matchesHeroBannerKey = matchesHeroKey,
                                matchesHeroTitle = matchesHeroTitle.trim(),
                                matchesHeroSubtitle = matchesHeroSub.trim()
                            )
                        )
                    },
                    modifier = Modifier.fillMaxWidth().testTag("save_cms_controls_btn")
                )
            }
        }
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                SectionHeader("Promo Banners (${banners.size})")
                OutlinedButton(onClick = { editingBanner = CmsBanner() }) { Text("+ Banner", color = NeonCyan) }
            }
        }
        items(banners, key = { it.id }) { b ->
            CyberGlassCard(glowColor = ElectricViolet) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(b.title, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                        Text("${b.subtitle} • Route: ${b.redirectRoute}", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                    }
                    IconButton(onClick = { adminVm.deleteBanner(b.id) }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete Banner", tint = CrimsonDanger)
                    }
                }
            }
        }
    }

    editingBanner?.let { init ->
        var bTitle by remember { mutableStateOf(init.title) }
        var bSub by remember { mutableStateOf(init.subtitle) }
        var bUrl by remember { mutableStateOf(init.imageUrl) }
        var bKey by remember { mutableStateOf(init.drawableKey) }
        var bRoute by remember { mutableStateOf(init.redirectRoute) }

        ModalBottomSheet(onDismissRequest = { editingBanner = null }, containerColor = ObsidianSurface) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Text("Add Promo Banner", style = MaterialTheme.typography.headlineSmall, color = NeonCyan)
                OutlinedTextField(value = bTitle, onValueChange = { bTitle = it }, label = { Text("Title") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = bSub, onValueChange = { bSub = it }, label = { Text("Subtitle") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = bUrl, onValueChange = { bUrl = it }, label = { Text("Image URL (Optional)") }, modifier = Modifier.fillMaxWidth())
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(listOf("bgmi", "freefire", "codm", "hero")) { k -> FilterChip(selected = bKey == k, onClick = { bKey = k }, label = { Text(k) }) }
                }
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(listOf("tournaments", "wallet", "refer", "support")) { r -> FilterChip(selected = bRoute == r, onClick = { bRoute = r }, label = { Text(r) }) }
                }
                Spacer(Modifier.height(10.dp))
                NeonPrimaryButton(
                    text = "Save Banner",
                    enabled = bTitle.isNotBlank(),
                    onClick = {
                        adminVm.saveBanner(CmsBanner(title = bTitle, subtitle = bSub, imageUrl = bUrl, drawableKey = bKey, redirectRoute = bRoute, active = true, sortOrder = banners.size.toLong() + 1L))
                        editingBanner = null
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(20.dp))
            }
        }
    }
}

@Composable
fun AdminAuditLogsModule(logs: List<AuditLog>) {
    var actionFilter by remember { mutableStateOf("") }
    val filtered = remember(logs, actionFilter) {
        if (actionFilter.isBlank()) logs else logs.filter { it.actionType.contains(actionFilter, true) || it.summary.contains(actionFilter, true) }
    }
    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            SectionHeader("Immutable Admin Audit Logs", "${logs.size} recorded actions")
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = actionFilter,
                onValueChange = { actionFilter = it },
                label = { Text("Filter by action or summary") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }
        items(filtered, key = { it.id }) { log ->
            CyberGlassCard(glowColor = AmberWarning) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    StatusPill(log.actionType, NeonCyan)
                    Text(log.adminEmail, style = MaterialTheme.typography.labelSmall, color = TextMuted)
                }
                Spacer(Modifier.height(6.dp))
                Text(log.summary, style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                if (log.oldValue.isNotBlank() || log.newValue.isNotBlank()) {
                    Text("${log.oldValue} -> ${log.newValue}", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                }
            }
        }
    }
}
