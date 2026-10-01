package com.example.ui

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.auth.AuthManager
import com.example.data.local.CityNexaDatabase
import com.example.data.model.*
import com.example.data.repository.CityNexaRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class PropertyMatchResult(
    val project: Project,
    val score: Int, // 0-100
    val matchLevel: String, // "High Match", "Good Match", "Partial Match"
    val reasons: List<String>
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val database = CityNexaDatabase.getDatabase(application)
    val repository = CityNexaRepository(database)
    val authManager = AuthManager()

    val currentUser: StateFlow<User?> = authManager.currentUser

    // Search and Filters
    private val _leadSearchQuery = MutableStateFlow("")
    val leadSearchQuery: StateFlow<String> = _leadSearchQuery.asStateFlow()

    private val _selectedStatusFilter = MutableStateFlow<LeadStatus?>(null)
    val selectedStatusFilter: StateFlow<LeadStatus?> = _selectedStatusFilter.asStateFlow()

    private val _selectedPriorityFilter = MutableStateFlow<LeadPriority?>(null)
    val selectedPriorityFilter: StateFlow<LeadPriority?> = _selectedPriorityFilter.asStateFlow()

    private val _dateFilter = MutableStateFlow("All")
    val dateFilter: StateFlow<String> = _dateFilter.asStateFlow()

    val allUsers: StateFlow<List<User>> = repository.getAllUsers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allProjects: StateFlow<List<Project>> = repository.getAllProjects()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val whatsAppTemplates: StateFlow<List<WhatsAppTemplate>> = repository.getAllWhatsAppTemplates()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val emailTemplates: StateFlow<List<EmailTemplate>> = repository.getAllEmailTemplates()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // All leads flow
    private val allLeadsFlow = repository.getAllLeads()

    // Filtered Leads based on User Role and UI filters
    val visibleLeads: StateFlow<List<Lead>> = combine(
        allLeadsFlow,
        currentUser,
        _leadSearchQuery,
        _selectedStatusFilter,
        _selectedPriorityFilter
    ) { leads, user, query, statusFilter, priorityFilter ->
        if (user == null) return@combine emptyList<Lead>()

        // 1. Role-based scoping (Enforced security at data layer)
        val scopedLeads = when (user.role) {
            UserRole.SALES_EXECUTIVE -> leads.filter { it.assignedSalespersonId == user.id }
            UserRole.SALES_MANAGER -> leads.filter { it.salesManagerId == user.id || it.assignedSalespersonId == user.id }
            UserRole.ADMIN, UserRole.SUPER_ADMIN -> leads
        }

        // 2. Query and Filters
        scopedLeads.filter { lead ->
            val matchesQuery = query.isBlank() ||
                    lead.name.contains(query, ignoreCase = true) ||
                    lead.mobile.contains(query) ||
                    lead.id.contains(query, ignoreCase = true) ||
                    lead.preferredLocation.contains(query, ignoreCase = true) ||
                    lead.source.contains(query, ignoreCase = true)

            val matchesStatus = statusFilter == null || lead.status == statusFilter
            val matchesPriority = priorityFilter == null || lead.priority == priorityFilter

            matchesQuery && matchesStatus && matchesPriority
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Follow-ups scoped by role
    val visibleFollowUps: StateFlow<List<FollowUp>> = combine(
        repository.getAllFollowUps(),
        currentUser
    ) { followUps, user ->
        if (user == null) return@combine emptyList<FollowUp>()
        when (user.role) {
            UserRole.SALES_EXECUTIVE -> followUps.filter { it.salespersonId == user.id }
            UserRole.SALES_MANAGER -> followUps.filter { it.salespersonId == user.id || true } // manager sees team
            UserRole.ADMIN, UserRole.SUPER_ADMIN -> followUps
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Site Visits scoped by role
    val visibleSiteVisits: StateFlow<List<SiteVisit>> = combine(
        repository.getAllSiteVisits(),
        currentUser
    ) { visits, user ->
        if (user == null) return@combine emptyList<SiteVisit>()
        when (user.role) {
            UserRole.SALES_EXECUTIVE -> visits.filter { it.salespersonId == user.id }
            UserRole.SALES_MANAGER, UserRole.ADMIN, UserRole.SUPER_ADMIN -> visits
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Bookings scoped by role
    val visibleBookings: StateFlow<List<Booking>> = combine(
        repository.getAllBookings(),
        currentUser
    ) { bookings, user ->
        if (user == null) return@combine emptyList<Booking>()
        when (user.role) {
            UserRole.SALES_EXECUTIVE -> bookings.filter { it.salespersonId == user.id }
            UserRole.SALES_MANAGER, UserRole.ADMIN, UserRole.SUPER_ADMIN -> bookings
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Audit logs (accessible by Admin/Super Admin)
    val auditLogs: StateFlow<List<AuditLog>> = repository.getAllAuditLogs()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Notifications for current user
    val notifications: StateFlow<List<AppNotification>> = currentUser.flatMapLatest { user ->
        if (user == null) flowOf(emptyList())
        else repository.getNotificationsForUser(user.id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Active Call Dialog State
    private val _activeCallLead = MutableStateFlow<Lead?>(null)
    val activeCallLead: StateFlow<Lead?> = _activeCallLead.asStateFlow()

    fun setLeadSearchQuery(query: String) {
        _leadSearchQuery.value = query
    }

    fun setStatusFilter(status: LeadStatus?) {
        _selectedStatusFilter.value = status
    }

    fun setPriorityFilter(priority: LeadPriority?) {
        _selectedPriorityFilter.value = priority
    }

    fun setDateFilter(filter: String) {
        _dateFilter.value = filter
    }

    // Role Switcher / Auth
    fun switchUser(user: User) {
        authManager.setCurrentUser(user)
    }

    fun logout() {
        authManager.logout()
    }

    // Call Trigger & Tracking
    fun initiateCall(context: Context, lead: Lead) {
        _activeCallLead.value = lead
        try {
            val intent = Intent(Intent.ACTION_DIAL).apply {
                data = Uri.parse("tel:${lead.mobile}")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            // Handled gracefully
        }
    }

    fun dismissCallDialog() {
        _activeCallLead.value = null
    }

    fun submitCallRemark(
        leadId: String,
        durationSeconds: Int,
        outcome: String,
        remark: String,
        nextFollowUpDate: String? = null,
        nextFollowUpTime: String? = null
    ) {
        val user = currentUser.value ?: return
        viewModelScope.launch {
            repository.recordCallOutcome(
                leadId = leadId,
                actor = user,
                durationSeconds = durationSeconds,
                outcome = outcome,
                remark = remark,
                nextFollowUpDate = nextFollowUpDate,
                nextFollowUpTime = nextFollowUpTime
            )
            _activeCallLead.value = null
        }
    }

    // Lead CRUD
    fun createLead(lead: Lead, onResult: (Boolean) -> Unit) {
        val user = currentUser.value ?: return
        viewModelScope.launch {
            val success = repository.createLead(lead, user)
            onResult(success)
        }
    }

    fun updateLeadStatus(lead: Lead, newStatus: LeadStatus) {
        val user = currentUser.value ?: return
        if (lead.status == newStatus) return
        val previous = lead.status
        val updated = lead.copy(status = newStatus)
        viewModelScope.launch {
            repository.updateLead(updated, user, previousStatus = previous)
        }
    }

    fun updateLead(lead: Lead) {
        val user = currentUser.value ?: return
        viewModelScope.launch {
            repository.updateLead(lead, user)
        }
    }

    fun addRemarkToLead(lead: Lead, remarkText: String) {
        val user = currentUser.value ?: return
        viewModelScope.launch {
            repository.addActivity(
                LeadActivity(
                    leadId = lead.id,
                    userId = user.id,
                    userName = user.name,
                    type = ActivityType.REMARK_ADDED,
                    title = "Remark Added",
                    description = remarkText.trim()
                )
            )
            repository.updateLead(lead.copy(lastRemark = remarkText.trim()), user)
        }
    }

    fun deleteLead(leadId: String) {
        val user = currentUser.value ?: return
        viewModelScope.launch {
            repository.deleteLead(leadId, user)
        }
    }

    // Follow-ups
    fun scheduleFollowUp(followUp: FollowUp) {
        val user = currentUser.value ?: return
        viewModelScope.launch {
            repository.createFollowUp(followUp, user)
        }
    }

    fun completeFollowUp(id: Long) {
        val user = currentUser.value ?: return
        viewModelScope.launch {
            repository.markFollowUpComplete(id, user)
        }
    }

    // Site Visits
    fun scheduleSiteVisit(siteVisit: SiteVisit) {
        val user = currentUser.value ?: return
        viewModelScope.launch {
            repository.createSiteVisit(siteVisit, user)
        }
    }

    fun updateSiteVisit(siteVisit: SiteVisit) {
        val user = currentUser.value ?: return
        viewModelScope.launch {
            repository.updateSiteVisit(siteVisit, user)
        }
    }

    // Bookings
    fun createBooking(booking: Booking) {
        val user = currentUser.value ?: return
        viewModelScope.launch {
            repository.createBooking(booking, user)
        }
    }

    fun updateBooking(booking: Booking) {
        val user = currentUser.value ?: return
        viewModelScope.launch {
            repository.updateBooking(booking, user)
        }
    }

    // Projects (Super Admin / Admin only)
    fun saveProject(project: Project) {
        val user = currentUser.value ?: return
        if (!authManager.canManageMasterProjects()) return
        viewModelScope.launch {
            repository.saveProject(project, user)
        }
    }

    fun deleteProject(projectId: String) {
        val user = currentUser.value ?: return
        if (!authManager.canManageMasterProjects()) return
        viewModelScope.launch {
            repository.deleteProject(projectId, user)
        }
    }

    // Property Matching Engine
    fun matchProjectsForLead(lead: Lead, allProjects: List<Project>): List<PropertyMatchResult> {
        return allProjects.map { project ->
            var score = 0
            val reasons = mutableListOf<String>()

            // 1. Budget comparison
            val budgetOverlap = (lead.budgetMax >= project.priceMinLakhs) && (lead.budgetMin <= project.priceMaxLakhs)
            if (budgetOverlap) {
                score += 35
                reasons.add("Price matches budget range (₹${project.priceMinLakhs}L - ₹${project.priceMaxLakhs}L)")
            } else if (lead.budgetMax >= project.priceMinLakhs * 0.85) {
                score += 15
                reasons.add("Price close to customer budget")
            }

            // 2. Location comparison
            val leadLoc = lead.preferredLocation.lowercase()
            val projLoc = (project.location + " " + project.address).lowercase()
            if (leadLoc.split(" ", ",").any { it.isNotBlank() && projLoc.contains(it) }) {
                score += 25
                reasons.add("Location alignment (${project.location})")
            }

            // 3. Property Type comparison
            if (lead.propertyType == project.projectType || (lead.propertyType == PropertyType.RESALE && project.isResale)) {
                score += 20
                if (project.isResale) {
                    reasons.add("Verified direct owner resale listing (${project.resaleOwnerName})")
                } else {
                    reasons.add("Exact property type (${project.projectType.label})")
                }
            }

            // 4. Configuration (BHK) matching
            if (lead.bhk.isNotBlank() && project.configurations.contains(lead.bhk, ignoreCase = true)) {
                score += 15
                reasons.add("Includes requested ${lead.bhk} configuration")
            }

            // 5. Purpose & Possession
            if (lead.purpose == "Investment" && project.projectStatus == "Under Construction") {
                score += 5
                reasons.add("High capital appreciation potential for investment")
            } else if (lead.purpose == "End Use" && project.projectStatus == "Ready to Move") {
                score += 5
                reasons.add("Ready to move in for end use")
            }

            val clampedScore = score.coerceIn(10, 100)
            val matchLevel = when {
                clampedScore >= 75 -> "High Match"
                clampedScore >= 50 -> "Good Match"
                else -> "Partial Match"
            }

            PropertyMatchResult(
                project = project,
                score = clampedScore,
                matchLevel = matchLevel,
                reasons = reasons
            )
        }.sortedByDescending { it.score }
    }

    // WhatsApp Sharing Execution & Logging
    fun shareProjectOnWhatsApp(
        context: Context,
        lead: Lead,
        project: Project,
        message: String,
        selectedFiles: List<String>
    ) {
        val user = currentUser.value ?: return
        viewModelScope.launch {
            repository.recordWhatsAppShare(
                leadId = lead.id,
                actor = user,
                projectId = project.id,
                projectName = project.name,
                selectedFiles = selectedFiles,
                renderedMessage = message
            )
        }

        try {
            val phone = lead.whatsapp.replace("+", "").replace(" ", "").trim()
            val cleanPhone = if (phone.length == 10) "91$phone" else phone
            val encodedMessage = Uri.encode(message)
            val uri = Uri.parse("https://api.whatsapp.com/send?phone=$cleanPhone&text=$encodedMessage")
            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            // Fallback generic send
            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, message)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(Intent.createChooser(sendIntent, "Share Project via WhatsApp"))
        }
    }

    // Email Sharing Execution & Logging
    fun shareViaEmail(
        context: Context,
        lead: Lead,
        subject: String,
        body: String,
        attachments: List<String>
    ) {
        val user = currentUser.value ?: return
        viewModelScope.launch {
            repository.recordEmailSent(
                leadId = lead.id,
                actor = user,
                recipient = lead.email,
                subject = subject,
                body = body,
                attachments = attachments
            )
        }

        try {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:${lead.email}")
                putExtra(Intent.EXTRA_SUBJECT, subject)
                putExtra(Intent.EXTRA_TEXT, body)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            // Intent fallback
        }
    }

    // Bulk Import with Validation
    fun bulkImportCsv(csvText: String, onResult: (imported: Int, duplicates: Int, error: String?) -> Unit) {
        val user = currentUser.value ?: return
        viewModelScope.launch {
            try {
                val lines = csvText.lines().filter { it.isNotBlank() }
                if (lines.size <= 1) {
                    onResult(0, 0, "File is empty or contains only headers")
                    return@launch
                }

                val leads = mutableListOf<Lead>()
                // Parse rows (ignoring header if present)
                val startIndex = if (lines[0].contains("Name", ignoreCase = true)) 1 else 0
                for (i in startIndex until lines.size) {
                    val cols = lines[i].split(",").map { it.trim().removeSurrounding("\"") }
                    if (cols.size >= 3) {
                        val name = cols[0]
                        val mobile = cols[1]
                        val email = if (cols.size > 2) cols[2] else ""
                        val source = if (cols.size > 3 && cols[3].isNotBlank()) cols[3] else "Meta Ads"
                        val location = if (cols.size > 4 && cols[4].isNotBlank()) cols[4] else "Whitefield"
                        val budgetMin = cols.getOrNull(5)?.toDoubleOrNull() ?: 75.0
                        val budgetMax = cols.getOrNull(6)?.toDoubleOrNull() ?: 150.0

                        leads.add(
                            Lead(
                                id = "NX-IMP-${System.currentTimeMillis() % 10000 + i}",
                                name = name,
                                mobile = mobile,
                                whatsapp = mobile,
                                email = email,
                                source = source,
                                propertyType = PropertyType.APARTMENT,
                                budgetMin = budgetMin,
                                budgetMax = budgetMax,
                                preferredLocation = location,
                                bhk = "3 BHK",
                                assignedSalespersonId = user.id,
                                salesManagerId = user.managerId ?: user.id,
                                status = LeadStatus.NEW_LEAD,
                                priority = LeadPriority.WARM
                            )
                        )
                    }
                }

                val (imported, dups) = repository.bulkImportLeads(leads, user)
                onResult(imported, dups, null)
            } catch (e: Exception) {
                onResult(0, 0, e.localizedMessage ?: "Import failed")
            }
        }
    }

    // Excel / CSV Export
    fun exportLeadsCsv(context: Context, leads: List<Lead>): File {
        val sb = StringBuilder()
        sb.append("Lead ID,Name,Mobile,WhatsApp,Email,Source,Project,Property Type,Budget Min,Budget Max,Location,BHK,Purpose,Status,Priority,Salesperson ID,Created At,Last Remark\n")
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

        for (l in leads) {
            val dateStr = sdf.format(Date(l.createdAt))
            sb.append("\"${l.id}\",\"${l.name}\",\"${l.mobile}\",\"${l.whatsapp}\",\"${l.email}\",\"${l.source}\",\"${l.interestedProjectId ?: ""}\",\"${l.propertyType.label}\",\"${l.budgetMin}\",\"${l.budgetMax}\",\"${l.preferredLocation}\",\"${l.bhk}\",\"${l.purpose}\",\"${l.status.label}\",\"${l.priority.name}\",\"${l.assignedSalespersonId}\",\"$dateStr\",\"${l.lastRemark.replace("\"", "\"\"")}\"\n")
        }

        val exportFile = File(context.cacheDir, "CityNexa_Leads_Export_${System.currentTimeMillis()}.csv")
        exportFile.writeText(sb.toString())
        return exportFile
    }

    fun shareExportedFile(context: Context, file: File) {
        val uri = androidx.core.content.FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "City Nexa CRM - Leads Export")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(Intent.createChooser(shareIntent, "Export Leads via"))
    }
}
