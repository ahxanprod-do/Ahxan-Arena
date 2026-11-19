package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.Coupon
import com.example.data.model.GameCategory
import com.example.data.model.GeneralConfig
import com.example.data.model.MatchSlot
import com.example.data.model.RoomSecret
import com.example.data.model.Tournament
import com.example.data.model.UserProfile
import com.example.ui.components.CoinBadge
import com.example.ui.components.GameCategoryGrid
import com.example.ui.components.MatchBannerImage
import com.example.ui.components.StatusPill
import com.example.ui.components.SubScreenTopBar
import com.example.ui.components.matchesCategoryFilter
import com.example.ui.components.resolveGameCategories
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.ArenaBlue
import com.example.ui.theme.ArenaCharcoal
import com.example.ui.theme.ArenaGreen
import com.example.ui.theme.ArenaOrange
import com.example.ui.theme.ArenaPurple
import com.example.ui.theme.CardDarkText
import com.example.ui.theme.CardMutedText
import com.example.ui.theme.CoinYellow
import com.example.ui.theme.CrimsonDanger
import com.example.ui.theme.HeaderNavy
import com.example.ui.theme.TextSecondary
import com.example.utils.ArenaFormatters
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun TournamentsScreen(
    tournaments: List<Tournament>,
    joinedMatches: List<MatchSlot>,
    generalConfig: GeneralConfig = GeneralConfig(),
    categories: List<GameCategory> = emptyList(),
    selectedCategory: String? = null,
    initialStatusTab: Int = 1,
    onSelectCategory: (String) -> Unit = {},
    onClearCategory: () -> Unit = {},
    onOpenMyMatches: () -> Unit = {},
    onSelectTournament: (Tournament) -> Unit
) {
    // Tabs matching screenshot: ONGOING | UPCOMING | RESULTS
    val tabs = listOf("ONGOING", "UPCOMING", "RESULTS")
    val statusKeys = listOf("live", "upcoming", "completed")
    var selectedTab by remember(initialStatusTab) { mutableStateOf(initialStatusTab.coerceIn(0, 2)) }

    if (selectedCategory == null) {
        // STEP 1: Game Categories First (Home Theme Charcoal + Orange Category Bars)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(ArenaCharcoal)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                SubScreenTopBar(
                    title = "eSport Games",
                    containerColor = HeaderNavy
                )

                LazyColumn(
                    contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 12.dp, bottom = 84.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(136.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .border(1.dp, ArenaOrange.copy(alpha = 0.55f), RoundedCornerShape(12.dp))
                                .testTag("matches_admin_hero_banner")
                        ) {
                            MatchBannerImage(
                                bannerUrl = generalConfig.matchesHeroBannerUrl,
                                bannerKey = generalConfig.matchesHeroBannerKey,
                                contentDescription = generalConfig.matchesHeroTitle.ifBlank { "Featured Arena Banner" },
                                modifier = Modifier.fillMaxSize()
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Brush.verticalGradient(listOf(Color.Transparent, HeaderNavy.copy(alpha = 0.88f))))
                            )
                            Column(
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(12.dp)
                            ) {
                                StatusPill(label = "SELECT GAME TO PLAY", color = ArenaOrange)
                                if (generalConfig.matchesHeroTitle.isNotBlank()) {
                                    Spacer(Modifier.height(4.dp))
                                    Text(generalConfig.matchesHeroTitle, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                }
                            }
                        }
                    }

                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                text = "Choose a Category",
                                color = Color.White,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            val displayCategories = remember(categories) { resolveGameCategories(categories) }
                            GameCategoryGrid(
                                categories = displayCategories,
                                tournaments = tournaments,
                                onCategoryClick = { cat -> onSelectCategory(cat.filterKey) }
                            )
                        }
                    }
                }
            }

            FloatingActionButton(
                onClick = onOpenMyMatches,
                containerColor = ArenaPurple,
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
            ) {
                Icon(Icons.Default.EmojiEvents, contentDescription = "My Matches & Leaderboard")
            }
        }
        return
    }

    // STEP 2: Matches List for Selected Game Category (Purple Screenshot 162334)
    BackHandler { onClearCategory() }

    val statusKey = statusKeys[selectedTab]
    val filtered = remember(tournaments, statusKey, selectedCategory) {
        tournaments.filter { m ->
            m.status == statusKey && matchesCategoryFilter(m, selectedCategory)
        }
    }

    val categoryHeaderTitle = if (selectedCategory.equals("All", ignoreCase = true)) {
        "ALL MATCHES"
    } else {
        selectedCategory.uppercase()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ArenaPurple)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            SubScreenTopBar(
                title = categoryHeaderTitle,
                containerColor = HeaderNavy,
                onBack = onClearCategory
            )

            // Purple TabRow: ONGOING | UPCOMING | RESULTS
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = ArenaPurple,
                contentColor = Color.White,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        height = 3.dp,
                        color = Color.White
                    )
                }
            ) {
                tabs.forEachIndexed { idx, title ->
                    Tab(
                        selected = selectedTab == idx,
                        onClick = { selectedTab = idx },
                        text = {
                            Text(
                                text = title,
                                fontWeight = if (selectedTab == idx) FontWeight.Bold else FontWeight.Medium,
                                color = if (selectedTab == idx) Color.White else Color.White.copy(alpha = 0.72f),
                                fontSize = 13.sp
                            )
                        }
                    )
                }
            }

            LazyColumn(
                contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 84.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                // Admin-controlled Matches Hero Banner inside the purple list (Screenshot 162334)
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .testTag("matches_admin_hero_banner")
                    ) {
                        MatchBannerImage(
                            bannerUrl = generalConfig.matchesHeroBannerUrl,
                            bannerKey = generalConfig.matchesHeroBannerKey,
                            contentDescription = generalConfig.matchesHeroTitle.ifBlank { "Featured Arena Banner" },
                            modifier = Modifier.fillMaxSize()
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Brush.verticalGradient(listOf(Color.Transparent, HeaderNavy.copy(alpha = 0.82f))))
                        )
                        Column(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(12.dp)
                        ) {
                            if (generalConfig.matchesHeroTitle.isNotBlank()) {
                                Text(generalConfig.matchesHeroTitle, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                            if (generalConfig.matchesHeroSubtitle.isNotBlank()) {
                                Text(generalConfig.matchesHeroSubtitle, color = Color(0xFFE2E8F0), fontSize = 11.sp)
                            }
                        }
                    }
                }

                if (filtered.isEmpty()) {
                    item {
                        Surface(
                            color = Color.White,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "No ${tabs[selectedTab].lowercase()} matches in $categoryHeaderTitle",
                                    color = CardDarkText,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    text = "Check Upcoming or tap Back to pick another game category.",
                                    color = CardMutedText,
                                    fontSize = 13.sp
                                )
                                Spacer(Modifier.height(12.dp))
                                OutlinedButton(onClick = onClearCategory) {
                                    Text("Browse All Categories", color = ArenaPurple, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                } else {
                    items(filtered, key = { it.id }) { match ->
                        val alreadyJoined = joinedMatches.any {
                            it.matchId == match.id && it.status != "kicked" && it.status != "refunded"
                        }
                        TournamentLobbyCard(
                            match = match,
                            isJoined = alreadyJoined,
                            onClick = { onSelectTournament(match) }
                        )
                    }
                }
            }
        }

        // Purple Trophy FAB bottom-right matching screenshot 162334
        FloatingActionButton(
            onClick = onOpenMyMatches,
            containerColor = ArenaPurple,
            contentColor = Color.White,
            shape = CircleShape,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .border(1.5.dp, Color.White.copy(alpha = 0.4f), CircleShape)
        ) {
            Icon(Icons.Default.EmojiEvents, contentDescription = "My Matches & Leaderboard")
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TournamentDetailSheet(
    match: Tournament,
    user: UserProfile?,
    joinedSlot: MatchSlot?,
    roomSecret: RoomSecret?,
    participants: List<MatchSlot> = emptyList(),
    busy: Boolean,
    onLoadRoomSecret: (String) -> Unit,
    onLoadParticipants: (String) -> Unit = {},
    onOpenMyMatches: () -> Unit = {},
    onValidateCoupon: suspend (String, Double) -> Result<Coupon>,
    onJoinMatch: (Tournament, String, String, List<String>, String, Long?, () -> Unit) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Step: "detail", "slot_picker", or "confirm_dialog"
    var currentStep by remember(match.id) { mutableStateOf("detail") }
    var selectedSlotNum by remember(match.id) { mutableStateOf<Long?>(null) }
    var participantsLoaded by remember(match.id) { mutableStateOf(false) }

    var ign by remember(user) { mutableStateOf(user?.inGameName.orEmpty().ifBlank { user?.displayName.orEmpty() }) }
    var gameUid by remember(user) { mutableStateOf(user?.gameUid.orEmpty()) }
    var teammate2 by remember { mutableStateOf("") }
    var teammate3 by remember { mutableStateOf("") }
    var teammate4 by remember { mutableStateOf("") }
    var couponCode by remember { mutableStateOf("") }
    var appliedCoupon by remember { mutableStateOf<Coupon?>(null) }
    var couponError by remember { mutableStateOf<String?>(null) }
    var joinValidationError by remember { mutableStateOf<String?>(null) }

    var nowMillis by remember(match.id) { mutableStateOf(System.currentTimeMillis()) }
    var remainingMillis by remember(match.scheduledAtMillis) {
        mutableStateOf((match.scheduledAtMillis - System.currentTimeMillis()).coerceAtLeast(0L))
    }

    LaunchedEffect(match.id, joinedSlot, match.roomPublished) {
        if (joinedSlot != null && match.roomPublished) {
            onLoadRoomSecret(match.id)
        }
    }

    LaunchedEffect(match.scheduledAtMillis, match.registrationOpenAtMillis, match.registrationCloseAtMillis) {
        while (true) {
            nowMillis = System.currentTimeMillis()
            remainingMillis = (match.scheduledAtMillis - nowMillis).coerceAtLeast(0L)
            delay(1000L)
        }
    }

    val regNotStarted = match.isRegistrationNotStarted(nowMillis)
    val regClosed = match.isRegistrationClosed(nowMillis)
    val isFull = match.filledSlots >= match.maxSlots
    val joinEnabled = joinedSlot == null && match.status == "upcoming" && !isFull && !regNotStarted && !regClosed

    val canRevealRoom = joinedSlot != null && (match.roomPublished || remainingMillis <= 15 * 60 * 1000L)
    val discount = appliedCoupon?.computeBenefit(match.entryFee) ?: 0.0
    val finalFee = (match.entryFee - discount).coerceAtLeast(0.0)

    Dialog(
        onDismissRequest = {
            if (currentStep == "slot_picker") currentStep = "detail" else onDismiss()
        },
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        BackHandler {
            when (currentStep) {
                "confirm_dialog" -> currentStep = "slot_picker"
                "slot_picker" -> currentStep = "detail"
                else -> onDismiss()
            }
        }

        if (currentStep == "slot_picker" || currentStep == "confirm_dialog") {
            // Choose Your Match Slot Screen (matching Screenshot 155619)
            val takenSlots = remember(participants, match.filledSlots) {
                if (participants.isNotEmpty()) {
                    participants.map { it.slotNumber }.toSet()
                } else {
                    (1L..match.filledSlots).toSet()
                }
            }
            val totalSlots = match.maxSlots.coerceIn(2L, 120L).toInt()
            val slotNumbers = remember(totalSlots) { (1..totalSlots).map { it.toLong() } }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White)
                    .statusBarsPadding()
                    .navigationBarsPadding()
            ) {
                SubScreenTopBar(
                    title = "Choose your match slot",
                    containerColor = ArenaPurple,
                    onBack = { currentStep = "detail" }
                )

                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f).fillMaxWidth()
                ) {
                    items(slotNumbers, key = { it }) { slotNum ->
                        val isTaken = slotNum in takenSlots
                        val isSelected = selectedSlotNum == slotNum
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable(enabled = !isTaken) {
                                    selectedSlotNum = slotNum
                                }
                                .padding(vertical = 6.dp)
                                .testTag("slot_picker_item_$slotNum")
                        ) {
                            Text(
                                text = "$slotNum",
                                color = if (isTaken) CardMutedText else CardDarkText,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                modifier = Modifier.width(28.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Box(
                                modifier = Modifier
                                    .size(22.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(
                                        when {
                                            isTaken -> Color(0xFFBDBDBD)
                                            isSelected -> ArenaPurple
                                            else -> Color.White
                                        }
                                    )
                                    .border(
                                        width = 1.6.dp,
                                        color = when {
                                            isTaken -> Color(0xFFBDBDBD)
                                            isSelected -> ArenaPurple
                                            else -> CardDarkText
                                        },
                                        shape = RoundedCornerShape(4.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isTaken || isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 18.dp)
                ) {
                    Button(
                        onClick = {
                            if (selectedSlotNum == null) {
                                val firstFree = slotNumbers.firstOrNull { it !in takenSlots }
                                selectedSlotNum = firstFree
                            }
                            currentStep = "confirm_dialog"
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ArenaBlue, contentColor = Color.White),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("slot_picker_next_btn")
                    ) {
                        Text(
                            text = if (selectedSlotNum != null) "NEXT (SLOT #$selectedSlotNum)" else "NEXT",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                }
            }

            if (currentStep == "confirm_dialog") {
                Dialog(onDismissRequest = { currentStep = "slot_picker" }) {
                    Surface(
                        color = HeaderNavy,
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, ArenaOrange.copy(alpha = 0.55f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState())
                                .padding(16.dp)
                        ) {
                            Text(
                                text = "Slot #${selectedSlotNum ?: (match.filledSlots + 1)} Registration",
                                style = MaterialTheme.typography.titleLarge,
                                color = CoinYellow
                            )
                            Spacer(Modifier.height(10.dp))
                            OutlinedTextField(
                                value = ign,
                                onValueChange = {
                                    ign = it
                                    joinValidationError = null
                                },
                                label = { Text("In-Game Name (IGN) *") },
                                singleLine = true,
                                isError = joinValidationError != null && ign.isBlank(),
                                modifier = Modifier.fillMaxWidth().testTag("join_ign_input")
                            )
                            Spacer(Modifier.height(8.dp))
                            OutlinedTextField(
                                value = gameUid,
                                onValueChange = {
                                    gameUid = it
                                    joinValidationError = null
                                },
                                label = { Text("Game UID / Character ID *") },
                                singleLine = true,
                                isError = joinValidationError != null && gameUid.isBlank(),
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
                                        label = { Text("Match Coupon (FIRST50)") },
                                        leadingIcon = { Icon(Icons.Default.LocalOffer, contentDescription = null, tint = ArenaOrange) },
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
                                        colors = ButtonDefaults.buttonColors(containerColor = ArenaPurple)
                                    ) { Text("Apply") }
                                }
                                if (appliedCoupon != null) {
                                    Text(
                                        text = "Applied ${appliedCoupon!!.code}: -${ArenaFormatters.formatInr(discount)} OFF!",
                                        color = ArenaGreen,
                                        style = MaterialTheme.typography.labelLarge,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                } else if (couponError != null) {
                                    Text(couponError!!, color = CrimsonDanger, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(top = 4.dp))
                                }
                            }

                            if (joinValidationError != null) {
                                Spacer(Modifier.height(8.dp))
                                Text(joinValidationError!!, color = CrimsonDanger, style = MaterialTheme.typography.labelLarge)
                            }

                            Spacer(Modifier.height(14.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(
                                    onClick = { currentStep = "slot_picker" },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Back", color = TextSecondary)
                                }
                                Button(
                                    onClick = {
                                        when {
                                            ign.trim().isBlank() -> joinValidationError = "In-Game Name (IGN) is required."
                                            gameUid.trim().length < 3 -> joinValidationError = "Valid Game UID is required."
                                            else -> {
                                                joinValidationError = null
                                                onJoinMatch(
                                                    match,
                                                    ign.trim(),
                                                    gameUid.trim(),
                                                    listOf(teammate2, teammate3, teammate4).filter { it.isNotBlank() },
                                                    appliedCoupon?.code ?: "",
                                                    selectedSlotNum,
                                                    onDismiss
                                                )
                                            }
                                        }
                                    },
                                    enabled = !busy,
                                    colors = ButtonDefaults.buttonColors(containerColor = ArenaGreen, contentColor = Color.White),
                                    modifier = Modifier.weight(1.4f).testTag("confirm_join_match_btn")
                                ) {
                                    Text(if (busy) "Joining..." else "Pay ${finalFee.toLong()} Coins", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // Match Detail View (with safe system bar insets + inline Join button + sticky bottom bar)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White)
                    .statusBarsPadding()
                    .navigationBarsPadding()
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                    ) {
                        MatchBannerImage(
                            bannerUrl = match.bannerUrl,
                            bannerKey = match.bannerKey,
                            contentDescription = match.title,
                            modifier = Modifier.fillMaxSize()
                        )
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(10.dp)
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.45f))
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                        }
                    }

                    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                        Text(
                            text = match.title,
                            color = ArenaBlue,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(10.dp))

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            DetailInfoChip("Type: ${match.mode}")
                            DetailInfoChip("Version: TPP")
                            DetailInfoChip("Map: ${match.mapName}")
                            DetailInfoChip("Match Type: ${match.entryType}")
                            DetailCoinChip("Entry Fee:", if (match.entryFee <= 0.0) "FREE" else "${match.entryFee.toLong()}")
                            DetailInfoChip("Match Schedule: ${ArenaFormatters.formatTimestampMillis(match.scheduledAtMillis)}")
                            DetailInfoChip("Slots: ${match.filledSlots}/${match.maxSlots}")
                        }

                        Spacer(Modifier.height(14.dp))
                        // Registration Window Status + Prominent In-Page Join Button
                        Surface(
                            color = Color(0xFFF8FAFC),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(
                                1.2.dp,
                                when {
                                    joinedSlot != null -> ArenaGreen
                                    joinEnabled -> ArenaBlue
                                    regNotStarted -> ArenaOrange
                                    else -> CrimsonDanger
                                }
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = when {
                                            joinedSlot != null -> "YOU ARE JOINED (SLOT #${joinedSlot.slotNumber})"
                                            regNotStarted -> "OPENS IN ${ArenaFormatters.formatCountdown(match.registrationOpenAtMillis, nowMillis)}"
                                            regClosed -> "REGISTRATION CLOSED"
                                            isFull -> "MATCH FULL"
                                            match.status != "upcoming" -> match.status.uppercase()
                                            match.registrationCloseAtMillis > 0L -> "OPEN • CLOSES IN ${ArenaFormatters.formatCountdown(match.registrationCloseAtMillis, nowMillis)}"
                                            else -> "REGISTRATION OPEN"
                                        },
                                        color = when {
                                            joinedSlot != null -> ArenaGreen
                                            joinEnabled -> ArenaBlue
                                            regNotStarted -> ArenaOrange
                                            else -> CrimsonDanger
                                        },
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 13.sp
                                    )
                                }
                                if (match.registrationOpenAtMillis > 0L || match.registrationCloseAtMillis > 0L) {
                                    Text(
                                        text = "Reg Opens: ${if (match.registrationOpenAtMillis > 0L) ArenaFormatters.formatTimestampMillis(match.registrationOpenAtMillis) else "Immediate"} • Closes: ${if (match.registrationCloseAtMillis > 0L) ArenaFormatters.formatTimestampMillis(match.registrationCloseAtMillis) else "Until Full"}",
                                        color = CardMutedText,
                                        fontSize = 12.sp
                                    )
                                }
                                Button(
                                    onClick = {
                                        onLoadParticipants(match.id)
                                        currentStep = "slot_picker"
                                    },
                                    enabled = joinEnabled,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (joinedSlot != null) ArenaGreen else ArenaBlue,
                                        contentColor = Color.White,
                                        disabledContainerColor = Color(0xFFCBD5E1),
                                        disabledContentColor = Color(0xFF475569)
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth().height(48.dp).testTag("detail_inline_join_btn")
                                ) {
                                    Text(
                                        text = when {
                                            joinedSlot != null -> "SLOT #${joinedSlot.slotNumber} CONFIRMED"
                                            regNotStarted -> "REGISTRATION OPENS AT ${ArenaFormatters.formatTimestampMillis(match.registrationOpenAtMillis)}"
                                            regClosed -> "REGISTRATION CLOSED"
                                            isFull -> "MATCH FULL"
                                            match.status != "upcoming" -> match.status.uppercase()
                                            match.entryFee <= 0.0 -> "JOIN MATCH NOW (FREE)"
                                            else -> "JOIN MATCH NOW (${match.entryFee.toLong()} COINS)"
                                        },
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(18.dp))
                        Text("Prize Details", color = ArenaBlue, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        Surface(
                            color = Color.White,
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            shadowElevation = 1.dp,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = "PRIZE POOL: ${match.prizePool.toLong()} Coins • PER KILL: ${match.perKillReward.toLong()} Coins",
                                    color = CardDarkText,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                if (match.prizeBreakdown.isNotBlank()) {
                                    Text(match.prizeBreakdown, color = CardDarkText, fontSize = 14.sp)
                                }
                            }
                        }

                        if (joinedSlot != null) {
                            Spacer(Modifier.height(16.dp))
                            Surface(
                                color = Color(0xFFF0FDF4),
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, ArenaGreen.copy(alpha = 0.6f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = if (canRevealRoom && roomSecret != null) Icons.Default.LockOpen else Icons.Default.Lock,
                                            contentDescription = null,
                                            tint = if (canRevealRoom && roomSecret != null) ArenaGreen else AmberWarning
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            text = "Room Credentials (Slot #${joinedSlot.slotNumber})",
                                            color = CardDarkText,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp
                                        )
                                    }
                                    Spacer(Modifier.height(8.dp))
                                    if (canRevealRoom && roomSecret != null) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Text("ROOM ID: ${roomSecret.roomId}", color = ArenaBlue, fontWeight = FontWeight.Bold)
                                                Text("PASS: ${roomSecret.roomPassword}", color = ArenaGreen, fontWeight = FontWeight.Bold)
                                            }
                                            Row {
                                                IconButton(onClick = {
                                                    ArenaFormatters.copyToClipboard(context, "Room ID", roomSecret.roomId)
                                                    Toast.makeText(context, "Room ID copied", Toast.LENGTH_SHORT).show()
                                                }) {
                                                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy Room ID", tint = ArenaBlue)
                                                }
                                                IconButton(onClick = {
                                                    ArenaFormatters.copyToClipboard(context, "Room Pass", roomSecret.roomPassword)
                                                    Toast.makeText(context, "Password copied", Toast.LENGTH_SHORT).show()
                                                }) {
                                                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy Password", tint = ArenaGreen)
                                                }
                                            }
                                        }
                                    } else {
                                        Text(
                                            text = "Room ID & password will be shared here 15 minutes before match start or when Admin publishes.",
                                            color = CardMutedText,
                                            fontSize = 13.sp
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(Modifier.height(18.dp))
                        Text("About this Match", color = ArenaBlue, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = match.rulesText.ifBlank {
                                "• Minimum Level 40+ ID required.\n• Teaming, emulators & third-party hacks are strictly banned.\n• Grab Room ID & Password 10 mins before start."
                            },
                            color = CardDarkText,
                            fontSize = 14.sp,
                            lineHeight = 20.sp
                        )

                        Spacer(Modifier.height(18.dp))
                        Text("Participants", color = ArenaBlue, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(10.dp))
                        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            OutlinedButton(
                                onClick = {
                                    participantsLoaded = true
                                    onLoadParticipants(match.id)
                                },
                                colors = ButtonDefaults.outlinedButtonColors(containerColor = Color(0xFFE5E7EB)),
                                border = BorderStroke(1.dp, Color(0xFFD1D5DB)),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, tint = ArenaBlue, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("LOAD PARTICIPANTS", color = ArenaBlue, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                        if (participantsLoaded || participants.isNotEmpty()) {
                            Spacer(Modifier.height(10.dp))
                            if (participants.isEmpty()) {
                                Text("No participants registered yet (${match.filledSlots}/${match.maxSlots}).", color = CardMutedText, fontSize = 13.sp)
                            } else {
                                participants.forEach { slot ->
                                    Surface(
                                        color = Color(0xFFF8FAFC),
                                        shape = RoundedCornerShape(8.dp),
                                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text("#${slot.slotNumber} • ${slot.inGameName}", color = CardDarkText, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                            Text("UID: ${slot.gameUid}", color = CardMutedText, fontSize = 12.sp)
                                        }
                                    }
                                }
                            }
                        }
                        Spacer(Modifier.height(20.dp))
                    }
                }

                // Sticky Bottom Split Action Bar (Green MY ENTRIES | Blue JOIN NOW)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxSize()
                            .background(ArenaGreen)
                            .clickable {
                                onDismiss()
                                onOpenMyMatches()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("MY ENTRIES", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxSize()
                            .background(if (joinEnabled) ArenaBlue else Color(0xFF64748B))
                            .clickable(enabled = joinEnabled) {
                                onLoadParticipants(match.id)
                                currentStep = "slot_picker"
                            }
                            .testTag("open_join_match_form_btn"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = when {
                                joinedSlot != null -> "SLOT #${joinedSlot.slotNumber} JOINED"
                                isFull -> "MATCH FULL"
                                regNotStarted -> "OPENS SOON"
                                regClosed -> "REG CLOSED"
                                match.status != "upcoming" -> match.status.uppercase()
                                else -> "JOIN NOW"
                            },
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailInfoChip(text: String) {
    Surface(
        color = Color.White,
        shape = RoundedCornerShape(4.dp),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        shadowElevation = 1.dp
    ) {
        Text(
            text = text,
            color = CardDarkText,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        )
    }
}

@Composable
private fun DetailCoinChip(prefix: String, value: String) {
    Surface(
        color = Color.White,
        shape = RoundedCornerShape(4.dp),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        shadowElevation = 1.dp
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Text(text = prefix, color = CardDarkText, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.width(5.dp))
            CoinBadge(size = 16.dp)
            Spacer(Modifier.width(4.dp))
            Text(text = value, color = CardDarkText, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
    }
}
