package com.example.ui.screens

import android.widget.Toast
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.LeaderboardEntry
import com.example.data.model.MatchSlot
import com.example.data.model.RoomSecret
import com.example.data.model.Tournament
import com.example.ui.components.CyberGlassCard
import com.example.ui.components.StatusPill
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.EmeraldProfit
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.utils.ArenaFormatters

@Composable
fun MyMatchesScreen(
    joinedMatches: List<MatchSlot>,
    tournaments: List<Tournament>,
    roomSecretCache: Map<String, RoomSecret>,
    leaderboard: List<LeaderboardEntry>,
    onLoadRoomSecret: (String) -> Unit,
    onSelectTournament: (Tournament) -> Unit
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf(0) }
    var leaderboardSortByKills by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "MY MATCHES & RANKS",
            style = MaterialTheme.typography.headlineMedium,
            color = NeonCyan,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
        )

        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = ObsidianSurface,
            contentColor = NeonCyan
        ) {
            Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("My Matches (${joinedMatches.size})") })
            Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Leaderboard") })
        }

        if (selectedTab == 0) {
            if (joinedMatches.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                    CyberGlassCard {
                        Text("No Joined Matches Yet", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
                        Spacer(Modifier.height(6.dp))
                        Text("Join an upcoming tournament from the Arena Lobby to view your Slot #, Room ID, and Scorecards here.", style = MaterialTheme.typography.bodyMedium, color = TextMuted)
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

                        CyberGlassCard(
                            glowColor = if (slot.status == "settled") EmeraldProfit else NeonCyan,
                            onClick = { if (match != null) onSelectTournament(match) }
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                StatusPill("SLOT #${slot.slotNumber}", NeonCyan)
                                StatusPill(
                                    label = slot.status,
                                    color = when (slot.status) {
                                        "settled" -> EmeraldProfit
                                        "kicked", "refunded" -> AmberWarning
                                        else -> ElectricViolet
                                    }
                                )
                            }
                            Spacer(Modifier.height(8.dp))
                            Text(slot.matchTitle.ifBlank { match?.title ?: "Tournament" }, style = MaterialTheme.typography.titleLarge, color = TextPrimary)
                            Text("IGN: ${slot.inGameName} • UID: ${slot.gameUid}", style = MaterialTheme.typography.labelSmall, color = TextMuted)

                            if (secret != null) {
                                Spacer(Modifier.height(10.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Key, contentDescription = null, tint = EmeraldProfit, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text("ID: ${secret.roomId} | Pass: ${secret.roomPassword}", style = MaterialTheme.typography.labelLarge, color = EmeraldProfit)
                                    }
                                    IconButton(onClick = {
                                        ArenaFormatters.copyToClipboard(context, "Room Credentials", "ID: ${secret.roomId} Pass: ${secret.roomPassword}")
                                        Toast.makeText(context, "Room credentials copied", Toast.LENGTH_SHORT).show()
                                    }) {
                                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = NeonCyan)
                                    }
                                }
                            }

                            if (slot.status == "settled") {
                                Spacer(Modifier.height(10.dp))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Rank: #${slot.rank}", style = MaterialTheme.typography.titleMedium, color = AmberWarning)
                                    Text("Kills: ${slot.kills}", style = MaterialTheme.typography.titleMedium, color = NeonCyan)
                                    Text("Won: ${ArenaFormatters.formatInr(slot.prizeWon)}", style = MaterialTheme.typography.titleMedium, color = EmeraldProfit)
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
                    androidx.compose.material3.FilterChip(
                        selected = !leaderboardSortByKills,
                        onClick = { leaderboardSortByKills = false },
                        label = { Text("Top Earners") },
                        leadingIcon = { Icon(Icons.Default.EmojiEvents, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    )
                    androidx.compose.material3.FilterChip(
                        selected = leaderboardSortByKills,
                        onClick = { leaderboardSortByKills = true },
                        label = { Text("Top Killers") },
                        leadingIcon = { Icon(Icons.Default.Leaderboard, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    )
                }

                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    itemsIndexed(sorted, key = { _, e -> e.userId }) { idx, entry ->
                        val rankColor = when (idx) {
                            0 -> AmberWarning
                            1 -> NeonCyan
                            2 -> ElectricViolet
                            else -> TextSecondary
                        }
                        CyberGlassCard(glowColor = rankColor) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(rankColor.copy(alpha = 0.2f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("#${idx + 1}", style = MaterialTheme.typography.labelLarge, color = rankColor, fontWeight = FontWeight.Bold)
                                    }
                                    Spacer(Modifier.width(12.dp))
                                    Column {
                                        Text(entry.displayName, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                                        Text("${entry.matchesPlayed} Matches • ${entry.totalKills} Kills", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                                    }
                                }
                                Text(
                                    text = if (leaderboardSortByKills) "${entry.totalKills} Kills" else ArenaFormatters.formatInr(entry.totalWinnings),
                                    style = MaterialTheme.typography.titleLarge,
                                    color = EmeraldProfit
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
