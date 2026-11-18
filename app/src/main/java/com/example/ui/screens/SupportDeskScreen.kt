package com.example.ui.screens

import android.graphics.BitmapFactory
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.data.model.SupportTicket
import com.example.data.model.TicketMessage
import com.example.ui.components.CyberGlassCard
import com.example.ui.components.NeonPrimaryButton
import com.example.ui.components.StatusPill
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CrimsonDanger
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.EmeraldProfit
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.ObsidianElevated
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
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

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack, modifier = Modifier.testTag("support_back_btn")) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = NeonCyan)
                }
                Text("24/7 SUPPORT DESK", style = MaterialTheme.typography.headlineSmall, color = NeonCyan)
            }
            NeonPrimaryButton(
                text = "New Ticket",
                icon = Icons.Default.Add,
                onClick = { showCreateSheet = true },
                modifier = Modifier.testTag("new_ticket_btn")
            )
        }

        if (activeTicket != null) {
            val ticket = activeTicket!!
            var replyText by remember { mutableStateOf("") }
            Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(onClick = { activeTicket = null }) { Text("All Tickets", color = NeonCyan) }
                    StatusPill(ticket.status, if (ticket.status == "resolved") EmeraldProfit else AmberWarning)
                }
                Spacer(Modifier.height(8.dp))
                Text(ticket.subject, style = MaterialTheme.typography.titleLarge, color = TextPrimary)
                Text("${ticket.category}${if (ticket.matchId.isNotBlank()) " • Match: ${ticket.matchId}" else ""}", style = MaterialTheme.typography.labelSmall, color = TextMuted)

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
                        CyberGlassCard(glowColor = if (isAdmin) EmeraldProfit else NeonCyan) {
                            Text(
                                text = if (isAdmin) "Ahxan Admin" else msg.senderName,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isAdmin) EmeraldProfit else NeonCyan
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(msg.text, style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
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
                        modifier = Modifier.weight(1f).testTag("ticket_reply_input")
                    )
                    IconButton(
                        onClick = {
                            if (replyText.isNotBlank()) {
                                onSendReply(ticket.id, replyText)
                                replyText = ""
                            }
                        },
                        modifier = Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)).background(NeonCyan)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = Color.Black)
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
                        CyberGlassCard {
                            Text("No Support Tickets", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
                            Spacer(Modifier.height(4.dp))
                            Text("Report hackers, payment delays, or room issues anytime with screenshot evidence.", style = MaterialTheme.typography.bodyMedium, color = TextMuted)
                        }
                    }
                } else {
                    items(tickets, key = { it.id }) { t ->
                        val statusColor = when (t.status) {
                            "resolved", "closed" -> EmeraldProfit
                            "in_progress" -> NeonCyan
                            else -> AmberWarning
                        }
                        CyberGlassCard(
                            glowColor = if (t.category.contains("Hacker")) CrimsonDanger else statusColor,
                            onClick = {
                                onObserveMessages(t.id)
                                activeTicket = t
                            }
                        ) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                StatusPill(t.category, ElectricViolet)
                                StatusPill(t.status, statusColor)
                            }
                            Spacer(Modifier.height(8.dp))
                            Text(t.subject, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                            if (t.lastMessage.isNotBlank()) {
                                Text(t.lastMessage, style = MaterialTheme.typography.bodyMedium, color = TextSecondary, maxLines = 2)
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

        ModalBottomSheet(onDismissRequest = { showCreateSheet = false }, containerColor = ObsidianSurface) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                Text("Open Support Ticket", style = MaterialTheme.typography.headlineSmall, color = NeonCyan)
                Spacer(Modifier.height(10.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(categories) { cat ->
                        FilterChip(selected = selectedCat == cat, onClick = { selectedCat = cat }, label = { Text(cat) })
                    }
                }
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = subject, onValueChange = { subject = it }, label = { Text("Subject") }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = matchId, onValueChange = { matchId = it }, label = { Text("Match ID (Optional)") }, modifier = Modifier.fillMaxWidth())
                if (selectedCat == "Hacker/Cheater Report") {
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(value = reportedUid, onValueChange = { reportedUid = it }, label = { Text("Suspect Game UID") }, modifier = Modifier.fillMaxWidth())
                }
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = message, onValueChange = { message = it }, label = { Text("Describe the issue") }, minLines = 3, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(10.dp))
                OutlinedButton(
                    onClick = {
                        photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Image, contentDescription = null, tint = NeonCyan)
                    Spacer(Modifier.width(8.dp))
                    Text(if (screenshotBase64.isNotBlank()) "Screenshot Attached (Tap to change)" else "Attach Screenshot (Photo Picker)", color = NeonCyan)
                }
                Spacer(Modifier.height(14.dp))
                NeonPrimaryButton(
                    text = if (busy) "Submitting..." else "Submit Ticket",
                    enabled = !busy,
                    onClick = {
                        onCreateTicket(selectedCat, subject, message, matchId, reportedUid, screenshotBase64) {
                            showCreateSheet = false
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(24.dp))
            }
        }
    }

    if (zoomScreenshotBase64 != null) {
        Dialog(onDismissRequest = { zoomScreenshotBase64 = null }) {
            val bmp = QrCodeUtils.decodeBase64Image(zoomScreenshotBase64!!)
            if (bmp != null) {
                Box(modifier = Modifier.clip(RoundedCornerShape(16.dp)).background(ObsidianElevated).padding(8.dp)) {
                    Image(bitmap = bmp.asImageBitmap(), contentDescription = "Screenshot Preview", modifier = Modifier.fillMaxWidth())
                }
            }
        }
    }
}
