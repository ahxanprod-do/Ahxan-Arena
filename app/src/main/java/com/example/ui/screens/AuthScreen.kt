package com.example.ui.screens

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Pin
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import com.example.data.auth.AuthHelper
import com.example.data.network.EmailOtpClient
import com.example.ui.components.ArenaBrandEmblem
import com.example.ui.components.CyberGlassCard
import com.example.ui.components.StatusPill
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CrimsonDanger
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.EmeraldProfit
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.ObsidianElevated
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.launch

@Composable
fun AuthScreen(
    initialReferralCode: String = "",
    onAuthSuccess: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }
    val otpClient = remember { EmailOtpClient() }

    // 0 = Player Login, 1 = Player Register, 2 = Admin Google Login
    var selectedTab by remember { mutableIntStateOf(if (initialReferralCode.isNotBlank()) 1 else 0) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var infoMsg by remember { mutableStateOf<String?>(null) }

    // Player Login fields
    var loginIdentifier by remember { mutableStateOf("") }

    // Player Register fields
    var regUsername by remember { mutableStateOf("") }
    var regGameName by remember { mutableStateOf("") }
    var regGameUid by remember { mutableStateOf("") }
    var regPhone by remember { mutableStateOf("") }
    var regEmail by remember { mutableStateOf("") }
    var regPromoCode by remember { mutableStateOf(initialReferralCode) }
    var regOtpInput by remember { mutableStateOf("") }
    var codeSentStage by remember { mutableStateOf(false) }
    var resendMissingConfig by remember { mutableStateOf(false) }
    val secretPrefs = remember { context.getSharedPreferences("ahxan_secrets", Context.MODE_PRIVATE) }
    var resendKeyInput by remember { mutableStateOf(secretPrefs.getString("resend_api_key", "").orEmpty()) }
    var showKeyConfigBox by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        AuthHelper.attemptAutoSignIn(
            context = context,
            credentialManager = credentialManager,
            scope = scope,
            onAuthSuccess = onAuthSuccess,
            onUnauthenticated = {}
        )
    }

    fun launchCredentialSignIn(mode: String) {
        errorMsg = null
        val prefs = context.getSharedPreferences("ahxan_app", Context.MODE_PRIVATE).edit()
        prefs.putString("pending_login_mode", mode)
        if (mode == "player" && selectedTab == 1) {
            prefs.putString("pending_reg_username", regUsername.trim())
            prefs.putString("pending_reg_ign", regGameName.trim())
            prefs.putString("pending_reg_uid", regGameUid.trim())
            prefs.putString("pending_reg_phone", regPhone.trim())
            if (regPromoCode.isNotBlank()) {
                prefs.putString("pending_ref_code", regPromoCode.trim().uppercase())
            }
        }
        prefs.apply()

        isLoading = true
        AuthHelper.onGoogleSignInClicked(
            context = context,
            credentialManager = credentialManager,
            scope = scope,
            onAuthSuccess = {
                isLoading = false
                onAuthSuccess()
            },
            onAuthError = { msg ->
                isLoading = false
                errorMsg = msg
            },
            onAuthCancelled = {
                isLoading = false
            }
        )
    }

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = NeonCyan,
        unfocusedBorderColor = Color(0x33FFFFFF),
        focusedTextColor = TextPrimary,
        unfocusedTextColor = TextPrimary,
        focusedLabelColor = NeonCyan,
        unfocusedLabelColor = TextMuted
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(ObsidianBg, Color(0xFF0C1225), ObsidianBg)))
            .statusBarsPadding()
            .navigationBarsPadding(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 520.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // New Brand Emblem Header
            ArenaBrandEmblem(size = 86.dp)
            Spacer(Modifier.height(12.dp))
            StatusPill(label = "ESPORTS TOURNAMENT COMMAND", color = NeonCyan)
            Spacer(Modifier.height(6.dp))
            Text(
                text = "AHXAN ARENA",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.ExtraBold,
                color = TextPrimary
            )
            Text(
                text = "BGMI • Free Fire MAX • COD Mobile • Valorant",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary
            )

            Spacer(Modifier.height(18.dp))

            // 3-Mode Auth Switcher: Player Login | Player Register | Admin Login
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(ObsidianElevated)
                    .border(1.dp, Color(0x28FFFFFF), RoundedCornerShape(14.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf(
                    Triple(0, "Login", "auth_tab_login"),
                    Triple(1, "Register", "auth_tab_register"),
                    Triple(2, "Admin", "auth_tab_admin")
                ).forEach { (idx, title, tag) ->
                    val selected = selectedTab == idx
                    val activeColor = if (idx == 2) AmberWarning else NeonCyan
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (selected) activeColor else Color.Transparent)
                            .clickable {
                                selectedTab = idx
                                errorMsg = null
                                infoMsg = null
                            }
                            .padding(vertical = 11.dp)
                            .testTag(tag),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = if (selected) ObsidianBg else TextSecondary
                        )
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            CyberGlassCard(glowColor = if (selectedTab == 2) AmberWarning else NeonCyan) {
                when (selectedTab) {
                    0 -> {
                        // PLAYER LOGIN
                        Text("Player Login", style = MaterialTheme.typography.headlineSmall, color = TextPrimary)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Enter your registered email or username to continue to your tournament lobby & wallet.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                        Spacer(Modifier.height(14.dp))

                        OutlinedTextField(
                            value = loginIdentifier,
                            onValueChange = { loginIdentifier = it },
                            label = { Text("Email Address or Username") },
                            leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = NeonCyan) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            colors = fieldColors,
                            modifier = Modifier.fillMaxWidth().testTag("login_identifier_input")
                        )

                        if (errorMsg != null) {
                            Spacer(Modifier.height(10.dp))
                            Text(errorMsg!!, color = CrimsonDanger, style = MaterialTheme.typography.bodyMedium)
                        }

                        Spacer(Modifier.height(16.dp))
                        Button(
                            onClick = {
                                if (loginIdentifier.trim().isBlank()) {
                                    errorMsg = "Please enter your email or username to continue."
                                } else {
                                    launchCredentialSignIn("player")
                                }
                            },
                            enabled = !isLoading,
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = ObsidianBg),
                            modifier = Modifier.fillMaxWidth().height(52.dp).testTag("player_login_button")
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(Modifier.size(22.dp), color = ObsidianBg, strokeWidth = 2.dp)
                            } else {
                                Text("Continue Player Login", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("New player?", style = MaterialTheme.typography.bodyMedium, color = TextMuted)
                            TextButton(onClick = { selectedTab = 1; errorMsg = null }) {
                                Text("Create Account", color = NeonCyan, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    1 -> {
                        // PLAYER REGISTER (Username, Game Name, Game UID, Phone, Email + Email Code, Promo Optional)
                        Text("Player Registration", style = MaterialTheme.typography.headlineSmall, color = TextPrimary)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Create your esports profile, verify your email code, and link an optional promo code.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                        Spacer(Modifier.height(12.dp))

                        OutlinedTextField(
                            value = regUsername,
                            onValueChange = { regUsername = it },
                            label = { Text("Username *") },
                            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = NeonCyan) },
                            singleLine = true,
                            enabled = !codeSentStage,
                            colors = fieldColors,
                            modifier = Modifier.fillMaxWidth().testTag("reg_username_input")
                        )
                        Spacer(Modifier.height(8.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = regGameName,
                                onValueChange = { regGameName = it },
                                label = { Text("Game Name *") },
                                leadingIcon = { Icon(Icons.Default.SportsEsports, contentDescription = null, tint = ElectricViolet) },
                                singleLine = true,
                                enabled = !codeSentStage,
                                colors = fieldColors,
                                modifier = Modifier.weight(1f).testTag("reg_game_name_input")
                            )
                            OutlinedTextField(
                                value = regGameUid,
                                onValueChange = { regGameUid = it },
                                label = { Text("Game UID *") },
                                leadingIcon = { Icon(Icons.Default.Badge, contentDescription = null, tint = EmeraldProfit) },
                                singleLine = true,
                                enabled = !codeSentStage,
                                colors = fieldColors,
                                modifier = Modifier.weight(1f).testTag("reg_game_uid_input")
                            )
                        }
                        Spacer(Modifier.height(8.dp))

                        OutlinedTextField(
                            value = regPhone,
                            onValueChange = { regPhone = it.filter { ch -> ch.isDigit() || ch == '+' || ch == ' ' }.take(16) },
                            label = { Text("Phone Number *") },
                            leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = NeonCyan) },
                            singleLine = true,
                            enabled = !codeSentStage,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            colors = fieldColors,
                            modifier = Modifier.fillMaxWidth().testTag("reg_phone_input")
                        )
                        Spacer(Modifier.height(8.dp))

                        OutlinedTextField(
                            value = regEmail,
                            onValueChange = { regEmail = it },
                            label = { Text("Email Address *") },
                            leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = NeonCyan) },
                            singleLine = true,
                            enabled = !codeSentStage,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            colors = fieldColors,
                            modifier = Modifier.fillMaxWidth().testTag("reg_email_input")
                        )
                        Spacer(Modifier.height(8.dp))

                        OutlinedTextField(
                            value = regPromoCode,
                            onValueChange = { regPromoCode = it.uppercase() },
                            label = { Text("Promo / Referral Code (Optional)") },
                            leadingIcon = { Icon(Icons.Default.CardGiftcard, contentDescription = null, tint = AmberWarning) },
                            singleLine = true,
                            enabled = !codeSentStage,
                            colors = fieldColors,
                            modifier = Modifier.fillMaxWidth().testTag("reg_promo_input")
                        )

                        if (infoMsg != null) {
                            Spacer(Modifier.height(10.dp))
                            Surface(
                                color = EmeraldProfit.copy(alpha = 0.12f),
                                border = BorderStroke(1.dp, EmeraldProfit.copy(alpha = 0.45f)),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = infoMsg!!,
                                    color = EmeraldProfit,
                                    style = MaterialTheme.typography.labelLarge,
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                        }

                        if (errorMsg != null) {
                            Spacer(Modifier.height(10.dp))
                            Text(errorMsg!!, color = CrimsonDanger, style = MaterialTheme.typography.bodyMedium)
                        }

                        Spacer(Modifier.height(14.dp))

                        if (!codeSentStage) {
                            Button(
                                onClick = {
                                    errorMsg = null
                                    infoMsg = null
                                    val digits = regPhone.filter { it.isDigit() }
                                    when {
                                        regUsername.trim().length < 2 -> errorMsg = "Enter a valid Username (min 2 chars)."
                                        regGameName.trim().isBlank() -> errorMsg = "Game Name (IGN) is required."
                                        regGameUid.trim().length < 3 -> errorMsg = "Enter a valid Game UID."
                                        digits.length < 7 -> errorMsg = "Enter a valid Phone Number."
                                        !regEmail.contains("@") || !regEmail.contains(".") -> errorMsg = "Enter a valid Email address."
                                        else -> {
                                            isLoading = true
                                            scope.launch {
                                                val res = otpClient.sendVerificationCode(
                                                    email = regEmail.trim(),
                                                    username = regUsername.trim(),
                                                    overrideKey = resendKeyInput.trim()
                                                )
                                                isLoading = false
                                                if (res.sent) {
                                                    codeSentStage = true
                                                    resendMissingConfig = false
                                                    showKeyConfigBox = false
                                                    infoMsg = res.message
                                                } else if (res.missingConfig) {
                                                    resendMissingConfig = true
                                                    showKeyConfigBox = true
                                                    errorMsg = res.message
                                                } else {
                                                    errorMsg = res.message
                                                }
                                            }
                                        }
                                    }
                                },
                                enabled = !isLoading,
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = ObsidianBg),
                                modifier = Modifier.fillMaxWidth().height(52.dp).testTag("reg_send_code_button")
                            ) {
                                if (isLoading) {
                                    CircularProgressIndicator(Modifier.size(22.dp), color = ObsidianBg, strokeWidth = 2.dp)
                                } else {
                                    Icon(Icons.Default.MarkEmailRead, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text("Send Code on Email", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                }
                            }

                            if (!showKeyConfigBox && otpClient.resolveResendKey(resendKeyInput).isBlank()) {
                                TextButton(
                                    onClick = { showKeyConfigBox = true },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Add Resend API Key Value", color = AmberWarning)
                                }
                            }

                            if (showKeyConfigBox || resendMissingConfig) {
                                Spacer(Modifier.height(10.dp))
                                Surface(
                                    color = ObsidianElevated,
                                    border = BorderStroke(1.dp, AmberWarning.copy(alpha = 0.55f)),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text(
                                            text = "Configure Email OTP Key (Resend)",
                                            style = MaterialTheme.typography.labelLarge,
                                            color = AmberWarning,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(Modifier.height(6.dp))
                                        OutlinedTextField(
                                            value = resendKeyInput,
                                            onValueChange = { resendKeyInput = it },
                                            label = { Text("Paste Resend API Key (re_...)") },
                                            leadingIcon = { Icon(Icons.Default.Key, contentDescription = null, tint = AmberWarning) },
                                            singleLine = true,
                                            colors = fieldColors,
                                            modifier = Modifier.fillMaxWidth().testTag("reg_resend_key_input")
                                        )
                                        Spacer(Modifier.height(8.dp))
                                        Button(
                                            onClick = {
                                                val cleanKey = resendKeyInput.trim()
                                                if (!cleanKey.startsWith("re_")) {
                                                    errorMsg = "Enter a valid Resend API Key starting with re_..."
                                                    return@Button
                                                }
                                                secretPrefs.edit().putString("resend_api_key", cleanKey).apply()
                                                errorMsg = null
                                                if (regEmail.contains("@") && regEmail.contains(".")) {
                                                    isLoading = true
                                                    scope.launch {
                                                        val res = otpClient.sendVerificationCode(
                                                            email = regEmail.trim(),
                                                            username = regUsername.trim(),
                                                            overrideKey = cleanKey
                                                        )
                                                        isLoading = false
                                                        if (res.sent) {
                                                            codeSentStage = true
                                                            resendMissingConfig = false
                                                            showKeyConfigBox = false
                                                            infoMsg = res.message
                                                        } else {
                                                            errorMsg = res.message
                                                        }
                                                    }
                                                } else {
                                                    showKeyConfigBox = false
                                                    infoMsg = "Resend API Key saved! Fill your details above and tap Send Code on Email."
                                                }
                                            },
                                            enabled = !isLoading && resendKeyInput.trim().isNotBlank(),
                                            colors = ButtonDefaults.buttonColors(containerColor = AmberWarning, contentColor = ObsidianBg),
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier.fillMaxWidth().height(48.dp).testTag("reg_save_key_send_otp_btn")
                                        ) {
                                            Text("Save Key & Send Email OTP", fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }

                            if (resendMissingConfig) {
                                Spacer(Modifier.height(8.dp))
                                OutlinedButton(
                                    onClick = { launchCredentialSignIn("player") },
                                    enabled = !isLoading,
                                    border = BorderStroke(1.dp, EmeraldProfit),
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.fillMaxWidth().height(50.dp).testTag("reg_google_complete_button")
                                ) {
                                    Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = EmeraldProfit, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text("Verify Email via Google & Register", color = EmeraldProfit, fontWeight = FontWeight.Bold)
                                }
                            }
                        } else {
                            OutlinedTextField(
                                value = regOtpInput,
                                onValueChange = { regOtpInput = it.filter { ch -> ch.isDigit() }.take(6) },
                                label = { Text("6-Digit Email Verification Code *") },
                                leadingIcon = { Icon(Icons.Default.Pin, contentDescription = null, tint = EmeraldProfit) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                colors = fieldColors,
                                modifier = Modifier.fillMaxWidth().testTag("reg_otp_input")
                            )
                            Spacer(Modifier.height(12.dp))
                            Button(
                                onClick = {
                                    errorMsg = null
                                    val check = otpClient.verifyCode(regEmail.trim(), regOtpInput)
                                    check.onSuccess {
                                        launchCredentialSignIn("player")
                                    }.onFailure { err ->
                                        errorMsg = err.localizedMessage ?: "Invalid verification code"
                                    }
                                },
                                enabled = !isLoading && regOtpInput.length == 6,
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldProfit, contentColor = ObsidianBg),
                                modifier = Modifier.fillMaxWidth().height(52.dp).testTag("reg_verify_complete_button")
                            ) {
                                if (isLoading) {
                                    CircularProgressIndicator(Modifier.size(22.dp), color = ObsidianBg, strokeWidth = 2.dp)
                                } else {
                                    Text("Verify Code & Complete Registration", fontWeight = FontWeight.Bold)
                                }
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                TextButton(onClick = { codeSentStage = false; regOtpInput = "" }) {
                                    Text("Edit Details", color = TextMuted)
                                }
                                TextButton(onClick = {
                                    isLoading = true
                                    scope.launch {
                                        val res = otpClient.sendVerificationCode(
                                            email = regEmail.trim(),
                                            username = regUsername.trim(),
                                            overrideKey = resendKeyInput.trim()
                                        )
                                        isLoading = false
                                        if (res.sent) infoMsg = res.message else errorMsg = res.message
                                    }
                                }) {
                                    Text("Resend Code", color = NeonCyan)
                                }
                            }
                        }
                    }

                    2 -> {
                        // ADMIN ONLY — GOOGLE LOGIN
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AdminPanelSettings, contentDescription = null, tint = AmberWarning, modifier = Modifier.size(26.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Admin Login (Google Only)", style = MaterialTheme.typography.headlineSmall, color = AmberWarning)
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Google Sign-In is exclusively dedicated to Super Admin & authorized RBAC Staff to access the 12-Module Command Center.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                        Spacer(Modifier.height(12.dp))
                        Surface(
                            color = AmberWarning.copy(alpha = 0.12f),
                            border = BorderStroke(1.dp, AmberWarning.copy(alpha = 0.45f)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Players: please use the Login or Register tabs above.",
                                style = MaterialTheme.typography.labelLarge,
                                color = AmberWarning,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(10.dp)
                            )
                        }

                        if (errorMsg != null) {
                            Spacer(Modifier.height(10.dp))
                            Text(errorMsg!!, color = CrimsonDanger, style = MaterialTheme.typography.bodyMedium)
                        }

                        Spacer(Modifier.height(16.dp))
                        Button(
                            onClick = { launchCredentialSignIn("admin") },
                            enabled = !isLoading,
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = AmberWarning, contentColor = ObsidianBg),
                            modifier = Modifier.fillMaxWidth().height(54.dp).testTag("admin_google_sign_in_button")
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(Modifier.size(22.dp), color = ObsidianBg, strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Default.AdminPanelSettings, contentDescription = null, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Admin Sign in with Google", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))
                Text(
                    text = "Protected by Zero-Trust Firebase Security & Google Identity",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
