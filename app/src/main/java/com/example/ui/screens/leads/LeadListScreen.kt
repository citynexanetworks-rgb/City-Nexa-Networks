package com.example.ui.screens.leads

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Lead
import com.example.data.model.LeadPriority
import com.example.data.model.LeadStatus
import com.example.ui.MainViewModel
import com.example.ui.components.EmptyStateView
import com.example.ui.components.LeadPriorityBadge
import com.example.ui.components.LeadStatusBadge
import com.example.ui.theme.NexaGoldDark
import com.example.ui.theme.NexaNavyMedium
import com.example.ui.theme.NexaTeal

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeadListScreen(
    viewModel: MainViewModel,
    onLeadSelected: (Lead) -> Unit,
    onOpenPropertyMatch: (Lead) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val leads by viewModel.visibleLeads.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val searchQuery by viewModel.leadSearchQuery.collectAsState()
    val selectedStatus by viewModel.selectedStatusFilter.collectAsState()
    val selectedPriority by viewModel.selectedPriorityFilter.collectAsState()

    var showAddLeadDialog by remember { mutableStateOf(false) }
    var showBulkImportDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddLeadDialog = true },
                icon = { Icon(Icons.Default.PersonAdd, contentDescription = null) },
                text = { Text("Add Lead") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("add_lead_fab")
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Top action bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Lead CRM Pipeline",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${leads.size} leads managed",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row {
                    if (viewModel.authManager.canBulkImport()) {
                        IconButton(
                            onClick = { showBulkImportDialog = true },
                            modifier = Modifier.testTag("bulk_import_icon_button")
                        ) {
                            Icon(Icons.Default.UploadFile, contentDescription = "Bulk Import", tint = MaterialTheme.colorScheme.primary)
                        }
                    }

                    if (viewModel.authManager.canExportReports()) {
                        IconButton(
                            onClick = {
                                val file = viewModel.exportLeadsCsv(context, leads)
                                viewModel.shareExportedFile(context, file)
                            },
                            modifier = Modifier.testTag("export_csv_icon_button")
                        ) {
                            Icon(Icons.Default.FileDownload, contentDescription = "Export Excel", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.setLeadSearchQuery(it) },
                placeholder = { Text("Search by name, phone, lead ID, location...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.setLeadSearchQuery("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear search")
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .testTag("leads_search_field"),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            // Pipeline Status Filter Horizontal Scroll
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FilterChip(
                    selected = selectedStatus == null,
                    onClick = { viewModel.setStatusFilter(null) },
                    label = { Text("All Statuses (${leads.size})", fontSize = 12.sp) }
                )
                LeadStatus.values().forEach { st ->
                    FilterChip(
                        selected = selectedStatus == st,
                        onClick = {
                            viewModel.setStatusFilter(if (selectedStatus == st) null else st)
                        },
                        label = { Text(st.label, fontSize = 12.sp) }
                    )
                }
            }

            // Priority Filter Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Priority:",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                FilterChip(
                    selected = selectedPriority == null,
                    onClick = { viewModel.setPriorityFilter(null) },
                    label = { Text("All", fontSize = 11.sp) }
                )
                LeadPriority.values().forEach { p ->
                    FilterChip(
                        selected = selectedPriority == p,
                        onClick = {
                            viewModel.setPriorityFilter(if (selectedPriority == p) null else p)
                        },
                        label = { Text(p.name, fontSize = 11.sp) }
                    )
                }
            }

            // Leads List
            if (leads.isEmpty()) {
                EmptyStateView(
                    title = "No leads found",
                    message = if (searchQuery.isNotEmpty()) "Try adjusting your search criteria" else "No leads assigned in this pipeline stage.",
                    icon = Icons.Outlined.People,
                    actionText = "Create New Lead",
                    onAction = { showAddLeadDialog = true }
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(leads, key = { it.id }) { lead ->
                        LeadCard(
                            lead = lead,
                            onClick = { onLeadSelected(lead) },
                            onCall = { viewModel.initiateCall(context, lead) },
                            onWhatsApp = {
                                val user = viewModel.currentUser.value
                                val msg = "Hello ${lead.name}, greetings from City Nexa Networks! Following up on your requirement in ${lead.preferredLocation}."
                                val defaultProj = viewModel.allProjects.value.firstOrNull()
                                if (defaultProj != null) {
                                    viewModel.shareProjectOnWhatsApp(
                                        context = context,
                                        lead = lead,
                                        project = defaultProj,
                                        message = msg,
                                        selectedFiles = listOf("Direct Follow-up")
                                    )
                                }
                            },
                            onMatch = { onOpenPropertyMatch(lead) }
                        )
                    }
                    item {
                        Spacer(modifier = Modifier.height(72.dp))
                    }
                }
            }
        }
    }

    if (showAddLeadDialog) {
        AddEditLeadDialog(
            viewModel = viewModel,
            onDismiss = { showAddLeadDialog = false }
        )
    }

    if (showBulkImportDialog) {
        BulkImportDialog(
            viewModel = viewModel,
            onDismiss = { showBulkImportDialog = false }
        )
    }
}

@Composable
fun LeadCard(
    lead: Lead,
    onClick: () -> Unit,
    onCall: () -> Unit,
    onWhatsApp: () -> Unit,
    onMatch: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("lead_card_${lead.id}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header: ID, Source, Priority, Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "#${lead.id}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = lead.source,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    LeadPriorityBadge(priority = lead.priority)
                    LeadStatusBadge(status = lead.status)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Name
            Text(
                text = lead.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            // Budget, Location, BHK
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CurrencyRupee, contentDescription = null, modifier = Modifier.size(14.dp), tint = NexaGoldDark)
                    Text(
                        text = "₹${lead.budgetMin.toInt()}L - ₹${lead.budgetMax.toInt()}L",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Place, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = lead.preferredLocation,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Surface(
                    color = NexaNavyMedium.copy(alpha = 0.08f),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = lead.bhk,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium,
                        color = NexaNavyMedium,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                    )
                }
            }

            // Last remark snippet
            if (lead.lastRemark.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Remark: ${lead.lastRemark}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
            Divider(color = MaterialTheme.colorScheme.surfaceVariant)
            Spacer(modifier = Modifier.height(8.dp))

            // Action Buttons: Quick Call, WhatsApp, Match, Details
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilledTonalButton(
                        onClick = onCall,
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("call_lead_${lead.id}")
                    ) {
                        Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("CALL", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = onWhatsApp,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = NexaTeal),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("whatsapp_lead_${lead.id}")
                    ) {
                        Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("WHATSAPP", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(onClick = onMatch) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = "Match Projects", tint = NexaGoldDark)
                    }
                    IconButton(onClick = onClick) {
                        Icon(Icons.Default.ChevronRight, contentDescription = "View Details")
                    }
                }
            }
        }
    }
}
