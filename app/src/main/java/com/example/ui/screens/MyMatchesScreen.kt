package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.LeaderboardEntry
import com.example.data.model.MatchSlot
import com.example.data.model.RoomSecret
import com.example.data.model.Tournament
import com.example.ui.components.CoinBadge
import com.example.ui.components.SubScreenTopBar
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.ArenaBlue
import com.example.ui.theme.ArenaGreen
import com.example.ui.theme.ArenaOrange
import com.example.ui.theme.ArenaPurple
import com.example.ui.theme.CardDarkText
import com.example.ui.theme.CardMutedText
import com.example.ui.theme.HeaderNavy
import com.example.ui.theme.LightSheetBg
import com.example.utils.ArenaFormatters

@Composable
fun MyMatchesScreen(
    joinedMatches: List<MatchSlot>,
    tournaments: List<Tournament>,
    roomSecretCache: Map<String, RoomSecret>,
    leaderboard: List<LeaderboardEntry>,
    onLoadRoomSecret: (String) -> Unit,
    onSelectTournament: (Tournament) -> Unit,
    onBack: (() -> Unit)? = null
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf(0) }
    var leaderboardSortByKills by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LightSheetBg)
    ) {
        SubScreenTopBar(
            title = "My Matches & Ranks",
            containerColor = HeaderNavy,
            onBack = onBack
        )

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
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("MY MATCHES (${joinedMatches.size})", fontWeight = FontWeight.Bold) }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("TOP PLAYERS", fontWeight = FontWeight.Bold) }
            )
        }

        if (selectedTab == 0) {
            if (joinedMatches.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = ArenaPurple, modifier = Modifier.size(44.dp))
                            Spacer(Modifier.height(10.dp))
                            Text("No Joined Matches Yet", color = CardDarkText, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            Spacer(Modifier.height(6.dp))
                            Text(
                                "Join an upcoming tournament from Matches to view your Slot #, Room ID, and Scorecards here.",
                                color = CardMutedText,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(joinedMatches, key = { it.matchId }) { slot ->
                        val match = tournaments.firstOrNull { it.id == slot.matchId }
                        val secret = roomSecretCache[slot.matchId]
                        LaunchedEffect(slot.matchId, match?.roomPublished) {
                            if (match?.roomPublished == true && secret == null) {
                                onLoadRoomSecret(slot.matchId)
                            }
                        }

                        Card(
                            onClick = { if (match != null) onSelectTournament(match) },
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            shape = RoundedCornerShape(16.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        color = ArenaPurple.copy(alpha = 0.14f),
                                        shape = RoundedCornerShape(50)
                                    ) {
                                        Text(
                                            text = "SLOT #${slot.slotNumber}",
                                            color = ArenaPurple,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                        )
                                    }
                                    val statusColor = when (slot.status) {
                                        "settled" -> ArenaGreen
                                        "kicked", "refunded" -> ArenaOrange
                                        else -> ArenaBlue
                                    }
                                    Surface(
                                        color = statusColor.copy(alpha = 0.14f),
                                        shape = RoundedCornerShape(50)
                                    ) {
                                        Text(
                                            text = slot.status.uppercase(),
                                            color = statusColor,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                        )
                                    }
                                }

                                Spacer(Modifier.height(8.dp))
                                Text(
                                    text = slot.matchTitle.ifBlank { match?.title ?: "Tournament" },
                                    color = CardDarkText,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                                Text(
                                    text = "IGN: ${slot.inGameName} • UID: ${slot.gameUid}",
                                    color = CardMutedText,
                                    fontSize = 12.sp
                                )

                                if (secret != null) {
                                    Spacer(Modifier.height(10.dp))
                                    Surface(
                                        color = Color(0xFFECFDF5),
                                        border = BorderStroke(1.dp, ArenaGreen.copy(alpha = 0.45f)),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 6.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.Key, contentDescription = null, tint = ArenaGreen, modifier = Modifier.size(18.dp))
                                                Spacer(Modifier.width(6.dp))
                                                Text(
                                                    text = "ID: ${secret.roomId} | Pass: ${secret.roomPassword}",
                                                    color = ArenaGreen,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 12.sp
                                                )
                                            }
                                            IconButton(onClick = {
                                                ArenaFormatters.copyToClipboard(context, "Room Credentials", "ID: ${secret.roomId} Pass: ${secret.roomPassword}")
                                                Toast.makeText(context, "Room credentials copied", Toast.LENGTH_SHORT).show()
                                            }) {
                                                Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = ArenaGreen, modifier = Modifier.size(18.dp))
                                            }
                                        }
                                    }
                                }

                                if (slot.status == "settled") {
                                    Spacer(Modifier.height(10.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Rank: #${slot.rank}", color = ArenaOrange, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Text("Kills: ${slot.kills}", color = ArenaBlue, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            CoinBadge(size = 14.dp)
                                            Spacer(Modifier.width(4.dp))
                                            Text("Won: ${ArenaFormatters.formatInr(slot.prizeWon)}", color = ArenaGreen, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            val sorted = remember(leaderboard, leaderboardSortByKills) {
                if (leaderboardSortByKills) leaderboard.sortedByDescending { it.totalKills }
                else leaderboard.sortedByDescending { it.totalWinnings }
            }
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = !leaderboardSortByKills,
                        onClick = { leaderboardSortByKills = false },
                        label = { Text("Top Earners") },
                        leadingIcon = { Icon(Icons.Default.EmojiEvents, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = ArenaPurple,
                            selectedLabelColor = Color.White
                        )
                    )
                    FilterChip(
                        selected = leaderboardSortByKills,
                        onClick = { leaderboardSortByKills = true },
                        label = { Text("Top Killers") },
                        leadingIcon = { Icon(Icons.Default.Leaderboard, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = ArenaPurple,
                            selectedLabelColor = Color.White
                        )
                    )
                }

                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    itemsIndexed(sorted, key = { _, e -> e.userId }) { idx, entry ->
                        val rankColor = when (idx) {
                            0 -> AmberWarning
                            1 -> ArenaBlue
                            2 -> ArenaPurple
                            else -> CardMutedText
                        }
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Box(
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(CircleShape)
                                            .background(rankColor.copy(alpha = 0.16f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("#${idx + 1}", color = rankColor, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
                                    }
                                    Spacer(Modifier.width(12.dp))
                                    Column {
                                        Text(entry.displayName, color = CardDarkText, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                        Text("${entry.matchesPlayed} Matches • ${entry.totalKills} Kills", color = CardMutedText, fontSize = 12.sp)
                                    }
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (!leaderboardSortByKills) {
                                        CoinBadge(size = 16.dp)
                                        Spacer(Modifier.width(4.dp))
                                    }
                                    Text(
                                        text = if (leaderboardSortByKills) "${entry.totalKills} Kills" else ArenaFormatters.formatInr(entry.totalWinnings),
                                        color = ArenaGreen,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 15.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
