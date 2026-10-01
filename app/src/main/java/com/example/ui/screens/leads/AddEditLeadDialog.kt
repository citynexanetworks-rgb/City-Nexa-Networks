package com.example.ui.screens.leads

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.*
import com.example.ui.MainViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditLeadDialog(
    viewModel: MainViewModel,
    existingLead: Lead? = null,
    onDismiss: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val currentUser by viewModel.currentUser.collectAsState()
    val allUsers by viewModel.allUsers.collectAsState()
    val allProjects by viewModel.allProjects.collectAsState()

    var name by remember { mutableStateOf(existingLead?.name ?: "") }
    var mobile by remember { mutableStateOf(existingLead?.mobile ?: "") }
    var whatsapp by remember { mutableStateOf(existingLead?.whatsapp ?: "") }
    var email by remember { mutableStateOf(existingLead?.email ?: "") }
    var location by remember { mutableStateOf(existingLead?.preferredLocation ?: "Whitefield / Hoskote") }
    var budgetMin by remember { mutableStateOf(existingLead?.budgetMin?.toString() ?: "85") }
    var budgetMax by remember { mutableStateOf(existingLead?.budgetMax?.toString() ?: "150") }
    var bhk by remember { mutableStateOf(existingLead?.bhk ?: "3 BHK") }
    var propertyType by remember { mutableStateOf(existingLead?.propertyType ?: PropertyType.APARTMENT) }
    var purpose by remember { mutableStateOf(existingLead?.purpose ?: "End Use") }
    var source by remember { mutableStateOf(existingLead?.source ?: "Meta Ads") }
    var priority by remember { mutableStateOf(existingLead?.priority ?: LeadPriority.WARM) }
    var status by remember { mutableStateOf(existingLead?.status ?: LeadStatus.NEW_LEAD) }
    var selectedProjectId by remember { mutableStateOf(existingLead?.interestedProjectId ?: allProjects.firstOrNull()?.id) }
    var assignedUserId by remember { mutableStateOf(existingLead?.assignedSalespersonId ?: currentUser?.id ?: "USR-004") }
    var notes by remember { mutableStateOf(existingLead?.requirementNotes ?: "") }

    // Duplicate detection state
    var duplicateFoundLead by remember { mutableStateOf<Lead?>(null) }

    LaunchedEffect(mobile, email) {
        if (mobile.length >= 10 || email.contains("@")) {
            val dup = viewModel.repository.findDuplicateLead(mobile.trim(), email.trim())
            if (dup != null && dup.id != existingLead?.id) {
                duplicateFoundLead = dup
            } else {
                duplicateFoundLead = null
            }
        } else {
            duplicateFoundLead = null
        }
    }

    val sources = listOf("Meta Ads", "Google Ads", "99acres", "MagicBricks", "Housing", "Website", "Referral", "Walk-in", "Calling", "Other")
    val bhkOptions = listOf("1 BHK", "2 BHK", "3 BHK", "4 BHK", "Villa / Penthouse", "Plot")

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
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
                    Text(
                        text = if (existingLead == null) "Add New Real Estate Lead" else "Edit Lead Details",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                // Duplicate Warning Banner
                if (duplicateFoundLead != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        color = Color(0xFFFEF2F2),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFDC2626), modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Possible duplicate lead found!",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = Color(0xFFDC2626)
                                )
                                Text(
                                    text = "Lead #${duplicateFoundLead!!.id} (${duplicateFoundLead!!.name}) already exists with this phone/email.",
                                    fontSize = 11.sp,
                                    color = Color(0xFF7F1D1D)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Name
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Customer Full Name *") },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("lead_name_input")
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Mobile
                OutlinedTextField(
                    value = mobile,
                    onValueChange = {
                        mobile = it
                        if (whatsapp.isBlank()) whatsapp = it
                    },
                    label = { Text("Mobile Number *") },
                    leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("lead_mobile_input")
                )

                Spacer(modifier = Modifier.height(8.dp))

                // WhatsApp & Email
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = whatsapp,
                        onValueChange = { whatsapp = it },
                        label = { Text("WhatsApp") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email") },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Location & BHK
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = location,
                        onValueChange = { location = it },
                        label = { Text("Preferred Location") },
                        modifier = Modifier.weight(1.2f)
                    )
                    OutlinedTextField(
                        value = bhk,
                        onValueChange = { bhk = it },
                        label = { Text("BHK") },
                        modifier = Modifier.weight(0.8f)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Budget Range (in Lakhs)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = budgetMin,
                        onValueChange = { budgetMin = it },
                        label = { Text("Min Budget (₹ Lakhs)") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = budgetMax,
                        onValueChange = { budgetMax = it },
                        label = { Text("Max Budget (₹ Lakhs)") },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Property Type Chips
                Text("Property Type", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    PropertyType.values().forEach { pt ->
                        FilterChip(
                            selected = propertyType == pt,
                            onClick = { propertyType = pt },
                            label = { Text(pt.label, fontSize = 11.sp) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Priority & Purpose
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Priority", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                        Row(modifier = Modifier.padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            LeadPriority.values().forEach { p ->
                                FilterChip(
                                    selected = priority == p,
                                    onClick = { priority = p },
                                    label = { Text(p.name, fontSize = 10.sp) }
                                )
                            }
                        }
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text("Purpose", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                        Row(modifier = Modifier.padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            listOf("End Use", "Investment").forEach { purp ->
                                FilterChip(
                                    selected = purpose == purp,
                                    onClick = { purpose = purp },
                                    label = { Text(purp, fontSize = 10.sp) }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Lead Source & Assignee
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = source,
                        onValueChange = { source = it },
                        label = { Text("Lead Source") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Requirement Notes") },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Submit button
                Button(
                    onClick = {
                        if (name.isBlank() || mobile.isBlank()) return@Button

                        val leadId = existingLead?.id ?: "NX-${(1000..9999).random()}"
                        val bMin = budgetMin.toDoubleOrNull() ?: 75.0
                        val bMax = budgetMax.toDoubleOrNull() ?: 150.0

                        val newLead = Lead(
                            id = leadId,
                            name = name.trim(),
                            mobile = mobile.trim(),
                            whatsapp = if (whatsapp.isBlank()) mobile.trim() else whatsapp.trim(),
                            email = email.trim(),
                            source = source,
                            interestedProjectId = selectedProjectId,
                            propertyType = propertyType,
                            budgetMin = bMin,
                            budgetMax = bMax,
                            preferredLocation = location.trim(),
                            bhk = bhk,
                            purpose = purpose,
                            requirementNotes = notes.trim(),
                            status = status,
                            priority = priority,
                            assignedSalespersonId = assignedUserId,
                            salesManagerId = currentUser?.managerId ?: "USR-003",
                            lastRemark = if (existingLead == null) "Lead entered into CRM" else existingLead.lastRemark
                        )

                        if (existingLead == null) {
                            viewModel.createLead(newLead) { onDismiss() }
                        } else {
                            viewModel.updateLead(newLead)
                            onDismiss()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("save_lead_button"),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(if (existingLead == null) "Create Lead" else "Save Changes")
                }
            }
        }
    }
}
