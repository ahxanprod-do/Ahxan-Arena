package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.model.ReferralConfig
import com.example.data.model.ReferralRecord
import com.example.data.model.UserProfile
import com.example.ui.components.CyberGlassCard
import com.example.ui.components.KpiMiniStat
import com.example.ui.components.SectionHeader
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
import com.example.utils.ArenaFormatters

@Composable
fun ReferEarnScreen(
    user: UserProfile?,
    referralConfig: ReferralConfig,
    referrals: List<ReferralRecord>,
    onApplyReferralCode: (String) -> Unit,
    onBack: () -> Unit
) {
    BackHandler { onBack() }
    val context = LocalContext.current
    var manualCode by remember { mutableStateOf("") }

    val myCode = user?.referralCode.orEmpty().ifBlank { "AHXAN" }
    val inviteLink = "ahxanarena://register?ref=$myCode"
    val shareMessage = "Join me on Ahxan Arena Esports! Use my referral code $myCode ($inviteLink) to claim ₹${referralConfig.friendSignupBonus.toInt()} signup bonus!"

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack, modifier = Modifier.testTag("refer_back_btn")) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = NeonCyan)
                }
                Text("REFER & EARN", style = MaterialTheme.typography.headlineMedium, color = NeonCyan)
            }
        }

        item {
            CyberGlassCard(glowColor = NeonCyan) {
                Text("YOUR INVITE CODE", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(myCode, style = MaterialTheme.typography.displaySmall, color = NeonCyan)
                    IconButton(onClick = {
                        ArenaFormatters.copyToClipboard(context, "Referral Link", inviteLink)
                        Toast.makeText(context, "Invite link copied!", Toast.LENGTH_SHORT).show()
                    }) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy Link", tint = EmeraldProfit)
                    }
                }
                Text(inviteLink, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                Spacer(Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            val url = "https://wa.me/?text=${Uri.encode(shareMessage)}"
                            runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldProfit, contentColor = ObsidianBg),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("WhatsApp")
                    }
                    Button(
                        onClick = {
                            val url = "https://t.me/share/url?url=${Uri.encode(inviteLink)}&text=${Uri.encode(shareMessage)}"
                            runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = ObsidianBg),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Telegram")
                    }
                    OutlinedButton(
                        onClick = {
                            val share = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, shareMessage)
                            }
                            context.startActivity(Intent.createChooser(share, "Share Invite"))
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }

        // Stats Row
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                KpiMiniStat("Invited", "${referrals.size}", NeonCyan, Modifier.weight(1f))
                KpiMiniStat("Qualified", "${referrals.count { it.status == "qualified" }}", EmeraldProfit, Modifier.weight(1f))
                KpiMiniStat("Earnings", ArenaFormatters.formatInr(user?.referralEarnings ?: 0.0), AmberWarning, Modifier.weight(1f))
            }
        }

        // Apply Referral Code if not already referred
        if (user?.referredByUid.isNullOrBlank()) {
            item {
                CyberGlassCard(glowColor = ElectricViolet) {
                    Text("Have a Friend's Code?", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = manualCode,
                            onValueChange = { manualCode = it.uppercase() },
                            label = { Text("Enter Referral Code") },
                            singleLine = true,
                            modifier = Modifier.weight(1f).testTag("apply_referral_input")
                        )
                        Button(
                            onClick = { onApplyReferralCode(manualCode) },
                            colors = ButtonDefaults.buttonColors(containerColor = ElectricViolet)
                        ) { Text("Apply") }
                    }
                }
            }
        }

        // How It Works (Dynamic Admin Config)
        item {
            CyberGlassCard(glowColor = EmeraldProfit) {
                Text("How It Works", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
                Spacer(Modifier.height(8.dp))
                val rewardLabel = if (referralConfig.referrerRewardType == "percent") {
                    "${referralConfig.referrerRewardValue.toInt()}% of friend's first deposit"
                } else {
                    "₹${referralConfig.referrerRewardValue.toInt()} flat bonus"
                }
                val triggerDesc = when (referralConfig.triggerRule) {
                    "signup" -> "triggered immediately on friend's signup"
                    "first_match" -> "triggered when friend joins their first paid match"
                    else -> "triggered on friend's first PayZapi deposit (min ₹${referralConfig.minDepositThreshold.toInt()})"
                }
                Text("1. Share your unique invite code or link.", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                Text("2. Your friend receives ₹${referralConfig.friendSignupBonus.toInt()} signup bonus.", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                Text("3. You earn $rewardLabel, $triggerDesc.", style = MaterialTheme.typography.bodyMedium, color = EmeraldProfit)
            }
        }

        item {
            SectionHeader(title = "Referral Earnings History")
        }

        if (referrals.isEmpty()) {
            item {
                CyberGlassCard {
                    Text("No referrals yet. Share your link to start earning!", color = TextMuted)
                }
            }
        } else {
            items(referrals, key = { it.id }) { rec ->
                val color = when (rec.status) {
                    "qualified" -> EmeraldProfit
                    "blocked" -> CrimsonDanger
                    else -> AmberWarning
                }
                CyberGlassCard(glowColor = color) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text(rec.referredName.ifBlank { "Player" }, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                            Text("Trigger: ${rec.triggerType}", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            StatusPill(rec.status, color)
                            Spacer(Modifier.height(4.dp))
                            Text("+${ArenaFormatters.formatInr(rec.rewardAmount)}", style = MaterialTheme.typography.titleMedium, color = EmeraldProfit)
                        }
                    }
                }
            }
        }
    }
}
