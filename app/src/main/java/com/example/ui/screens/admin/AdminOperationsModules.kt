package com.example.ui.screens.admin

import android.graphics.BitmapFactory
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.data.model.BroadcastNotification
import com.example.data.model.GameCategory
import com.example.data.model.MatchSlot
import com.example.data.model.PushDevice
import com.example.data.model.SupportTicket
import com.example.data.model.TicketMessage
import com.example.data.model.Tournament
import com.example.data.model.UserProfile
import com.example.ui.components.CyberGlassCard
import com.example.ui.components.MatchBannerImage
import com.example.ui.components.NeonPrimaryButton
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatusPill
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CrimsonDanger
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.EmeraldProfit
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

@Composable
fun AdminNotificationStudioModule(
    broadcasts: List<BroadcastNotification>,
    pushDevices: List<PushDevice>,
    busy: Boolean,
    adminVm: AdminViewModel
) {
    var title by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }
    var imageUrl by remember { mutableStateOf("") }
    var route by remember { mutableStateOf("tournaments") }
    var audience by remember { mutableStateOf("all") }
    var targetId by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            SectionHeader(
                title = "Send Notification Studio",
                subtitle = "${pushDevices.count { it.enabled }} active Android push devices registered"
            )
        }
        item {
            CyberGlassCard(glowColor = NeonCyan) {
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Push Title") }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = body, onValueChange = { body = it }, label = { Text("Message Body") }, minLines = 2, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = imageUrl, onValueChange = { imageUrl = it }, label = { Text("Big Banner Image URL (Optional)") }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                Text("Target Route", style = MaterialTheme.typography.labelLarge, color = TextSecondary)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(listOf("home", "tournaments", "wallet", "my_matches", "refer", "support")) { r ->
                        FilterChip(selected = route == r, onClick = { route = r }, label = { Text(r) })
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text("Audience Segment", style = MaterialTheme.typography.labelLarge, color = TextSecondary)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(listOf("all", "depositors", "zero_matches", "user", "match")) { a ->
                        FilterChip(selected = audience == a, onClick = { audience = a }, label = { Text(a) })
                    }
                }
                if (audience == "user" || audience == "match") {
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = targetId,
                        onValueChange = { targetId = it },
                        label = { Text(if (audience == "user") "Target User UID" else "Target Match ID") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(Modifier.height(12.dp))
                // Live Mobile Shutter Preview
                Text("LIVE ANDROID SHUTTER PREVIEW", style = MaterialTheme.typography.labelSmall, color = AmberWarning)
                Spacer(Modifier.height(6.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF141820))
                        .border(1.dp, NeonCyan.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                        .padding(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Ahxan Arena • now", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                        }
                        StatusPill("OPEN NOW", NeonCyan)
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(title.ifBlank { "Notification Title Preview" }, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                    Text(body.ifBlank { "Your tournament alert or bonus offer appears in the lock-screen shutter." }, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                    if (imageUrl.isNotBlank()) {
                        Spacer(Modifier.height(8.dp))
                        AsyncImage(
                            model = imageUrl,
                            contentDescription = "Shutter Banner Preview",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxWidth().height(90.dp).clip(RoundedCornerShape(10.dp))
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))
                NeonPrimaryButton(
                    text = if (busy) "Dispatching..." else "Dispatch Mobile Shutter Push",
                    icon = Icons.Default.Send,
                    enabled = !busy && title.isNotBlank() && body.isNotBlank(),
                    onClick = {
                        adminVm.sendBroadcast(title, body, imageUrl, route, audience, targetId)
                        title = ""
                        body = ""
                    },
                    modifier = Modifier.fillMaxWidth().testTag("admin_send_broadcast_btn")
                )
            }
        }
        item { SectionHeader("Broadcast History (${broadcasts.size})") }
        items(broadcasts, key = { it.id }) { b ->
            CyberGlassCard(glowColor = ElectricViolet) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(b.title, style = MaterialTheme.typography.titleMedium, color = TextPrimary, modifier = Modifier.weight(1f))
                    StatusPill(b.audienceType, NeonCyan)
                }
                Text(b.body, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                Text("Delivered: ${b.deliveredCount} devices • Route: ${b.redirectRoute}", style = MaterialTheme.typography.labelSmall, color = TextMuted)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminTournamentEngineModule(
    tournaments: List<Tournament>,
    roster: List<MatchSlot>,
    categories: List<GameCategory> = emptyList(),
    busy: Boolean,
    adminVm: AdminViewModel
) {
    var editingMatch by remember { mutableStateOf<Tournament?>(null) }
    var publishingRoomMatch by remember { mutableStateOf<Tournament?>(null) }
    var rosterMatch by remember { mutableStateOf<Tournament?>(null) }
    var resultsMatch by remember { mutableStateOf<Tournament?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                SectionHeader("Tournament & Match Engine", "${tournaments.size} matches")
            }
            Spacer(Modifier.height(8.dp))
            NeonPrimaryButton(
                text = "Create New Match",
                icon = Icons.Default.Add,
                onClick = {
                    editingMatch = Tournament(
                        scheduledAtMillis = System.currentTimeMillis() + 3600_000L * 2,
                        maxSlots = 100L,
                        entryFee = 30.0,
                        prizePool = 2000.0,
                        perKillReward = 20.0
                    )
                },
                modifier = Modifier.fillMaxWidth().testTag("admin_create_match_btn")
            )
        }

        items(tournaments, key = { it.id }) { m ->
            CyberGlassCard(glowColor = if (m.status == "live") EmeraldProfit else NeonCyan) {
                MatchBannerImage(
                    bannerUrl = m.bannerUrl,
                    bannerKey = m.bannerKey,
                    contentDescription = m.title,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(92.dp)
                        .clip(RoundedCornerShape(12.dp))
                )
                Spacer(Modifier.height(10.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(m.title, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                        Text("${m.game} • ${m.mode} • ${m.filledSlots}/${m.maxSlots} slots", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                        Text("Entry: ${ArenaFormatters.formatInr(m.entryFee)} • Pool: ${ArenaFormatters.formatInr(m.prizePool)} • Kill: ${ArenaFormatters.formatInr(m.perKillReward)}", style = MaterialTheme.typography.labelSmall, color = EmeraldProfit)
                    }
                    StatusPill(m.status, if (m.status == "completed") ElectricViolet else NeonCyan)
                }
                Spacer(Modifier.height(10.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    item { OutlinedButton(onClick = { editingMatch = m }) { Text("Edit", color = NeonCyan) } }
                    item { OutlinedButton(onClick = { adminVm.cloneTournament(m) }) { Text("Clone", color = ElectricViolet) } }
                    item {
                        OutlinedButton(onClick = { publishingRoomMatch = m }) {
                            Icon(Icons.Default.Key, contentDescription = null, modifier = Modifier.size(14.dp), tint = AmberWarning)
                            Spacer(Modifier.width(4.dp))
                            Text(if (m.roomPublished) "Update Room" else "Publish Room", color = AmberWarning)
                        }
                    }
                    item {
                        OutlinedButton(onClick = {
                            adminVm.loadRoster(m.id)
                            rosterMatch = m
                        }) {
                            Icon(Icons.Default.People, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Roster")
                        }
                    }
                    if (m.status != "completed" && m.status != "cancelled") {
                        item {
                            OutlinedButton(onClick = {
                                adminVm.loadRoster(m.id)
                                resultsMatch = m
                            }) {
                                Icon(Icons.Default.EmojiEvents, contentDescription = null, modifier = Modifier.size(14.dp), tint = EmeraldProfit)
                                Spacer(Modifier.width(4.dp))
                                Text("Distribute Prizes", color = EmeraldProfit)
                            }
                        }
                        item {
                            OutlinedButton(onClick = { adminVm.cancelAndRefundAll(m) }) {
                                Icon(Icons.Default.Cancel, contentDescription = null, modifier = Modifier.size(14.dp), tint = CrimsonDanger)
                                Spacer(Modifier.width(4.dp))
                                Text("Cancel & Refund", color = CrimsonDanger)
                            }
                        }
                    }
                }
            }
        }
    }

    editingMatch?.let { init ->
        val context = LocalContext.current
        var title by remember(init.id) { mutableStateOf(init.title) }
        var game by remember(init.id) { mutableStateOf(init.game) }
        var mode by remember(init.id) { mutableStateOf(init.mode) }
        var mapName by remember(init.id) { mutableStateOf(init.mapName) }
        var entryText by remember(init.id) { mutableStateOf(init.entryFee.toString()) }
        var poolText by remember(init.id) { mutableStateOf(init.prizePool.toString()) }
        var killText by remember(init.id) { mutableStateOf(init.perKillReward.toString()) }
        var maxSlotsText by remember(init.id) { mutableStateOf(init.maxSlots.toString()) }
        var bannerKey by remember(init.id) { mutableStateOf(init.bannerKey.ifBlank { "bgmi" }) }
        var bannerUrl by remember(init.id) { mutableStateOf(init.bannerUrl) }
        var rules by remember(init.id) { mutableStateOf(init.rulesText) }
        var breakdown by remember(init.id) { mutableStateOf(init.prizeBreakdown) }

        val matchBannerPicker = rememberLauncherForActivityResult(
            ActivityResultContracts.PickVisualMedia()
        ) { uri ->
            if (uri != null) {
                runCatching {
                    context.contentResolver.openInputStream(uri)?.use { stream ->
                        BitmapFactory.decodeStream(stream)
                    }
                }.getOrNull()?.let { bmp ->
                    bannerUrl = QrCodeUtils.compressBitmapToBase64(bmp, maxWidth = 720, quality = 65)
                }
            }
        }

        ModalBottomSheet(onDismissRequest = { editingMatch = null }, containerColor = ObsidianSurface) {
            Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp)) {
                Text(if (init.id.isBlank()) "Create Match" else "Edit Match", style = MaterialTheme.typography.headlineSmall, color = NeonCyan)
                Spacer(Modifier.height(10.dp))

                // Match Banner Preview & Selector
                Text("Match Banner (Optional)", style = MaterialTheme.typography.labelLarge, color = TextSecondary)
                Spacer(Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .border(1.dp, NeonCyan.copy(alpha = 0.45f), RoundedCornerShape(14.dp))
                ) {
                    MatchBannerImage(
                        bannerUrl = bannerUrl,
                        bannerKey = bannerKey,
                        contentDescription = "Match Banner Preview",
                        modifier = Modifier.fillMaxSize()
                    )
                }
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(
                        onClick = {
                            matchBannerPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        },
                        modifier = Modifier.weight(1f).testTag("admin_pick_match_banner_btn")
                    ) {
                        Icon(Icons.Default.Image, contentDescription = null, tint = NeonCyan)
                        Spacer(Modifier.width(6.dp))
                        Text("Pick Banner from Gallery", color = NeonCyan)
                    }
                    if (bannerUrl.isNotBlank()) {
                        OutlinedButton(
                            onClick = { bannerUrl = "" },
                            modifier = Modifier.testTag("admin_reset_match_banner_btn")
                        ) {
                            Text("Reset", color = AmberWarning)
                        }
                    }
                }
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    value = if (bannerUrl.startsWith("http", ignoreCase = true) || bannerUrl.isBlank()) bannerUrl else "[Custom Gallery Image Uploaded]",
                    onValueChange = { bannerUrl = it },
                    label = { Text("Or Banner Image URL (https://...)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("admin_match_banner_url_input")
                )
                Spacer(Modifier.height(4.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(listOf("bgmi", "freefire", "codm", "hero")) { k ->
                        FilterChip(
                            selected = bannerUrl.isBlank() && bannerKey == k,
                            onClick = {
                                bannerUrl = ""
                                bannerKey = k
                            },
                            label = { Text(k.uppercase()) }
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Match Title") }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(6.dp))
                val gameChips = remember(categories) {
                    val defaults = listOf(
                        "BGMI",
                        "Free Fire MAX",
                        "Clash Squad",
                        "BGMI TDM",
                        "FF Survival",
                        "Lone Wolf",
                        "Sniper TDM",
                        "WOW Mode",
                        "CS 4v4",
                        "COD Mobile",
                        "Valorant"
                    )
                    val custom = categories
                        .map { it.filterKey.ifBlank { it.title }.trim() }
                        .filter { it.isNotBlank() && !it.equals("All", ignoreCase = true) }
                    (custom + defaults).distinct()
                }
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(gameChips) { g ->
                        FilterChip(
                            selected = game == g,
                            onClick = {
                                game = g
                                if (bannerUrl.isBlank() && init.id.isBlank()) {
                                    val matchingCat = categories.firstOrNull {
                                        it.filterKey.equals(g, ignoreCase = true) || it.title.equals(g, ignoreCase = true)
                                    }
                                    bannerKey = matchingCat?.drawableKey?.ifBlank { "bgmi" } ?: when {
                                        g.contains("Free", true) || g.contains("Clash", true) || g.contains("FF", true) || g.contains("CS", true) -> "freefire"
                                        g.contains("COD", true) -> "codm"
                                        g.contains("Lone", true) || g.contains("Valorant", true) -> "hero"
                                        else -> "bgmi"
                                    }
                                }
                            },
                            label = { Text(g) }
                        )
                    }
                }
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(listOf("Solo", "Duo", "Squad")) { md ->
                        FilterChip(selected = mode == md, onClick = { mode = md }, label = { Text(md) })
                    }
                }
                OutlinedTextField(value = mapName, onValueChange = { mapName = it }, label = { Text("Map Name") }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = entryText, onValueChange = { entryText = it }, label = { Text("Entry ₹") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                    OutlinedTextField(value = poolText, onValueChange = { poolText = it }, label = { Text("Prize Pool ₹") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                }
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = killText, onValueChange = { killText = it }, label = { Text("Per Kill ₹") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                    OutlinedTextField(value = maxSlotsText, onValueChange = { maxSlotsText = it }, label = { Text("Max Slots") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                }
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(value = breakdown, onValueChange = { breakdown = it }, label = { Text("Prize Breakdown") }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(value = rules, onValueChange = { rules = it }, label = { Text("Rules") }, minLines = 2, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(12.dp))
                NeonPrimaryButton(
                    text = if (busy) "Saving..." else "Save Match",
                    enabled = !busy && title.isNotBlank(),
                    onClick = {
                        adminVm.saveTournament(
                            init.copy(
                                title = title,
                                game = game,
                                mode = mode,
                                mapName = mapName,
                                entryFee = entryText.toDoubleOrNull() ?: 0.0,
                                prizePool = poolText.toDoubleOrNull() ?: 0.0,
                                perKillReward = killText.toDoubleOrNull() ?: 0.0,
                                maxSlots = maxSlotsText.toLongOrNull() ?: 100L,
                                bannerKey = bannerKey,
                                bannerUrl = bannerUrl.trim(),
                                rulesText = rules,
                                prizeBreakdown = breakdown
                            )
                        )
                        editingMatch = null
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(24.dp))
            }
        }
    }

    publishingRoomMatch?.let { m ->
        var rId by remember(m.id) { mutableStateOf("") }
        var rPass by remember(m.id) { mutableStateOf("") }
        var region by remember(m.id) { mutableStateOf("Asia / India") }
        var autoPush by remember(m.id) { mutableStateOf(true) }

        ModalBottomSheet(onDismissRequest = { publishingRoomMatch = null }, containerColor = ObsidianSurface) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Text("Publish Room Credentials", style = MaterialTheme.typography.headlineSmall, color = AmberWarning)
                Text(m.title, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(value = rId, onValueChange = { rId = it }, label = { Text("Room ID") }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = rPass, onValueChange = { rPass = it }, label = { Text("Room Password") }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = region, onValueChange = { region = it }, label = { Text("Server Region") }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text("Auto-Send Shutter Push to Joined Players", color = TextPrimary)
                    Switch(checked = autoPush, onCheckedChange = { autoPush = it })
                }
                Spacer(Modifier.height(12.dp))
                NeonPrimaryButton(
                    text = if (busy) "Publishing..." else "Publish & Go Live",
                    accentColor = AmberWarning,
                    enabled = !busy && rId.isNotBlank() && rPass.isNotBlank(),
                    onClick = {
                        adminVm.publishRoom(m, rId, rPass, region, autoPush)
                        publishingRoomMatch = null
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(24.dp))
            }
        }
    }

    rosterMatch?.let { m ->
        ModalBottomSheet(onDismissRequest = { rosterMatch = null }, containerColor = ObsidianSurface) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Text("Slot Roster • ${m.title}", style = MaterialTheme.typography.titleLarge, color = NeonCyan)
                Spacer(Modifier.height(10.dp))
                if (roster.isEmpty()) {
                    Text("No players joined yet.", color = TextMuted)
                } else {
                    LazyColumn(modifier = Modifier.height(340.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(roster, key = { it.userId }) { s ->
                            CyberGlassCard(glowColor = if (s.status == "joined") NeonCyan else CrimsonDanger) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("#${s.slotNumber} • ${s.inGameName} (${s.gameUid})", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                                        Text("${s.displayName} • Status: ${s.status} • Paid: ${ArenaFormatters.formatInr(s.entryPaid)}", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                                    }
                                    if (s.status == "joined") {
                                        Button(
                                            onClick = { adminVm.kickAndRefund(m, s) },
                                            colors = ButtonDefaults.buttonColors(containerColor = CrimsonDanger, contentColor = TextPrimary)
                                        ) { Text("Kick & Refund") }
                                    }
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(20.dp))
            }
        }
    }

    resultsMatch?.let { m ->
        val activeSlots = roster.filter { it.status == "joined" }
        val killsMap = remember(activeSlots) { mutableStateMapOf<String, String>().apply { activeSlots.forEach { put(it.userId, it.kills.toString()) } } }
        val rankMap = remember(activeSlots) { mutableStateMapOf<String, String>().apply { activeSlots.forEach { put(it.userId, it.rank.toString()) } } }
        val bonusPrizeMap = remember(activeSlots) { mutableStateMapOf<String, String>().apply { activeSlots.forEach { put(it.userId, "0") } } }

        val totalEntryCollected = activeSlots.sumOf { it.entryPaid }
        val computedSlots = activeSlots.map { s ->
            val k = killsMap[s.userId]?.toLongOrNull() ?: 0L
            val r = rankMap[s.userId]?.toLongOrNull() ?: 0L
            val rankPrize = bonusPrizeMap[s.userId]?.toDoubleOrNull() ?: 0.0
            val totalWon = (k * m.perKillReward) + rankPrize
            s.copy(kills = k, rank = r, prizeWon = totalWon)
        }
        val totalPayout = computedSlots.sumOf { it.prizeWon }
        val netProfit = totalEntryCollected - totalPayout

        ModalBottomSheet(onDismissRequest = { resultsMatch = null }, containerColor = ObsidianSurface) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Text("Bulk Result & Profit Calculator", style = MaterialTheme.typography.titleLarge, color = EmeraldProfit)
                Text("Collected: ${ArenaFormatters.formatInr(totalEntryCollected)} • Payout: ${ArenaFormatters.formatInr(totalPayout)} • Net Profit: ${ArenaFormatters.formatInr(netProfit)}", style = MaterialTheme.typography.labelLarge, color = if (netProfit >= 0) EmeraldProfit else AmberWarning)
                Spacer(Modifier.height(10.dp))
                if (activeSlots.isEmpty()) {
                    Text("No active joined players to settle.", color = TextMuted)
                } else {
                    LazyColumn(modifier = Modifier.height(300.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(activeSlots, key = { it.userId }) { s ->
                            CyberGlassCard {
                                Text("#${s.slotNumber} ${s.inGameName} (${s.gameUid})", style = MaterialTheme.typography.titleMedium, color = NeonCyan)
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    OutlinedTextField(value = rankMap[s.userId].orEmpty(), onValueChange = { rankMap[s.userId] = it }, label = { Text("Rank") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                                    OutlinedTextField(value = killsMap[s.userId].orEmpty(), onValueChange = { killsMap[s.userId] = it }, label = { Text("Kills") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                                    OutlinedTextField(value = bonusPrizeMap[s.userId].orEmpty(), onValueChange = { bonusPrizeMap[s.userId] = it }, label = { Text("Rank ₹") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    NeonPrimaryButton(
                        text = if (busy) "Distributing..." else "1-Click Distribute ${ArenaFormatters.formatInr(totalPayout)}",
                        accentColor = EmeraldProfit,
                        enabled = !busy,
                        onClick = {
                            adminVm.distributeResults(m, computedSlots)
                            resultsMatch = null
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Spacer(Modifier.height(20.dp))
            }
        }
    }
}

@Composable
fun AdminSupportDeskModule(
    tickets: List<SupportTicket>,
    users: List<UserProfile>,
    ticketMessages: List<TicketMessage>,
    busy: Boolean,
    adminVm: AdminViewModel
) {
    var filter by remember { mutableStateOf("All") }
    var activeTicket by remember { mutableStateOf<SupportTicket?>(null) }
    var zoomImg by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current

    val filtered = remember(tickets, filter) {
        when (filter) {
            "Open" -> tickets.filter { it.status == "open" }
            "In Progress" -> tickets.filter { it.status == "in_progress" }
            "Resolved" -> tickets.filter { it.status == "resolved" || it.status == "closed" }
            "Hackers" -> tickets.filter { it.category.contains("Hacker", true) }
            else -> tickets
        }
    }

    if (activeTicket != null) {
        val t = activeTicket!!
        val userSnap = users.firstOrNull { it.userId == t.userId }
        var replyText by remember { mutableStateOf("") }
        var newStatus by remember { mutableStateOf(t.status) }
        var refundAmt by remember { mutableStateOf("50") }

        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(onClick = { activeTicket = null }) { Text("Back to Queue", color = NeonCyan) }
                StatusPill(t.category, CrimsonDanger)
            }
            Spacer(Modifier.height(8.dp))
            CyberGlassCard(glowColor = ElectricViolet) {
                Text(t.subject, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                Text("User: ${t.userName} (${t.userEmail}) • Bal: ${ArenaFormatters.formatInr(userSnap?.totalBalance ?: 0.0)}", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                if (t.reportedPlayerUid.isNotBlank()) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth().padding(top = 6.dp)) {
                        Text("Reported Suspect UID: ${t.reportedPlayerUid}", style = MaterialTheme.typography.labelLarge, color = CrimsonDanger)
                        OutlinedButton(onClick = {
                            ArenaFormatters.copyToClipboard(context, "Suspect UID", t.reportedPlayerUid)
                            Toast.makeText(context, "UID copied", Toast.LENGTH_SHORT).show()
                        }) { Text("Copy UID") }
                    }
                    val suspect = users.firstOrNull { it.gameUid == t.reportedPlayerUid || it.userId == t.reportedPlayerUid }
                    if (suspect != null) {
                        Button(
                            onClick = { adminVm.toggleBanUser(suspect.userId, true, "Banned via Hacker Report ${t.id}") },
                            colors = ButtonDefaults.buttonColors(containerColor = CrimsonDanger, contentColor = TextPrimary),
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            Icon(Icons.Default.Block, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Ban Suspect (${suspect.displayName})")
                        }
                    }
                }
                if (t.screenshotBase64.isNotBlank()) {
                    val bmp = remember(t.screenshotBase64) { QrCodeUtils.decodeBase64Image(t.screenshotBase64) }
                    if (bmp != null) {
                        Spacer(Modifier.height(6.dp))
                        Image(
                            bitmap = bmp.asImageBitmap(),
                            contentDescription = "Evidence Screenshot",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxWidth().height(90.dp).clip(RoundedCornerShape(10.dp)).clickable { zoomImg = t.screenshotBase64 }
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                    OutlinedTextField(value = refundAmt, onValueChange = { refundAmt = it }, label = { Text("Direct Refund ₹") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                    Button(
                        onClick = {
                            val amt = refundAmt.toDoubleOrNull() ?: 0.0
                            if (amt > 0) adminVm.adjustUserWallet(t.userId, "deposit", true, amt, "Support Ticket Refund #${t.id.take(6)}")
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldProfit, contentColor = ObsidianBg)
                    ) { Text("Refund") }
                }
            }

            Spacer(Modifier.height(8.dp))
            LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                items(ticketMessages, key = { it.id }) { m ->
                    CyberGlassCard(glowColor = if (m.senderRole == "admin") EmeraldProfit else NeonCyan) {
                        Text(m.senderName, style = MaterialTheme.typography.labelSmall, color = if (m.senderRole == "admin") EmeraldProfit else NeonCyan)
                        Text(m.text, style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                    }
                }
            }

            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(vertical = 6.dp)) {
                items(listOf("open", "in_progress", "resolved", "closed")) { st ->
                    FilterChip(selected = newStatus == st, onClick = { newStatus = st }, label = { Text(st) })
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = replyText, onValueChange = { replyText = it }, label = { Text("Admin Reply") }, modifier = Modifier.weight(1f))
                NeonPrimaryButton(
                    text = "Send",
                    enabled = !busy,
                    onClick = {
                        adminVm.replyTicket(t, replyText, newStatus)
                        replyText = ""
                    }
                )
            }
        }
    } else {
        LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item { SectionHeader("Support & Hacker Report Desk", "${tickets.size} total tickets") }
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(listOf("All", "Open", "In Progress", "Resolved", "Hackers")) { f ->
                        FilterChip(selected = filter == f, onClick = { filter = f }, label = { Text(f) })
                    }
                }
            }
            items(filtered, key = { it.id }) { t ->
                CyberGlassCard(
                    glowColor = if (t.category.contains("Hacker", true)) CrimsonDanger else AmberWarning,
                    onClick = {
                        adminVm.observeTicketThread(t.id)
                        activeTicket = t
                    }
                ) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        StatusPill(t.category, ElectricViolet)
                        StatusPill(t.status, if (t.status == "resolved") EmeraldProfit else AmberWarning)
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(t.subject, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                    Text("${t.userName} • ${t.lastMessage}", style = MaterialTheme.typography.labelSmall, color = TextMuted, maxLines = 2)
                }
            }
        }
    }

    zoomImg?.let { raw ->
        Dialog(onDismissRequest = { zoomImg = null }) {
            val bmp = QrCodeUtils.decodeBase64Image(raw)
            if (bmp != null) {
                Box(modifier = Modifier.clip(RoundedCornerShape(16.dp)).background(ObsidianElevated).padding(8.dp)) {
                    Image(bitmap = bmp.asImageBitmap(), contentDescription = "Evidence Zoom", modifier = Modifier.fillMaxWidth())
                }
            }
        }
    }
}
