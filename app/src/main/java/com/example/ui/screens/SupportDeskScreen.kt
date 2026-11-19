package com.example.ui.screens

import android.graphics.BitmapFactory
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.SupportTicket
import com.example.data.model.TicketMessage
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
import com.example.utils.QrCodeUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupportDeskScreen(
    tickets: List<SupportTicket>,
    selectedMessages: List<TicketMessage>,
    busy: Boolean,
    onObserveMessages: (String) -> Unit,
    onCreateTicket: (String, String, String, String, String, String, () -> Unit) -> Unit,
    onSendReply: (String, String) -> Unit,
    onBack: () -> Unit
) {
    BackHandler { onBack() }
    val context = LocalContext.current
    var showCreateSheet by remember { mutableStateOf(false) }
    var activeTicket by remember { mutableStateOf<SupportTicket?>(null) }
    var zoomScreenshotBase64 by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ArenaCharcoal)
    ) {
        SubScreenTopBar(
            title = "Contact Us",
            containerColor = HeaderNavy,
            onBack = onBack,
            trailingContent = {
                Button(
                    onClick = { showCreateSheet = true },
                    colors = ButtonDefaults.buttonColors(containerColor = ArenaOrange, contentColor = Color.White),
                    shape = RoundedCornerShape(50),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("new_ticket_btn")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("New Ticket", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        )

        if (activeTicket != null) {
            val ticket = activeTicket!!
            var replyText by remember { mutableStateOf("") }
            Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = { activeTicket = null },
                        border = BorderStroke(1.dp, ArenaOrange)
                    ) { Text("All Tickets", color = ArenaOrange, fontWeight = FontWeight.Bold) }
                    val stColor = if (ticket.status == "resolved") ArenaGreen else ArenaOrange
                    Surface(color = stColor.copy(alpha = 0.2f), shape = RoundedCornerShape(50)) {
                        Text(
                            text = ticket.status.uppercase(),
                            color = stColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(ticket.subject, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text(
                    "${ticket.category}${if (ticket.matchId.isNotBlank()) " • Match: ${ticket.matchId}" else ""}",
                    color = Color(0xFFCBD5E1),
                    fontSize = 12.sp
                )

                if (ticket.screenshotBase64.isNotBlank()) {
                    val bmp = remember(ticket.screenshotBase64) { QrCodeUtils.decodeBase64Image(ticket.screenshotBase64) }
                    if (bmp != null) {
                        Spacer(Modifier.height(8.dp))
                        Image(
                            bitmap = bmp.asImageBitmap(),
                            contentDescription = "Uploaded Screenshot",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .height(110.dp)
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { zoomScreenshotBase64 = ticket.screenshotBase64 }
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(selectedMessages, key = { it.id }) { msg ->
                        val isAdmin = msg.senderRole == "admin"
                        Surface(
                            color = if (isAdmin) Color(0xFFECFDF5) else Color.White,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = if (isAdmin) "Ahxan Arena Support" else msg.senderName,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isAdmin) ArenaGreen else ArenaPurple
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(msg.text, color = CardDarkText, fontSize = 14.sp)
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = replyText,
                        onValueChange = { replyText = it },
                        placeholder = { Text("Type your reply...") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = ArenaOrange,
                            unfocusedBorderColor = Color(0xFF64748B)
                        ),
                        modifier = Modifier.weight(1f).testTag("ticket_reply_input")
                    )
                    IconButton(
                        onClick = {
                            if (replyText.isNotBlank()) {
                                onSendReply(ticket.id, replyText)
                                replyText = ""
                            }
                        },
                        modifier = Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)).background(ArenaPurple)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = Color.White)
                    }
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (tickets.isEmpty()) {
                    item {
                        Surface(
                            color = Color.White,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(Icons.Default.SupportAgent, contentDescription = null, tint = ArenaOrange, modifier = Modifier.size(44.dp))
                                Spacer(Modifier.height(10.dp))
                                Text("No Support Tickets", color = CardDarkText, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    "Report hackers, payment delays, or room issues anytime with screenshot evidence.",
                                    color = CardMutedText,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                } else {
                    items(tickets, key = { it.id }) { t ->
                        val statusColor = when (t.status) {
                            "resolved", "closed" -> ArenaGreen
                            "in_progress" -> ArenaBlue
                            else -> ArenaOrange
                        }
                        Surface(
                            color = Color.White,
                            shape = RoundedCornerShape(14.dp),
                            shadowElevation = 2.dp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .clickable {
                                    onObserveMessages(t.id)
                                    activeTicket = t
                                }
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Surface(color = ArenaPurple.copy(alpha = 0.12f), shape = RoundedCornerShape(50)) {
                                        Text(t.category, color = ArenaPurple, fontWeight = FontWeight.Bold, fontSize = 10.sp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                                    }
                                    Surface(color = statusColor.copy(alpha = 0.14f), shape = RoundedCornerShape(50)) {
                                        Text(t.status.uppercase(), color = statusColor, fontWeight = FontWeight.Bold, fontSize = 10.sp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                                    }
                                }
                                Spacer(Modifier.height(8.dp))
                                Text(t.subject, color = CardDarkText, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                if (t.lastMessage.isNotBlank()) {
                                    Text(t.lastMessage, color = CardMutedText, fontSize = 13.sp, maxLines = 2)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreateSheet) {
        val categories = listOf("Payment Issue", "Withdrawal Delay", "Room ID Issue", "Hacker/Cheater Report", "Account/Other")
        var selectedCat by remember { mutableStateOf(categories.first()) }
        var subject by remember { mutableStateOf("") }
        var message by remember { mutableStateOf("") }
        var matchId by remember { mutableStateOf("") }
        var reportedUid by remember { mutableStateOf("") }
        var screenshotBase64 by remember { mutableStateOf("") }

        val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            if (uri != null) {
                runCatching {
                    context.contentResolver.openInputStream(uri)?.use { stream ->
                        val bmp = BitmapFactory.decodeStream(stream)
                        if (bmp != null) {
                            screenshotBase64 = QrCodeUtils.compressBitmapToBase64(bmp)
                        }
                    }
                }
            }
        }

        val sheetFieldColors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = CardDarkText,
            unfocusedTextColor = CardDarkText,
            focusedBorderColor = ArenaPurple,
            unfocusedBorderColor = Color(0xFFCBD5E1),
            focusedLabelColor = ArenaPurple,
            unfocusedLabelColor = CardMutedText
        )

        ModalBottomSheet(onDismissRequest = { showCreateSheet = false }, containerColor = Color.White) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 18.dp, vertical = 8.dp)
            ) {
                Text("Open Support Ticket", color = CardDarkText, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(10.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(categories) { cat ->
                        FilterChip(
                            selected = selectedCat == cat,
                            onClick = { selectedCat = cat },
                            label = { Text(cat) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = ArenaPurple,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = subject, onValueChange = { subject = it }, label = { Text("Subject") }, colors = sheetFieldColors, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = matchId, onValueChange = { matchId = it }, label = { Text("Match ID (Optional)") }, colors = sheetFieldColors, modifier = Modifier.fillMaxWidth())
                if (selectedCat == "Hacker/Cheater Report") {
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(value = reportedUid, onValueChange = { reportedUid = it }, label = { Text("Suspect Game UID") }, colors = sheetFieldColors, modifier = Modifier.fillMaxWidth())
                }
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = message, onValueChange = { message = it }, label = { Text("Describe the issue") }, minLines = 3, colors = sheetFieldColors, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(10.dp))
                OutlinedButton(
                    onClick = {
                        photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                    border = BorderStroke(1.dp, ArenaOrange),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Image, contentDescription = null, tint = ArenaOrange)
                    Spacer(Modifier.width(8.dp))
                    Text(if (screenshotBase64.isNotBlank()) "Screenshot Attached (Tap to change)" else "Attach Screenshot (Photo Picker)", color = ArenaOrange, fontWeight = FontWeight.SemiBold)
                }
                Spacer(Modifier.height(14.dp))
                Button(
                    onClick = {
                        onCreateTicket(selectedCat, subject, message, matchId, reportedUid, screenshotBase64) {
                            showCreateSheet = false
                        }
                    },
                    enabled = !busy,
                    colors = ButtonDefaults.buttonColors(containerColor = ArenaPurple, contentColor = Color.White),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                ) {
                    Text(if (busy) "Submitting..." else "SUBMIT TICKET", fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }

    if (zoomScreenshotBase64 != null) {
        Dialog(onDismissRequest = { zoomScreenshotBase64 = null }) {
            val bmp = QrCodeUtils.decodeBase64Image(zoomScreenshotBase64!!)
            if (bmp != null) {
                Box(modifier = Modifier.clip(RoundedCornerShape(16.dp)).background(HeaderNavy).padding(8.dp)) {
                    Image(bitmap = bmp.asImageBitmap(), contentDescription = "Screenshot Preview", modifier = Modifier.fillMaxWidth())
                }
            }
        }
    }
}
