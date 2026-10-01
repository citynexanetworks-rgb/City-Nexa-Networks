package com.example.ui.screens.sitevisits

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.example.data.model.SiteVisit
import com.example.data.model.SiteVisitStatus
import com.example.ui.MainViewModel
import com.example.ui.theme.NexaGoldDark
import com.example.ui.theme.StatusSiteVisit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SiteVisitScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val siteVisits by viewModel.visibleSiteVisits.collectAsState()
    var selectedVisitForEdit by remember { mutableStateOf<SiteVisit?>(null) }

    Scaffold(
        modifier = modifier.fillMaxSize()
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Site Visits & Tour Management",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${siteVisits.size} customer tours scheduled",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(siteVisits, key = { it.id }) { visit ->
                    SiteVisitCard(
                        visit = visit,
                        onUpdateStatus = { selectedVisitForEdit = visit }
                    )
                }
                item {
                    Spacer(modifier = Modifier.height(72.dp))
                }
            }
        }
    }

    if (selectedVisitForEdit != null) {
        val currentVisit = selectedVisitForEdit!!
        var newStatus by remember { mutableStateOf(currentVisit.status) }
        var remarks by remember { mutableStateOf(currentVisit.remarks) }
        var nextAction by remember { mutableStateOf(currentVisit.nextAction) }

        AlertDialog(
            onDismissRequest = { selectedVisitForEdit = null },
            title = { Text("Update Site Visit Status") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("${currentVisit.leadName} • ${currentVisit.projectName}", fontWeight = FontWeight.Bold)

                    Text("Visit Status:", style = MaterialTheme.typography.labelSmall)
                    SiteVisitStatus.values().forEach { st ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { newStatus = st }
                        ) {
                            RadioButton(selected = newStatus == st, onClick = { newStatus = st })
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(st.label, fontSize = 13.sp)
                        }
                    }

                    OutlinedTextField(
                        value = remarks,
                        onValueChange = { remarks = it },
                        label = { Text("Tour Remarks / Customer Feedback") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = nextAction,
                        onValueChange = { nextAction = it },
                        label = { Text("Next Action (e.g. Send offer, Negotiation)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updateSiteVisit(
                            currentVisit.copy(
                                status = newStatus,
                                remarks = remarks,
                                nextAction = nextAction
                            )
                        )
                        selectedVisitForEdit = null
                    }
                ) {
                    Text("Save Update")
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedVisitForEdit = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun SiteVisitCard(
    visit: SiteVisit,
    onUpdateStatus: () -> Unit
) {
    val statusColor = when (visit.status) {
        SiteVisitStatus.PLANNED -> StatusSiteVisit
        SiteVisitStatus.CONFIRMED -> Color(0xFF2563EB)
        SiteVisitStatus.COMPLETED -> Color(0xFF059669)
        SiteVisitStatus.RESCHEDULED -> NexaGoldDark
        SiteVisitStatus.CANCELLED, SiteVisitStatus.NO_SHOW -> Color(0xFFDC2626)
    }

    ElevatedCard(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = statusColor.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = visit.status.label,
                        color = statusColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                Text(
                    text = "${visit.visitDate} • ${visit.visitTime}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = visit.leadName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = "Project: ${visit.projectName}  (${visit.numberOfVisitors} Visitors)",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )

            if (visit.cabRequired) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.DirectionsCar, contentDescription = null, tint = NexaGoldDark, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Chauffeur Pick & Drop Requested", fontSize = 11.sp, color = NexaGoldDark, fontWeight = FontWeight.SemiBold)
                }
            }

            if (visit.remarks.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Remarks: ${visit.remarks}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Divider(color = MaterialTheme.colorScheme.surfaceVariant)
            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Advisor: ${visit.salespersonName}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                TextButton(
                    onClick = onUpdateStatus,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text("Update Status", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }
    }
}
