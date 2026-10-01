package com.example.ui.screens.leads

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.*
import com.example.ui.MainViewModel
import com.example.ui.components.EmptyStateView
import com.example.ui.components.LeadPriorityBadge
import com.example.ui.components.LeadStatusBadge
import com.example.ui.components.TimelineItem
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeadDetailScreen(
    leadId: String,
    viewModel: MainViewModel,
    onBack: () -> Unit,
    onOpenPropertyMatch: (Lead) -> Unit,
    onOpenWhatsAppShare: (Lead, Project) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val leads by viewModel.visibleLeads.collectAsState()
    val lead = leads.firstOrNull { it.id == leadId }

    val allProjects by viewModel.allProjects.collectAsState()
    val activities by viewModel.repository.getActivitiesForLead(leadId).collectAsState(initial = emptyList())

    var showFollowUpDialog by remember { mutableStateOf(false) }
    var showSiteVisitDialog by remember { mutableStateOf(false) }
    var showAddRemarkDialog by remember { mutableStateOf(false) }
    var showEmailDialog by remember { mutableStateOf(false) }

    if (lead == null) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Lead not found or unauthorized access.")
                Spacer(modifier = Modifier.height(12.dp))
                Button(onClick = onBack) { Text("Go Back") }
            }
        }
        return
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(lead.name, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text("#${lead.id} • ${lead.source}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("lead_detail_back")) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    LeadPriorityBadge(priority = lead.priority)
                    Spacer(modifier = Modifier.width(8.dp))
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Pipeline Status Stepper
            item {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Pipeline Status",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            LeadStatusBadge(status = lead.status)
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Horizontal status selector
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            LeadStatus.values().forEach { st ->
                                val isCurrent = lead.status == st
                                FilterChip(
                                    selected = isCurrent,
                                    onClick = {
                                        if (!isCurrent) {
                                            viewModel.updateLeadStatus(lead, st)
                                        }
                                    },
                                    label = { Text(st.label, fontSize = 11.sp) }
                                )
                            }
                        }
                    }
                }
            }

            // 2. Action Buttons Row (CALL, WHATSAPP, EMAIL, PROJECT DETAILS, FOLLOW-UP, SITE VISIT, ADD REMARK)
            item {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "Customer Actions",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { viewModel.initiateCall(context, lead) },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("detail_call_button"),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("CALL", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = {
                                    val defaultProj = allProjects.firstOrNull() ?: return@Button
                                    onOpenWhatsAppShare(lead, defaultProj)
                                },
                                modifier = Modifier
                                    .weight(1.2f)
                                    .testTag("detail_whatsapp_button"),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = NexaTeal)
                            ) {
                                Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("WHATSAPP", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            FilledTonalButton(
                                onClick = { showEmailDialog = true },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("detail_email_button"),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.Email, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("EMAIL", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { onOpenPropertyMatch(lead) },
                                modifier = Modifier
                                    .weight(1.2f)
                                    .testTag("detail_match_projects_button"),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = NexaGoldDark, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("MATCH PROJECTS", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }

                            OutlinedButton(
                                onClick = { showFollowUpDialog = true },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("detail_followup_button"),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.Event, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("FOLLOW-UP", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }

                            OutlinedButton(
                                onClick = { showSiteVisitDialog = true },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("detail_site_visit_button"),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.Tour, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("SITE VISIT", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // 3. Customer & Requirement Overview Card
            item {
                ElevatedCard(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Customer Profile & Requirements",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        DetailRow(label = "Mobile Number", value = lead.mobile, isConfidential = true)
                        DetailRow(label = "WhatsApp", value = lead.whatsapp, isConfidential = true)
                        DetailRow(label = "Email Address", value = lead.email.ifBlank { "Not provided" })
                        DetailRow(label = "Budget Range", value = "₹${lead.budgetMin} Lakhs — ₹${lead.budgetMax} Lakhs")
                        DetailRow(label = "Location Preference", value = lead.preferredLocation)
                        DetailRow(label = "Property Type", value = lead.propertyType.label)
                        DetailRow(label = "Configuration", value = "${lead.bhk} (${lead.sizeRequirementSqFt})")
                        DetailRow(label = "Purpose of Purchase", value = lead.purpose)
                        if (lead.requirementNotes.isNotBlank()) {
                            DetailRow(label = "Requirement Notes", value = lead.requirementNotes)
                        }
                    }
                }
            }

            // 4. Activity Timeline
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Activity Timeline (${activities.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    TextButton(onClick = { showAddRemarkDialog = true }) {
                        Icon(Icons.Default.AddComment, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Remark", fontSize = 12.sp)
                    }
                }
            }

            if (activities.isEmpty()) {
                item {
                    Text(
                        text = "No recorded interactions yet. Make a call or add a remark to start tracking.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                items(activities) { act ->
                    TimelineItem(activity = act)
                }
            }

            item {
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }

    // Follow-up Dialog
    if (showFollowUpDialog) {
        var fDate by remember { mutableStateOf(SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())) }
        var fTime by remember { mutableStateOf("11:30 AM") }
        var fType by remember { mutableStateOf(FollowUpType.CALL) }
        var fNotes by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showFollowUpDialog = false },
            title = { Text("Schedule Follow-up") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Type", style = MaterialTheme.typography.labelSmall)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FollowUpType.values().forEach { t ->
                            FilterChip(
                                selected = fType == t,
                                onClick = { fType = t },
                                label = { Text(t.label, fontSize = 11.sp) }
                            )
                        }
                    }
                    OutlinedTextField(value = fDate, onValueChange = { fDate = it }, label = { Text("Date (YYYY-MM-DD)") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = fTime, onValueChange = { fTime = it }, label = { Text("Time") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = fNotes, onValueChange = { fNotes = it }, label = { Text("Notes") }, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val user = viewModel.currentUser.value ?: return@Button
                        viewModel.scheduleFollowUp(
                            FollowUp(
                                leadId = lead.id,
                                leadName = lead.name,
                                customerPhone = lead.mobile,
                                projectId = lead.interestedProjectId,
                                salespersonId = user.id,
                                salespersonName = user.name,
                                dueDate = fDate,
                                dueTime = fTime,
                                type = fType,
                                notes = fNotes
                            )
                        )
                        showFollowUpDialog = false
                    }
                ) {
                    Text("Schedule")
                }
            },
            dismissButton = {
                TextButton(onClick = { showFollowUpDialog = false }) { Text("Cancel") }
            }
        )
    }

    // Site Visit Dialog
    if (showSiteVisitDialog) {
        var vDate by remember { mutableStateOf(SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())) }
        var vTime by remember { mutableStateOf("03:30 PM") }
        var vVisitors by remember { mutableStateOf("2") }
        var vProject by remember { mutableStateOf(allProjects.firstOrNull()) }
        var vRemarks by remember { mutableStateOf("") }
        var vCab by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showSiteVisitDialog = false },
            title = { Text("Plan Site Visit") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Select Project:", style = MaterialTheme.typography.labelSmall)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        allProjects.forEach { p ->
                            FilterChip(
                                selected = vProject?.id == p.id,
                                onClick = { vProject = p },
                                label = { Text(p.name, fontSize = 11.sp) }
                            )
                        }
                    }
                    OutlinedTextField(value = vDate, onValueChange = { vDate = it }, label = { Text("Date (YYYY-MM-DD)") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = vTime, onValueChange = { vTime = it }, label = { Text("Time") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = vVisitors, onValueChange = { vVisitors = it }, label = { Text("Number of Visitors") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = vRemarks, onValueChange = { vRemarks = it }, label = { Text("Special Instructions / Notes") }, modifier = Modifier.fillMaxWidth())
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = vCab, onCheckedChange = { vCab = it })
                        Text("Chauffeur Pick & Drop Requested", fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val user = viewModel.currentUser.value ?: return@Button
                        val proj = vProject ?: allProjects.firstOrNull() ?: return@Button
                        viewModel.scheduleSiteVisit(
                            SiteVisit(
                                leadId = lead.id,
                                leadName = lead.name,
                                customerPhone = lead.mobile,
                                projectId = proj.id,
                                projectName = proj.name,
                                visitDate = vDate,
                                visitTime = vTime,
                                salespersonId = user.id,
                                salespersonName = user.name,
                                numberOfVisitors = vVisitors.toIntOrNull() ?: 2,
                                remarks = vRemarks,
                                cabRequired = vCab
                            )
                        )
                        showSiteVisitDialog = false
                    }
                ) {
                    Text("Book Visit")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSiteVisitDialog = false }) { Text("Cancel") }
            }
        )
    }

    // Add Remark Dialog
    if (showAddRemarkDialog) {
        var rText by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddRemarkDialog = false },
            title = { Text("Add Sales Remark") },
            text = {
                OutlinedTextField(
                    value = rText,
                    onValueChange = { rText = it },
                    placeholder = { Text("Enter discussion details, customer reaction or next steps...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (rText.isNotBlank()) {
                            viewModel.addRemarkToLead(lead, rText)
                            showAddRemarkDialog = false
                        }
                    }
                ) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddRemarkDialog = false }) { Text("Cancel") }
            }
        )
    }

    // Email Dialog
    if (showEmailDialog) {
        var emailSubject by remember { mutableStateOf("City Nexa Networks: Property Information & Pricing") }
        var emailBody by remember {
            mutableStateOf(
                "Dear ${lead.name},\n\nThank you for connecting with City Nexa Networks regarding your real estate inquiry in ${lead.preferredLocation}.\n\nPlease find the project specifications, brochures, and payment terms attached.\n\nWarm regards,\n${viewModel.currentUser.value?.name ?: "Sales Executive"}\nCity Nexa Networks"
            )
        }

        AlertDialog(
            onDismissRequest = { showEmailDialog = false },
            title = { Text("Compose Customer Email") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("To: ${lead.email.ifBlank { "customer@example.com" }}", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    OutlinedTextField(value = emailSubject, onValueChange = { emailSubject = it }, label = { Text("Subject") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = emailBody, onValueChange = { emailBody = it }, label = { Text("Message Body") }, modifier = Modifier.fillMaxWidth().height(120.dp))
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.shareViaEmail(
                            context = context,
                            lead = lead,
                            subject = emailSubject,
                            body = emailBody,
                            attachments = listOf("Brochure_Deck.pdf", "Cost_Sheet.pdf")
                        )
                        showEmailDialog = false
                    }
                ) {
                    Text("Send Email")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEmailDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun DetailRow(label: String, value: String, isConfidential: Boolean = false) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

