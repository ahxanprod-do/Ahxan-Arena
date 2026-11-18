package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
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
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.model.Coupon
import com.example.data.model.DepositOrder
import com.example.data.model.GatewayConfig
import com.example.data.model.UserProfile
import com.example.data.model.WalletTransaction
import com.example.data.model.WithdrawalRequest
import com.example.ui.components.CyberGlassCard
import com.example.ui.components.KpiMiniStat
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
    onSubmitWithdrawal: (String, String, Double, () -> Unit) -> Unit
) {
    val scope = rememberCoroutineScope()
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

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("CYBER WALLET", style = MaterialTheme.typography.headlineMedium, color = NeonCyan)
                OutlinedButton(
                    onClick = { showOffersSheet = true },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("wallet_offers_button")
                ) {
                    Icon(Icons.Default.LocalOffer, contentDescription = null, tint = AmberWarning, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Offers (${depositOffers.size})", color = AmberWarning)
                }
            }
        }

        // Segregated Balance Cards
        item {
            CyberGlassCard(glowColor = NeonCyan) {
                Text("TOTAL ARENA BALANCE", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                Spacer(Modifier.height(4.dp))
                Text(
                    text = ArenaFormatters.formatInr(user?.totalBalance ?: 0.0),
                    style = MaterialTheme.typography.displaySmall,
                    color = TextPrimary
                )
                Spacer(Modifier.height(14.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    KpiMiniStat("Deposit", ArenaFormatters.formatInr(user?.depositBalance ?: 0.0), NeonCyan, Modifier.weight(1f))
                    KpiMiniStat("Winning", ArenaFormatters.formatInr(user?.winningBalance ?: 0.0), EmeraldProfit, Modifier.weight(1f))
                    KpiMiniStat("Bonus", ArenaFormatters.formatInr(user?.bonusBalance ?: 0.0), ElectricViolet, Modifier.weight(1f))
                }
                Spacer(Modifier.height(14.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    NeonPrimaryButton(
                        text = "Add Funds",
                        icon = Icons.Default.AddCircle,
                        onClick = { showDepositSheet = true },
                        accentColor = NeonCyan,
                        modifier = Modifier.weight(1f).testTag("wallet_add_funds_btn")
                    )
                    NeonPrimaryButton(
                        text = "Withdraw",
                        icon = Icons.Default.Send,
                        onClick = { showWithdrawSheet = true },
                        accentColor = EmeraldProfit,
                        modifier = Modifier.weight(1f).testTag("wallet_withdraw_btn")
                    )
                }
            }
        }

        // Pending Deposits with prominent "Check Payment" button
        val pendingDeposits = deposits.filter { it.status == "pending" }
        if (pendingDeposits.isNotEmpty()) {
            item {
                SectionHeader(title = "Pending PayZapi Orders", subtitle = "Tap Check Payment after completing UPI/USDT")
            }
            items(pendingDeposits, key = { it.id }) { dep ->
                CyberGlassCard(glowColor = AmberWarning) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("${ArenaFormatters.formatInr(dep.amount)} • ${dep.provider.uppercase()}", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                            Text("Order: ${dep.payzapiOrderId}", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                            if (dep.expectedPromoBonus > 0.0) {
                                Text("+₹${dep.expectedPromoBonus.toInt()} Promo Bonus attached", style = MaterialTheme.typography.labelSmall, color = EmeraldProfit)
                            }
                        }
                        StatusPill("PENDING", AmberWarning)
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { onOpenCheckout(dep) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Open QR", color = NeonCyan)
                        }
                        NeonPrimaryButton(
                            text = if (busy) "Checking..." else "Check Payment",
                            icon = Icons.Default.Refresh,
                            enabled = !busy,
                            onClick = { onCheckPaymentNow(dep) },
                            accentColor = AmberWarning,
                            modifier = Modifier.weight(1.3f).testTag("pending_check_payment_${dep.id}")
                        )
                    }
                }
            }
        }

        // Recent Withdrawals Status
        if (withdrawals.isNotEmpty()) {
            item {
                SectionHeader(title = "Recent Withdrawals", subtitle = "UPI & Binance BEP-20 payouts")
            }
            items(withdrawals.take(4), key = { it.id }) { wd ->
                val color = when (wd.status) {
                    "approved" -> EmeraldProfit
                    "rejected" -> CrimsonDanger
                    else -> AmberWarning
                }
                CyberGlassCard(glowColor = color) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text("${ArenaFormatters.formatInr(wd.netAmount)} (${wd.method})", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                            Text(wd.destination, style = MaterialTheme.typography.labelSmall, color = TextMuted)
                            if (wd.utrOrTxHash.isNotBlank()) {
                                Text("Ref: ${wd.utrOrTxHash}", style = MaterialTheme.typography.labelSmall, color = EmeraldProfit)
                            }
                            if (wd.rejectReason.isNotBlank()) {
                                Text("Reason: ${wd.rejectReason}", style = MaterialTheme.typography.labelSmall, color = CrimsonDanger)
                            }
                        }
                        StatusPill(wd.status, color)
                    }
                }
            }
        }

        // Full Transaction Ledger
        item {
            SectionHeader(title = "Transaction Ledger")
            Spacer(Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(filterOptions) { opt ->
                    FilterChip(
                        selected = txFilter == opt,
                        onClick = { txFilter = opt },
                        label = { Text(opt) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = NeonCyan,
                            selectedLabelColor = ObsidianBg,
                            containerColor = ObsidianElevated
                        )
                    )
                }
            }
        }

        if (filteredTx.isEmpty()) {
            item {
                CyberGlassCard {
                    Text("No transactions in $txFilter", style = MaterialTheme.typography.titleMedium, color = TextMuted)
                }
            }
        } else {
            items(filteredTx, key = { it.id }) { tx ->
                val isCredit = tx.direction == "credit"
                val accent = if (isCredit) EmeraldProfit else CrimsonDanger
                CyberGlassCard(glowColor = accent.copy(alpha = 0.5f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(tx.title, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                            if (tx.subtitle.isNotBlank()) {
                                Text(tx.subtitle, style = MaterialTheme.typography.labelSmall, color = TextMuted)
                            }
                            Text(tx.walletBucket.uppercase(), style = MaterialTheme.typography.labelSmall, color = NeonCyan)
                        }
                        Text(
                            text = "${if (isCredit) "+" else "-"}${ArenaFormatters.formatInr(tx.amount)}",
                            style = MaterialTheme.typography.titleLarge,
                            color = accent
                        )
                    }
                }
            }
        }
    }

    // Available Offers Drawer
    if (showOffersSheet) {
        ModalBottomSheet(onDismissRequest = { showOffersSheet = false }, containerColor = ObsidianSurface) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Text("Available Deposit Offers", style = MaterialTheme.typography.headlineSmall, color = AmberWarning)
                Spacer(Modifier.height(10.dp))
                if (depositOffers.isEmpty()) {
                    Text("No active deposit promo codes right now.", color = TextMuted, modifier = Modifier.padding(vertical = 16.dp))
                } else {
                    depositOffers.forEach { coupon ->
                        CyberGlassCard(glowColor = AmberWarning, modifier = Modifier.padding(bottom = 10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(coupon.code, style = MaterialTheme.typography.titleLarge, color = NeonCyan)
                                    Text(coupon.description.ifBlank { "Min deposit ₹${coupon.minAmount.toInt()}" }, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                                }
                                Button(
                                    onClick = {
                                        selectedPromoPrefill = coupon.code
                                        showOffersSheet = false
                                        showDepositSheet = true
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = AmberWarning, contentColor = ObsidianBg)
                                ) {
                                    Text("Apply")
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(20.dp))
            }
        }
    }

    // Add Funds Modal
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

    // PayZapi Checkout Modal (QR + UPI App + Hosted Checkout + Prominent Check Payment Button)
    if (activeCheckoutOrder != null) {
        PayZapiCheckoutSheet(
            order = activeCheckoutOrder,
            busy = busy,
            onCheckPaymentNow = { onCheckPaymentNow(activeCheckoutOrder) },
            onDismiss = onCloseCheckout
        )
    }

    // Withdraw Modal
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

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = ObsidianSurface) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Text("Add Funds via PayZapi", style = MaterialTheme.typography.headlineSmall, color = NeonCyan)
            Spacer(Modifier.height(10.dp))

            OutlinedTextField(
                value = amountText,
                onValueChange = { amountText = it.filter { ch -> ch.isDigit() || ch == '.' } },
                label = { Text("Amount (₹${gatewayConfig.minDeposit.toInt()} - ₹${gatewayConfig.maxDeposit.toInt()})") },
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
                        label = { Text("₹$preset") }
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
            Text("Select Payment Provider", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
            Spacer(Modifier.height(6.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(availableProviders) { (key, label) ->
                    FilterChip(
                        selected = selectedProvider == key,
                        onClick = { selectedProvider = key },
                        label = { Text(label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = NeonCyan,
                            selectedLabelColor = ObsidianBg
                        )
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = promoCode,
                    onValueChange = { promoCode = it.uppercase() },
                    label = { Text("Deposit Promo Code (Optional)") },
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
                                    promoPreview = "+₹${bonus.toInt()} Extra Bonus on completion!"
                                }
                                .onFailure { promoError = it.localizedMessage }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricViolet)
                ) { Text("Check") }
            }
            if (promoPreview != null) {
                Text(promoPreview!!, color = EmeraldProfit, style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 4.dp))
            }
            if (promoError != null) {
                Text(promoError!!, color = CrimsonDanger, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(top = 4.dp))
            }

            Spacer(Modifier.height(18.dp))
            NeonPrimaryButton(
                text = if (busy) "Creating PayZapi Order..." else "Proceed to Pay",
                icon = Icons.Default.Payment,
                enabled = !busy,
                onClick = {
                    val amt = amountText.toDoubleOrNull() ?: 0.0
                    onSubmit(amt, selectedProvider, promoCode)
                },
                modifier = Modifier.fillMaxWidth().testTag("submit_deposit_btn")
            )
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

    val decodedQr = remember(order.qrCodeUrl, order.upiIntentUrl) {
        when {
            order.qrCodeUrl.startsWith("data:image", ignoreCase = true) -> QrCodeUtils.decodeBase64Image(order.qrCodeUrl)
            order.upiIntentUrl.isNotBlank() -> QrCodeUtils.generateQrBitmap(order.upiIntentUrl, 480)
            else -> QrCodeUtils.generateQrBitmap(order.checkoutUrl, 480)
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = ObsidianSurface) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            StatusPill("PAYZAPI LIVE CHECKOUT • AUTO-CHECKING EVERY 6s", NeonCyan)
            Spacer(Modifier.height(10.dp))
            Text(ArenaFormatters.formatInr(order.amount), style = MaterialTheme.typography.displaySmall, color = EmeraldProfit)
            Text("Expires in %02d:%02d".format(remainingSecs / 60, remainingSecs % 60), style = MaterialTheme.typography.labelLarge, color = AmberWarning)
            Spacer(Modifier.height(12.dp))

            Box(
                modifier = Modifier
                    .size(210.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White)
                    .padding(10.dp),
                contentAlignment = Alignment.Center
            ) {
                if (decodedQr != null) {
                    Image(bitmap = decodedQr.asImageBitmap(), contentDescription = "PayZapi QR Code", modifier = Modifier.fillMaxSize())
                } else if (order.qrCodeUrl.startsWith("http")) {
                    AsyncImage(model = order.qrCodeUrl, contentDescription = "PayZapi QR Code", modifier = Modifier.fillMaxSize())
                }
            }

            Spacer(Modifier.height(10.dp))
            if (decodedQr != null) {
                OutlinedButton(
                    onClick = {
                        val saved = QrCodeUtils.saveQrToPictures(context, decodedQr, "AhxanPay_${order.clientOrderId}")
                        Toast.makeText(context, if (saved) "QR saved to Pictures/AhxanArena" else "Unable to save QR", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Icon(Icons.Default.Download, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Download QR", color = NeonCyan)
                }
            }

            Spacer(Modifier.height(10.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (order.upiIntentUrl.isNotBlank()) {
                    OutlinedButton(
                        onClick = {
                            runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(order.upiIntentUrl))) }
                                .onFailure { Toast.makeText(context, "No UPI app found", Toast.LENGTH_SHORT).show() }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Pay via UPI App", color = TextPrimary)
                    }
                }
                if (order.checkoutUrl.isNotBlank()) {
                    OutlinedButton(
                        onClick = {
                            runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(order.checkoutUrl))) }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.OpenInBrowser, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("PayZapi Checkout", color = TextPrimary)
                    }
                }
            }

            Spacer(Modifier.height(14.dp))
            NeonPrimaryButton(
                text = if (busy) "Verifying with PayZapi..." else "Check Payment",
                icon = Icons.Default.CheckCircle,
                enabled = !busy,
                onClick = onCheckPaymentNow,
                accentColor = EmeraldProfit,
                modifier = Modifier.fillMaxWidth().testTag("checkout_check_payment_btn")
            )
            Spacer(Modifier.height(6.dp))
            Text("Order ID: ${order.payzapiOrderId}", style = MaterialTheme.typography.labelSmall, color = TextMuted)
            Spacer(Modifier.height(24.dp))
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

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = ObsidianSurface) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Text("Withdraw Winnings", style = MaterialTheme.typography.headlineSmall, color = EmeraldProfit)
            Text("Available Winning Balance: ${ArenaFormatters.formatInr(winningBalance)}", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
            Spacer(Modifier.height(10.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = method == "UPI",
                    onClick = { method = "UPI" },
                    label = { Text("UPI ID") }
                )
                FilterChip(
                    selected = method == "BINANCE_BEP20",
                    onClick = { method = "BINANCE_BEP20" },
                    label = { Text("Binance BEP-20 USDT") }
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
                Text("Fee: ${ArenaFormatters.formatInr(fee)} • You Receive: ${ArenaFormatters.formatInr(net)}", style = MaterialTheme.typography.labelLarge, color = EmeraldProfit)
            }
            Spacer(Modifier.height(14.dp))
            NeonPrimaryButton(
                text = if (busy) "Submitting..." else "Request Withdrawal",
                enabled = !busy,
                onClick = { onSubmit(method, destination, gross) },
                accentColor = EmeraldProfit,
                modifier = Modifier.fillMaxWidth().testTag("submit_withdraw_btn")
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}
