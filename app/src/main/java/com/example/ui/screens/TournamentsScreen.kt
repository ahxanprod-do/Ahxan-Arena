package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.Coupon
import com.example.data.model.MatchSlot
import com.example.data.model.RoomSecret
import com.example.data.model.Tournament
import com.example.data.model.UserProfile
import com.example.ui.components.CyberGlassCard
import com.example.ui.components.NeonPrimaryButton
import com.example.ui.components.SlotsProgressBar
import com.example.ui.components.StatusPill
import com.example.ui.components.bannerDrawableForKey
import com.example.ui.theme.AmberWarning
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun TournamentsScreen(
    tournaments: List<Tournament>,
    joinedMatches: List<MatchSlot>,
    onSelectTournament: (Tournament) -> Unit
) {
    val tabs = listOf("Upcoming", "Live", "Completed")
    var selectedTab by remember { mutableStateOf(0) }
    var selectedGame by remember { mutableStateOf("All") }
    var selectedMode by remember { mutableStateOf("All") }
    var selectedEntry by remember { mutableStateOf("All") }

    val statusKey = tabs[selectedTab].lowercase()
    val filtered = remember(tournaments, statusKey, selectedGame, selectedMode, selectedEntry) {
        tournaments.filter { m ->
            m.status == statusKey &&
                (selectedGame == "All" || m.game.equals(selectedGame, ignoreCase = true)) &&
                (selectedMode == "All" || m.mode.equals(selectedMode, ignoreCase = true)) &&
                (selectedEntry == "All" || m.entryType.equals(selectedEntry, ignoreCase = true))
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "TOURNAMENTS",
            style = MaterialTheme.typography.headlineMedium,
            color = NeonCyan,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
        )

        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = ObsidianSurface,
            contentColor = NeonCyan
        ) {
            tabs.forEachIndexed { idx, title ->
                Tab(
                    selected = selectedTab == idx,
                    onClick = { selectedTab = idx },
                    text = { Text(title, style = MaterialTheme.typography.labelLarge) }
                )
            }
        }

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(listOf("All", "BGMI", "Free Fire MAX", "COD Mobile", "Valorant")) { g ->
                FilterChip(
                    selected = selectedGame == g,
                    onClick = { selectedGame = g },
                    label = { Text(g) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = NeonCyan,
                        selectedLabelColor = ObsidianBg,
                        containerColor = ObsidianElevated
                    )
                )
            }
            items(listOf("Solo", "Duo", "Squad")) { mode ->
                FilterChip(
                    selected = selectedMode == mode,
                    onClick = { selectedMode = if (selectedMode == mode) "All" else mode },
                    label = { Text(mode) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = ElectricViolet,
                        selectedLabelColor = TextPrimary,
                        containerColor = ObsidianElevated
                    )
                )
            }
            items(listOf("Free", "Paid")) { entry ->
                FilterChip(
                    selected = selectedEntry == entry,
                    onClick = { selectedEntry = if (selectedEntry == entry) "All" else entry },
                    label = { Text(entry) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = EmeraldProfit,
                        selectedLabelColor = ObsidianBg,
                        containerColor = ObsidianElevated
                    )
                )
            }
        }

        if (filtered.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                CyberGlassCard {
                    Text("No $statusKey matches found", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
                    Spacer(Modifier.height(6.dp))
                    Text("Try clearing filters or check another status tab.", style = MaterialTheme.typography.bodyMedium, color = TextMuted)
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filtered, key = { it.id }) { match ->
                    val alreadyJoined = joinedMatches.any { it.matchId == match.id && it.status != "kicked" && it.status != "refunded" }
                    Column {
                        TournamentLobbyCard(match = match, onClick = { onSelectTournament(match) })
                        if (alreadyJoined) {
                            Spacer(Modifier.height(4.dp))
                            StatusPill(label = "JOINED • TAP FOR ROOM DETAILS", color = EmeraldProfit)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TournamentDetailSheet(
    match: Tournament,
    user: UserProfile?,
    joinedSlot: MatchSlot?,
    roomSecret: RoomSecret?,
    busy: Boolean,
    onLoadRoomSecret: (String) -> Unit,
    onValidateCoupon: suspend (String, Double) -> Result<Coupon>,
    onJoinMatch: (Tournament, String, String, List<String>, String, () -> Unit) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var showJoinForm by remember { mutableStateOf(false) }
    var ign by remember(user) { mutableStateOf(user?.inGameName.orEmpty().ifBlank { user?.displayName.orEmpty() }) }
    var gameUid by remember(user) { mutableStateOf(user?.gameUid.orEmpty()) }
    var teammate2 by remember { mutableStateOf("") }
    var teammate3 by remember { mutableStateOf("") }
    var teammate4 by remember { mutableStateOf("") }
    var couponCode by remember { mutableStateOf("") }
    var appliedCoupon by remember { mutableStateOf<Coupon?>(null) }
    var couponError by remember { mutableStateOf<String?>(null) }

    var remainingMillis by remember(match.scheduledAtMillis) {
        mutableStateOf((match.scheduledAtMillis - System.currentTimeMillis()).coerceAtLeast(0L))
    }

    LaunchedEffect(match.id, joinedSlot, match.roomPublished) {
        if (joinedSlot != null && match.roomPublished) {
            onLoadRoomSecret(match.id)
        }
    }

    LaunchedEffect(match.scheduledAtMillis) {
        while (true) {
            remainingMillis = (match.scheduledAtMillis - System.currentTimeMillis()).coerceAtLeast(0L)
            delay(1000L)
        }
    }

    val canRevealRoom = joinedSlot != null && (match.roomPublished || remainingMillis <= 15 * 60 * 1000L)
    val discount = appliedCoupon?.computeBenefit(match.entryFee) ?: 0.0
    val finalFee = (match.entryFee - discount).coerceAtLeast(0.0)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = ObsidianSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .clip(RoundedCornerShape(16.dp))
            ) {
                Image(
                    painter = painterResource(id = bannerDrawableForKey(match.bannerKey)),
                    contentDescription = match.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Brush.verticalGradient(listOf(Color.Transparent, ObsidianBg.copy(alpha = 0.9f))))
                )
                Column(modifier = Modifier.align(Alignment.BottomStart).padding(12.dp)) {
                    StatusPill(label = "${match.game} • ${match.mode}", color = NeonCyan)
                    Spacer(Modifier.height(4.dp))
                    Text(match.title, style = MaterialTheme.typography.titleLarge, color = TextPrimary)
                }
            }

            Spacer(Modifier.height(12.dp))

            // Countdown + Slots
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Timer, contentDescription = null, tint = AmberWarning, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    val hrs = remainingMillis / 3600000L
                    val mins = (remainingMillis % 3600000L) / 60000L
                    val secs = (remainingMillis % 60000L) / 1000L
                    Text(
                        text = if (remainingMillis > 0L) "Starts in %02dh %02dm %02ds".format(hrs, mins, secs) else "Match Time Reached",
                        style = MaterialTheme.typography.labelLarge,
                        color = AmberWarning
                    )
                }
                Text("Map: ${match.mapName}", style = MaterialTheme.typography.labelSmall, color = TextMuted)
            }

            Spacer(Modifier.height(10.dp))
            SlotsProgressBar(filled = match.filledSlots, max = match.maxSlots)
            Spacer(Modifier.height(12.dp))

            // Prize & Entry Breakdown
            CyberGlassCard(glowColor = EmeraldProfit) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text("PRIZE POOL", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                        Text(ArenaFormatters.formatInr(match.prizePool), style = MaterialTheme.typography.titleLarge, color = EmeraldProfit)
                    }
                    Column {
                        Text("PER KILL", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                        Text(ArenaFormatters.formatInr(match.perKillReward), style = MaterialTheme.typography.titleLarge, color = AmberWarning)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("ENTRY FEE", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                        Text(
                            if (match.entryFee <= 0.0) "FREE" else ArenaFormatters.formatInr(match.entryFee),
                            style = MaterialTheme.typography.titleLarge,
                            color = NeonCyan
                        )
                    }
                }
                if (match.prizeBreakdown.isNotBlank()) {
                    Spacer(Modifier.height(8.dp))
                    Text(match.prizeBreakdown, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                }
            }

            Spacer(Modifier.height(12.dp))

            // Room ID & Password Section
            if (joinedSlot != null) {
                CyberGlassCard(glowColor = if (canRevealRoom && roomSecret != null) EmeraldProfit else AmberWarning) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (canRevealRoom && roomSecret != null) Icons.Default.LockOpen else Icons.Default.Lock,
                            contentDescription = null,
                            tint = if (canRevealRoom && roomSecret != null) EmeraldProfit else AmberWarning
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Room Credentials (Slot #${joinedSlot.slotNumber})", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                    }
                    Spacer(Modifier.height(8.dp))
                    if (canRevealRoom && roomSecret != null) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("ROOM ID: ${roomSecret.roomId}", style = MaterialTheme.typography.titleMedium, color = NeonCyan)
                                Text("PASS: ${roomSecret.roomPassword}", style = MaterialTheme.typography.titleMedium, color = EmeraldProfit)
                            }
                            Row {
                                IconButton(onClick = {
                                    ArenaFormatters.copyToClipboard(context, "Room ID", roomSecret.roomId)
                                    Toast.makeText(context, "Room ID copied", Toast.LENGTH_SHORT).show()
                                }) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy Room ID", tint = NeonCyan)
                                }
                                IconButton(onClick = {
                                    ArenaFormatters.copyToClipboard(context, "Room Pass", roomSecret.roomPassword)
                                    Toast.makeText(context, "Password copied", Toast.LENGTH_SHORT).show()
                                }) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy Password", tint = EmeraldProfit)
                                }
                            }
                        }
                    } else {
                        Text(
                            text = "Locked until 15 mins before match start or when Admin publishes Room ID.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextMuted
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
            }

            // Rules
            if (match.rulesText.isNotBlank()) {
                CyberGlassCard(glowColor = ElectricViolet) {
                    Text("Match Rules", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                    Spacer(Modifier.height(4.dp))
                    Text(match.rulesText, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                }
                Spacer(Modifier.height(12.dp))
            }

            // Join Action / Slot Picker + Coupon Validator
            if (joinedSlot == null && match.status == "upcoming" && match.filledSlots < match.maxSlots) {
                if (!showJoinForm) {
                    NeonPrimaryButton(
                        text = "Join Match • ${if (match.entryFee <= 0.0) "FREE" else ArenaFormatters.formatInr(match.entryFee)}",
                        onClick = { showJoinForm = true },
                        modifier = Modifier.fillMaxWidth().testTag("open_join_match_form_btn")
                    )
                } else {
                    CyberGlassCard(glowColor = NeonCyan) {
                        Text("Slot Registration", style = MaterialTheme.typography.titleLarge, color = NeonCyan)
                        Spacer(Modifier.height(10.dp))
                        OutlinedTextField(
                            value = ign,
                            onValueChange = { ign = it },
                            label = { Text("In-Game Name (IGN)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("join_ign_input")
                        )
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = gameUid,
                            onValueChange = { gameUid = it },
                            label = { Text("Game UID / Character ID") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("join_uid_input")
                        )
                        if (match.mode == "Duo" || match.mode == "Squad") {
                            Spacer(Modifier.height(8.dp))
                            OutlinedTextField(
                                value = teammate2,
                                onValueChange = { teammate2 = it },
                                label = { Text("Teammate 2 IGN (Optional)") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        if (match.mode == "Squad") {
                            Spacer(Modifier.height(8.dp))
                            OutlinedTextField(
                                value = teammate3,
                                onValueChange = { teammate3 = it },
                                label = { Text("Teammate 3 IGN (Optional)") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(Modifier.height(8.dp))
                            OutlinedTextField(
                                value = teammate4,
                                onValueChange = { teammate4 = it },
                                label = { Text("Teammate 4 IGN (Optional)") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        if (match.entryFee > 0.0) {
                            Spacer(Modifier.height(10.dp))
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = couponCode,
                                    onValueChange = { couponCode = it.uppercase() },
                                    label = { Text("Match Coupon (e.g. FIRST50)") },
                                    leadingIcon = { Icon(Icons.Default.LocalOffer, contentDescription = null, tint = NeonCyan) },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f).testTag("join_coupon_input")
                                )
                                Button(
                                    onClick = {
                                        couponError = null
                                        scope.launch {
                                            val res = onValidateCoupon(couponCode, match.entryFee)
                                            res.onSuccess { appliedCoupon = it }
                                                .onFailure {
                                                    appliedCoupon = null
                                                    couponError = it.localizedMessage
                                                }
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = ElectricViolet)
                                ) {
                                    Text("Apply")
                                }
                            }
                            if (appliedCoupon != null) {
                                Text(
                                    text = "Applied ${appliedCoupon!!.code}: -${ArenaFormatters.formatInr(discount)} OFF!",
                                    color = EmeraldProfit,
                                    style = MaterialTheme.typography.labelLarge,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            } else if (couponError != null) {
                                Text(couponError!!, color = Color(0xFFFF3B5C), style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(top = 4.dp))
                            }
                        }

                        Spacer(Modifier.height(14.dp))
                        NeonPrimaryButton(
                            text = if (busy) "Confirming Slot..." else "Confirm & Pay ${ArenaFormatters.formatInr(finalFee)}",
                            enabled = !busy,
                            onClick = {
                                onJoinMatch(
                                    match,
                                    ign,
                                    gameUid,
                                    listOf(teammate2, teammate3, teammate4).filter { it.isNotBlank() },
                                    appliedCoupon?.code ?: "",
                                    onDismiss
                                )
                            },
                            modifier = Modifier.fillMaxWidth().testTag("confirm_join_match_btn")
                        )
                    }
                }
            }

            Spacer(Modifier.height(28.dp))
        }
    }
}
