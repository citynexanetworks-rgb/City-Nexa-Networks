package com.example.ui.screens.dashboard

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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.*
import com.example.ui.MainViewModel
import com.example.ui.components.KpiCard
import com.example.ui.components.SectionHeader
import com.example.ui.components.TimelineItem
import com.example.ui.components.UserRoleBadge
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: MainViewModel,
    onNavigateToLeads: () -> Unit,
    onNavigateToFollowUps: () -> Unit,
    onNavigateToProjects: () -> Unit,
    onNavigateToSiteVisits: () -> Unit,
    onNavigateToBookings: () -> Unit,
    onOpenAccountSwitcher: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentUser by viewModel.currentUser.collectAsState()
    val leads by viewModel.visibleLeads.collectAsState()
    val followUps by viewModel.visibleFollowUps.collectAsState()
    val siteVisits by viewModel.visibleSiteVisits.collectAsState()
    val bookings by viewModel.visibleBookings.collectAsState()
    val recentActivities by viewModel.repository.getAllActivities().collectAsState(initial = emptyList())

    val todayStr = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()) }
    var selectedDateFilter by remember { mutableStateOf("This Month") }
    val dateFilters = listOf("Today", "Yesterday", "This Week", "This Month", "All")

    // Metrics calculations
    val totalLeads = leads.size
    val newLeads = leads.count { it.status == LeadStatus.NEW_LEAD }
    val contactedLeads = leads.count { it.status == LeadStatus.CONTACTED }
    val qualifiedLeads = leads.count { it.status == LeadStatus.QUALIFIED }
    val siteVisitsPlanned = leads.count { it.status == LeadStatus.SITE_VISIT_PLANNED || it.status == LeadStatus.SITE_VISIT_DONE }
    val negotiations = leads.count { it.status == LeadStatus.NEGOTIATION }
    val totalBookings = leads.count { it.status == LeadStatus.BOOKING }

    val followUpsToday = followUps.count { it.dueDate == todayStr && it.status == FollowUpStatus.PENDING }
    val followUpsOverdue = followUps.count { it.dueDate < todayStr && it.status == FollowUpStatus.PENDING }
    val hotLeads = leads.count { it.priority == LeadPriority.HOT }

    val totalSalesValueLakhs = bookings.sumOf { it.totalDealValueLakhs }
    val totalCommissionLakhs = bookings.sumOf { it.commissionAmount }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color.White),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = androidx.compose.ui.res.painterResource(id = com.example.R.drawable.ic_cn_company_logo),
                                contentDescription = "City Nexa Networks Logo",
                                tint = Color.Unspecified,
                                modifier = Modifier.size(30.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("CITY NEXA CRM", fontWeight = FontWeight.Bold, fontSize = 16.sp, letterSpacing = 0.5.sp, color = MaterialTheme.colorScheme.primary)
                            Text("Sales & Performance Suite", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                },
                actions = {
                    currentUser?.let { user ->
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .clickable { onOpenAccountSwitcher() }
                                .padding(end = 8.dp)
                                .testTag("account_switcher_trigger")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                UserRoleBadge(role = user.role)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(user.name.split(" ")[0], fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(16.dp))
                            }
                        }
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
            // Welcome Header & Date Filter
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Hello, ${currentUser?.name ?: "Sales Agent"}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Here is your sales performance overview",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Date Filter Chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    dateFilters.forEach { filter ->
                        FilterChip(
                            selected = selectedDateFilter == filter,
                            onClick = { selectedDateFilter = filter },
                            label = { Text(filter, fontSize = 11.sp) }
                        )
                    }
                }
            }

            // High-Priority Alert Banner if Overdue Follow-ups exist
            if (followUpsOverdue > 0) {
                item {
                    Surface(
                        color = Color(0xFFFEF2F2),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigateToFollowUps() }
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFDC2626), modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "$followUpsOverdue Overdue Client Follow-ups!",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = Color(0xFF991B1B)
                                )
                                Text(
                                    text = "Tap to review past-due commitments and keep customer satisfaction high.",
                                    fontSize = 11.sp,
                                    color = Color(0xFFB91C1C)
                                )
                            }
                            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color(0xFFDC2626))
                        }
                    }
                }
            }

            // Quick Workflow Row
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    QuickActionButton(
                        icon = Icons.Default.Event,
                        label = "Follow-ups ($followUpsToday)",
                        color = NexaNavyMedium,
                        onClick = onNavigateToFollowUps
                    )
                    QuickActionButton(
                        icon = Icons.Default.People,
                        label = "All Leads ($totalLeads)",
                        color = StatusNew,
                        onClick = onNavigateToLeads
                    )
                    QuickActionButton(
                        icon = Icons.Default.Tour,
                        label = "Site Visits (${siteVisits.size})",
                        color = StatusSiteVisit,
                        onClick = onNavigateToSiteVisits
                    )
                    QuickActionButton(
                        icon = Icons.Default.Handshake,
                        label = "Bookings ($totalBookings)",
                        color = StatusBooking,
                        onClick = onNavigateToBookings
                    )
                    QuickActionButton(
                        icon = Icons.Default.Autorenew,
                        label = "Resale Properties",
                        color = Color(0xFF003B95),
                        onClick = onNavigateToProjects
                    )
                }
            }

            // Key KPI Metric Cards Grid
            item {
                Text(
                    text = "Key Sales Metrics",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    KpiCard(
                        title = "Total Leads",
                        value = "$totalLeads",
                        subtitle = "$newLeads Fresh Leads",
                        icon = Icons.Default.Groups,
                        tintColor = StatusNew,
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateToLeads
                    )
                    KpiCard(
                        title = "Follow-ups Today",
                        value = "$followUpsToday",
                        subtitle = "$followUpsOverdue Overdue",
                        icon = Icons.Default.PhoneCallback,
                        tintColor = if (followUpsOverdue > 0) Color(0xFFDC2626) else NexaTeal,
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateToFollowUps
                    )
                }
            }

            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    KpiCard(
                        title = "Hot Opportunities",
                        value = "$hotLeads",
                        subtitle = "$negotiations in Negotiation",
                        icon = Icons.Default.Whatshot,
                        tintColor = PriorityHot,
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateToLeads
                    )
                    KpiCard(
                        title = "Site Visits",
                        value = "${siteVisits.size}",
                        subtitle = "$siteVisitsPlanned Active",
                        icon = Icons.Default.Tour,
                        tintColor = StatusSiteVisit,
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateToSiteVisits
                    )
                }
            }

            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    KpiCard(
                        title = "Confirmed Deals",
                        value = "$totalBookings",
                        subtitle = "₹${String.format(Locale.getDefault(), "%.1f", totalSalesValueLakhs)}L Deal Volume",
                        icon = Icons.Default.CheckCircle,
                        tintColor = StatusBooking,
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateToBookings
                    )
                    KpiCard(
                        title = "Commission",
                        value = "₹${String.format(Locale.getDefault(), "%.2f", totalCommissionLakhs)}L",
                        subtitle = "Approved & Expected",
                        icon = Icons.Default.MonetizationOn,
                        tintColor = NexaGoldDark,
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateToBookings
                    )
                }
            }

            // Lead Pipeline Funnel
            item {
                ElevatedCard(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Lead Conversion Pipeline Funnel",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        val stages = listOf(
                            Triple("New Leads", newLeads, StatusNew),
                            Triple("Contacted", contactedLeads, StatusContacted),
                            Triple("Qualified", qualifiedLeads, StatusQualified),
                            Triple("Site Visits", siteVisitsPlanned, StatusSiteVisit),
                            Triple("Negotiation", negotiations, StatusNegotiation),
                            Triple("Booked Deals", totalBookings, StatusBooking)
                        )

                        stages.forEach { (label, count, color) ->
                            val maxCount = totalLeads.coerceAtLeast(1)
                            val fraction = (count.toFloat() / maxCount).coerceIn(0.08f, 1f)
                            FunnelBar(label = label, count = count, fraction = fraction, color = color)
                            Spacer(modifier = Modifier.height(6.dp))
                        }
                    }
                }
            }

            // Lead Source Distribution
            item {
                ElevatedCard(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Leads by Marketing Channel",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        val sourceGroups = leads.groupBy { it.source }
                        sourceGroups.forEach { (src, list) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primary)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(text = src, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                }
                                Text(
                                    text = "${list.size} leads",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }

            // Recent CRM Activities
            item {
                SectionHeader(
                    title = "Recent Sales Activity Stream",
                    actionLabel = "View All",
                    onAction = onNavigateToLeads
                )
            }

            val topActivities = recentActivities.take(5)
            if (topActivities.isEmpty()) {
                item {
                    Text(
                        text = "No recent interactions recorded.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                items(topActivities) { act ->
                    TimelineItem(activity = act)
                }
            }

            item {
                Spacer(modifier = Modifier.height(72.dp))
            }
        }
    }
}

@Composable
fun QuickActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    color: Color,
    onClick: () -> Unit
) {
    Surface(
        color = color.copy(alpha = 0.12f),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(text = label, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = color)
        }
    }
}

@Composable
fun FunnelBar(
    label: String,
    count: Int,
    fraction: Float,
    color: Color
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            Text("$count leads", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = color)
        }
        Spacer(modifier = Modifier.height(3.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(4.dp))
                    .background(color)
            )
        }
    }
}
