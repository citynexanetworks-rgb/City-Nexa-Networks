package com.example.ui.screens.followups

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Event
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
import com.example.data.model.FollowUp
import com.example.data.model.FollowUpStatus
import com.example.data.model.FollowUpType
import com.example.data.model.Lead
import com.example.ui.MainViewModel
import com.example.ui.components.EmptyStateView
import com.example.ui.theme.NexaGoldDark
import com.example.ui.theme.NexaTeal
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FollowUpScreen(
    viewModel: MainViewModel,
    onLeadSelected: (Lead) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val followUps by viewModel.visibleFollowUps.collectAsState()
    val allLeads by viewModel.visibleLeads.collectAsState()

    var selectedTab by remember { mutableStateOf("TODAY") }
    val tabs = listOf("TODAY", "TOMORROW", "UPCOMING", "OVERDUE")

    val todayStr = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()) }
    val tomorrowStr = remember {
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(System.currentTimeMillis() + 86400000L))
    }

    val filteredFollowUps = remember(followUps, selectedTab) {
        when (selectedTab) {
            "TODAY" -> followUps.filter { it.dueDate == todayStr && it.status == FollowUpStatus.PENDING }
            "TOMORROW" -> followUps.filter { it.dueDate == tomorrowStr && it.status == FollowUpStatus.PENDING }
            "UPCOMING" -> followUps.filter { it.dueDate > tomorrowStr && it.status == FollowUpStatus.PENDING }
            "OVERDUE" -> followUps.filter { it.dueDate < todayStr && it.status == FollowUpStatus.PENDING }
            else -> followUps
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize()
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Sales Follow-ups",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Real-time task reminders and customer commitments",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Tabs: TODAY, TOMORROW, UPCOMING, OVERDUE
            TabRow(
                selectedTabIndex = tabs.indexOf(selectedTab),
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                tabs.forEach { tab ->
                    val count = when (tab) {
                        "TODAY" -> followUps.count { it.dueDate == todayStr && it.status == FollowUpStatus.PENDING }
                        "TOMORROW" -> followUps.count { it.dueDate == tomorrowStr && it.status == FollowUpStatus.PENDING }
                        "UPCOMING" -> followUps.count { it.dueDate > tomorrowStr && it.status == FollowUpStatus.PENDING }
                        "OVERDUE" -> followUps.count { it.dueDate < todayStr && it.status == FollowUpStatus.PENDING }
                        else -> 0
                    }
                    val isOverdue = tab == "OVERDUE" && count > 0

                    Tab(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(tab, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                if (count > 0) {
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Surface(
                                        color = if (isOverdue) Color(0xFFEF4444) else MaterialTheme.colorScheme.primaryContainer,
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Text(
                                            text = "$count",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isOverdue) Color.White else MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                        )
                                    }
                                }
                            }
                        }
                    )
                }
            }

            // List of Follow-ups
            if (filteredFollowUps.isEmpty()) {
                EmptyStateView(
                    title = "No $selectedTab Follow-ups",
                    message = "All scheduled client follow-ups in this stage are completed.",
                    icon = Icons.Outlined.Event
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredFollowUps, key = { it.id }) { item ->
                        FollowUpCard(
                            followUp = item,
                            onComplete = { viewModel.completeFollowUp(item.id) },
                            onCall = {
                                val lead = allLeads.firstOrNull { it.id == item.leadId }
                                if (lead != null) {
                                    viewModel.initiateCall(context, lead)
                                }
                            },
                            onOpenLead = {
                                val lead = allLeads.firstOrNull { it.id == item.leadId }
                                if (lead != null) onLeadSelected(lead)
                            }
                        )
                    }
                    item {
                        Spacer(modifier = Modifier.height(72.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun FollowUpCard(
    followUp: FollowUp,
    onComplete: () -> Unit,
    onCall: () -> Unit,
    onOpenLead: () -> Unit
) {
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = when (followUp.type) {
                            FollowUpType.CALL -> Icons.Default.Call
                            FollowUpType.WHATSAPP -> Icons.Default.Send
                            FollowUpType.SITE_VISIT -> Icons.Default.Tour
                            FollowUpType.MEETING -> Icons.Default.Groups
                            FollowUpType.NEGOTIATION -> Icons.Default.Handshake
                            FollowUpType.PAYMENT, FollowUpType.BOOKING -> Icons.Default.CheckCircle
                        },
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = followUp.type.label,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = "${followUp.dueDate} • ${followUp.dueTime}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = followUp.leadName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.clickable { onOpenLead() }
            )

            if (!followUp.projectName.isNullOrBlank()) {
                Text(
                    text = "Project: ${followUp.projectName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (followUp.notes.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Note: ${followUp.notes}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
            Divider(color = MaterialTheme.colorScheme.surfaceVariant)
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilledTonalButton(
                    onClick = onCall,
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("CALL", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = onComplete,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("MARK DONE", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
