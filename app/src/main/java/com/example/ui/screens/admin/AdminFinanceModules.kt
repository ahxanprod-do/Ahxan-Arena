package com.example.ui.screens.admin

import android.widget.Toast
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Refresh
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.Coupon
import com.example.data.model.DepositOrder
import com.example.data.model.GatewayConfig
import com.example.data.model.ReferralConfig
import com.example.data.model.ReferralRecord
import com.example.data.model.UserProfile
import com.example.data.model.WithdrawalRequest
import com.example.ui.components.CyberGlassCard
import com.example.ui.components.NeonPrimaryButton
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatusPill
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
fun AdminCouponsModule(
    coupons: List<Coupon>,
    busy: Boolean,
    adminVm: AdminViewModel
) {
    var editing by remember { mutableStateOf<Coupon?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            SectionHeader("Promo Codes & Coupons", "${coupons.size} active/archived")
            Spacer(Modifier.height(8.dp))
            NeonPrimaryButton(
                text = "Create Promo / Coupon",
                icon = Icons.Default.Add,
                onClick = { editing = Coupon(maxGlobalUses = 200L, maxPerUserUses = 1L, active = true) },
                modifier = Modifier.fillMaxWidth()
            )
        }
        items(coupons, key = { it.code }) { c ->
            CyberGlassCard(glowColor = if (c.active) NeonCyan else TextMuted) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(c.code, style = MaterialTheme.typography.titleLarge, color = NeonCyan)
                        Text("${c.couponType} • ${c.value}${if (c.valueType == "percentage") "%" else "₹"} • Uses: ${c.usedCount}/${c.maxGlobalUses}", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                        Text("Given: ${ArenaFormatters.formatInr(c.totalDiscountGiven)}", style = MaterialTheme.typography.labelSmall, color = EmeraldProfit)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Switch(checked = c.active, onCheckedChange = { adminVm.toggleCoupon(c.code, it) })
                        IconButton(onClick = { adminVm.deleteCoupon(c.code) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete Coupon", tint = CrimsonDanger)
                        }
                    }
                }
            }
        }
    }

    editing?.let { init ->
        var code by remember { mutableStateOf(init.code) }
        var couponType by remember { mutableStateOf(init.couponType) }
        var valueType by remember { mutableStateOf(init.valueType) }
        var value by remember { mutableStateOf("20") }
        var minAmt by remember { mutableStateOf("50") }
        var maxCap by remember { mutableStateOf("100") }
        var maxUses by remember { mutableStateOf("200") }
        var desc by remember { mutableStateOf(init.description) }

        ModalBottomSheet(onDismissRequest = { editing = null }, containerColor = ObsidianSurface) {
            Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp)) {
                Text("Save Promo / Coupon", style = MaterialTheme.typography.headlineSmall, color = NeonCyan)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = code, onValueChange = { code = it.uppercase() }, label = { Text("Coupon Code") }, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = couponType == "deposit_bonus", onClick = { couponType = "deposit_bonus" }, label = { Text("Deposit Bonus") })
                    FilterChip(selected = couponType == "match_discount", onClick = { couponType = "match_discount" }, label = { Text("Match Discount") })
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = valueType == "percentage", onClick = { valueType = "percentage" }, label = { Text("Percentage %") })
                    FilterChip(selected = valueType == "flat", onClick = { valueType = "flat" }, label = { Text("Flat ₹") })
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = value, onValueChange = { value = it }, label = { Text("Value") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                    OutlinedTextField(value = minAmt, onValueChange = { minAmt = it }, label = { Text("Min ₹") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = maxCap, onValueChange = { maxCap = it }, label = { Text("Max Cap ₹") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                    OutlinedTextField(value = maxUses, onValueChange = { maxUses = it }, label = { Text("Max Uses") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                }
                OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text("Offer Description") }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(12.dp))
                NeonPrimaryButton(
                    text = if (busy) "Saving..." else "Save Coupon",
                    enabled = !busy && code.isNotBlank(),
                    onClick = {
                        adminVm.saveCoupon(
                            Coupon(
                                code = code,
                                couponType = couponType,
                                valueType = valueType,
                                value = value.toDoubleOrNull() ?: 10.0,
                                minAmount = minAmt.toDoubleOrNull() ?: 0.0,
                                maxCap = maxCap.toDoubleOrNull() ?: 100.0,
                                maxGlobalUses = maxUses.toLongOrNull() ?: 100L,
                                maxPerUserUses = 1L,
                                active = true,
                                expiresAtMillis = System.currentTimeMillis() + 86400_000L * 60,
                                description = desc
                            )
                        )
                        editing = null
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
fun AdminReferralsModule(
    referralConfig: ReferralConfig,
    referrals: List<ReferralRecord>,
    users: List<UserProfile>,
    busy: Boolean,
    adminVm: AdminViewModel
) {
    var enabled by remember(referralConfig) { mutableStateOf(referralConfig.enabled) }
    var rewardType by remember(referralConfig) { mutableStateOf(referralConfig.referrerRewardType) }
    var rewardVal by remember(referralConfig) { mutableStateOf(referralConfig.referrerRewardValue.toString()) }
    var friendBonus by remember(referralConfig) { mutableStateOf(referralConfig.friendSignupBonus.toString()) }
    var triggerRule by remember(referralConfig) { mutableStateOf(referralConfig.triggerRule) }
    var minDep by remember(referralConfig) { mutableStateOf(referralConfig.minDepositThreshold.toString()) }

    val duplicateFingerprints = remember(referrals) {
        referrals.groupBy { it.deviceFingerprint }.filter { it.key.isNotBlank() && it.value.size > 1 }.keys
    }
    val topReferrers = remember(users) { users.filter { it.referralCount > 0L }.sortedByDescending { it.referralEarnings }.take(5) }

    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { SectionHeader("Refer & Earn Control Center", "${referrals.size} total invites") }
        item {
            CyberGlassCard(glowColor = ElectricViolet) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Referral System Enabled", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                    Switch(checked = enabled, onCheckedChange = { enabled = it })
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = rewardType == "flat", onClick = { rewardType = "flat" }, label = { Text("Flat ₹") })
                    FilterChip(selected = rewardType == "percent", onClick = { rewardType = "percent" }, label = { Text("Deposit %") })
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = rewardVal, onValueChange = { rewardVal = it }, label = { Text("Referrer Reward") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                    OutlinedTextField(value = friendBonus, onValueChange = { friendBonus = it }, label = { Text("Friend Bonus ₹") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                }
                Spacer(Modifier.height(6.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(listOf("first_deposit", "signup", "first_match")) { r ->
                        FilterChip(selected = triggerRule == r, onClick = { triggerRule = r }, label = { Text(r) })
                    }
                }
                OutlinedTextField(value = minDep, onValueChange = { minDep = it }, label = { Text("Min Deposit Threshold ₹") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(10.dp))
                NeonPrimaryButton(
                    text = if (busy) "Saving..." else "Save Referral Config",
                    enabled = !busy,
                    onClick = {
                        adminVm.saveReferralConfig(
                            ReferralConfig(
                                enabled = enabled,
                                referrerRewardType = rewardType,
                                referrerRewardValue = rewardVal.toDoubleOrNull() ?: 25.0,
                                friendSignupBonus = friendBonus.toDoubleOrNull() ?: 15.0,
                                triggerRule = triggerRule,
                                minDepositThreshold = minDep.toDoubleOrNull() ?: 50.0
                            )
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        if (topReferrers.isNotEmpty()) {
            item { SectionHeader("Top Referrers") }
            items(topReferrers, key = { it.userId }) { u ->
                CyberGlassCard(glowColor = EmeraldProfit) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("${u.displayName} (${u.referralCode})", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                        Text("${u.referralCount} refs • ${ArenaFormatters.formatInr(u.referralEarnings)}", color = EmeraldProfit)
                    }
                }
            }
        }
        item { SectionHeader("Referral & Anti-Fraud Ledger") }
        items(referrals, key = { it.id }) { rec ->
            val isSuspicious = rec.flaggedFraud || rec.deviceFingerprint in duplicateFingerprints
            CyberGlassCard(glowColor = if (isSuspicious || rec.status == "blocked") CrimsonDanger else NeonCyan) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("${rec.referredName} via ${rec.referrerCode}", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                        Text("Device: ${rec.deviceFingerprint.ifBlank { "unknown" }} • Status: ${rec.status}", style = MaterialTheme.typography.labelSmall, color = if (isSuspicious) CrimsonDanger else TextMuted)
                    }
                    OutlinedButton(onClick = { adminVm.toggleBlockReferral(rec.id, rec.status != "blocked") }) {
                        Text(if (rec.status == "blocked") "Unblock" else "Block", color = if (rec.status == "blocked") EmeraldProfit else CrimsonDanger)
                    }
                }
            }
        }
    }
}

@Composable
fun AdminGatewayDepositsModule(
    deposits: List<DepositOrder>,
    gatewayConfig: GatewayConfig,
    referralConfig: ReferralConfig,
    busy: Boolean,
    adminVm: AdminViewModel
) {
    val context = LocalContext.current
    var statusFilter by remember { mutableStateOf("All") }
    var apiOverride by remember(gatewayConfig) { mutableStateOf(gatewayConfig.payzapiApiKeyOverride) }
    var resendOverride by remember(gatewayConfig) { mutableStateOf(gatewayConfig.resendApiKey) }
    var fampay by remember(gatewayConfig) { mutableStateOf(gatewayConfig.fampayEnabled) }
    var paytm by remember(gatewayConfig) { mutableStateOf(gatewayConfig.paytmEnabled) }
    var bharatpe by remember(gatewayConfig) { mutableStateOf(gatewayConfig.bharatpeEnabled) }
    var binance by remember(gatewayConfig) { mutableStateOf(gatewayConfig.binanceEnabled) }
    var minDep by remember(gatewayConfig) { mutableStateOf(gatewayConfig.minDeposit.toString()) }
    var maxDep by remember(gatewayConfig) { mutableStateOf(gatewayConfig.maxDeposit.toString()) }
    var depMaint by remember(gatewayConfig) { mutableStateOf(gatewayConfig.depositMaintenance) }

    val filtered = remember(deposits, statusFilter) {
        if (statusFilter == "All") deposits else deposits.filter { it.status == statusFilter.lowercase() }
    }

    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { SectionHeader("PayZapi Gateway & Deposits", "${deposits.size} orders") }
        item {
            CyberGlassCard(glowColor = NeonCyan) {
                Text("Gateway & Email OTP Settings", style = MaterialTheme.typography.titleMedium, color = NeonCyan)
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(value = apiOverride, onValueChange = { apiOverride = it }, label = { Text("PayZapi API Key Override (Optional)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(value = resendOverride, onValueChange = { resendOverride = it }, label = { Text("Resend Email OTP API Key (Optional)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(6.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Row(verticalAlignment = Alignment.CenterVertically) { Switch(checked = fampay, onCheckedChange = { fampay = it }); Text("FamPay", color = TextPrimary) }
                    Row(verticalAlignment = Alignment.CenterVertically) { Switch(checked = paytm, onCheckedChange = { paytm = it }); Text("Paytm", color = TextPrimary) }
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Row(verticalAlignment = Alignment.CenterVertically) { Switch(checked = bharatpe, onCheckedChange = { bharatpe = it }); Text("BharatPe", color = TextPrimary) }
                    Row(verticalAlignment = Alignment.CenterVertically) { Switch(checked = binance, onCheckedChange = { binance = it }); Text("Binance", color = TextPrimary) }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = minDep, onValueChange = { minDep = it }, label = { Text("Min Dep ₹") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                    OutlinedTextField(value = maxDep, onValueChange = { maxDep = it }, label = { Text("Max Dep ₹") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Deposit Maintenance Pause", color = AmberWarning)
                    Switch(checked = depMaint, onCheckedChange = { depMaint = it })
                }
                NeonPrimaryButton(
                    text = if (busy) "Saving..." else "Save Gateway Config",
                    enabled = !busy,
                    onClick = {
                        context.getSharedPreferences("ahxan_secrets", android.content.Context.MODE_PRIVATE)
                            .edit().putString("resend_api_key", resendOverride.trim()).apply()
                        adminVm.saveGatewayConfig(
                            gatewayConfig.copy(
                                payzapiApiKeyOverride = apiOverride.trim(),
                                resendApiKey = resendOverride.trim(),
                                fampayEnabled = fampay,
                                paytmEnabled = paytm,
                                bharatpeEnabled = bharatpe,
                                binanceEnabled = binance,
                                minDeposit = minDep.toDoubleOrNull() ?: 10.0,
                                maxDeposit = maxDep.toDoubleOrNull() ?: 50000.0,
                                depositMaintenance = depMaint
                            )
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(listOf("All", "Pending", "Completed", "Expired", "Failed")) { s ->
                    FilterChip(selected = statusFilter == s, onClick = { statusFilter = s }, label = { Text(s) })
                }
            }
        }
        items(filtered, key = { it.id }) { dep ->
            CyberGlassCard(glowColor = if (dep.status == "completed") EmeraldProfit else AmberWarning) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("${dep.userName} • ${ArenaFormatters.formatInr(dep.amount)}", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                        Text("${dep.provider.uppercase()} • ${dep.payzapiOrderId}", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                        if (dep.utr.isNotBlank()) Text("UTR: ${dep.utr}", style = MaterialTheme.typography.labelSmall, color = EmeraldProfit)
                    }
                    StatusPill(dep.status, if (dep.status == "completed") EmeraldProfit else AmberWarning)
                }
                if (dep.status == "pending") {
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { adminVm.verifyDepositWithPayZapi(dep, gatewayConfig, referralConfig) },
                        enabled = !busy,
                        modifier = Modifier.fillMaxWidth().testTag("admin_verify_deposit_${dep.id}")
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Verify with PayZapi", color = NeonCyan)
                    }
                }
            }
        }
    }
}

@Composable
fun AdminWithdrawalsModule(
    withdrawals: List<WithdrawalRequest>,
    gatewayConfig: GatewayConfig,
    busy: Boolean,
    adminVm: AdminViewModel
) {
    val context = LocalContext.current
    var minWd by remember(gatewayConfig) { mutableStateOf(gatewayConfig.minWithdrawal.toString()) }
    var maxWd by remember(gatewayConfig) { mutableStateOf(gatewayConfig.maxWithdrawal.toString()) }
    var feePct by remember(gatewayConfig) { mutableStateOf(gatewayConfig.withdrawalFeePercent.toString()) }
    var dailyLimit by remember(gatewayConfig) { mutableStateOf(gatewayConfig.dailyWithdrawalLimit.toString()) }
    var payoutsPaused by remember(gatewayConfig) { mutableStateOf(gatewayConfig.payoutsPaused) }
    var inspectingQrWd by remember { mutableStateOf<WithdrawalRequest?>(null) }
    var utrInput by remember { mutableStateOf("") }
    var rejectReasonInput by remember { mutableStateOf("") }

    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                SectionHeader("Withdrawals & Payouts", "${withdrawals.count { it.status == "pending" }} pending")
                OutlinedButton(onClick = {
                    val csv = buildString {
                        appendLine("ID,User,Email,Method,Destination,Gross,Fee,Net,Status,Ref")
                        withdrawals.forEach { w ->
                            appendLine("${w.id},${w.userName},${w.userEmail},${w.method},${w.destination},${w.grossAmount},${w.feeAmount},${w.netAmount},${w.status},${w.utrOrTxHash}")
                        }
                    }
                    ArenaFormatters.shareCsvText(context, "Ahxan_Withdrawals_Report", csv)
                }) {
                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("CSV")
                }
            }
        }
        item {
            CyberGlassCard(glowColor = EmeraldProfit) {
                Text("Withdrawal Rules Config", style = MaterialTheme.typography.titleMedium, color = EmeraldProfit)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = minWd, onValueChange = { minWd = it }, label = { Text("Min ₹") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                    OutlinedTextField(value = maxWd, onValueChange = { maxWd = it }, label = { Text("Max ₹") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = feePct, onValueChange = { feePct = it }, label = { Text("Fee %") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                    OutlinedTextField(value = dailyLimit, onValueChange = { dailyLimit = it }, label = { Text("Daily Cap ₹") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Pause All Payouts", color = CrimsonDanger)
                    Switch(checked = payoutsPaused, onCheckedChange = { payoutsPaused = it })
                }
                NeonPrimaryButton(
                    text = "Save Withdrawal Rules",
                    accentColor = EmeraldProfit,
                    enabled = !busy,
                    onClick = {
                        adminVm.saveGatewayConfig(
                            gatewayConfig.copy(
                                minWithdrawal = minWd.toDoubleOrNull() ?: 50.0,
                                maxWithdrawal = maxWd.toDoubleOrNull() ?: 25000.0,
                                withdrawalFeePercent = feePct.toDoubleOrNull() ?: 2.0,
                                dailyWithdrawalLimit = dailyLimit.toDoubleOrNull() ?: 50000.0,
                                payoutsPaused = payoutsPaused
                            )
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        items(withdrawals, key = { it.id }) { wd ->
            val stColor = when (wd.status) {
                "approved" -> EmeraldProfit
                "rejected" -> CrimsonDanger
                else -> AmberWarning
            }
            CyberGlassCard(glowColor = stColor) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("${wd.userName} • Pay ${ArenaFormatters.formatInr(wd.netAmount)}", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                        Text("${wd.method}: ${wd.destination}", style = MaterialTheme.typography.labelSmall, color = NeonCyan)
                        Text("Gross: ${ArenaFormatters.formatInr(wd.grossAmount)} • Fee: ${ArenaFormatters.formatInr(wd.feeAmount)}", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                    }
                    IconButton(onClick = {
                        ArenaFormatters.copyToClipboard(context, "Payout Destination", wd.destination)
                        Toast.makeText(context, "Copied ${wd.destination}", Toast.LENGTH_SHORT).show()
                    }) { Icon(Icons.Default.ContentCopy, contentDescription = "Copy Destination", tint = NeonCyan) }
                    StatusPill(wd.status, stColor)
                }

                if (wd.status == "pending") {
                    if (wd.method == "UPI") {
                        val upiQr = remember(wd.id) {
                            QrCodeUtils.generateQrBitmap("upi://pay?pa=${wd.destination}&pn=${wd.userName}&am=${wd.netAmount}&cu=INR", 260)
                        }
                        if (upiQr != null) {
                            Spacer(Modifier.height(8.dp))
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Box(modifier = Modifier.size(84.dp).clip(RoundedCornerShape(10.dp)).background(Color.White).padding(4.dp)) {
                                    Image(bitmap = upiQr.asImageBitmap(), contentDescription = "Scan to Pay UPI", modifier = Modifier.fillMaxSize())
                                }
                                Text("Scan with any UPI app to pay exact net amount ${ArenaFormatters.formatInr(wd.netAmount)}, then enter UTR below.", style = MaterialTheme.typography.labelSmall, color = TextSecondary, modifier = Modifier.weight(1f))
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    if (inspectingQrWd?.id == wd.id) {
                        OutlinedTextField(value = utrInput, onValueChange = { utrInput = it }, label = { Text("UTR / Tx Hash (for Approve)") }, modifier = Modifier.fillMaxWidth())
                        Spacer(Modifier.height(6.dp))
                        OutlinedTextField(value = rejectReasonInput, onValueChange = { rejectReasonInput = it }, label = { Text("Reject Reason (for Reject & Refund)") }, modifier = Modifier.fillMaxWidth())
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = {
                                    adminVm.approveWithdrawal(wd, utrInput)
                                    inspectingQrWd = null
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldProfit, contentColor = ObsidianBg),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Approve")
                            }
                            Button(
                                onClick = {
                                    adminVm.rejectWithdrawal(wd, rejectReasonInput)
                                    inspectingQrWd = null
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = CrimsonDanger, contentColor = TextPrimary),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Reject & Refund")
                            }
                        }
                    } else {
                        OutlinedButton(onClick = {
                            utrInput = ""
                            rejectReasonInput = ""
                            inspectingQrWd = wd
                        }, modifier = Modifier.fillMaxWidth()) {
                            Text("Process Payout (Approve / Reject)", color = AmberWarning)
                        }
                    }
                }
            }
        }
    }
}
