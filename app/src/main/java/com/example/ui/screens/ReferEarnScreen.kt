package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ReferralConfig
import com.example.data.model.ReferralRecord
import com.example.data.model.UserProfile
import com.example.ui.components.CoinBadge
import com.example.ui.components.SubScreenTopBar
import com.example.ui.theme.ArenaBlue
import com.example.ui.theme.ArenaCharcoal
import com.example.ui.theme.ArenaGreen
import com.example.ui.theme.ArenaOrange
import com.example.ui.theme.ArenaPurple
import com.example.ui.theme.CardDarkText
import com.example.ui.theme.CardMutedText
import com.example.ui.theme.CrimsonDanger
import com.example.ui.theme.HeaderNavy
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
    val inviteLink = remember(referralConfig.websiteBaseUrl, myCode) {
        referralConfig.buildInviteLink(myCode)
    }
    val shareMessage = "Join me on Ahxan Arena Esports! Use my referral code $myCode ($inviteLink) to claim ₹${referralConfig.friendSignupBonus.toInt()} signup bonus!"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ArenaCharcoal)
    ) {
        SubScreenTopBar(
            title = "Refer & Earn",
            containerColor = HeaderNavy,
            onBack = onBack
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Invite Code Card (White Card + Orange/Purple Accents matching Home Theme)
            item {
                Surface(
                    color = Color.White,
                    shape = RoundedCornerShape(16.dp),
                    shadowElevation = 3.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CardGiftcard, contentDescription = null, tint = ArenaOrange, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("YOUR INVITE CODE", color = CardMutedText, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = myCode,
                                color = ArenaPurple,
                                fontSize = 28.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                            IconButton(onClick = {
                                ArenaFormatters.copyToClipboard(context, "Referral Link", inviteLink)
                                Toast.makeText(context, "Invite link copied!", Toast.LENGTH_SHORT).show()
                            }) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "Copy Link", tint = ArenaOrange)
                            }
                        }
                        Text(inviteLink, color = CardMutedText, fontSize = 12.sp)
                        Spacer(Modifier.height(14.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = {
                                    val url = "https://wa.me/?text=${Uri.encode(shareMessage)}"
                                    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = ArenaGreen, contentColor = Color.White),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("WhatsApp", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                            Button(
                                onClick = {
                                    val url = "https://t.me/share/url?url=${Uri.encode(inviteLink)}&text=${Uri.encode(shareMessage)}"
                                    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = ArenaBlue, contentColor = Color.White),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Telegram", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                            OutlinedButton(
                                onClick = {
                                    val share = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_TEXT, shareMessage)
                                    }
                                    context.startActivity(Intent.createChooser(share, "Share Invite"))
                                },
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.2.dp, ArenaOrange),
                                modifier = Modifier.weight(0.75f)
                            ) {
                                Icon(Icons.Default.Share, contentDescription = "Share", tint = ArenaOrange, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }

            // Stats Row (3 crisp white KPI cards matching Wallet/Me)
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ReferKpiCard(label = "Invited", value = "${referrals.size}", showCoin = false, modifier = Modifier.weight(1f))
                    ReferKpiCard(label = "Qualified", value = "${referrals.count { it.status == "qualified" }}", showCoin = false, modifier = Modifier.weight(1f))
                    ReferKpiCard(label = "Earnings", value = "${(user?.referralEarnings ?: 0.0).toLong()}", showCoin = true, modifier = Modifier.weight(1f))
                }
            }

            // Apply Referral Code if not already referred
            if (user?.referredByUid.isNullOrBlank()) {
                item {
                    Surface(
                        color = Color.White,
                        shape = RoundedCornerShape(14.dp),
                        shadowElevation = 2.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
                            Text("Have a Friend's Code?", color = CardDarkText, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Spacer(Modifier.height(8.dp))
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = manualCode,
                                    onValueChange = { manualCode = it.uppercase() },
                                    label = { Text("Enter Referral Code") },
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = ArenaPurple,
                                        unfocusedBorderColor = Color(0xFFCBD5E1),
                                        focusedTextColor = CardDarkText,
                                        unfocusedTextColor = CardDarkText,
                                        focusedLabelColor = ArenaPurple,
                                        unfocusedLabelColor = CardMutedText
                                    ),
                                    modifier = Modifier.weight(1f).testTag("apply_referral_input")
                                )
                                Button(
                                    onClick = { onApplyReferralCode(manualCode) },
                                    colors = ButtonDefaults.buttonColors(containerColor = ArenaPurple, contentColor = Color.White),
                                    shape = RoundedCornerShape(10.dp)
                                ) { Text("Apply", fontWeight = FontWeight.Bold) }
                            }
                        }
                    }
                }
            }

            // How It Works
            item {
                Surface(
                    color = Color.White,
                    shape = RoundedCornerShape(14.dp),
                    shadowElevation = 2.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                        Text("How It Works", color = CardDarkText, fontWeight = FontWeight.Bold, fontSize = 16.sp)
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
                        Text("1. Share your unique invite code or link.", color = CardMutedText, fontSize = 13.sp)
                        Spacer(Modifier.height(4.dp))
                        Text("2. Your friend receives ₹${referralConfig.friendSignupBonus.toInt()} signup bonus.", color = CardMutedText, fontSize = 13.sp)
                        Spacer(Modifier.height(4.dp))
                        Text("3. You earn $rewardLabel, $triggerDesc.", color = ArenaGreen, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    }
                }
            }

            item {
                Text(
                    text = "Referral Earnings History",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            if (referrals.isEmpty()) {
                item {
                    Surface(
                        color = HeaderNavy,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "No referrals yet. Share your link to start earning coins!",
                            color = Color(0xFFCBD5E1),
                            fontSize = 13.sp,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            } else {
                items(referrals, key = { it.id }) { rec ->
                    val statusColor = when (rec.status) {
                        "qualified" -> ArenaGreen
                        "blocked" -> CrimsonDanger
                        else -> ArenaOrange
                    }
                    Surface(
                        color = Color.White,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(rec.referredName.ifBlank { "Player" }, color = CardDarkText, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Text("Trigger: ${rec.triggerType}", color = CardMutedText, fontSize = 12.sp)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Surface(
                                    color = statusColor.copy(alpha = 0.14f),
                                    shape = RoundedCornerShape(50)
                                ) {
                                    Text(
                                        text = rec.status.uppercase(),
                                        color = statusColor,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                                Spacer(Modifier.height(4.dp))
                                Text("+${ArenaFormatters.formatInr(rec.rewardAmount)}", color = ArenaGreen, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReferKpiCard(
    label: String,
    value: String,
    showCoin: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        color = Color.White,
        shape = RoundedCornerShape(12.dp),
        shadowElevation = 2.dp,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (showCoin) {
                    CoinBadge(size = 16.dp)
                    Spacer(Modifier.width(4.dp))
                }
                Text(value, color = ArenaPurple, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
            }
            Spacer(Modifier.height(4.dp))
            Text(label, color = CardMutedText, fontSize = 11.sp, fontWeight = FontWeight.Medium)
        }
    }
}
