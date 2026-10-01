package com.example.ui.screens.leads

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.Lead
import com.example.data.model.LeadStatus
import com.example.ui.MainViewModel
import com.example.ui.theme.NexaTeal
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CallRemarkDialog(
    lead: Lead,
    viewModel: MainViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var callDurationSeconds by remember { mutableStateOf(180) } // Default 3 mins, adjustable
    var isCallActive by remember { mutableStateOf(false) }

    val outcomes = listOf(
        "Connected - Interested",
        "Connected - Follow-up Requested",
        "Connected - Not Interested",
        "Busy",
        "RNR (Ringing No Response)",
        "Switched Off",
        "Out of Service",
        "Call Not Answered"
    )

    var selectedOutcome by remember { mutableStateOf(outcomes[0]) }
    var remarkText by remember { mutableStateOf("") }

    val notInterestedReasons = listOf(
        "Budget Issue",
        "Location Issue",
        "Project Not Suitable",
        "Requirement Changed",
        "Already Purchased Elsewhere",
        "Not Interested Now",
        "Other"
    )
    var selectedNotInterestedReason by remember { mutableStateOf(notInterestedReasons[0]) }

    val isNotConnected = selectedOutcome in listOf(
        "Busy", "RNR (Ringing No Response)", "Switched Off", "Out of Service", "Call Not Answered"
    )
    val isNotInterested = selectedOutcome == "Connected - Not Interested"

    val todayStr = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()) }
    var scheduleFollowUp by remember { mutableStateOf(true) }
    var followUpDate by remember { mutableStateOf(todayStr) }
    var followUpTime by remember { mutableStateOf("11:30 AM") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Call Remark & Log",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${lead.name} (${lead.mobile})",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Call Duration display & adjustments
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Timer,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Duration: ",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            val minutes = callDurationSeconds / 60
                            val seconds = callDurationSeconds % 60
                            Text(
                                text = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Row {
                            FilledTonalIconButton(
                                onClick = { if (callDurationSeconds >= 30) callDurationSeconds -= 30 },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Text("-", fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            FilledTonalIconButton(
                                onClick = { callDurationSeconds += 30 },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Text("+", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Call Outcome
                Text(
                    text = "Call Outcome *",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(6.dp))

                var outcomeExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = outcomeExpanded,
                    onExpandedChange = { outcomeExpanded = !outcomeExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedOutcome,
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = outcomeExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                            .testTag("call_outcome_dropdown")
                    )
                    ExposedDropdownMenu(
                        expanded = outcomeExpanded,
                        onDismissRequest = { outcomeExpanded = false }
                    ) {
                        outcomes.forEach { outcome ->
                            DropdownMenuItem(
                                text = { Text(outcome) },
                                onClick = {
                                    selectedOutcome = outcome
                                    outcomeExpanded = false
                                    if (outcome.startsWith("Connected - Interested")) {
                                        if (remarkText.isBlank()) {
                                            remarkText = "Customer interested in 3 BHK. Requested brochure and price quote. Follow-up scheduled."
                                        }
                                    }
                                }
                            )
                        }
                    }
                }

                // If Not Connected -> Quick Approved WhatsApp Option
                if (isNotConnected) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = NexaTeal.copy(alpha = 0.12f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Customer Not Connected",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = NexaTeal
                                )
                                Text(
                                    text = "Send approved 'Call Missed' WhatsApp template to keep lead warm.",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    val user = viewModel.currentUser.value
                                    val msg = "Dear ${lead.name},\n\nI just attempted to reach you regarding your property requirement with City Nexa Networks, but could not connect.\n\nPlease let me know a suitable time to speak, or reply with your preferences.\n\nWarm regards,\n${user?.name ?: "City Nexa Sales"}\n📞 ${user?.phone ?: ""}"
                                    viewModel.shareProjectOnWhatsApp(
                                        context = context,
                                        lead = lead,
                                        project = viewModel.allProjects.value.firstOrNull() ?: return@Button,
                                        message = msg,
                                        selectedFiles = listOf("Call Missed WhatsApp Notification")
                                    )
                                },
                                shape = RoundedCornerShape(6.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = NexaTeal),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text("WhatsApp", fontSize = 11.sp)
                            }
                        }
                    }
                }

                // If Not Interested -> Reason picker
                if (isNotInterested) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Reason for Not Interested *",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    var reasonExpanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = reasonExpanded,
                        onExpandedChange = { reasonExpanded = !reasonExpanded }
                    ) {
                        OutlinedTextField(
                            value = selectedNotInterestedReason,
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = reasonExpanded) },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = reasonExpanded,
                            onDismissRequest = { reasonExpanded = false }
                        ) {
                            notInterestedReasons.forEach { reason ->
                                DropdownMenuItem(
                                    text = { Text(reason) },
                                    onClick = {
                                        selectedNotInterestedReason = reason
                                        reasonExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Remarks text
                Text(
                    text = "Sales Executive Remark *",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = remarkText,
                    onValueChange = { remarkText = it },
                    placeholder = { Text("e.g. Customer looking for 3 BHK within ₹1.25 Cr. Interested in project. Brochure requested.") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .testTag("call_remark_input"),
                    maxLines = 4
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Schedule Follow-up toggle
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Checkbox(
                        checked = scheduleFollowUp,
                        onCheckedChange = { scheduleFollowUp = it }
                    )
                    Text(
                        text = "Schedule Next Follow-up",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }

                if (scheduleFollowUp) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = followUpDate,
                            onValueChange = { followUpDate = it },
                            label = { Text("Date (YYYY-MM-DD)") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = followUpTime,
                            onValueChange = { followUpTime = it },
                            label = { Text("Time") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Submit button
                Button(
                    onClick = {
                        val finalRemark = if (isNotInterested) {
                            "Reason: $selectedNotInterestedReason. $remarkText".trim()
                        } else {
                            if (remarkText.isBlank()) "Call completed ($selectedOutcome)" else remarkText
                        }

                        viewModel.submitCallRemark(
                            leadId = lead.id,
                            durationSeconds = callDurationSeconds,
                            outcome = selectedOutcome,
                            remark = finalRemark,
                            nextFollowUpDate = if (scheduleFollowUp) followUpDate else null,
                            nextFollowUpTime = if (scheduleFollowUp) followUpTime else null
                        )

                        if (isNotInterested) {
                            viewModel.updateLeadStatus(lead, LeadStatus.CLOSED_LOST)
                        } else if (lead.status == LeadStatus.NEW_LEAD || lead.status == LeadStatus.ASSIGNED) {
                            viewModel.updateLeadStatus(lead, LeadStatus.CONTACTED)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("submit_call_remark_button"),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Save Remark & Update CRM")
                }
            }
        }
    }
}
