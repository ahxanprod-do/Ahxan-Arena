package com.example.ui.screens.admin

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.GameCategory
import com.example.data.model.Tournament
import com.example.ui.components.CyberGlassCard
import com.example.ui.components.KpiMiniStat
import com.example.ui.components.MatchBannerImage
import com.example.ui.components.NeonPrimaryButton
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatusPill
import com.example.ui.theme.ArenaBlue
import com.example.ui.theme.ArenaGreen
import com.example.ui.theme.ArenaOrange
import com.example.ui.theme.ArenaPurple
import com.example.ui.theme.CoinYellow
import com.example.ui.theme.CrimsonDanger
import com.example.ui.theme.HeaderNavy
import com.example.ui.theme.ObsidianElevated
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.utils.ArenaFormatters
import com.example.utils.QrCodeUtils
import com.example.viewmodel.AdminViewModel
import kotlinx.coroutines.delay
import java.util.Calendar

@Composable
fun AdminTournamentSchedulerModule(
    tournaments: List<Tournament>,
    categories: List<GameCategory> = emptyList(),
    busy: Boolean,
    adminVm: AdminViewModel
) {
    var nowMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            nowMillis = System.currentTimeMillis()
            delay(1000L)
        }
    }

    var filterTab by remember { mutableStateOf("All") }
    var schedulingMatch by remember { mutableStateOf<Tournament?>(null) }

    val upcomingMatches = remember(tournaments) {
        tournaments.filter { it.status == "upcoming" }.sortedBy { it.scheduledAtMillis }
    }
    val openCount = remember(upcomingMatches, nowMillis) {
        upcomingMatches.count { it.isRegistrationOpen(nowMillis) }
    }
    val pendingOpenCount = remember(upcomingMatches, nowMillis) {
        upcomingMatches.count { it.isRegistrationNotStarted(nowMillis) }
    }
    val closedCount = remember(upcomingMatches, nowMillis) {
        upcomingMatches.count { it.isRegistrationClosed(nowMillis) }
    }

    val filtered = remember(upcomingMatches, filterTab, nowMillis) {
        when (filterTab) {
            "Reg Open" -> upcomingMatches.filter { it.isRegistrationOpen(nowMillis) }
            "Opens Soon" -> upcomingMatches.filter { it.isRegistrationNotStarted(nowMillis) }
            "Reg Closed" -> upcomingMatches.filter { it.isRegistrationClosed(nowMillis) }
            else -> upcomingMatches
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            SectionHeader(
                title = "Tournament Scheduler",
                subtitle = "Auto registration open/close timestamps"
            )
            Spacer(Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                KpiMiniStat("Upcoming", "${upcomingMatches.size}", ArenaOrange, Modifier.weight(1f))
                KpiMiniStat("Reg Open", "$openCount", ArenaGreen, Modifier.weight(1f))
                KpiMiniStat("Pending", "$pendingOpenCount", CoinYellow, Modifier.weight(1f))
                KpiMiniStat("Closed", "$closedCount", CrimsonDanger, Modifier.weight(1f))
            }
            Spacer(Modifier.height(10.dp))
            NeonPrimaryButton(
                text = "Schedule Upcoming Tournament",
                icon = Icons.Default.CalendarMonth,
                accentColor = ArenaOrange,
                onClick = {
                    val now = System.currentTimeMillis()
                    val matchStart = now + 3600_000L * 3
                    schedulingMatch = Tournament(
                        scheduledAtMillis = matchStart,
                        registrationOpenAtMillis = now + 15 * 60_000L,
                        registrationCloseAtMillis = matchStart - 15 * 60_000L,
                        maxSlots = 100L,
                        entryFee = 20.0,
                        prizePool = 1500.0,
                        perKillReward = 15.0
                    )
                },
                modifier = Modifier.fillMaxWidth().testTag("admin_schedule_new_tournament_btn")
            )
            Spacer(Modifier.height(10.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(listOf("All", "Reg Open", "Opens Soon", "Reg Closed")) { tab ->
                    FilterChip(
                        selected = filterTab == tab,
                        onClick = { filterTab = tab },
                        label = { Text(tab) },
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
                        text = "No scheduled upcoming tournaments in this filter. Tap 'Schedule Upcoming Tournament' to create one with automatic registration open & close times.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
            }
        }

        items(filtered, key = { it.id }) { m ->
            val regNotStarted = m.isRegistrationNotStarted(nowMillis)
            val regClosed = m.isRegistrationClosed(nowMillis)
            val isFull = m.filledSlots >= m.maxSlots

            val (pillText, pillColor) = when {
                isFull -> "SLOTS FULL (${m.filledSlots}/${m.maxSlots})" to CrimsonDanger
                regNotStarted -> "OPENS IN ${ArenaFormatters.formatCountdown(m.registrationOpenAtMillis, nowMillis)}" to CoinYellow
                regClosed -> "REG CLOSED" to CrimsonDanger
                m.registrationCloseAtMillis > 0L -> "OPEN • CLOSES IN ${ArenaFormatters.formatCountdown(m.registrationCloseAtMillis, nowMillis)}" to ArenaGreen
                else -> "OPEN NOW" to ArenaGreen
            }

            CyberGlassCard(glowColor = pillColor) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        MatchBannerImage(
                            bannerUrl = m.bannerUrl,
                            bannerKey = m.bannerKey,
                            contentDescription = m.title,
                            modifier = Modifier
                                .size(54.dp)
                                .clip(RoundedCornerShape(10.dp))
                        )
                        Spacer(Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(m.title, style = MaterialTheme.typography.titleMedium, color = TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("${m.game} • ${m.mode} • ${m.filledSlots}/${m.maxSlots} slots", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                            Text("Entry: ${m.entryFee.toInt()} C • Pool: ${m.prizePool.toInt()} C", style = MaterialTheme.typography.labelSmall, color = ArenaGreen)
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                StatusPill(pillText, pillColor)
                Spacer(Modifier.height(8.dp))

                Surface(
                    color = ObsidianElevated,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, HeaderNavy),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        TimelineRow("Reg Opens", if (m.registrationOpenAtMillis > 0L) ArenaFormatters.formatTimestampMillis(m.registrationOpenAtMillis) else "Immediate", CoinYellow)
                        TimelineRow("Reg Closes", if (m.registrationCloseAtMillis > 0L) ArenaFormatters.formatTimestampMillis(m.registrationCloseAtMillis) else "Until Full / Start", ArenaOrange)
                        TimelineRow("Match Start", ArenaFormatters.formatTimestampMillis(m.scheduledAtMillis), ArenaBlue)
                    }
                }

                Spacer(Modifier.height(10.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    item {
                        OutlinedButton(
                            onClick = { schedulingMatch = m },
                            modifier = Modifier.testTag("admin_edit_schedule_${m.id}")
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null, tint = ArenaOrange, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Edit Schedule", color = ArenaOrange)
                        }
                    }
                    if (regNotStarted || regClosed) {
                        item {
                            OutlinedButton(
                                enabled = !busy,
                                onClick = {
                                    val now = System.currentTimeMillis()
                                    val newClose = if (m.registrationCloseAtMillis <= now) {
                                        maxOf(now + 3600_000L, m.scheduledAtMillis - 15 * 60_000L)
                                    } else {
                                        m.registrationCloseAtMillis
                                    }
                                    adminVm.saveTournament(
                                        m.copy(
                                            registrationOpenAtMillis = now,
                                            registrationCloseAtMillis = newClose
                                        )
                                    )
                                },
                                modifier = Modifier.testTag("admin_open_reg_now_${m.id}")
                            ) {
                                Icon(Icons.Default.LockOpen, contentDescription = null, tint = ArenaGreen, modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Open Reg Now", color = ArenaGreen)
                            }
                        }
                    }
                    if (!regClosed) {
                        item {
                            OutlinedButton(
                                enabled = !busy,
                                onClick = {
                                    adminVm.saveTournament(m.copy(registrationCloseAtMillis = System.currentTimeMillis()))
                                },
                                modifier = Modifier.testTag("admin_close_reg_now_${m.id}")
                            ) {
                                Icon(Icons.Default.Lock, contentDescription = null, tint = CrimsonDanger, modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Close Reg Now", color = CrimsonDanger)
                            }
                        }
                    }
                    item {
                        OutlinedButton(
                            enabled = !busy,
                            onClick = { adminVm.cloneTournament(m) }
                        ) {
                            Text("Clone", color = ArenaPurple)
                        }
                    }
                }
            }
        }
    }

    schedulingMatch?.let { init ->
        ScheduleTournamentSheet(
            init = init,
            categories = categories,
            busy = busy,
            adminVm = adminVm,
            onDismiss = { schedulingMatch = null }
        )
    }
}

@Composable
private fun TimelineRow(label: String, value: String, color: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = TextMuted)
        Text(value, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = color)
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ScheduleTournamentSheet(
    init: Tournament,
    categories: List<GameCategory>,
    busy: Boolean,
    adminVm: AdminViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var title by remember(init.id) { mutableStateOf(init.title) }
    var game by remember(init.id) { mutableStateOf(init.game) }
    var mode by remember(init.id) { mutableStateOf(init.mode) }
    var mapName by remember(init.id) { mutableStateOf(init.mapName) }
    var entryText by remember(init.id) { mutableStateOf(init.entryFee.toInt().toString()) }
    var poolText by remember(init.id) { mutableStateOf(init.prizePool.toInt().toString()) }
    var killText by remember(init.id) { mutableStateOf(init.perKillReward.toInt().toString()) }
    var maxSlotsText by remember(init.id) { mutableStateOf(init.maxSlots.toString()) }
    var bannerKey by remember(init.id) { mutableStateOf(init.bannerKey.ifBlank { "bgmi" }) }
    var bannerUrl by remember(init.id) { mutableStateOf(init.bannerUrl) }
    var rules by remember(init.id) { mutableStateOf(init.rulesText) }
    var breakdown by remember(init.id) { mutableStateOf(init.prizeBreakdown) }

    val defaultMatchStart = remember(init.id) {
        if (init.scheduledAtMillis > 0L) init.scheduledAtMillis else System.currentTimeMillis() + 3600_000L * 3
    }
    var scheduledAt by remember(init.id) { mutableLongStateOf(defaultMatchStart) }
    var regOpenAt by remember(init.id) { mutableLongStateOf(init.registrationOpenAtMillis) }
    var regCloseAt by remember(init.id) { mutableLongStateOf(init.registrationCloseAtMillis) }

    val bannerPicker = rememberLauncherForActivityResult(
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

    val scheduleInvalid = (regOpenAt > 0L && regCloseAt > 0L && regCloseAt <= regOpenAt) ||
        (regCloseAt > 0L && scheduledAt > 0L && regCloseAt > scheduledAt)

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = ObsidianSurface) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Text(
                text = if (init.id.isBlank()) "Schedule Upcoming Tournament" else "Update Tournament Schedule",
                style = MaterialTheme.typography.headlineSmall,
                color = ArenaOrange
            )
            Text(
                text = "Set automatic registration opening & closing windows + match kickoff.",
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted
            )
            Spacer(Modifier.height(10.dp))

            // Banner Preview & Upload
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(105.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, ArenaOrange.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
            ) {
                MatchBannerImage(
                    bannerUrl = bannerUrl,
                    bannerKey = bannerKey,
                    contentDescription = "Tournament Banner",
                    modifier = Modifier.fillMaxSize()
                )
            }
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { bannerPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Image, contentDescription = null, tint = ArenaOrange, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Pick Banner", color = ArenaOrange)
                }
                if (bannerUrl.isNotBlank()) {
                    OutlinedButton(onClick = { bannerUrl = "" }) {
                        Text("Reset", color = CoinYellow)
                    }
                }
            }
            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Tournament Title *") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("scheduler_title_input")
            )
            Spacer(Modifier.height(6.dp))

            val gameChips = remember(categories) {
                val defaults = listOf("BGMI", "Free Fire MAX", "Clash Squad", "BGMI TDM", "FF Survival", "Lone Wolf", "COD Mobile", "Valorant")
                val custom = categories.map { it.filterKey.ifBlank { it.title }.trim() }.filter { it.isNotBlank() && !it.equals("All", true) }
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
                                    it.filterKey.equals(g, true) || it.title.equals(g, true)
                                }
                                bannerKey = matchingCat?.drawableKey?.ifBlank { "bgmi" } ?: when {
                                    g.contains("Free", true) || g.contains("Clash", true) || g.contains("FF", true) -> "freefire"
                                    g.contains("COD", true) -> "codm"
                                    else -> "bgmi"
                                }
                            }
                        },
                        label = { Text(g) }
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("Solo", "Duo", "Squad").forEach { md ->
                    FilterChip(selected = mode == md, onClick = { mode = md }, label = { Text(md) })
                }
            }
            OutlinedTextField(value = mapName, onValueChange = { mapName = it }, label = { Text("Map / Arena") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = entryText, onValueChange = { entryText = it }, label = { Text("Entry Coins") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                OutlinedTextField(value = poolText, onValueChange = { poolText = it }, label = { Text("Prize Pool") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
            }
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = killText, onValueChange = { killText = it }, label = { Text("Per Kill") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                OutlinedTextField(value = maxSlotsText, onValueChange = { maxSlotsText = it }, label = { Text("Max Slots") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
            }

            Spacer(Modifier.height(14.dp))
            Text("Automatic Registration & Match Timestamps", style = MaterialTheme.typography.titleMedium, color = CoinYellow)
            Spacer(Modifier.height(6.dp))

            // 1. Registration Opens At
            TimestampSelectorCard(
                label = "1. Registration Opens At",
                millis = regOpenAt,
                unrestrictedLabel = "Open Immediately",
                accent = CoinYellow,
                onPickDateTime = { pickDateTime(context, if (regOpenAt > 0L) regOpenAt else System.currentTimeMillis()) { regOpenAt = it } },
                quickChips = listOf(
                    "Immediate" to { regOpenAt = 0L },
                    "Now" to { regOpenAt = System.currentTimeMillis() },
                    "+15m" to { regOpenAt = System.currentTimeMillis() + 15 * 60_000L },
                    "+1h" to { regOpenAt = System.currentTimeMillis() + 3600_000L },
                    "+3h" to { regOpenAt = System.currentTimeMillis() + 3 * 3600_000L }
                )
            )
            Spacer(Modifier.height(8.dp))

            // 2. Registration Closes At
            TimestampSelectorCard(
                label = "2. Registration Closes At",
                millis = regCloseAt,
                unrestrictedLabel = "Until Full / Start",
                accent = ArenaOrange,
                onPickDateTime = { pickDateTime(context, if (regCloseAt > 0L) regCloseAt else (scheduledAt - 15 * 60_000L)) { regCloseAt = it } },
                quickChips = listOf(
                    "No Cutoff" to { regCloseAt = 0L },
                    "15m Before" to { regCloseAt = (scheduledAt - 15 * 60_000L).coerceAtLeast(System.currentTimeMillis() + 60_000L) },
                    "30m Before" to { regCloseAt = (scheduledAt - 30 * 60_000L).coerceAtLeast(System.currentTimeMillis() + 60_000L) },
                    "1h Before" to { regCloseAt = (scheduledAt - 3600_000L).coerceAtLeast(System.currentTimeMillis() + 60_000L) }
                )
            )
            Spacer(Modifier.height(8.dp))

            // 3. Match Start Kickoff
            TimestampSelectorCard(
                label = "3. Match Start Time",
                millis = scheduledAt,
                unrestrictedLabel = "TBD",
                accent = ArenaGreen,
                onPickDateTime = { pickDateTime(context, scheduledAt) { scheduledAt = it } },
                quickChips = listOf(
                    "+1h" to { scheduledAt = System.currentTimeMillis() + 3600_000L },
                    "+3h" to { scheduledAt = System.currentTimeMillis() + 3 * 3600_000L },
                    "+6h" to { scheduledAt = System.currentTimeMillis() + 6 * 3600_000L },
                    "8 PM Today" to {
                        val cal = Calendar.getInstance().apply {
                            set(Calendar.HOUR_OF_DAY, 20)
                            set(Calendar.MINUTE, 0)
                            set(Calendar.SECOND, 0)
                            if (timeInMillis <= System.currentTimeMillis()) add(Calendar.DAY_OF_MONTH, 1)
                        }
                        scheduledAt = cal.timeInMillis
                    }
                )
            )

            if (scheduleInvalid) {
                Spacer(Modifier.height(8.dp))
                Surface(
                    color = CrimsonDanger.copy(alpha = 0.16f),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, CrimsonDanger),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "Invalid window: Registration Close must be after Registration Open and before Match Start.",
                            color = CrimsonDanger,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.height(4.dp))
                        OutlinedButton(onClick = {
                            if (scheduledAt <= System.currentTimeMillis()) scheduledAt = System.currentTimeMillis() + 3 * 3600_000L
                            regCloseAt = scheduledAt - 15 * 60_000L
                            if (regOpenAt >= regCloseAt) regOpenAt = System.currentTimeMillis()
                        }) {
                            Text("Auto-Align Times", color = CoinYellow, fontSize = 11.sp)
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            OutlinedTextField(value = breakdown, onValueChange = { breakdown = it }, label = { Text("Prize Breakdown") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(6.dp))
            OutlinedTextField(value = rules, onValueChange = { rules = it }, label = { Text("Match Rules") }, minLines = 2, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(14.dp))

            NeonPrimaryButton(
                text = if (busy) "Saving Schedule..." else if (init.id.isBlank()) "Create & Schedule Tournament" else "Save Tournament Schedule",
                icon = Icons.Default.Schedule,
                accentColor = ArenaGreen,
                enabled = !busy && title.isNotBlank() && !scheduleInvalid,
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
                            scheduledAtMillis = scheduledAt,
                            registrationOpenAtMillis = regOpenAt,
                            registrationCloseAtMillis = regCloseAt,
                            bannerKey = bannerKey,
                            bannerUrl = bannerUrl.trim(),
                            rulesText = rules,
                            prizeBreakdown = breakdown
                        )
                    )
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth().testTag("scheduler_save_btn")
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TimestampSelectorCard(
    label: String,
    millis: Long,
    unrestrictedLabel: String,
    accent: Color,
    onPickDateTime: () -> Unit,
    quickChips: List<Pair<String, () -> Unit>>
) {
    Surface(
        color = ObsidianElevated,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.45f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(label, style = MaterialTheme.typography.labelLarge, color = accent, fontWeight = FontWeight.Bold)
                    Text(
                        text = if (millis > 0L) ArenaFormatters.formatTimestampMillis(millis) else unrestrictedLabel,
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary
                    )
                }
                OutlinedButton(
                    onClick = onPickDateTime,
                    border = BorderStroke(1.dp, accent)
                ) {
                    Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = accent, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Pick", color = accent, fontSize = 12.sp)
                }
            }
            Spacer(Modifier.height(6.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                quickChips.forEach { (chipLabel, onClick) ->
                    FilterChip(
                        selected = false,
                        onClick = onClick,
                        label = { Text(chipLabel, fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = HeaderNavy,
                            labelColor = TextSecondary
                        )
                    )
                }
            }
        }
    }
}

fun pickDateTime(
    context: android.content.Context,
    initialMillis: Long,
    onPicked: (Long) -> Unit
) {
    val cal = Calendar.getInstance().apply {
        timeInMillis = if (initialMillis > 0L) initialMillis else System.currentTimeMillis()
    }
    DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            TimePickerDialog(
                context,
                { _, hourOfDay, minute ->
                    val pickedCal = Calendar.getInstance().apply {
                        set(year, month, dayOfMonth, hourOfDay, minute, 0)
                        set(Calendar.MILLISECOND, 0)
                    }
                    onPicked(pickedCal.timeInMillis)
                },
                cal.get(Calendar.HOUR_OF_DAY),
                cal.get(Calendar.MINUTE),
                false
            ).show()
        },
        cal.get(Calendar.YEAR),
        cal.get(Calendar.MONTH),
        cal.get(Calendar.DAY_OF_MONTH)
    ).show()
}
