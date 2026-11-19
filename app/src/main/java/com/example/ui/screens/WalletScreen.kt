package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.Coupon
import com.example.data.model.DepositOrder
import com.example.data.model.GatewayConfig
import com.example.data.model.UserProfile
import com.example.data.model.WalletTransaction
import com.example.data.model.WithdrawalRequest
import com.example.ui.components.CoinBadge
import com.example.ui.components.StatusPill
import com.example.ui.components.SubScreenTopBar
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
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.utils.ArenaFormatters
import com.example.utils.QrCodeUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WalletScreen(
    user: UserProfile?,
    transactions: List<WalletTransaction>,
    deposits: List<DepositOrder>,
    withdrawals: List<WithdrawalRequest>,
    activeCoupons: List<Coupon>,
    gatewayConfig: GatewayConfig,
    activeCheckoutOrder: DepositOrder?,
    busy: Boolean,
    onValidatePromo: suspend (String, Double) -> Result<Coupon>,
    onCreateDeposit: (Double, String, String) -> Unit,
    onCheckPaymentNow: (DepositOrder) -> Unit,
    onOpenCheckout: (DepositOrder) -> Unit,
    onCloseCheckout: () -> Unit,
    onSubmitWithdrawal: (String, String, Double, () -> Unit) -> Unit,
    onBack: (() -> Unit)? = null
) {
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    var showDepositSheet by remember { mutableStateOf(false) }
    var showWithdrawSheet by remember { mutableStateOf(false) }
    var showOffersSheet by remember { mutableStateOf(false) }
    var selectedPromoPrefill by remember { mutableStateOf("") }
    var txFilter by remember { mutableStateOf("All") }

    val depositOffers = activeCoupons.filter { it.couponType == "deposit_bonus" && it.active }
    val filterOptions = listOf("All", "Deposits", "Winnings", "Entry Fees", "Referrals", "Withdrawals", "Refunds")

    val filteredTx = remember(transactions, txFilter) {
        when (txFilter) {
            "Deposits" -> transactions.filter { it.type == "deposit" || it.type == "promo_bonus" }
            "Winnings" -> transactions.filter { it.type == "winning" }
            "Entry Fees" -> transactions.filter { it.type == "entry_fee" }
            "Referrals" -> transactions.filter { it.type == "referral" }
            "Withdrawals" -> transactions.filter { it.type == "withdrawal" }
            "Refunds" -> transactions.filter { it.type == "refund" }
            else -> transactions
        }
    }

    val pendingDeposits = deposits.filter { it.status == "pending" }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ArenaCharcoal)
    ) {
        SubScreenTopBar(
            title = "My Wallet",
            containerColor = HeaderNavy,
            onBack = onBack,
            trailingContent = {
                Surface(
                    color = Color.White.copy(alpha = 0.14f),
                    shape = RoundedCornerShape(50),
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .clickable { showOffersSheet = true }
                        .testTag("wallet_offers_button")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            Icons.Default.LocalOffer,
                            contentDescription = "Offers",
                            tint = AmberWarning,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "Offers (${depositOffers.size})",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        )

        // Upper Dark Charcoal Wallet Hero + 3 White KPI Cards (matching Screenshot 155427)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(ArenaCharcoal)
                .padding(horizontal = 16.dp, vertical = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                CoinBadge(size = 42.dp)
                Spacer(Modifier.width(10.dp))
                Text(
                    text = formatCoinWhole(user?.totalBalance ?: 0.0),
                    color = Color.White,
                    fontSize = 36.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }

            Spacer(Modifier.height(18.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                WalletBalanceKpiCard(
                    label = "Deposited",
                    amount = user?.depositBalance ?: 0.0,
                    modifier = Modifier.weight(1f)
                )
                WalletBalanceKpiCard(
                    label = "Winning",
                    amount = user?.winningBalance ?: 0.0,
                    modifier = Modifier.weight(1f)
                )
                WalletBalanceKpiCard(
                    label = "Bonus",
                    amount = user?.bonusBalance ?: 0.0,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // White Bottom Rounded Sheet Content
        Surface(
            color = Color.White,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            modifier = Modifier.weight(1f).fillMaxWidth()
        ) {
            LazyColumn(
                state = listState,
                contentPadding = PaddingValues(18.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                item {
                    Text(
                        text = "My Wallet",
                        color = CardDarkText,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        WalletQuickActionTile(
                            label = "Add Coins",
                            icon = Icons.Default.AddCircle,
                            tint = Color(0xFF06B6D4),
                            onClick = { showDepositSheet = true },
                            modifier = Modifier.weight(1f).testTag("wallet_add_funds_btn")
                        )
                        WalletQuickActionTile(
                            label = "Redeem Coins",
                            icon = Icons.Default.Send,
                            tint = ArenaBlue,
                            onClick = { showWithdrawSheet = true },
                            modifier = Modifier.weight(1f).testTag("wallet_withdraw_btn")
                        )
                        WalletQuickActionTile(
                            label = "Transactions",
                            icon = Icons.Default.ReceiptLong,
                            tint = ArenaPurple,
                            onClick = {
                                scope.launch { listState.animateScrollToItem(2) }
                            },
                            modifier = Modifier.weight(1f).testTag("wallet_transactions_btn")
                        )
                    }
                }

                // Pending PayZapi Orders with In-App QR + Check Payment
                if (pendingDeposits.isNotEmpty()) {
                    item {
                        Text(
                            text = "Pending Payments (${pendingDeposits.size})",
                            color = CardDarkText,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    items(pendingDeposits, key = { it.id }) { dep ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFBEB)),
                            border = BorderStroke(1.dp, AmberWarning.copy(alpha = 0.55f)),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "${ArenaFormatters.formatInr(dep.amount)} • ${dep.provider.uppercase()}",
                                            color = CardDarkText,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp
                                        )
                                        Text(
                                            text = "Order: ${dep.payzapiOrderId}",
                                            color = CardMutedText,
                                            fontSize = 11.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        if (dep.expectedPromoBonus > 0.0) {
                                            Text(
                                                text = "+₹${dep.expectedPromoBonus.toInt()} Bonus on completion",
                                                color = ArenaGreen,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }
                                    Surface(
                                        color = AmberWarning.copy(alpha = 0.18f),
                                        shape = RoundedCornerShape(50)
                                    ) {
                                        Text(
                                            text = "PENDING",
                                            color = Color(0xFFB45309),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                                Spacer(Modifier.height(10.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedButton(
                                        onClick = { onOpenCheckout(dep) },
                                        shape = RoundedCornerShape(10.dp),
                                        border = BorderStroke(1.dp, ArenaPurple),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Default.QrCode2, contentDescription = null, tint = ArenaPurple, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("Show QR", color = ArenaPurple, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }
                                    Button(
                                        onClick = { onCheckPaymentNow(dep) },
                                        enabled = !busy,
                                        colors = ButtonDefaults.buttonColors(containerColor = ArenaGreen, contentColor = Color.White),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.weight(1.2f).testTag("pending_check_payment_${dep.id}")
                                    ) {
                                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text(if (busy) "Checking..." else "Check Payment", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }
                }

                // Recent Withdrawals
                if (withdrawals.isNotEmpty()) {
                    item {
                        Text(
                            text = "Recent Redemptions",
                            color = CardDarkText,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    items(withdrawals.take(4), key = { it.id }) { wd ->
                        val statusColor = when (wd.status) {
                            "approved" -> ArenaGreen
                            "rejected" -> CrimsonDanger
                            else -> Color(0xFFD97706)
                        }
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${ArenaFormatters.formatInr(wd.netAmount)} (${wd.method})",
                                        color = CardDarkText,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                    Text(wd.destination, color = CardMutedText, fontSize = 11.sp)
                                    if (wd.utrOrTxHash.isNotBlank()) {
                                        Text("Ref: ${wd.utrOrTxHash}", color = ArenaGreen, fontSize = 11.sp)
                                    }
                                    if (wd.rejectReason.isNotBlank()) {
                                        Text("Reason: ${wd.rejectReason}", color = CrimsonDanger, fontSize = 11.sp)
                                    }
                                }
                                Surface(
                                    color = statusColor.copy(alpha = 0.14f),
                                    shape = RoundedCornerShape(50)
                                ) {
                                    Text(
                                        text = wd.status.uppercase(),
                                        color = statusColor,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Full Transaction Ledger Header & Filters
                item {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Transaction Ledger",
                                color = CardDarkText,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${filteredTx.size} entries",
                                color = CardMutedText,
                                fontSize = 12.sp
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(filterOptions) { opt ->
                                FilterChip(
                                    selected = txFilter == opt,
                                    onClick = { txFilter = opt },
                                    label = { Text(opt, fontSize = 12.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = ArenaPurple,
                                        selectedLabelColor = Color.White,
                                        containerColor = Color(0xFFF1F5F9),
                                        labelColor = CardDarkText
                                    )
                                )
                            }
                        }
                    }
                }

                if (filteredTx.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0xFFF8FAFC))
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No transactions in $txFilter",
                                color = CardMutedText,
                                fontSize = 14.sp
                            )
                        }
                    }
                } else {
                    items(filteredTx, key = { it.id }) { tx ->
                        val isCredit = tx.direction == "credit"
                        val amountColor = if (isCredit) ArenaGreen else CrimsonDanger
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(amountColor.copy(alpha = 0.14f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CoinBadge(size = 20.dp)
                                    }
                                    Spacer(Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = tx.title,
                                            color = CardDarkText,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 14.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        if (tx.subtitle.isNotBlank()) {
                                            Text(
                                                text = tx.subtitle,
                                                color = CardMutedText,
                                                fontSize = 11.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        Text(
                                            text = tx.walletBucket.uppercase(),
                                            color = ArenaPurple,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = "${if (isCredit) "+" else "-"}${ArenaFormatters.formatInr(tx.amount)}",
                                    color = amountColor,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 15.sp
                                )
                            }
                        }
                    }
                }

                item { Spacer(Modifier.height(24.dp)) }
            }
        }
    }

    // Available Offers Drawer
    if (showOffersSheet) {
        ModalBottomSheet(onDismissRequest = { showOffersSheet = false }, containerColor = Color.White) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Text("Available Deposit Offers", style = MaterialTheme.typography.headlineSmall, color = HeaderNavy, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(10.dp))
                if (depositOffers.isEmpty()) {
                    Text("No active deposit promo codes right now.", color = CardMutedText, modifier = Modifier.padding(vertical = 16.dp))
                } else {
                    depositOffers.forEach { coupon ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                            border = BorderStroke(1.dp, ArenaOrange.copy(alpha = 0.55f)),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(coupon.code, style = MaterialTheme.typography.titleLarge, color = ArenaPurple, fontWeight = FontWeight.ExtraBold)
                                    Text(coupon.description.ifBlank { "Min deposit ₹${coupon.minAmount.toInt()}" }, style = MaterialTheme.typography.bodyMedium, color = CardMutedText)
                                }
                                Button(
                                    onClick = {
                                        selectedPromoPrefill = coupon.code
                                        showOffersSheet = false
                                        showDepositSheet = true
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = ArenaOrange, contentColor = Color.White)
                                ) {
                                    Text("Apply", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(20.dp))
            }
        }
    }

    // Add Coins Modal -> Generates PayZapi Order & Immediately Shows In-App Payment QR
    if (showDepositSheet) {
        AddFundsSheet(
            initialPromo = selectedPromoPrefill,
            gatewayConfig = gatewayConfig,
            busy = busy,
            onValidatePromo = onValidatePromo,
            onSubmit = { amount, provider, promo ->
                onCreateDeposit(amount, provider, promo)
                showDepositSheet = false
            },
            onDismiss = { showDepositSheet = false }
        )
    }

    // In-App Payment QR Checkout Modal (QR rendered inside the app + Auto-Check + Check Payment)
    if (activeCheckoutOrder != null) {
        PayZapiCheckoutSheet(
            order = activeCheckoutOrder,
            busy = busy,
            onCheckPaymentNow = { onCheckPaymentNow(activeCheckoutOrder) },
            onDismiss = onCloseCheckout
        )
    }

    // Redeem / Withdraw Modal
    if (showWithdrawSheet) {
        WithdrawSheet(
            winningBalance = user?.winningBalance ?: 0.0,
            gatewayConfig = gatewayConfig,
            busy = busy,
            onSubmit = { method, dest, amt ->
                onSubmitWithdrawal(method, dest, amt) { showWithdrawSheet = false }
            },
            onDismiss = { showWithdrawSheet = false }
        )
    }
}

@Composable
private fun WalletBalanceKpiCard(
    label: String,
    amount: Double,
    modifier: Modifier = Modifier
) {
    Surface(
        color = Color.White,
        shape = RoundedCornerShape(14.dp),
        shadowElevation = 2.dp,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CoinBadge(size = 18.dp)
                Spacer(Modifier.width(5.dp))
                Text(
                    text = formatCoinWhole(amount),
                    color = ArenaPurple,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = label,
                color = CardDarkText,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun WalletQuickActionTile(
    label: String,
    icon: ImageVector,
    tint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = Color.White,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        shadowElevation = 2.dp,
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(tint.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = tint,
                    modifier = Modifier.size(26.dp)
                )
            }
            Spacer(Modifier.height(10.dp))
            Text(
                text = label,
                color = CardMutedText,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private fun formatCoinWhole(value: Double): String {
    return if (value % 1.0 == 0.0) {
        value.toLong().toString()
    } else {
        String.format("%.2f", value)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddFundsSheet(
    initialPromo: String,
    gatewayConfig: GatewayConfig,
    busy: Boolean,
    onValidatePromo: suspend (String, Double) -> Result<Coupon>,
    onSubmit: (Double, String, String) -> Unit,
    onDismiss: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var amountText by remember { mutableStateOf("100") }
    var promoCode by remember(initialPromo) { mutableStateOf(initialPromo) }
    var promoPreview by remember { mutableStateOf<String?>(null) }
    var promoError by remember { mutableStateOf<String?>(null) }

    val availableProviders = buildList {
        if (gatewayConfig.fampayEnabled) add("fampay" to "FamPay UPI")
        if (gatewayConfig.paytmEnabled) add("paytm" to "Paytm UPI")
        if (gatewayConfig.bharatpeEnabled) add("bharatpe" to "BharatPe UPI")
        if (gatewayConfig.binanceEnabled) add("binance" to "Binance USDT")
    }.ifEmpty { listOf("fampay" to "FamPay UPI") }

    var selectedProvider by remember { mutableStateOf(availableProviders.first().first) }

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Color.White) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CoinBadge(size = 26.dp)
                Spacer(Modifier.width(8.dp))
                Column {
                    Text(
                        "Add Coins via In-App QR",
                        color = CardDarkText,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Instant PayZapi QR generated right inside the app",
                        color = CardMutedText,
                        fontSize = 12.sp
                    )
                }
            }
            Spacer(Modifier.height(14.dp))

            OutlinedTextField(
                value = amountText,
                onValueChange = { amountText = it.filter { ch -> ch.isDigit() || ch == '.' } },
                label = { Text("Coins / Amount (₹${gatewayConfig.minDeposit.toInt()} - ₹${gatewayConfig.maxDeposit.toInt()})") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("deposit_amount_input")
            )
            Spacer(Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(listOf(50, 100, 250, 500, 1000)) { preset ->
                    FilterChip(
                        selected = amountText == preset.toString(),
                        onClick = { amountText = preset.toString() },
                        label = { Text("₹$preset", fontWeight = FontWeight.SemiBold) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = ArenaPurple,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
            Text("Payment Gateway", color = CardDarkText, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Spacer(Modifier.height(6.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(availableProviders) { (key, label) ->
                    FilterChip(
                        selected = selectedProvider == key,
                        onClick = { selectedProvider = key },
                        label = { Text(label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = HeaderNavy,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = promoCode,
                    onValueChange = { promoCode = it.uppercase() },
                    label = { Text("Promo Code (Optional)") },
                    singleLine = true,
                    modifier = Modifier.weight(1f).testTag("deposit_promo_input")
                )
                Button(
                    onClick = {
                        promoError = null
                        promoPreview = null
                        val amt = amountText.toDoubleOrNull() ?: 0.0
                        scope.launch {
                            onValidatePromo(promoCode, amt)
                                .onSuccess {
                                    val bonus = it.computeBenefit(amt)
                                    promoPreview = "+₹${bonus.toInt()} Extra Bonus Coins!"
                                }
                                .onFailure { promoError = it.localizedMessage }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ArenaPurple, contentColor = Color.White),
                    shape = RoundedCornerShape(10.dp)
                ) { Text("Apply") }
            }
            if (promoPreview != null) {
                Text(promoPreview!!, color = ArenaGreen, fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
            }
            if (promoError != null) {
                Text(promoError!!, color = CrimsonDanger, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
            }

            Spacer(Modifier.height(18.dp))
            Button(
                onClick = {
                    val amt = amountText.toDoubleOrNull() ?: 0.0
                    onSubmit(amt, selectedProvider, promoCode)
                },
                enabled = !busy,
                colors = ButtonDefaults.buttonColors(containerColor = ArenaPurple, contentColor = Color.White),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("submit_deposit_btn")
            ) {
                Icon(Icons.Default.QrCode2, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    text = if (busy) "Generating Payment QR..." else "GENERATE PAYMENT QR",
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PayZapiCheckoutSheet(
    order: DepositOrder,
    busy: Boolean,
    onCheckPaymentNow: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var remainingSecs by remember(order.expiryMillis) {
        mutableStateOf(((order.expiryMillis - System.currentTimeMillis()) / 1000L).coerceAtLeast(0L))
    }

    LaunchedEffect(order.expiryMillis) {
        while (remainingSecs > 0L) {
            delay(1000L)
            remainingSecs = ((order.expiryMillis - System.currentTimeMillis()) / 1000L).coerceAtLeast(0L)
        }
    }

    // Generate or decode QR completely in-app so user never has to open external webpage
    val decodedQr = remember(order.qrCodeUrl, order.upiIntentUrl, order.checkoutUrl) {
        when {
            order.qrCodeUrl.startsWith("data:image", ignoreCase = true) ->
                QrCodeUtils.decodeBase64Image(order.qrCodeUrl)
            order.upiIntentUrl.isNotBlank() ->
                QrCodeUtils.generateQrBitmap(order.upiIntentUrl, 520)
            order.checkoutUrl.isNotBlank() ->
                QrCodeUtils.generateQrBitmap(order.checkoutUrl, 520)
            else -> null
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = HeaderNavy) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                color = ArenaPurple.copy(alpha = 0.35f),
                border = BorderStroke(1.dp, CoinYellow.copy(alpha = 0.65f)),
                shape = RoundedCornerShape(50)
            ) {
                Text(
                    text = "IN-APP PAYMENT QR • AUTO-VERIFYING EVERY 6s",
                    color = CoinYellow,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                )
            }

            Spacer(Modifier.height(10.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                CoinBadge(size = 28.dp)
                Spacer(Modifier.width(8.dp))
                Text(
                    text = ArenaFormatters.formatInr(order.amount),
                    color = ArenaGreen,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
            Text(
                text = "Scan with any UPI app (PhonePe, GPay, Paytm, FamPay) • Expires %02d:%02d".format(
                    remainingSecs / 60,
                    remainingSecs % 60
                ),
                color = AmberWarning,
                fontSize = 12.sp,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(14.dp))

            // In-App White QR Code Frame
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                modifier = Modifier.size(236.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize().padding(14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (decodedQr != null) {
                        Image(
                            bitmap = decodedQr.asImageBitmap(),
                            contentDescription = "In-App Payment QR Code",
                            modifier = Modifier.fillMaxSize()
                        )
                    } else if (order.qrCodeUrl.startsWith("http", ignoreCase = true)) {
                        AsyncImage(
                            model = order.qrCodeUrl,
                            contentDescription = "In-App Payment QR Code",
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Text(
                            text = "QR unavailable for this order",
                            color = CardMutedText,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (decodedQr != null) {
                    OutlinedButton(
                        onClick = {
                            val saved = QrCodeUtils.saveQrToPictures(context, decodedQr, "AhxanPay_${order.clientOrderId}")
                            Toast.makeText(
                                context,
                                if (saved) "QR saved to Pictures/AhxanArena" else "Unable to save QR",
                                Toast.LENGTH_SHORT
                            ).show()
                        },
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, ArenaOrange.copy(alpha = 0.8f)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, tint = ArenaOrange, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Save QR", color = ArenaOrange, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
                if (order.upiIntentUrl.isNotBlank()) {
                    OutlinedButton(
                        onClick = {
                            runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(order.upiIntentUrl))) }
                                .onFailure { Toast.makeText(context, "No UPI app installed", Toast.LENGTH_SHORT).show() }
                        },
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.4f)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Payment, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Pay via UPI App", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // Prominent Check Payment Button
            Button(
                onClick = onCheckPaymentNow,
                enabled = !busy,
                colors = ButtonDefaults.buttonColors(containerColor = ArenaGreen, contentColor = Color.White),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("checkout_check_payment_btn")
            ) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    text = if (busy) "Verifying Payment..." else "I HAVE PAID • CHECK PAYMENT",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 14.sp
                )
            }

            Spacer(Modifier.height(10.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable {
                        ArenaFormatters.copyToClipboard(context, "Order ID", order.payzapiOrderId)
                        Toast.makeText(context, "Order ID copied", Toast.LENGTH_SHORT).show()
                    }
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text("Order: ${order.payzapiOrderId}", color = TextMuted, fontSize = 11.sp)
                Spacer(Modifier.width(4.dp))
                Icon(Icons.Default.ContentCopy, contentDescription = "Copy Order ID", tint = TextMuted, modifier = Modifier.size(13.dp))
            }
            Spacer(Modifier.height(22.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WithdrawSheet(
    winningBalance: Double,
    gatewayConfig: GatewayConfig,
    busy: Boolean,
    onSubmit: (String, String, Double) -> Unit,
    onDismiss: () -> Unit
) {
    var method by remember { mutableStateOf("UPI") }
    var destination by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }

    val gross = amountText.toDoubleOrNull() ?: 0.0
    val fee = gross * gatewayConfig.withdrawalFeePercent / 100.0
    val net = (gross - fee).coerceAtLeast(0.0)

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Color.White) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 8.dp)) {
            Text("Redeem Coins", color = CardDarkText, fontSize = 19.sp, fontWeight = FontWeight.Bold)
            Text(
                "Available Winning Coins: ${ArenaFormatters.formatInr(winningBalance)}",
                color = ArenaGreen,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(10.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = method == "UPI",
                    onClick = { method = "UPI" },
                    label = { Text("UPI Transfer") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = ArenaPurple,
                        selectedLabelColor = Color.White
                    )
                )
                FilterChip(
                    selected = method == "BINANCE_BEP20",
                    onClick = { method = "BINANCE_BEP20" },
                    label = { Text("Binance BEP-20 USDT") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = ArenaPurple,
                        selectedLabelColor = Color.White
                    )
                )
            }
            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = destination,
                onValueChange = { destination = it },
                label = { Text(if (method == "UPI") "Enter UPI ID (e.g. name@okaxis)" else "Enter Binance BEP-20 USDT Address") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("withdraw_dest_input")
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = amountText,
                onValueChange = { amountText = it.filter { ch -> ch.isDigit() || ch == '.' } },
                label = { Text("Amount (Min ₹${gatewayConfig.minWithdrawal.toInt()}, Fee ${gatewayConfig.withdrawalFeePercent}%)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("withdraw_amount_input")
            )
            if (gross > 0.0) {
                Spacer(Modifier.height(6.dp))
                Text(
                    "Fee: ${ArenaFormatters.formatInr(fee)} • Net Payout: ${ArenaFormatters.formatInr(net)}",
                    color = ArenaGreen,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
            Spacer(Modifier.height(14.dp))
            Button(
                onClick = { onSubmit(method, destination, gross) },
                enabled = !busy,
                colors = ButtonDefaults.buttonColors(containerColor = ArenaGreen, contentColor = Color.White),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("submit_withdraw_btn")
            ) {
                Text(
                    text = if (busy) "Submitting..." else "REQUEST REDEMPTION",
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}
