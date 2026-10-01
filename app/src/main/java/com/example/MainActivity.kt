package com.example

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Lead
import com.example.data.model.Project
import com.example.ui.MainViewModel
import com.example.ui.components.UserRoleBadge
import com.example.ui.screens.audit.AuditLogScreen
import com.example.ui.screens.bookings.BookingCommissionScreen
import com.example.ui.screens.dashboard.DashboardScreen
import com.example.ui.screens.followups.FollowUpScreen
import com.example.ui.screens.leads.CallRemarkDialog
import com.example.ui.screens.leads.LeadDetailScreen
import com.example.ui.screens.leads.LeadListScreen
import com.example.ui.screens.projects.ProjectDetailScreen
import com.example.ui.screens.projects.ProjectListScreen
import com.example.ui.screens.projects.PropertyMatchingScreen
import com.example.ui.screens.projects.WhatsAppShareScreen
import com.example.ui.screens.reports.ReportsScreen
import com.example.ui.screens.settings.SettingsScreen
import com.example.ui.screens.sitevisits.SiteVisitScreen
import com.example.ui.theme.CityNexaCRMTheme

enum class Screen {
    DASHBOARD,
    LEADS,
    FOLLOW_UPS,
    PROJECTS,
    MORE,
    LEAD_DETAIL,
    PROPERTY_MATCH,
    WHATSAPP_SHARE,
    PROJECT_DETAIL,
    SITE_VISITS,
    BOOKINGS,
    REPORTS,
    AUDIT_LOGS
}

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Enable secure screen protection for sensitive real estate CRM customer data
        try {
            window.setFlags(
                WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE
            )
        } catch (e: Exception) {
            // Ignored on environments where unsupported
        }

        setContent {
            CityNexaCRMTheme {
                CityNexaApp(viewModel = viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CityNexaApp(viewModel: MainViewModel) {
    var currentScreen by remember { mutableStateOf(Screen.DASHBOARD) }
    val screenStack = remember { mutableStateListOf<Screen>() }

    var selectedLeadId by remember { mutableStateOf<String?>(null) }
    var selectedProjectId by remember { mutableStateOf<String?>(null) }
    var shareTargetLead by remember { mutableStateOf<Lead?>(null) }
    var shareTargetProject by remember { mutableStateOf<Project?>(null) }

    var showAccountSwitcher by remember { mutableStateOf(false) }

    val activeCallLead by viewModel.activeCallLead.collectAsState()
    val allUsers by viewModel.allUsers.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()

    fun navigateTo(screen: Screen) {
        screenStack.add(currentScreen)
        currentScreen = screen
    }

    fun navigateBack() {
        if (screenStack.isNotEmpty()) {
            currentScreen = screenStack.removeLast()
        } else {
            currentScreen = Screen.DASHBOARD
        }
    }

    // Handle Hardware/Gesture Back button
    BackHandler(enabled = currentScreen != Screen.DASHBOARD) {
        navigateBack()
    }

    // Call Remark Dialog whenever a call is initiated
    activeCallLead?.let { lead ->
        CallRemarkDialog(
            lead = lead,
            viewModel = viewModel,
            onDismiss = { viewModel.dismissCallDialog() }
        )
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            // Only show bottom navigation on top-level tabs
            val isTopLevel = currentScreen in listOf(
                Screen.DASHBOARD, Screen.LEADS, Screen.FOLLOW_UPS, Screen.PROJECTS, Screen.MORE
            )
            if (isTopLevel) {
                NavigationBar(
                    modifier = Modifier
                        .windowInsetsPadding(WindowInsets.navigationBars)
                        .testTag("main_bottom_nav"),
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 6.dp
                ) {
                    NavigationBarItem(
                        selected = currentScreen == Screen.DASHBOARD,
                        onClick = { currentScreen = Screen.DASHBOARD },
                        icon = { Icon(Icons.Default.Dashboard, contentDescription = "Dashboard") },
                        label = { Text("Dashboard") },
                        modifier = Modifier.testTag("nav_dashboard")
                    )
                    NavigationBarItem(
                        selected = currentScreen == Screen.LEADS,
                        onClick = { currentScreen = Screen.LEADS },
                        icon = { Icon(Icons.Default.Groups, contentDescription = "Leads") },
                        label = { Text("Leads") },
                        modifier = Modifier.testTag("nav_leads")
                    )
                    NavigationBarItem(
                        selected = currentScreen == Screen.FOLLOW_UPS,
                        onClick = { currentScreen = Screen.FOLLOW_UPS },
                        icon = { Icon(Icons.Default.EventNote, contentDescription = "Follow-ups") },
                        label = { Text("Follow-ups") },
                        modifier = Modifier.testTag("nav_followups")
                    )
                    NavigationBarItem(
                        selected = currentScreen == Screen.PROJECTS,
                        onClick = { currentScreen = Screen.PROJECTS },
                        icon = { Icon(Icons.Default.Apartment, contentDescription = "Projects") },
                        label = { Text("Projects") },
                        modifier = Modifier.testTag("nav_projects")
                    )
                    NavigationBarItem(
                        selected = currentScreen == Screen.MORE,
                        onClick = { currentScreen = Screen.MORE },
                        icon = { Icon(Icons.Default.Menu, contentDescription = "More") },
                        label = { Text("More") },
                        modifier = Modifier.testTag("nav_more")
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentScreen) {
                Screen.DASHBOARD -> DashboardScreen(
                    viewModel = viewModel,
                    onNavigateToLeads = { currentScreen = Screen.LEADS },
                    onNavigateToFollowUps = { currentScreen = Screen.FOLLOW_UPS },
                    onNavigateToProjects = { currentScreen = Screen.PROJECTS },
                    onNavigateToSiteVisits = { navigateTo(Screen.SITE_VISITS) },
                    onNavigateToBookings = { navigateTo(Screen.BOOKINGS) },
                    onOpenAccountSwitcher = { showAccountSwitcher = true }
                )

                Screen.LEADS -> LeadListScreen(
                    viewModel = viewModel,
                    onLeadSelected = { lead ->
                        selectedLeadId = lead.id
                        navigateTo(Screen.LEAD_DETAIL)
                    },
                    onOpenPropertyMatch = { lead ->
                        selectedLeadId = lead.id
                        navigateTo(Screen.PROPERTY_MATCH)
                    }
                )

                Screen.FOLLOW_UPS -> FollowUpScreen(
                    viewModel = viewModel,
                    onLeadSelected = { lead ->
                        selectedLeadId = lead.id
                        navigateTo(Screen.LEAD_DETAIL)
                    }
                )

                Screen.PROJECTS -> ProjectListScreen(
                    viewModel = viewModel,
                    onProjectSelected = { proj ->
                        selectedProjectId = proj.id
                        navigateTo(Screen.PROJECT_DETAIL)
                    }
                )

                Screen.MORE -> SettingsScreen(
                    viewModel = viewModel,
                    onNavigateToAuditLogs = { navigateTo(Screen.AUDIT_LOGS) },
                    onNavigateToReports = { navigateTo(Screen.REPORTS) }
                )

                Screen.LEAD_DETAIL -> selectedLeadId?.let { id ->
                    LeadDetailScreen(
                        leadId = id,
                        viewModel = viewModel,
                        onBack = { navigateBack() },
                        onOpenPropertyMatch = { lead ->
                            selectedLeadId = lead.id
                            navigateTo(Screen.PROPERTY_MATCH)
                        },
                        onOpenWhatsAppShare = { lead, proj ->
                            shareTargetLead = lead
                            shareTargetProject = proj
                            navigateTo(Screen.WHATSAPP_SHARE)
                        }
                    )
                }

                Screen.PROPERTY_MATCH -> selectedLeadId?.let { id ->
                    PropertyMatchingScreen(
                        leadId = id,
                        viewModel = viewModel,
                        onBack = { navigateBack() },
                        onSelectProjectToShare = { lead, proj ->
                            shareTargetLead = lead
                            shareTargetProject = proj
                            navigateTo(Screen.WHATSAPP_SHARE)
                        }
                    )
                }

                Screen.WHATSAPP_SHARE -> {
                    val targetLead = shareTargetLead
                    val targetProject = shareTargetProject
                    if (targetLead != null && targetProject != null) {
                        WhatsAppShareScreen(
                            lead = targetLead,
                            project = targetProject,
                            viewModel = viewModel,
                            onBack = { navigateBack() }
                        )
                    } else {
                        navigateBack()
                    }
                }

                Screen.PROJECT_DETAIL -> selectedProjectId?.let { id ->
                    ProjectDetailScreen(
                        projectId = id,
                        viewModel = viewModel,
                        onBack = { navigateBack() }
                    )
                }

                Screen.SITE_VISITS -> SiteVisitScreen(
                    viewModel = viewModel
                )

                Screen.BOOKINGS -> BookingCommissionScreen(
                    viewModel = viewModel
                )

                Screen.REPORTS -> ReportsScreen(
                    viewModel = viewModel
                )

                Screen.AUDIT_LOGS -> AuditLogScreen(
                    viewModel = viewModel,
                    onBack = { navigateBack() }
                )
            }
        }
    }

    // Role / Account Switcher Dialog for rapid testing across all roles
    if (showAccountSwitcher) {
        AlertDialog(
            onDismissRequest = { showAccountSwitcher = false },
            title = { Text("Switch Role Profile") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Experience the CRM from each user's perspective & role permissions:",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    allUsers.forEach { user ->
                        val isSelected = currentUser?.id == user.id
                        Surface(
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.switchUser(user)
                                    showAccountSwitcher = false
                                }
                                .testTag("select_role_${user.role.name.lowercase()}")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(user.name, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text(user.designation, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                UserRoleBadge(role = user.role)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAccountSwitcher = false }) { Text("Close") }
            }
        )
    }
}
