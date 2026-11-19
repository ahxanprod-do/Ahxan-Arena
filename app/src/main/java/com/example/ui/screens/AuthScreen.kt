package com.example.ui.screens

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import com.example.data.auth.AuthHelper
import com.example.ui.components.ArenaBrandEmblem
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

@Composable
fun AuthScreen(
    initialReferralCode: String = "",
    onAuthSuccess: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }

    // 0 = Player Login, 1 = Player Register, 2 = Admin Google Login
    var selectedTab by remember { mutableIntStateOf(if (initialReferralCode.isNotBlank()) 1 else 0) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    var loginEmail by remember { mutableStateOf("") }
    var regUsername by remember { mutableStateOf("") }
    var regGameName by remember { mutableStateOf("") }
    var regGameUid by remember { mutableStateOf("") }
    var regPhone by remember { mutableStateOf("") }
    var regEmail by remember { mutableStateOf("") }
    var regPromoCode by remember { mutableStateOf(initialReferralCode) }

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
        focusedBorderColor = ArenaPurple,
        unfocusedBorderColor = Color(0xFFCBD5E1),
        focusedTextColor = CardDarkText,
        unfocusedTextColor = CardDarkText,
        focusedLabelColor = ArenaPurple,
        unfocusedLabelColor = CardMutedText
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ArenaCharcoal)
            .statusBarsPadding()
            .navigationBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Dark Navy Header Band matching Home Page
        Surface(
            color = HeaderNavy,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                ArenaBrandEmblem(size = 80.dp)
                Spacer(Modifier.height(10.dp))
                Text(
                    text = "AHXAN ARENA",
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = "BGMI • Free Fire MAX • COD Mobile • Valorant",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
            }
        }

        Column(
            modifier = Modifier
                .widthIn(max = 520.dp)
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Mode Switcher matching Home accents
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(HeaderNavy)
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf(
                    Triple(0, "Login", "auth_tab_login"),
                    Triple(1, "Register", "auth_tab_register"),
                    Triple(2, "Admin", "auth_tab_admin")
                ).forEach { (idx, title, tag) ->
                    val selected = selectedTab == idx
                    val activeColor = if (idx == 2) CoinYellow else ArenaOrange
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(9.dp))
                            .background(if (selected) activeColor else Color.Transparent)
                            .clickable {
                                selectedTab = idx
                                errorMsg = null
                            }
                            .padding(vertical = 11.dp)
                            .testTag(tag),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = title,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (selected) {
                                if (idx == 2) Color(0xFF111827) else Color.White
                            } else TextSecondary
                        )
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            // Clean White Card matching Home/Wallet cards
            Surface(
                color = Color.White,
                shape = RoundedCornerShape(16.dp),
                shadowElevation = 4.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(18.dp)) {
                    when (selectedTab) {
                        0 -> {
                            Text("Player Login", color = CardDarkText, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "Enter your registered email address to sign into your tournament lobby & wallet.",
                                color = CardMutedText,
                                fontSize = 13.sp
                            )
                            Spacer(Modifier.height(14.dp))

                            OutlinedTextField(
                                value = loginEmail,
                                onValueChange = { loginEmail = it },
                                label = { Text("Email Address *") },
                                leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = ArenaOrange) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                                colors = fieldColors,
                                modifier = Modifier.fillMaxWidth().testTag("login_identifier_input")
                            )

                            if (errorMsg != null) {
                                Spacer(Modifier.height(10.dp))
                                Text(errorMsg!!, color = CrimsonDanger, fontSize = 13.sp)
                            }

                            Spacer(Modifier.height(16.dp))
                            Button(
                                onClick = {
                                    if (!loginEmail.contains("@") || !loginEmail.contains(".")) {
                                        errorMsg = "Please enter a valid email address."
                                    } else {
                                        launchCredentialSignIn("player")
                                    }
                                },
                                enabled = !isLoading,
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = ArenaPurple, contentColor = Color.White),
                                modifier = Modifier.fillMaxWidth().height(50.dp).testTag("player_login_button")
                            ) {
                                if (isLoading) {
                                    CircularProgressIndicator(Modifier.size(22.dp), color = Color.White, strokeWidth = 2.dp)
                                } else {
                                    Text("CONTINUE PLAYER LOGIN", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                }
                            }

                            Spacer(Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("New player?", color = CardMutedText, fontSize = 13.sp)
                                TextButton(onClick = { selectedTab = 1; errorMsg = null }) {
                                    Text("Create Account", color = ArenaOrange, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        1 -> {
                            Text("Player Registration", color = CardDarkText, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "Enter your player details, email, and optional promo code.",
                                color = CardMutedText,
                                fontSize = 13.sp
                            )
                            Spacer(Modifier.height(12.dp))

                            OutlinedTextField(
                                value = regUsername,
                                onValueChange = { regUsername = it },
                                label = { Text("Username *") },
                                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = ArenaOrange) },
                                singleLine = true,
                                colors = fieldColors,
                                modifier = Modifier.fillMaxWidth().testTag("reg_username_input")
                            )
                            Spacer(Modifier.height(8.dp))

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = regGameName,
                                    onValueChange = { regGameName = it },
                                    label = { Text("Game Name *") },
                                    leadingIcon = { Icon(Icons.Default.SportsEsports, contentDescription = null, tint = ArenaPurple) },
                                    singleLine = true,
                                    colors = fieldColors,
                                    modifier = Modifier.weight(1f).testTag("reg_game_name_input")
                                )
                                OutlinedTextField(
                                    value = regGameUid,
                                    onValueChange = { regGameUid = it },
                                    label = { Text("Game UID *") },
                                    leadingIcon = { Icon(Icons.Default.Badge, contentDescription = null, tint = ArenaGreen) },
                                    singleLine = true,
                                    colors = fieldColors,
                                    modifier = Modifier.weight(1f).testTag("reg_game_uid_input")
                                )
                            }
                            Spacer(Modifier.height(8.dp))

                            OutlinedTextField(
                                value = regPhone,
                                onValueChange = { regPhone = it.filter { ch -> ch.isDigit() || ch == '+' || ch == ' ' }.take(16) },
                                label = { Text("Phone Number *") },
                                leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = ArenaOrange) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                colors = fieldColors,
                                modifier = Modifier.fillMaxWidth().testTag("reg_phone_input")
                            )
                            Spacer(Modifier.height(8.dp))

                            OutlinedTextField(
                                value = regEmail,
                                onValueChange = { regEmail = it },
                                label = { Text("Email Address *") },
                                leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = ArenaPurple) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                                colors = fieldColors,
                                modifier = Modifier.fillMaxWidth().testTag("reg_email_input")
                            )
                            Spacer(Modifier.height(8.dp))

                            OutlinedTextField(
                                value = regPromoCode,
                                onValueChange = { regPromoCode = it.uppercase() },
                                label = { Text("Promo / Referral Code (Optional)") },
                                leadingIcon = { Icon(Icons.Default.CardGiftcard, contentDescription = null, tint = ArenaOrange) },
                                singleLine = true,
                                colors = fieldColors,
                                modifier = Modifier.fillMaxWidth().testTag("reg_promo_input")
                            )

                            if (errorMsg != null) {
                                Spacer(Modifier.height(10.dp))
                                Text(errorMsg!!, color = CrimsonDanger, fontSize = 13.sp)
                            }

                            Spacer(Modifier.height(16.dp))
                            Button(
                                onClick = {
                                    errorMsg = null
                                    val digits = regPhone.filter { it.isDigit() }
                                    when {
                                        regUsername.trim().length < 2 -> errorMsg = "Enter a valid Username (min 2 chars)."
                                        regGameName.trim().isBlank() -> errorMsg = "Game Name (IGN) is required."
                                        regGameUid.trim().length < 3 -> errorMsg = "Enter a valid Game UID."
                                        digits.length < 7 -> errorMsg = "Enter a valid Phone Number."
                                        !regEmail.contains("@") || !regEmail.contains(".") -> errorMsg = "Enter a valid Email address."
                                        else -> launchCredentialSignIn("player")
                                    }
                                },
                                enabled = !isLoading,
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = ArenaPurple, contentColor = Color.White),
                                modifier = Modifier.fillMaxWidth().height(50.dp).testTag("reg_submit_button")
                            ) {
                                if (isLoading) {
                                    CircularProgressIndicator(Modifier.size(22.dp), color = Color.White, strokeWidth = 2.dp)
                                } else {
                                    Text("CREATE ACCOUNT & CONTINUE", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                }
                            }
                        }

                        2 -> {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.AdminPanelSettings, contentDescription = null, tint = ArenaOrange, modifier = Modifier.size(26.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Admin Login (Google Only)", color = CardDarkText, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                            }
                            Spacer(Modifier.height(6.dp))
                            Text(
                                "Google Sign-In is exclusively dedicated to Super Admin & authorized RBAC Staff.",
                                color = CardMutedText,
                                fontSize = 13.sp
                            )
                            Spacer(Modifier.height(12.dp))
                            Surface(
                                color = CoinYellow.copy(alpha = 0.2f),
                                border = BorderStroke(1.dp, ArenaOrange.copy(alpha = 0.45f)),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "Players: please use Login or Register above.",
                                    color = Color(0xFF92400E),
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.sp,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(10.dp)
                                )
                            }

                            if (errorMsg != null) {
                                Spacer(Modifier.height(10.dp))
                                Text(errorMsg!!, color = CrimsonDanger, fontSize = 13.sp)
                            }

                            Spacer(Modifier.height(16.dp))
                            Button(
                                onClick = { launchCredentialSignIn("admin") },
                                enabled = !isLoading,
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = ArenaOrange, contentColor = Color.White),
                                modifier = Modifier.fillMaxWidth().height(52.dp).testTag("admin_google_sign_in_button")
                            ) {
                                if (isLoading) {
                                    CircularProgressIndicator(Modifier.size(22.dp), color = Color.White, strokeWidth = 2.dp)
                                } else {
                                    Icon(Icons.Default.AdminPanelSettings, contentDescription = null, modifier = Modifier.size(20.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text("ADMIN SIGN IN WITH GOOGLE", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = "Secured by Firebase Authentication & Google Identity",
                        fontSize = 11.sp,
                        color = CardMutedText,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}
