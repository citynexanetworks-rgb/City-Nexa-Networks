package com.example.ui.screens.reports

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FileDownload
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
import com.example.data.model.LeadStatus
import com.example.ui.MainViewModel
import com.example.ui.screens.dashboard.FunnelBar
import com.example.ui.theme.NexaGoldDark
import com.example.ui.theme.NexaNavyMedium
import com.example.ui.theme.NexaTeal
import com.example.ui.theme.StatusBooking
import com.example.ui.theme.StatusNew
import com.example.ui.theme.StatusSiteVisit
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val leads by viewModel.visibleLeads.collectAsState()
    val bookings by viewModel.visibleBookings.collectAsState()
    val siteVisits by viewModel.visibleSiteVisits.collectAsState()
    val allUsers by viewModel.allUsers.collectAsState()

    val totalLeads = leads.size.coerceAtLeast(1)
    val totalBookings = bookings.size
    val conversionRate = (totalBookings.toFloat() / totalLeads) * 100

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Executive Reports & BI", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text("Sales funnel, conversion analytics & ROI", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            val file = viewModel.exportLeadsCsv(context, leads)
                            viewModel.shareExportedFile(context, file)
                        },
                        modifier = Modifier.testTag("export_report_button")
                    ) {
                        Icon(Icons.Default.FileDownload, contentDescription = "Export Report", tint = MaterialTheme.colorScheme.primary)
                    }
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
            // Conversion Highlights
            item {
                ElevatedCard(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Overall Sales Conversion", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text("Lead to Deal Ratio", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("${String.format(Locale.getDefault(), "%.1f", conversionRate)}%", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Color(0xFF059669))
                            }
                            Column {
                                Text("Total Site Visits", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("${siteVisits.size}", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = StatusSiteVisit)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Deals Closed", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("$totalBookings", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }

            // Lead Source Analytics Table
            item {
                ElevatedCard(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Marketing Channel Performance", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(12.dp))

                        val sources = leads.groupBy { it.source }
                        sources.forEach { (src, list) ->
                            val bookingsFromSrc = list.count { it.status == LeadStatus.BOOKING }
                            val conv = if (list.isNotEmpty()) (bookingsFromSrc.toFloat() / list.size) * 100 else 0f

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(src, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                    Text("${list.size} Leads generated", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Text(
                                    text = "$bookingsFromSrc Deals (${String.format(Locale.getDefault(), "%.0f", conv)}%)",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Divider(color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.padding(vertical = 4.dp))
                        }
                    }
                }
            }

            // Sales Advisor Performance
            item {
                ElevatedCard(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Salesperson Performance & Velocity", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(12.dp))

                        val execs = allUsers.filter { it.role.name.contains("EXECUTIVE") || it.role.name.contains("MANAGER") }
                        execs.forEach { user ->
                            val userLeads = leads.filter { it.assignedSalespersonId == user.id }
                            val userBookings = bookings.filter { it.salespersonId == user.id }
                            val userComm = userBookings.sumOf { it.commissionAmount }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(user.name, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text("${userLeads.size} Active Leads • ${userBookings.size} Deals", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Text(
                                    text = "₹${String.format(Locale.getDefault(), "%.2f", userComm)}L Comm",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NexaGoldDark
                                )
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(48.dp))
            }
        }
    }
}
