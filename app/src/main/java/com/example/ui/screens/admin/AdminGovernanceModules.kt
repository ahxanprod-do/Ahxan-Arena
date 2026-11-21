package com.example.ui.screens.admin

import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.BuildConfig
import com.example.data.model.AppUpdateConfig
import com.example.data.model.AuditLog
import com.example.data.model.CmsBanner
import com.example.data.model.GameCategory
import com.example.data.model.GeneralConfig
import com.example.data.model.StaffRole
import com.example.data.model.UserProfile
import com.example.ui.components.CoinBadge
import com.example.ui.components.CyberGlassCard
import com.example.ui.components.KpiMiniStat
import com.example.ui.components.MatchBannerImage
import com.example.ui.components.NeonPrimaryButton
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatusPill
import com.example.ui.components.bannerDrawableForKey
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.ArenaBlue
import com.example.ui.theme.ArenaCharcoal
import com.example.ui.theme.ArenaGreen
import com.example.ui.theme.ArenaOrange
import com.example.ui.theme.ArenaPurple
import com.example.ui.theme.CoinGold
import com.example.ui.theme.CoinYellow
import com.example.ui.theme.CrimsonDanger
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.EmeraldProfit
import com.example.ui.theme.HeaderNavy
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.ObsidianElevated
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.utils.ArenaFormatters
import com.example.utils.QrCodeUtils
import com.example.viewmodel.AdminViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AdminUsersCrmModule(
    users: List<UserProfile>,
    busy: Boolean,
    adminVm: AdminViewModel
) {
    var query by remember { mutableStateOf("") }
    var filterTab by remember { mutableStateOf("All") }
    var sortBy by remember { mutableStateOf("Highest Coins") }
    var selectedUser by remember { mutableStateOf<UserProfile?>(null) }
    var coinTargetUser by remember { mutableStateOf<UserProfile?>(null) }
    var showGlobalCoinPicker by remember { mutableStateOf(false) }
    var showBulkDropSheet by remember { mutableStateOf(false) }

    val totalCoinsInWallets = remember(users) { users.sumOf { it.totalBalance } }
    val bannedCount = remember(users) { users.count { it.banned } }

    val filtered = remember(users, query, filterTab, sortBy) {
        val q = query.trim()
        val base = users.filter { u ->
            val matchesQuery = q.isBlank() ||
                u.displayName.contains(q, true) ||
                u.email.contains(q, true) ||
                u.inGameName.contains(q, true) ||
                u.gameUid.contains(q, true) ||
                u.phoneNumber.contains(q, true) ||
                u.userId.contains(q, true) ||
                u.referralCode.contains(q, true)
            val matchesTab = when (filterTab) {
                "With Coins" -> u.totalBalance > 0.0
                "Zero Bal" -> u.totalBalance <= 0.0
                "Banned" -> u.banned
                "Admin/Staff" -> u.role.equals("admin", true) || u.role.equals("staff", true)
                else -> true
            }
            matchesQuery && matchesTab
        }
        when (sortBy) {
            "Highest Coins" -> base.sortedByDescending { it.totalBalance }
            "Matches" -> base.sortedByDescending { it.matchesPlayed }
            else -> base.sortedBy { it.displayName.lowercase() }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            SectionHeader("All Users & Coin Control", "${filtered.size} of ${users.size} registered players")
            Spacer(Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                KpiMiniStat("All Users", "${users.size}", ArenaOrange, Modifier.weight(1f))
                KpiMiniStat("Wallet Coins", "${totalCoinsInWallets.toInt()} C", CoinYellow, Modifier.weight(1f))
                KpiMiniStat("Banned", "$bannedCount", CrimsonDanger, Modifier.weight(1f))
            }
            Spacer(Modifier.height(10.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NeonPrimaryButton(
                    text = "+ Add Coin to User",
                    icon = Icons.Default.AccountBalanceWallet,
                    accentColor = ArenaGreen,
                    onClick = { showGlobalCoinPicker = true },
                    modifier = Modifier.weight(1f).testTag("admin_users_quick_add_coin_btn")
                )
                NeonPrimaryButton(
                    text = "Bulk Coin Drop",
                    icon = Icons.Default.CardGiftcard,
                    accentColor = ArenaPurple,
                    onClick = { showBulkDropSheet = true },
                    modifier = Modifier.weight(1f).testTag("admin_bulk_airdrop_btn")
                )
            }
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text("Search name, email, IGN, game UID, phone or code") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("admin_user_search_input")
            )
            Spacer(Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(listOf("All", "With Coins", "Zero Bal", "Banned", "Admin/Staff")) { tab ->
                    FilterChip(
                        selected = filterTab == tab,
                        onClick = { filterTab = tab },
                        label = { Text(tab) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = ArenaOrange,
                            selectedLabelColor = Color.White,
                            containerColor = ObsidianElevated
                        )
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Sort:", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                listOf("Highest Coins", "Matches", "A-Z").forEach { s ->
                    FilterChip(
                        selected = sortBy == s,
                        onClick = { sortBy = s },
                        label = { Text(s, fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = ArenaPurple,
                            selectedLabelColor = Color.White,
                            containerColor = ObsidianElevated
                        )
                    )
                }
            }
        }

        if (filtered.isEmpty()) {
            item {
                CyberGlassCard(glowColor = ArenaOrange) {
                    Text(
                        text = if (users.isEmpty()) "Loading registered players from Firestore..." else "No users matched your search/filter.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
            }
        }

        items(filtered, key = { it.userId }) { u ->
            CyberGlassCard(glowColor = if (u.banned) CrimsonDanger else ArenaOrange) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(if (u.banned) CrimsonDanger.copy(alpha = 0.25f) else ArenaPurple.copy(alpha = 0.35f))
                                .border(1.5.dp, if (u.banned) CrimsonDanger else ArenaOrange, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = u.displayName.ifBlank { u.email }.take(1).uppercase(),
                                color = Color.White,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 16.sp
                            )
                        }
                        Spacer(Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = u.displayName.ifBlank { "Unnamed Player" },
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = u.email.ifBlank { u.userId },
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    Spacer(Modifier.width(6.dp))
                    Surface(
                        color = CoinYellow.copy(alpha = 0.16f),
                        shape = RoundedCornerShape(50),
                        border = BorderStroke(1.dp, CoinGold.copy(alpha = 0.7f))
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            CoinBadge(size = 15.dp)
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = "${u.totalBalance.toInt()}",
                                color = CoinYellow,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Dep: ${u.depositBalance.toInt()} • Win: ${u.winningBalance.toInt()} • Bonus: ${u.bonusBalance.toInt()}",
                        style = MaterialTheme.typography.labelSmall,
                        color = ArenaGreen
                    )
                    StatusPill(
                        if (u.banned) "BANNED" else u.role.uppercase(),
                        if (u.banned) CrimsonDanger else ArenaBlue
                    )
                }
                Text(
                    text = "IGN: ${u.inGameName.ifBlank { "-" }} • Game UID: ${u.gameUid.ifBlank { "-" }} • WhatsApp: ${u.phoneNumber.ifBlank { "-" }} • Matches: ${u.matchesPlayed}",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { coinTargetUser = u },
                        colors = ButtonDefaults.buttonColors(containerColor = ArenaGreen, contentColor = Color.White),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f).testTag("user_add_coin_${u.userId}")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Add Coin", fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(
                        onClick = { selectedUser = u },
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, ArenaOrange.copy(alpha = 0.7f)),
                        modifier = Modifier.weight(1f).testTag("user_manage_${u.userId}")
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, tint = ArenaOrange, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Manage 360°", color = ArenaOrange, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }

    if (showGlobalCoinPicker || coinTargetUser != null) {
        AdminCoinAdjustmentSheet(
            initialUser = coinTargetUser,
            allUsers = users,
            busy = busy,
            adminVm = adminVm,
            onDismiss = {
                coinTargetUser = null
                showGlobalCoinPicker = false
            }
        )
    }

    if (showBulkDropSheet) {
        BulkCoinAirdropSheet(
            users = users,
            busy = busy,
            adminVm = adminVm,
            onDismiss = { showBulkDropSheet = false }
        )
    }

    selectedUser?.let { u ->
        var editName by remember(u.userId) { mutableStateOf(u.displayName) }
        var editIgn by remember(u.userId) { mutableStateOf(u.inGameName) }
        var editGameUid by remember(u.userId) { mutableStateOf(u.gameUid) }
        var editPhone by remember(u.userId) { mutableStateOf(u.phoneNumber) }
        var directTitle by remember(u.userId) { mutableStateOf("") }
        var directBody by remember(u.userId) { mutableStateOf("") }
        var banReason by remember(u.userId) { mutableStateOf(u.banReason.ifBlank { "Fair-play violation" }) }

        ModalBottomSheet(onDismissRequest = { selectedUser = null }, containerColor = ObsidianSurface) {
            Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp)) {
                Text("User 360° • ${u.displayName}", style = MaterialTheme.typography.headlineSmall, color = ArenaOrange)
                Text("Doc ID: ${u.userId}\nEmail: ${u.email} • Ref: ${u.referralCode}", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                Spacer(Modifier.height(8.dp))
                Text(
                    "Total: ${u.totalBalance.toInt()} Coins (Dep: ${u.depositBalance.toInt()} | Win: ${u.winningBalance.toInt()} | Bonus: ${u.bonusBalance.toInt()})",
                    style = MaterialTheme.typography.labelLarge,
                    color = EmeraldProfit
                )
                Spacer(Modifier.height(10.dp))
                NeonPrimaryButton(
                    text = "+ Add / Deduct Coins for ${u.displayName.ifBlank { "User" }}",
                    icon = Icons.Default.AccountBalanceWallet,
                    accentColor = ArenaGreen,
                    onClick = {
                        selectedUser = null
                        coinTargetUser = u
                    },
                    modifier = Modifier.fillMaxWidth().testTag("user360_open_coin_btn")
                )

                Spacer(Modifier.height(16.dp))
                Text("Edit Player Details", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(value = editName, onValueChange = { editName = it }, label = { Text("Display Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = editIgn, onValueChange = { editIgn = it }, label = { Text("In-Game Name (IGN)") }, singleLine = true, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = editGameUid, onValueChange = { editGameUid = it }, label = { Text("Game UID") }, singleLine = true, modifier = Modifier.weight(1f))
                }
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(value = editPhone, onValueChange = { editPhone = it }, label = { Text("WhatsApp / Phone Number") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                NeonPrimaryButton(
                    text = if (busy) "Saving..." else "Save Player Profile",
                    accentColor = ArenaPurple,
                    enabled = !busy,
                    onClick = {
                        adminVm.updateUserDetails(u.userId, editName, editIgn, editGameUid, editPhone)
                        selectedUser = null
                    },
                    modifier = Modifier.fillMaxWidth().testTag("user360_save_profile_btn")
                )

                Spacer(Modifier.height(16.dp))
                Text("Send Direct Alert to Player", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(value = directTitle, onValueChange = { directTitle = it }, label = { Text("Notification Title") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(value = directBody, onValueChange = { directBody = it }, label = { Text("Message Body") }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = {
                        adminVm.sendBroadcast(directTitle.trim(), directBody.trim(), "", "home", "user", u.userId)
                        directTitle = ""
                        directBody = ""
                    },
                    enabled = !busy && directTitle.isNotBlank() && directBody.isNotBlank(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = CoinYellow, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Send Direct Notification", color = CoinYellow)
                }

                Spacer(Modifier.height(16.dp))
                OutlinedTextField(value = banReason, onValueChange = { banReason = it }, label = { Text("Ban / Unban Reason") }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = {
                        adminVm.toggleBanUser(u.userId, !u.banned, banReason)
                        selectedUser = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = if (u.banned) EmeraldProfit else CrimsonDanger, contentColor = Color.White),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (u.banned) "Unban Player" else "Ban Player Account", fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AdminCoinAdjustmentSheet(
    initialUser: UserProfile?,
    allUsers: List<UserProfile>,
    busy: Boolean,
    adminVm: AdminViewModel,
    onDismiss: () -> Unit
) {
    var chosenUser by remember(initialUser?.userId) { mutableStateOf(initialUser) }
    var userSearch by remember { mutableStateOf("") }
    var bucket by remember { mutableStateOf("deposit") }
    var isCredit by remember { mutableStateOf(true) }
    var amountText by remember { mutableStateOf("100") }
    var reason by remember { mutableStateOf("") }

    val parsedAmount = amountText.toDoubleOrNull() ?: 0.0
    val filteredPickerUsers = remember(allUsers, userSearch) {
        val q = userSearch.trim()
        if (q.isBlank()) allUsers.sortedByDescending { it.totalBalance }.take(12)
        else allUsers.filter {
            it.displayName.contains(q, true) ||
                it.email.contains(q, true) ||
                it.inGameName.contains(q, true) ||
                it.phoneNumber.contains(q, true) ||
                it.userId.contains(q, true)
        }.take(20)
    }

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = ObsidianSurface) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CoinBadge(size = 24.dp)
                Spacer(Modifier.width(8.dp))
                Text(
                    text = if (chosenUser == null) "Select Player to Add Coins" else "Add / Deduct Coins",
                    style = MaterialTheme.typography.headlineSmall,
                    color = CoinYellow
                )
            }
            Spacer(Modifier.height(10.dp))

            if (chosenUser == null) {
                OutlinedTextField(
                    value = userSearch,
                    onValueChange = { userSearch = it },
                    label = { Text("Search player by name, email, IGN or phone") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("coin_picker_search")
                )
                Spacer(Modifier.height(8.dp))
                if (filteredPickerUsers.isEmpty()) {
                    Text("No players found.", color = TextMuted, style = MaterialTheme.typography.bodyMedium)
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        filteredPickerUsers.forEach { u ->
                            Surface(
                                color = ObsidianElevated,
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, ArenaOrange.copy(alpha = 0.35f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { chosenUser = u }
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(u.displayName.ifBlank { "Player" }, color = TextPrimary, fontWeight = FontWeight.Bold)
                                        Text(u.email.ifBlank { u.userId }, color = TextMuted, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                    Text("${u.totalBalance.toInt()} C", color = CoinYellow, fontWeight = FontWeight.ExtraBold)
                                }
                            }
                        }
                    }
                }
            } else {
                val u = chosenUser!!
                Surface(
                    color = ObsidianElevated,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, ArenaOrange.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(u.displayName.ifBlank { u.email }, color = TextPrimary, fontWeight = FontWeight.Bold)
                            Text(u.email.ifBlank { u.userId }, color = TextMuted, style = MaterialTheme.typography.labelSmall)
                            Text(
                                "Current: ${u.totalBalance.toInt()} Coins (Dep ${u.depositBalance.toInt()} • Win ${u.winningBalance.toInt()} • Bonus ${u.bonusBalance.toInt()})",
                                color = ArenaGreen,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                        if (initialUser == null) {
                            OutlinedButton(onClick = { chosenUser = null }) {
                                Text("Change", color = ArenaOrange, fontSize = 11.sp)
                            }
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = isCredit,
                        onClick = { isCredit = true },
                        label = { Text("Add Coins (+)", fontWeight = FontWeight.Bold) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = ArenaGreen,
                            selectedLabelColor = Color.White,
                            containerColor = ObsidianElevated
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = !isCredit,
                        onClick = { isCredit = false },
                        label = { Text("Deduct Coins (-)", fontWeight = FontWeight.Bold) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = CrimsonDanger,
                            selectedLabelColor = Color.White,
                            containerColor = ObsidianElevated
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(Modifier.height(8.dp))
                Text("Quick Amounts", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                Spacer(Modifier.height(4.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf(10, 50, 100, 250, 500, 1000, 5000).forEach { preset ->
                        FilterChip(
                            selected = amountText == preset.toString(),
                            onClick = { amountText = preset.toString() },
                            label = { Text("${if (isCredit) "+" else "-"}$preset") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = CoinYellow,
                                selectedLabelColor = Color(0xFF111827),
                                containerColor = ObsidianElevated
                            )
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Custom Coins Amount") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("admin_coin_amount_input")
                )

                Spacer(Modifier.height(8.dp))
                Text("Target Wallet Bucket", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("deposit" to "Deposit", "winning" to "Winning", "bonus" to "Bonus").forEach { (key, lbl) ->
                        FilterChip(
                            selected = bucket == key,
                            onClick = { bucket = key },
                            label = { Text(lbl) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = ArenaPurple,
                                selectedLabelColor = Color.White,
                                containerColor = ObsidianElevated
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text("Note / Reason (Optional — auto-fills)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                val currentBucketVal = when (bucket) {
                    "winning" -> u.winningBalance
                    "bonus" -> u.bonusBalance
                    else -> u.depositBalance
                }
                val newTotal = if (isCredit) u.totalBalance + parsedAmount else (u.totalBalance - parsedAmount).coerceAtLeast(0.0)
                val newBucketVal = if (isCredit) currentBucketVal + parsedAmount else (currentBucketVal - parsedAmount).coerceAtLeast(0.0)

                Spacer(Modifier.height(10.dp))
                Surface(
                    color = HeaderNavy,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, (if (isCredit) ArenaGreen else CrimsonDanger).copy(alpha = 0.6f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${bucket.uppercase()} Bucket: ${currentBucketVal.toInt()} -> ${newBucketVal.toInt()}",
                            color = TextSecondary,
                            style = MaterialTheme.typography.labelMedium
                        )
                        Text(
                            text = "Total: ${newTotal.toInt()} C",
                            color = CoinYellow,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))
                NeonPrimaryButton(
                    text = if (busy) "Updating Wallet..." else "${if (isCredit) "Add +" else "Deduct -"}${parsedAmount.toInt()} Coins",
                    icon = Icons.Default.AccountBalanceWallet,
                    accentColor = if (isCredit) ArenaGreen else CrimsonDanger,
                    enabled = !busy && parsedAmount > 0.0,
                    onClick = {
                        adminVm.adjustUserWallet(u.userId, bucket, isCredit, parsedAmount, reason.trim())
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth().testTag("admin_execute_coin_btn")
                )
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun BulkCoinAirdropSheet(
    users: List<UserProfile>,
    busy: Boolean,
    adminVm: AdminViewModel,
    onDismiss: () -> Unit
) {
    val activeUsers = remember(users) { users.filter { !it.banned } }
    var bucket by remember { mutableStateOf("bonus") }
    var amountText by remember { mutableStateOf("50") }
    var reason by remember { mutableStateOf("Festival / Event Coin Drop") }
    var sendNotif by remember { mutableStateOf(true) }
    val amount = amountText.toDoubleOrNull() ?: 0.0

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = ObsidianSurface) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Text("Bulk Coin Airdrop (All Players)", style = MaterialTheme.typography.headlineSmall, color = ArenaPurple)
            Text(
                "Credits coins simultaneously to ${activeUsers.size} active (non-banned) user accounts.",
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted
            )
            Spacer(Modifier.height(10.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(10, 25, 50, 100, 250, 500).forEach { preset ->
                    FilterChip(
                        selected = amountText == preset.toString(),
                        onClick = { amountText = preset.toString() },
                        label = { Text("+$preset C") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = CoinYellow,
                            selectedLabelColor = Color(0xFF111827),
                            containerColor = ObsidianElevated
                        )
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = amountText,
                onValueChange = { amountText = it },
                label = { Text("Coins Per User") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("bonus" to "Bonus", "deposit" to "Deposit", "winning" to "Winning").forEach { (key, lbl) ->
                    FilterChip(
                        selected = bucket == key,
                        onClick = { bucket = key },
                        label = { Text(lbl) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
            OutlinedTextField(
                value = reason,
                onValueChange = { reason = it },
                label = { Text("Event / Airdrop Reason") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Notify every player in-app", color = TextPrimary)
                Switch(checked = sendNotif, onCheckedChange = { sendNotif = it })
            }
            Spacer(Modifier.height(12.dp))
            NeonPrimaryButton(
                text = if (busy) "Airdropping..." else "Send +${amount.toInt()} Coins to ${activeUsers.size} Users",
                icon = Icons.Default.CardGiftcard,
                accentColor = ArenaPurple,
                enabled = !busy && activeUsers.isNotEmpty() && amount > 0.0,
                onClick = {
                    adminVm.bulkAddCoins(users, bucket, amount, reason.trim(), sendNotif)
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth().testTag("admin_execute_bulk_drop_btn")
            )
            Spacer(Modifier.height(24.dp))
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
            val granted = r.grantedPermissionLabels()
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
                Spacer(Modifier.height(6.dp))
                @OptIn(ExperimentalLayoutApi::class)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (granted.isEmpty()) {
                        StatusPill("No Permissions Enabled", CrimsonDanger)
                    } else {
                        granted.forEach { label ->
                            StatusPill(label, NeonCyan)
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

        val anySelected = pMatches || pRooms || pDeposits || pWithdraws || pCoupons || pTickets || pPush || pUsers || pSettings

        ModalBottomSheet(onDismissRequest = { editingRole = null }, containerColor = ObsidianSurface) {
            Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp)) {
                Text("Configure Staff Permissions", style = MaterialTheme.typography.headlineSmall, color = ElectricViolet)
                Text(
                    "Staff will ONLY see and access the exact switches enabled below.",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted
                )
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
                    "Manage Users & Coins" to (pUsers to { v: Boolean -> pUsers = v }),
                    "CMS, New Update & Settings" to (pSettings to { v: Boolean -> pSettings = v })
                ).forEach { (title, pair) ->
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(title, color = TextPrimary)
                        Switch(checked = pair.first, onCheckedChange = pair.second)
                    }
                }
                if (!anySelected) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Turn ON at least one permission switch for this staff member to enter Admin Hub.",
                        style = MaterialTheme.typography.labelSmall,
                        color = AmberWarning
                    )
                }
                Spacer(Modifier.height(10.dp))
                NeonPrimaryButton(
                    text = if (busy) "Saving..." else "Save Staff Role",
                    enabled = !busy && uid.trim().isNotBlank() && email.trim().isNotBlank(),
                    onClick = {
                        adminVm.saveStaffRole(
                            StaffRole(
                                userId = uid.trim(),
                                email = email.trim(),
                                name = name.trim(),
                                roleLabel = label.trim().ifBlank { "Moderator" },
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
fun AdminCategoriesModule(
    categories: List<GameCategory>,
    busy: Boolean,
    adminVm: AdminViewModel
) {
    val context = LocalContext.current
    var editingCat by remember { mutableStateOf<GameCategory?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            SectionHeader(
                title = "Game Categories Manager",
                subtitle = if (categories.isEmpty()) "Using built-in defaults (load defaults to customize/delete)" else "${categories.size} live categories in Firestore"
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NeonPrimaryButton(
                    text = "Add Category",
                    icon = Icons.Default.Add,
                    enabled = !busy,
                    onClick = {
                        editingCat = GameCategory(
                            sortOrder = (categories.size + 1).toLong(),
                            drawableKey = "bgmi",
                            active = true
                        )
                    },
                    modifier = Modifier.weight(1f).testTag("admin_add_category_btn")
                )
                OutlinedButton(
                    onClick = { adminVm.seedDefaultCategories() },
                    enabled = !busy,
                    modifier = Modifier.height(50.dp).testTag("admin_seed_categories_btn")
                ) {
                    Icon(Icons.Default.Download, contentDescription = null, tint = AmberWarning, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Load Defaults", color = AmberWarning)
                }
            }
        }

        if (categories.isEmpty()) {
            item {
                CyberGlassCard(glowColor = AmberWarning) {
                    Text(
                        text = "No custom categories in Firestore yet — players see the 12 default categories. Tap 'Load Defaults' to import all 12 into Firestore so you can edit images or delete any of them, or tap 'Add Category' to create your own.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
            }
        } else {
            items(categories, key = { it.id }) { cat ->
                CyberGlassCard(glowColor = if (cat.active) NeonCyan else CrimsonDanger) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .border(1.dp, NeonCyan.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                        ) {
                            MatchBannerImage(
                                bannerUrl = cat.imageUrl,
                                bannerKey = cat.drawableKey,
                                contentDescription = cat.title,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(cat.title, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                            Text("Filter: ${cat.filterKey} • Order: ${cat.sortOrder}", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                            Spacer(Modifier.height(4.dp))
                            StatusPill(if (cat.active) "ACTIVE" else "HIDDEN", if (cat.active) EmeraldProfit else CrimsonDanger)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            OutlinedButton(
                                onClick = { editingCat = cat },
                                modifier = Modifier.testTag("admin_edit_cat_${cat.id}")
                            ) {
                                Text("Edit", color = NeonCyan)
                            }
                            IconButton(
                                onClick = { adminVm.deleteCategory(cat.id) },
                                enabled = !busy,
                                modifier = Modifier.testTag("admin_delete_cat_${cat.id}")
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete Category", tint = CrimsonDanger)
                            }
                        }
                    }
                }
            }
        }
    }

    editingCat?.let { init ->
        var catTitle by remember(init.id) { mutableStateOf(init.title) }
        var catFilter by remember(init.id) { mutableStateOf(init.filterKey) }
        var catImageUrl by remember(init.id) { mutableStateOf(init.imageUrl) }
        var catKey by remember(init.id) { mutableStateOf(init.drawableKey.ifBlank { "bgmi" }) }
        var catOrderText by remember(init.id) { mutableStateOf(init.sortOrder.toString()) }
        var catActive by remember(init.id) { mutableStateOf(init.active) }

        val catPhotoPicker = rememberLauncherForActivityResult(
            ActivityResultContracts.PickVisualMedia()
        ) { uri ->
            if (uri != null) {
                runCatching {
                    context.contentResolver.openInputStream(uri)?.use { stream ->
                        BitmapFactory.decodeStream(stream)
                    }
                }.getOrNull()?.let { bmp ->
                    catImageUrl = QrCodeUtils.compressBitmapToBase64(bmp, maxWidth = 480, quality = 65)
                }
            }
        }

        ModalBottomSheet(onDismissRequest = { editingCat = null }, containerColor = ObsidianSurface) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                Text(
                    text = if (init.id.isBlank()) "Add Game Category" else "Edit Game Category",
                    style = MaterialTheme.typography.headlineSmall,
                    color = NeonCyan
                )
                Spacer(Modifier.height(10.dp))

                Text("Category Image Preview", style = MaterialTheme.typography.labelLarge, color = TextSecondary)
                Spacer(Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .size(110.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.dp, NeonCyan.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                ) {
                    MatchBannerImage(
                        bannerUrl = catImageUrl,
                        bannerKey = catKey,
                        contentDescription = "Category Image Preview",
                        modifier = Modifier.fillMaxSize()
                    )
                }
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(
                        onClick = {
                            catPhotoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        },
                        modifier = Modifier.weight(1f).testTag("admin_pick_cat_image_btn")
                    ) {
                        Icon(Icons.Default.Image, contentDescription = null, tint = NeonCyan)
                        Spacer(Modifier.width(6.dp))
                        Text("Pick from Gallery", color = NeonCyan)
                    }
                    if (catImageUrl.isNotBlank()) {
                        OutlinedButton(onClick = { catImageUrl = "" }) {
                            Text("Reset", color = AmberWarning)
                        }
                    }
                }
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    value = if (catImageUrl.startsWith("http", ignoreCase = true) || catImageUrl.isBlank()) catImageUrl else "[Custom Gallery Image Uploaded]",
                    onValueChange = { catImageUrl = it },
                    label = { Text("Or Paste Image URL (https://...)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(6.dp))
                Text("Fallback Artwork Preset", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(listOf("bgmi", "freefire", "codm", "hero")) { k ->
                        FilterChip(
                            selected = catImageUrl.isBlank() && catKey == k,
                            onClick = {
                                catImageUrl = ""
                                catKey = k
                            },
                            label = { Text(k.uppercase()) }
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = catTitle,
                    onValueChange = { newVal ->
                        catTitle = newVal
                        if (init.id.isBlank() && catFilter.isBlank()) {
                            catFilter = newVal
                        }
                    },
                    label = { Text("Category Display Title (e.g. BGMI, FF MAX 2)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("admin_cat_title_input")
                )
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    value = catFilter,
                    onValueChange = { catFilter = it },
                    label = { Text("Match Filter Key (e.g. BGMI, Free Fire MAX, All)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("admin_cat_filter_input")
                )
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = catOrderText,
                        onValueChange = { catOrderText = it },
                        label = { Text("Sort Order") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Visible", color = TextPrimary)
                        Spacer(Modifier.width(6.dp))
                        Switch(checked = catActive, onCheckedChange = { catActive = it })
                    }
                }
                Spacer(Modifier.height(14.dp))
                NeonPrimaryButton(
                    text = if (busy) "Saving..." else "Save Category",
                    enabled = !busy && catTitle.isNotBlank(),
                    onClick = {
                        adminVm.saveCategory(
                            init.copy(
                                title = catTitle.trim(),
                                filterKey = catFilter.trim().ifBlank { catTitle.trim() },
                                imageUrl = catImageUrl.trim(),
                                drawableKey = catKey,
                                sortOrder = catOrderText.toLongOrNull() ?: (categories.size + 1).toLong(),
                                active = catActive
                            )
                        )
                        editingCat = null
                    },
                    modifier = Modifier.fillMaxWidth().testTag("admin_save_cat_btn")
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
    var privacyText by remember(generalConfig) { mutableStateOf(generalConfig.privacyText) }
    var termsText by remember(generalConfig) { mutableStateOf(generalConfig.termsText) }
    var faqText by remember(generalConfig) { mutableStateOf(generalConfig.faqText) }
    var aboutUsText by remember(generalConfig) { mutableStateOf(generalConfig.aboutUsText) }
    var shareMessage by remember(generalConfig) { mutableStateOf(generalConfig.shareMessage) }
    var supportHours by remember(generalConfig) { mutableStateOf(generalConfig.supportHours) }
    var appTagline by remember(generalConfig) { mutableStateOf(generalConfig.appTagline) }
    var tgUrl by remember(generalConfig) { mutableStateOf(generalConfig.telegramUrl) }
    var dcUrl by remember(generalConfig) { mutableStateOf(generalConfig.discordUrl) }
    var waUrl by remember(generalConfig) { mutableStateOf(generalConfig.whatsappUrl) }
    var igUrl by remember(generalConfig) { mutableStateOf(generalConfig.instagramUrl) }
    var matchesHeroUrl by remember(generalConfig) { mutableStateOf(generalConfig.matchesHeroBannerUrl) }
    var matchesHeroKey by remember(generalConfig) { mutableStateOf(generalConfig.matchesHeroBannerKey) }
    var matchesHeroTitle by remember(generalConfig) { mutableStateOf(generalConfig.matchesHeroTitle) }
    var matchesHeroSub by remember(generalConfig) { mutableStateOf(generalConfig.matchesHeroSubtitle) }
    var editingBanner by remember { mutableStateOf<CmsBanner?>(null) }

    val saveAllCms: () -> Unit = {
        val parsedRewards = checkinCsv.split(",").mapNotNull { it.trim().toLongOrNull() }
            .ifEmpty { listOf(5L, 8L, 10L, 12L, 15L, 20L, 50L) }
        adminVm.saveGeneralConfig(
            generalConfig.copy(
                tickerText = ticker,
                maintenanceMode = maintMode,
                maintenanceMessage = maintMsg,
                maintenanceEta = maintEta,
                checkinEnabled = checkinEnabled,
                checkinRewards = parsedRewards,
                bonusUsablePercent = bonusPct.toLongOrNull() ?: 10L,
                telegramUrl = tgUrl.trim(),
                discordUrl = dcUrl.trim(),
                whatsappUrl = waUrl.trim(),
                instagramUrl = igUrl.trim(),
                rulesText = rulesText,
                privacyText = privacyText,
                termsText = termsText,
                faqText = faqText,
                aboutUsText = aboutUsText,
                shareMessage = shareMessage,
                supportHours = supportHours,
                appTagline = appTagline,
                matchesHeroBannerUrl = matchesHeroUrl.trim(),
                matchesHeroBannerKey = matchesHeroKey,
                matchesHeroTitle = matchesHeroTitle.trim(),
                matchesHeroSubtitle = matchesHeroSub.trim()
            )
        )
    }

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
        item { SectionHeader("CMS, Pages, Policies & Engagement") }
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
            CyberGlassCard(glowColor = EmeraldProfit) {
                SectionHeader("Legal & App Pages Content", "Privacy Policy, Terms, FAQ, About Us, Share & Socials")
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = privacyText,
                    onValueChange = { privacyText = it },
                    label = { Text("Privacy Policy") },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth().testTag("cms_privacy_input")
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = termsText,
                    onValueChange = { termsText = it },
                    label = { Text("Terms & Conditions") },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth().testTag("cms_terms_input")
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = faqText,
                    onValueChange = { faqText = it },
                    label = { Text("FAQ (Frequently Asked Questions)") },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth().testTag("cms_faq_input")
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = aboutUsText,
                    onValueChange = { aboutUsText = it },
                    label = { Text("About Us Page Text") },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth().testTag("cms_about_input")
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = rulesText,
                    onValueChange = { rulesText = it },
                    label = { Text("Default Match Rules") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = shareMessage,
                    onValueChange = { shareMessage = it },
                    label = { Text("Share App Message (use {CODE} placeholder)") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = supportHours,
                    onValueChange = { supportHours = it },
                    label = { Text("Contact Us Support Hours Note") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = appTagline,
                    onValueChange = { appTagline = it },
                    label = { Text("Brand Tagline") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = tgUrl, onValueChange = { tgUrl = it }, label = { Text("Telegram URL") }, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = dcUrl, onValueChange = { dcUrl = it }, label = { Text("Discord URL") }, modifier = Modifier.weight(1f))
                }
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = waUrl, onValueChange = { waUrl = it }, label = { Text("WhatsApp URL") }, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = igUrl, onValueChange = { igUrl = it }, label = { Text("Instagram URL") }, modifier = Modifier.weight(1f))
                }
                Spacer(Modifier.height(10.dp))
                NeonPrimaryButton(
                    text = if (busy) "Saving..." else "Save Legal, Pages & Socials",
                    accentColor = EmeraldProfit,
                    enabled = !busy,
                    onClick = saveAllCms,
                    modifier = Modifier.fillMaxWidth().testTag("save_legal_pages_btn")
                )
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
                Spacer(Modifier.height(10.dp))
                NeonPrimaryButton(
                    text = if (busy) "Saving..." else "Save CMS & App Controls",
                    enabled = !busy,
                    onClick = saveAllCms,
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

@Composable
fun AdminAppUpdateModule(
    config: AppUpdateConfig,
    busy: Boolean,
    adminVm: AdminViewModel
) {
    val context = LocalContext.current
    val currentCode = BuildConfig.VERSION_CODE
    val currentName = BuildConfig.VERSION_NAME

    var forceEnabled by remember(config) { mutableStateOf(config.forceUpdateEnabled) }
    var versionCodeText by remember(config) {
        mutableStateOf(
            (if (config.latestVersionCode > currentCode.toLong()) config.latestVersionCode else (currentCode + 1).toLong()).toString()
        )
    }
    var versionNameText by remember(config) {
        mutableStateOf(
            config.latestVersionName.takeIf { it.isNotBlank() && it != currentName }
                ?: "${currentCode + 1}.0"
        )
    }
    var downloadUrl by remember(config) { mutableStateOf(config.updateDownloadUrl) }
    var releaseNotes by remember(config) {
        mutableStateOf(
            config.releaseNotes.ifBlank {
                "New Ahxan Arena version available! Download and install the latest APK to continue playing."
            }
        )
    }

    val targetCode = versionCodeText.trim().toLongOrNull() ?: 0L
    val willBlockCurrentBuild = forceEnabled && downloadUrl.trim().isNotBlank() && targetCode > currentCode.toLong()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            SectionHeader(
                title = "New App Update Manager",
                subtitle = "Force all old-version users to download your latest update link"
            )
        }

        item {
            CyberGlassCard(glowColor = if (config.forceUpdateEnabled) ArenaOrange else NeonCyan) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Installed App Build", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                        Text(
                            "v$currentName (Build $currentCode)",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    StatusPill(
                        label = if (config.forceUpdateEnabled) "FORCE UPDATE ON" else "GATE OFF",
                        color = if (config.forceUpdateEnabled) ArenaOrange else EmeraldProfit
                    )
                }
                if (config.forceUpdateEnabled && config.updateDownloadUrl.isNotBlank()) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Live Target: v${config.latestVersionName} (Build ${config.latestVersionCode})",
                        style = MaterialTheme.typography.labelMedium,
                        color = CoinYellow
                    )
                    Text(
                        "Link: ${config.updateDownloadUrl}",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        item {
            CyberGlassCard(glowColor = ArenaBlue) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.SystemUpdate, contentDescription = null, tint = ArenaOrange, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Force Update (Block Old Versions)", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                        }
                        Text(
                            "Users below target Version Code cannot use the app until they update",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted
                        )
                    }
                    Switch(
                        checked = forceEnabled,
                        onCheckedChange = { forceEnabled = it },
                        modifier = Modifier.testTag("admin_force_update_switch")
                    )
                }

                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = versionCodeText,
                        onValueChange = { ch -> versionCodeText = ch.filter { it.isDigit() } },
                        label = { Text("Min Version Code") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f).testTag("admin_update_version_code_input")
                    )
                    OutlinedTextField(
                        value = versionNameText,
                        onValueChange = { versionNameText = it },
                        label = { Text("New Version Name") },
                        singleLine = true,
                        modifier = Modifier.weight(1f).testTag("admin_update_version_name_input")
                    )
                }

                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    FilterChip(
                        selected = targetCode == (currentCode + 1).toLong(),
                        onClick = {
                            versionCodeText = (currentCode + 1).toString()
                            if (versionNameText.isBlank() || versionNameText == currentName) {
                                versionNameText = "${currentCode + 1}.0"
                            }
                        },
                        label = { Text("Block Build $currentCode (Set Code ${currentCode + 1})") }
                    )
                }

                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = downloadUrl,
                    onValueChange = { downloadUrl = it },
                    label = { Text("New Update Download Link * (APK / Drive / Play Store)") },
                    placeholder = { Text("https://example.com/AhxanArena-v2.apk") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("admin_update_download_url_input")
                )

                if (downloadUrl.trim().isNotBlank()) {
                    Spacer(Modifier.height(6.dp))
                    OutlinedButton(
                        onClick = {
                            val normalized = if (downloadUrl.trim().contains("://")) downloadUrl.trim() else "https://${downloadUrl.trim()}"
                            runCatching {
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(normalized)))
                            }.onFailure {
                                Toast.makeText(context, "Invalid or unsupported URL", Toast.LENGTH_SHORT).show()
                            }
                        }
                    ) {
                        Icon(Icons.Default.OpenInNew, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Test Open Download Link", color = NeonCyan)
                    }
                }

                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = releaseNotes,
                    onValueChange = { releaseNotes = it },
                    label = { Text("What's New / Update Message") },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth().testTag("admin_update_notes_input")
                )

                Spacer(Modifier.height(8.dp))
                Text(
                    text = if (willBlockCurrentBuild) {
                        "Preview: Build $currentCode WILL see the mandatory Update Screen and redirect to your link."
                    } else if (forceEnabled && downloadUrl.trim().isBlank()) {
                        "Enter a download link to activate the forced update blocker."
                    } else {
                        "Preview: Current Build $currentCode is allowed without update prompt."
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = if (willBlockCurrentBuild) ArenaOrange else TextMuted
                )

                Spacer(Modifier.height(12.dp))
                NeonPrimaryButton(
                    text = if (busy) "Publishing..." else "Publish New Update",
                    icon = Icons.Default.SystemUpdate,
                    accentColor = ArenaOrange,
                    enabled = !busy && (!forceEnabled || (downloadUrl.trim().isNotBlank() && targetCode >= 1L)),
                    onClick = {
                        adminVm.saveAppUpdateConfig(
                            AppUpdateConfig(
                                forceUpdateEnabled = forceEnabled,
                                latestVersionCode = targetCode.coerceAtLeast(1L),
                                latestVersionName = versionNameText.trim().ifBlank { "2.0" },
                                updateDownloadUrl = downloadUrl.trim(),
                                releaseNotes = releaseNotes.trim()
                            )
                        )
                    },
                    modifier = Modifier.fillMaxWidth().testTag("admin_publish_update_btn")
                )

                if (config.forceUpdateEnabled) {
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = {
                            forceEnabled = false
                            adminVm.saveAppUpdateConfig(
                                config.copy(forceUpdateEnabled = false)
                            )
                        },
                        enabled = !busy,
                        modifier = Modifier.fillMaxWidth().testTag("admin_disable_update_btn")
                    ) {
                        Text("Turn Off Mandatory Update", color = CrimsonDanger)
                    }
                }
            }
        }
    }
}
