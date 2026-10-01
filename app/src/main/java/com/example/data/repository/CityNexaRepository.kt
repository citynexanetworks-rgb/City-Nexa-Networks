package com.example.data.repository

import android.content.Context
import com.example.data.local.CityNexaDatabase
import com.example.data.model.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CityNexaRepository(private val database: CityNexaDatabase) {

    private val userDao = database.userDao()
    private val leadDao = database.leadDao()
    private val activityDao = database.leadActivityDao()
    private val followUpDao = database.followUpDao()
    private val projectDao = database.projectDao()
    private val inventoryDao = database.projectInventoryDao()
    private val siteVisitDao = database.siteVisitDao()
    private val bookingDao = database.bookingDao()
    private val templateDao = database.templateDao()
    private val auditDao = database.auditLogDao()
    private val notificationDao = database.notificationDao()

    init {
        // Pre-populate if empty
        CoroutineScope(Dispatchers.IO).launch {
            checkAndSeedInitialData()
        }
    }

    // User Operations
    fun getAllUsers(): Flow<List<User>> = userDao.getAllUsers()
    fun getSalesExecutives(): Flow<List<User>> = userDao.getSalesExecutives()
    suspend fun getUserById(id: String): User? = userDao.getUserById(id)
    suspend fun getUserByEmail(email: String): User? = userDao.getUserByEmail(email)
    suspend fun getUserByPhone(phone: String): User? = userDao.getUserByPhone(phone)
    suspend fun saveUser(user: User, actor: User) {
        userDao.insertUser(user)
        logAudit(actor, "USER_SAVED", "User", user.id, "", "${user.name} (${user.role.displayName})")
    }

    // Lead Operations
    fun getAllLeads(): Flow<List<Lead>> = leadDao.getAllLeads()
    fun getLeadsForSalesperson(salespersonId: String): Flow<List<Lead>> = leadDao.getLeadsBySalesperson(salespersonId)
    fun getLeadsForManager(managerId: String): Flow<List<Lead>> = leadDao.getLeadsByManager(managerId)
    fun getLeadByIdFlow(id: String): Flow<Lead?> = leadDao.getLeadByIdFlow(id)
    suspend fun getLeadById(id: String): Lead? = leadDao.getLeadById(id)
    suspend fun findDuplicateLead(phone: String, email: String): Lead? = leadDao.findDuplicateLead(phone, email)

    suspend fun createLead(lead: Lead, actor: User): Boolean {
        leadDao.insertLead(lead)
        logAudit(actor, "LEAD_CREATED", "Lead", lead.id, "", "${lead.name} (${lead.status.label})")
        activityDao.insertActivity(
            LeadActivity(
                leadId = lead.id,
                userId = actor.id,
                userName = actor.name,
                type = ActivityType.STATUS_CHANGED,
                title = "Lead Created",
                description = "Lead generated via ${lead.source}. Initial status: ${lead.status.label}."
            )
        )
        // Notification
        notificationDao.insertNotification(
            AppNotification(
                userId = lead.assignedSalespersonId,
                title = "New Lead Assigned: ${lead.name}",
                message = "${lead.propertyType.label} in ${lead.preferredLocation} (₹${lead.budgetMin}L - ₹${lead.budgetMax}L)",
                type = "NEW_LEAD",
                referenceId = lead.id
            )
        )
        return true
    }

    suspend fun updateLead(lead: Lead, actor: User, previousStatus: LeadStatus? = null) {
        leadDao.updateLead(lead)
        if (previousStatus != null && previousStatus != lead.status) {
            logAudit(actor, "STATUS_UPDATED", "Lead", lead.id, previousStatus.label, lead.status.label)
            activityDao.insertActivity(
                LeadActivity(
                    leadId = lead.id,
                    userId = actor.id,
                    userName = actor.name,
                    type = ActivityType.STATUS_CHANGED,
                    title = "Status changed to ${lead.status.label}",
                    description = "Pipeline updated from ${previousStatus.label} to ${lead.status.label} by ${actor.name}."
                )
            )
        }
    }

    suspend fun deleteLead(id: String, actor: User) {
        leadDao.softDeleteLead(id)
        logAudit(actor, "LEAD_DELETED", "Lead", id, "Active", "Soft Deleted")
    }

    // Activities
    fun getActivitiesForLead(leadId: String): Flow<List<LeadActivity>> = activityDao.getActivitiesForLead(leadId)
    fun getAllActivities(): Flow<List<LeadActivity>> = activityDao.getAllActivities()
    suspend fun addActivity(activity: LeadActivity) {
        activityDao.insertActivity(activity)
    }

    // Call Recording and Remarks
    suspend fun recordCallOutcome(
        leadId: String,
        actor: User,
        durationSeconds: Int,
        outcome: String,
        remark: String,
        nextFollowUpDate: String? = null,
        nextFollowUpTime: String? = null
    ) {
        val lead = leadDao.getLeadById(leadId)
        val formattedDuration = String.format(Locale.getDefault(), "%02d:%02d", durationSeconds / 60, durationSeconds % 60)
        
        // Insert Call Activity
        activityDao.insertActivity(
            LeadActivity(
                leadId = leadId,
                userId = actor.id,
                userName = actor.name,
                type = ActivityType.CALL,
                title = "Outbound Call ($formattedDuration)",
                description = "Outcome: $outcome. Remark: $remark",
                durationSeconds = durationSeconds,
                callOutcome = outcome
            )
        )

        // If remark present, update lead lastRemark and lastContactedAt
        if (lead != null) {
            val updated = lead.copy(
                lastContactedAt = System.currentTimeMillis(),
                lastRemark = remark,
                status = if (lead.status == LeadStatus.NEW_LEAD || lead.status == LeadStatus.ASSIGNED) LeadStatus.CONTACTED else lead.status,
                nextFollowUpDate = nextFollowUpDate ?: lead.nextFollowUpDate,
                nextFollowUpTime = nextFollowUpTime ?: lead.nextFollowUpTime
            )
            leadDao.updateLead(updated)
            logAudit(actor, "CALL_LOGGED", "Lead", leadId, "", "Duration: $formattedDuration, Outcome: $outcome")
        }

        // Create follow-up if requested
        if (!nextFollowUpDate.isNullOrEmpty() && lead != null) {
            val followUp = FollowUp(
                leadId = lead.id,
                leadName = lead.name,
                customerPhone = lead.mobile,
                projectId = lead.interestedProjectId,
                salespersonId = actor.id,
                salespersonName = actor.name,
                dueDate = nextFollowUpDate,
                dueTime = nextFollowUpTime ?: "11:00 AM",
                type = FollowUpType.CALL,
                notes = "Follow up after call: $remark"
            )
            followUpDao.insertFollowUp(followUp)
        }
    }

    // Follow-ups
    fun getAllFollowUps(): Flow<List<FollowUp>> = followUpDao.getAllFollowUps()
    fun getFollowUpsForSalesperson(salespersonId: String): Flow<List<FollowUp>> = followUpDao.getFollowUpsForSalesperson(salespersonId)
    suspend fun createFollowUp(followUp: FollowUp, actor: User) {
        followUpDao.insertFollowUp(followUp)
        activityDao.insertActivity(
            LeadActivity(
                leadId = followUp.leadId,
                userId = actor.id,
                userName = actor.name,
                type = ActivityType.FOLLOW_UP_CREATED,
                title = "Follow-up Scheduled (${followUp.type.label})",
                description = "Scheduled for ${followUp.dueDate} at ${followUp.dueTime}. Note: ${followUp.notes}"
            )
        )
        logAudit(actor, "FOLLOWUP_CREATED", "FollowUp", followUp.leadId, "", "${followUp.type.label} on ${followUp.dueDate}")
    }

    suspend fun markFollowUpComplete(id: Long, actor: User) {
        followUpDao.updateStatus(id, FollowUpStatus.COMPLETED, System.currentTimeMillis())
        logAudit(actor, "FOLLOWUP_COMPLETED", "FollowUp", id.toString(), "Pending", "Completed")
    }

    // Projects
    fun getAllProjects(): Flow<List<Project>> = projectDao.getAllProjects()
    fun getProjectByIdFlow(id: String): Flow<Project?> = projectDao.getProjectByIdFlow(id)
    suspend fun getProjectById(id: String): Project? = projectDao.getProjectById(id)
    suspend fun saveProject(project: Project, actor: User) {
        projectDao.insertProject(project)
        logAudit(actor, "PROJECT_SAVED", "Project", project.id, "", project.name)
    }
    suspend fun deleteProject(id: String, actor: User) {
        projectDao.deleteProjectById(id)
        logAudit(actor, "PROJECT_DELETED", "Project", id, "Active", "Deleted")
    }

    // Inventory
    fun getInventoryForProject(projectId: String): Flow<List<ProjectInventory>> = inventoryDao.getInventoryForProject(projectId)

    // Site Visits
    fun getAllSiteVisits(): Flow<List<SiteVisit>> = siteVisitDao.getAllSiteVisits()
    fun getSiteVisitsForSalesperson(salespersonId: String): Flow<List<SiteVisit>> = siteVisitDao.getSiteVisitsForSalesperson(salespersonId)
    suspend fun createSiteVisit(siteVisit: SiteVisit, actor: User) {
        siteVisitDao.insertSiteVisit(siteVisit)
        activityDao.insertActivity(
            LeadActivity(
                leadId = siteVisit.leadId,
                userId = actor.id,
                userName = actor.name,
                type = ActivityType.SITE_VISIT_PLANNED,
                title = "Site Visit Planned at ${siteVisit.projectName}",
                description = "Visit scheduled for ${siteVisit.visitDate} at ${siteVisit.visitTime}. Visitors: ${siteVisit.numberOfVisitors}"
            )
        )
        // Also update lead pipeline status to SITE_VISIT_PLANNED if earlier
        val lead = leadDao.getLeadById(siteVisit.leadId)
        if (lead != null && lead.status.order < LeadStatus.SITE_VISIT_PLANNED.order) {
            leadDao.updateLead(lead.copy(status = LeadStatus.SITE_VISIT_PLANNED))
        }
        logAudit(actor, "SITE_VISIT_CREATED", "SiteVisit", siteVisit.leadId, "", "${siteVisit.projectName} on ${siteVisit.visitDate}")
    }

    suspend fun updateSiteVisit(siteVisit: SiteVisit, actor: User) {
        siteVisitDao.updateSiteVisit(siteVisit)
        activityDao.insertActivity(
            LeadActivity(
                leadId = siteVisit.leadId,
                userId = actor.id,
                userName = actor.name,
                type = ActivityType.SITE_VISIT_UPDATED,
                title = "Site Visit Status: ${siteVisit.status.label}",
                description = "Site visit updated: ${siteVisit.remarks}. Next Action: ${siteVisit.nextAction}"
            )
        )
        if (siteVisit.status == SiteVisitStatus.COMPLETED) {
            val lead = leadDao.getLeadById(siteVisit.leadId)
            if (lead != null && lead.status.order < LeadStatus.SITE_VISIT_DONE.order) {
                leadDao.updateLead(lead.copy(status = LeadStatus.SITE_VISIT_DONE))
            }
        }
        logAudit(actor, "SITE_VISIT_UPDATED", "SiteVisit", siteVisit.leadId, "", siteVisit.status.label)
    }

    // Bookings & Commission
    fun getAllBookings(): Flow<List<Booking>> = bookingDao.getAllBookings()
    fun getBookingsForSalesperson(salespersonId: String): Flow<List<Booking>> = bookingDao.getBookingsForSalesperson(salespersonId)
    suspend fun createBooking(booking: Booking, actor: User) {
        bookingDao.insertBooking(booking)
        activityDao.insertActivity(
            LeadActivity(
                leadId = booking.leadId,
                userId = actor.id,
                userName = actor.name,
                type = ActivityType.BOOKING_CREATED,
                title = "Booking Created: ${booking.unitNumber} (${booking.projectName})",
                description = "Deal value: ₹${booking.totalDealValueLakhs}L. Booking Amount: ₹${booking.bookingAmountPaidLakhs}L. Commission: ₹${booking.commissionAmount}L (${booking.commissionPercent}%)."
            )
        )
        // Update lead pipeline to BOOKING
        val lead = leadDao.getLeadById(booking.leadId)
        if (lead != null) {
            leadDao.updateLead(lead.copy(status = LeadStatus.BOOKING))
        }
        logAudit(actor, "BOOKING_CREATED", "Booking", booking.id, "", "${booking.projectName} Unit ${booking.unitNumber}")
    }

    suspend fun updateBooking(booking: Booking, actor: User) {
        bookingDao.updateBooking(booking)
        logAudit(actor, "BOOKING_UPDATED", "Booking", booking.id, "", "${booking.bookingStatus.label}, Comm: ${booking.commissionStatus.label}")
    }

    // Templates
    fun getAllWhatsAppTemplates(): Flow<List<WhatsAppTemplate>> = templateDao.getAllWhatsAppTemplates()
    fun getAllEmailTemplates(): Flow<List<EmailTemplate>> = templateDao.getAllEmailTemplates()
    suspend fun updateWhatsAppTemplate(template: WhatsAppTemplate, actor: User) {
        templateDao.updateWhatsAppTemplate(template)
        logAudit(actor, "TEMPLATE_UPDATED", "WhatsAppTemplate", template.id, "", template.title)
    }

    // Record WhatsApp Share
    suspend fun recordWhatsAppShare(
        leadId: String,
        actor: User,
        projectId: String,
        projectName: String,
        selectedFiles: List<String>,
        renderedMessage: String
    ) {
        val filesSummary = if (selectedFiles.isEmpty()) "No files" else selectedFiles.joinToString(", ")
        activityDao.insertActivity(
            LeadActivity(
                leadId = leadId,
                userId = actor.id,
                userName = actor.name,
                type = ActivityType.WHATSAPP_SENT,
                title = "Project Shared on WhatsApp ($projectName)",
                description = "Files shared: $filesSummary\nMessage preview: ${renderedMessage.take(120)}...",
                projectId = projectId
            )
        )
        // Update lead pipeline to PROJECT_SHARED if at CONTACTED
        val lead = leadDao.getLeadById(leadId)
        if (lead != null && lead.status.order < LeadStatus.PROJECT_SHARED.order) {
            leadDao.updateLead(lead.copy(status = LeadStatus.PROJECT_SHARED))
        }
        logAudit(actor, "PROJECT_SHARED_WHATSAPP", "Lead", leadId, "", "Project: $projectName, Files: $filesSummary")
    }

    // Record Email Sent
    suspend fun recordEmailSent(
        leadId: String,
        actor: User,
        recipient: String,
        subject: String,
        body: String,
        attachments: List<String>
    ) {
        activityDao.insertActivity(
            LeadActivity(
                leadId = leadId,
                userId = actor.id,
                userName = actor.name,
                type = ActivityType.EMAIL_SENT,
                title = "Email Sent: $subject",
                description = "To: $recipient\nAttachments: ${attachments.joinToString(", ")}\n${body.take(120)}..."
            )
        )
        logAudit(actor, "EMAIL_SENT", "Lead", leadId, "", "Subject: $subject to $recipient")
    }

    // Audit Logs
    fun getAllAuditLogs(): Flow<List<AuditLog>> = auditDao.getAllAuditLogs()
    suspend fun logAudit(
        user: User,
        action: String,
        recordType: String,
        recordId: String,
        prevVal: String,
        newVal: String
    ) {
        auditDao.insertAuditLog(
            AuditLog(
                userId = user.id,
                userName = user.name,
                userRole = user.role.displayName,
                action = action,
                recordType = recordType,
                recordId = recordId,
                previousValue = prevVal,
                newValue = newVal,
                timestamp = System.currentTimeMillis()
            )
        )
    }

    // Notifications
    fun getNotificationsForUser(userId: String): Flow<List<AppNotification>> = notificationDao.getNotificationsForUser(userId)
    suspend fun markNotificationRead(id: Long) = notificationDao.markAsRead(id)

    // Bulk Import
    suspend fun bulkImportLeads(leads: List<Lead>, actor: User): Pair<Int, Int> {
        var importedCount = 0
        var duplicateCount = 0
        for (lead in leads) {
            val dup = leadDao.findDuplicateLead(lead.mobile, lead.email)
            if (dup == null) {
                leadDao.insertLead(lead)
                importedCount++
            } else {
                duplicateCount++
            }
        }
        logAudit(actor, "BULK_LEAD_IMPORT", "Lead", "Batch", "", "Imported: $importedCount, Skipped duplicates: $duplicateCount")
        return Pair(importedCount, duplicateCount)
    }

    // Prepopulate comprehensive enterprise seed data
    private suspend fun checkAndSeedInitialData() {
        val users = userDao.getAllUsers().firstOrNull()
        if (!users.isNullOrEmpty()) return

        // 1. Users
        val seedUsers = listOf(
            User(
                id = "USR-001",
                name = "Rohan Nair",
                email = "rohan.md@citynexa.com",
                phone = "9880112233",
                role = UserRole.SUPER_ADMIN,
                designation = "Managing Director"
            ),
            User(
                id = "USR-002",
                name = "Pooja Sharma",
                email = "pooja.admin@citynexa.com",
                phone = "9880112244",
                role = UserRole.ADMIN,
                designation = "CRM Administrator"
            ),
            User(
                id = "USR-003",
                name = "Vikram Malhotra",
                email = "vikram.manager@citynexa.com",
                phone = "9880112255",
                role = UserRole.SALES_MANAGER,
                designation = "Senior Sales Manager"
            ),
            User(
                id = "USR-004",
                name = "Rahul Verma",
                email = "rahul.sales@citynexa.com",
                phone = "9880112266",
                role = UserRole.SALES_EXECUTIVE,
                managerId = "USR-003",
                designation = "Senior Relationship Executive"
            ),
            User(
                id = "USR-005",
                name = "Ananya Iyer",
                email = "ananya.sales@citynexa.com",
                phone = "9880112277",
                role = UserRole.SALES_EXECUTIVE,
                managerId = "USR-003",
                designation = "Key Client Advisor"
            )
        )
        userDao.insertUsers(seedUsers)

        // 2. Master Projects
        val seedProjects = listOf(
            Project(
                id = "NX-PRJ-01",
                name = "City Nexa Pinnacle Towers",
                developer = "City Nexa Infrastructure Ltd",
                location = "Hoskote Road, Whitefield Extn",
                address = "Survey 42/1, Old Madras Road, Hoskote Industrial Corridor, Bengaluru",
                reraNumber = "PRM/KA/RERA/1251/310/PR/220926/005123",
                projectType = PropertyType.APARTMENT,
                priceMinLakhs = 85.0,
                priceMaxLakhs = 185.0,
                totalUnits = 420,
                availableUnits = 68,
                configurations = "2, 3 & 4 BHK Luxury Condos",
                sizeRangeSqFt = "1180 - 2450 sq.ft",
                plotSize = "6.5 Acres",
                towers = 4,
                floors = 24,
                possessionDate = "Dec 2027",
                projectStatus = "Under Construction",
                amenities = "Clubhouse, Infinity Swimming Pool, Squash Courts, Gymnasium, Rooftop Sky Lounge, Children's Play Park, 24/7 Security",
                imageDrawableRes = "img_project_tower_1790708814742",
                brochurePdfName = "Pinnacle_Towers_Brochure.pdf",
                priceSheetPdfName = "Pinnacle_Price_Matrix_Q3.pdf",
                videoUrl = "https://youtu.be/pinnacle_citynexa_tour",
                googleMapsLink = "https://maps.google.com/?q=Pinnacle+Towers+Hoskote",
                nearbyLandmarks = "5 mins to Peripheral Ring Road, 10 mins to MVJ Hospital, 15 mins to ITPL"
            ),
            Project(
                id = "NX-PRJ-02",
                name = "Nexa Boulevard Luxury Villas",
                developer = "City Nexa Living Spaces",
                location = "Hoskote Eco Green Belt",
                address = "Near Chintamani Cross, Hoskote-Malur Road, Bengaluru Rural",
                reraNumber = "PRM/KA/RERA/1251/310/PR/230415/005890",
                projectType = PropertyType.VILLA,
                priceMinLakhs = 145.0,
                priceMaxLakhs = 280.0,
                totalUnits = 110,
                availableUnits = 24,
                configurations = "3 & 4 BHK Triplex Villas with Private Lawn",
                sizeRangeSqFt = "2400 - 3800 sq.ft",
                plotSize = "14 Acres",
                towers = 0,
                floors = 3,
                possessionDate = "Ready to Move",
                projectStatus = "Ready to Move",
                amenities = "Private Gardens, Heated Pool, Tennis Academy, Organic Farm, Banqueting Hall, Solar Powered, EV Charging",
                imageDrawableRes = "img_project_villa_1790708826849",
                brochurePdfName = "Boulevard_Villas_Luxury_Brochure.pdf",
                priceSheetPdfName = "Boulevard_Villa_Inventory.pdf",
                videoUrl = "https://youtu.be/nexa_boulevard_villas",
                googleMapsLink = "https://maps.google.com/?q=Nexa+Boulevard+Villas",
                nearbyLandmarks = "Adjacent to National Highway 75, 20 mins to Kempegowda International Airport"
            ),
            Project(
                id = "NX-PRJ-03",
                name = "City Nexa Green Meadows",
                developer = "City Nexa Land Ventures",
                location = "Sarjapur East Corridor",
                address = "Attibele-Sarjapur Main Road, Bengaluru",
                reraNumber = "PRM/KA/RERA/1251/310/PR/240118/006412",
                projectType = PropertyType.PLOT,
                priceMinLakhs = 48.0,
                priceMaxLakhs = 98.0,
                totalUnits = 250,
                availableUnits = 82,
                configurations = "BMRDA Approved Premium Villa Plots",
                sizeRangeSqFt = "1200 - 2400 sq.ft",
                plotSize = "22 Acres",
                towers = 0,
                floors = 0,
                possessionDate = "Immediate Registration",
                projectStatus = "Ready for Construction",
                amenities = "Underground Cabling, Wide Asphalt Roads, Street Lights, Overhead Water Reservoir, Grand Entrance Arch",
                imageDrawableRes = "img_project_villa_1790708826849",
                brochurePdfName = "Green_Meadows_Layout_Plan.pdf",
                priceSheetPdfName = "Green_Meadows_Plot_Rates.pdf",
                videoUrl = "https://youtu.be/green_meadows_plots",
                googleMapsLink = "https://maps.google.com/?q=City+Nexa+Green+Meadows",
                nearbyLandmarks = "10 mins to Indus International School, 15 mins to Decathlon Sarjapur"
            ),
            Project(
                id = "NX-PRJ-04",
                name = "Nexa Signature Business Hub",
                developer = "City Nexa Commercial Assets",
                location = "Outer Ring Road - Marathahalli",
                address = "Plot 18, Commercial Belt, ORR, Bengaluru",
                reraNumber = "PRM/KA/RERA/1251/310/PR/240502/006781",
                projectType = PropertyType.COMMERCIAL,
                priceMinLakhs = 110.0,
                priceMaxLakhs = 450.0,
                totalUnits = 95,
                availableUnits = 31,
                configurations = "Grade-A Boutique Offices & High-Street Retail",
                sizeRangeSqFt = "850 - 4500 sq.ft",
                plotSize = "3 Acres",
                towers = 1,
                floors = 14,
                possessionDate = "March 2027",
                projectStatus = "Under Construction",
                amenities = "Double Height Grand Lobby, High-Speed Elevators, Central HVAC, 100% DG Backup, Multi-tier Basement Parking",
                imageDrawableRes = "img_project_tower_1790708814742",
                brochurePdfName = "Signature_Business_Hub_Kit.pdf",
                priceSheetPdfName = "Commercial_Retail_Tariffs.pdf",
                videoUrl = "https://youtu.be/signature_hub_walkthrough",
                googleMapsLink = "https://maps.google.com/?q=Nexa+Signature+Business+Hub",
                nearbyLandmarks = "Opposite Prestige Tech Park, Walking distance to Kadubeesanahalli Metro"
            ),
            Project(
                id = "NX-PRJ-05",
                name = "Prestige Lakeside Habitat - 3 BHK Resale",
                developer = "Prestige Group (Direct Owner Resale)",
                location = "Varthur - Whitefield",
                address = "Tower 14, 18th Floor, Varthur Main Road, Bengaluru",
                reraNumber = "PRM/KA/RERA/1251/310/PR/170915/000176",
                projectType = PropertyType.RESALE,
                priceMinLakhs = 135.0,
                priceMaxLakhs = 145.0,
                totalUnits = 1,
                availableUnits = 1,
                configurations = "3 BHK + 3T Fully Furnished Resale",
                sizeRangeSqFt = "1820 sq.ft",
                plotSize = "80 Acres Township",
                towers = 1,
                floors = 24,
                possessionDate = "Immediate Ready to Move",
                projectStatus = "Ready to Move (Resale)",
                amenities = "Modular Kitchen, Italian Marble, Lake Facing Balcony, Clubhouse, 2 Covered Car Parks",
                imageDrawableRes = "img_project_tower_1790708814742",
                brochurePdfName = "Prestige_Resale_Verification_Report.pdf",
                priceSheetPdfName = "Resale_Cost_Sheet_Owner_Direct.pdf",
                videoUrl = "https://youtu.be/prestige_lakeside_walkthrough",
                googleMapsLink = "https://maps.google.com/?q=Prestige+Lakeside+Habitat",
                nearbyLandmarks = "Overlooking Varthur Lake, 10 mins to Whitefield Forum Mall",
                isResale = true,
                resaleOwnerName = "Col. Rajesh Mehra",
                propertyAgeYears = 4
            ),
            Project(
                id = "NX-PRJ-06",
                name = "Sobha Rose - 2 BHK Resale Unit",
                developer = "Sobha Limited (Owner Resale)",
                location = "Hoskote Road, Whitefield Extn",
                address = "Near Hope Farm Junction, Bengaluru",
                reraNumber = "PRM/KA/RERA/1251/310/PR/180210/000940",
                projectType = PropertyType.RESALE,
                priceMinLakhs = 78.0,
                priceMaxLakhs = 85.0,
                totalUnits = 1,
                availableUnits = 1,
                configurations = "2 BHK + 2T East Facing Resale",
                sizeRangeSqFt = "1280 sq.ft",
                plotSize = "5 Acres",
                towers = 2,
                floors = 12,
                possessionDate = "Immediate OC Received",
                projectStatus = "Ready to Move (Resale)",
                amenities = "A-Khata, OC/CC Received, Wooden Flooring in Master Bedroom, Gym, Power Backup",
                imageDrawableRes = "img_project_tower_1790708814742",
                brochurePdfName = "Sobha_Rose_Title_Deed_Verification.pdf",
                priceSheetPdfName = "Sobha_Resale_Offer.pdf",
                videoUrl = "https://youtu.be/sobha_rose_resale",
                googleMapsLink = "https://maps.google.com/?q=Sobha+Rose+Whitefield",
                nearbyLandmarks = "Walking distance to Hope Farm Circle Metro",
                isResale = true,
                resaleOwnerName = "Anita Swaminathan",
                propertyAgeYears = 3
            )
        )
        projectDao.insertProjects(seedProjects)

        // 3. Project Inventory Units
        val seedInventory = listOf(
            ProjectInventory(projectId = "NX-PRJ-01", unitNumber = "T1-402", tower = "Tower 1", floor = 4, configuration = "2 BHK", sizeSqFt = 1180, priceLakhs = 88.5, status = UnitStatus.AVAILABLE, facing = "East"),
            ProjectInventory(projectId = "NX-PRJ-01", unitNumber = "T1-1204", tower = "Tower 1", floor = 12, configuration = "3 BHK + 2T", sizeSqFt = 1520, priceLakhs = 118.0, status = UnitStatus.BOOKED, facing = "North-East"),
            ProjectInventory(projectId = "NX-PRJ-01", unitNumber = "T2-1801", tower = "Tower 2", floor = 18, configuration = "3 BHK + 3T", sizeSqFt = 1850, priceLakhs = 142.5, status = UnitStatus.AVAILABLE, facing = "East"),
            ProjectInventory(projectId = "NX-PRJ-01", unitNumber = "T2-2204", tower = "Tower 2", floor = 22, configuration = "4 BHK Penthouse", sizeSqFt = 2450, priceLakhs = 185.0, status = UnitStatus.HOLD, facing = "North"),
            ProjectInventory(projectId = "NX-PRJ-02", unitNumber = "V-12", tower = "Phase 1", floor = 1, configuration = "4 BHK Triplex", sizeSqFt = 2950, priceLakhs = 185.0, status = UnitStatus.AVAILABLE, facing = "East"),
            ProjectInventory(projectId = "NX-PRJ-02", unitNumber = "V-28", tower = "Phase 2", floor = 1, configuration = "4 BHK Premium Villa", sizeSqFt = 3600, priceLakhs = 245.0, status = UnitStatus.AVAILABLE, facing = "North")
        )
        inventoryDao.insertInventory(seedInventory)

        // 4. Templates
        val seedTemplates = listOf(
            WhatsAppTemplate(
                id = "WT-01",
                title = "Project Details & Price Offer",
                category = "Project Details",
                messageTemplate = "Hello {{customer_name}},\n\nGreetings from City Nexa Networks!\n\nAs per our discussion regarding your interest in {{project_name}} ({{location}}), I am pleased to share the project portfolio.\n\n✨ Key Highlights:\n• Premium configurations within your budget (₹{{budget}})\n• RERA Registered & 100% Clear Titles\n• World-class amenities & superior connectivity\n\nPlease find the official brochure and current inventory details attached.\n\nWould you like me to schedule a VIP site visit for this weekend?\n\nWarm regards,\n{{salesperson_name}}\nCity Nexa Networks\n📞 {{salesperson_phone}}"
            ),
            WhatsAppTemplate(
                id = "WT-02",
                title = "Quick Follow-up Call Request",
                category = "Follow-up",
                messageTemplate = "Hi {{customer_name}},\n\nHope you are having a productive day!\n\nThis is {{salesperson_name}} from City Nexa Networks. Following up regarding your requirement in {{location}} for {{project_name}}.\n\nWe have received 2 exclusive developer price revisions for the 3 BHK units this week. Could we connect for 2 minutes at your convenience?\n\nBest regards,\n{{salesperson_name}}"
            ),
            WhatsAppTemplate(
                id = "WT-03",
                title = "Call Missed / Not Connected",
                category = "Not Connected",
                messageTemplate = "Dear {{customer_name}},\n\nI just attempted to reach you regarding your recent property inquiry for {{project_name}} in {{location}}, but could not connect.\n\nPlease let me know a suitable time to call, or feel free to reply with any specific questions right here.\n\nLooking forward to assisting you.\n\n{{salesperson_name}}\nCity Nexa Networks\n📞 {{salesperson_phone}}"
            ),
            WhatsAppTemplate(
                id = "WT-04",
                title = "Site Visit Confirmation & Directions",
                category = "Site Visit Confirmation",
                messageTemplate = "Dear {{customer_name}},\n\nYour site inspection for {{project_name}} at {{location}} has been confirmed.\n\n📍 Site Lounge: City Nexa Experience Pavilion\n⏰ Sales Representative: {{salesperson_name}} ({{salesperson_phone}})\n\nOur hospitality team will assist you with model apartment walkthroughs and personalized price computations. Looking forward to welcoming you!"
            ),
            WhatsAppTemplate(
                id = "WT-05",
                title = "Requirement Not Matching / Not Interested",
                category = "Not Interested",
                messageTemplate = "Thank you {{customer_name}} for taking the time to speak with City Nexa Networks. We have logged your preferences in our system. As soon as a verified project matching your exact criteria launches in {{location}}, we will reach out. Wishing you the very best!"
            )
        )
        templateDao.insertWhatsAppTemplates(seedTemplates)

        val seedEmailTemplates = listOf(
            EmailTemplate(
                id = "ET-01",
                title = "Official Project Presentation & Pricing",
                subject = "Exclusive Investment Overview: {{project_name}} by City Nexa Networks",
                body = "Dear {{customer_name}},\n\nThank you for connecting with City Nexa Networks. We are pleased to present the comprehensive property details for {{project_name}}.\n\nProject Location: {{location}}\nIndicative Budget: {{budget}}\n\nPlease review the attached digital brochure, payment milestone schedule, and RERA approval documents.\n\nSincerely,\n{{salesperson_name}}\nCity Nexa Networks"
            )
        )
        templateDao.insertEmailTemplates(seedEmailTemplates)

        // 5. Leads for Rahul Verma (USR-004) and Ananya Iyer (USR-005)
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val seedLeads = listOf(
            Lead(
                id = "NX-1001",
                name = "Karthik Sundaram",
                mobile = "9845123456",
                whatsapp = "9845123456",
                email = "karthik.sundaram@gmail.com",
                source = "Meta Ads",
                interestedProjectId = "NX-PRJ-01",
                propertyType = PropertyType.APARTMENT,
                budgetMin = 90.0,
                budgetMax = 125.0,
                preferredLocation = "Hoskote Road",
                bhk = "3 BHK",
                sizeRequirementSqFt = "1500 sq.ft",
                purpose = "End Use",
                requirementNotes = "Looking for high floor with open view, vastu compliant north/east facing.",
                status = LeadStatus.SITE_VISIT_PLANNED,
                priority = LeadPriority.HOT,
                assignedSalespersonId = "USR-004", // Rahul Verma
                salesManagerId = "USR-003",
                createdAt = System.currentTimeMillis() - 86400000L * 4,
                lastContactedAt = System.currentTimeMillis() - 86400000L,
                nextFollowUpDate = todayStr,
                nextFollowUpTime = "04:30 PM",
                lastRemark = "Customer loved 3 BHK layout in brochure. Site visit planned for Saturday."
            ),
            Lead(
                id = "NX-1002",
                name = "Deepak & Shilpa Roy",
                mobile = "9740987654",
                whatsapp = "9740987654",
                email = "deepak.roy@outlook.com",
                source = "99acres",
                interestedProjectId = "NX-PRJ-02",
                propertyType = PropertyType.VILLA,
                budgetMin = 160.0,
                budgetMax = 220.0,
                preferredLocation = "Hoskote Eco Green Belt",
                bhk = "4 BHK",
                sizeRequirementSqFt = "2900 sq.ft",
                purpose = "End Use",
                requirementNotes = "Requires private garden and 2 covered car parks. Ready to move preferred.",
                status = LeadStatus.NEGOTIATION,
                priority = LeadPriority.HOT,
                assignedSalespersonId = "USR-004", // Rahul Verma
                salesManagerId = "USR-003",
                createdAt = System.currentTimeMillis() - 86400000L * 8,
                lastContactedAt = System.currentTimeMillis() - 3600000L * 6,
                nextFollowUpDate = todayStr,
                nextFollowUpTime = "11:00 AM",
                lastRemark = "Negotiating payment schedule and 2% spot closing discount with Developer."
            ),
            Lead(
                id = "NX-1003",
                name = "Dr. Manjunath Swamy",
                mobile = "9886001122",
                whatsapp = "9886001122",
                email = "dr.manjunath@apollo.com",
                source = "Google Ads",
                interestedProjectId = "NX-PRJ-01",
                propertyType = PropertyType.APARTMENT,
                budgetMin = 80.0,
                budgetMax = 110.0,
                preferredLocation = "Whitefield",
                bhk = "2 BHK",
                sizeRequirementSqFt = "1200 sq.ft",
                purpose = "Investment",
                requirementNotes = "Looking for rental yield and capital appreciation near upcoming metro.",
                status = LeadStatus.CONTACTED,
                priority = LeadPriority.WARM,
                assignedSalespersonId = "USR-004", // Rahul Verma
                salesManagerId = "USR-003",
                createdAt = System.currentTimeMillis() - 86400000L * 2,
                lastContactedAt = System.currentTimeMillis() - 86400000L * 2,
                nextFollowUpDate = todayStr,
                nextFollowUpTime = "02:00 PM",
                lastRemark = "Call connected. Customer requested brochure and floor plan on WhatsApp."
            ),
            Lead(
                id = "NX-1004",
                name = "Priya Venkatesh",
                mobile = "9900112233",
                whatsapp = "9900112233",
                email = "priya.v@tcs.com",
                source = "MagicBricks",
                interestedProjectId = "NX-PRJ-03",
                propertyType = PropertyType.PLOT,
                budgetMin = 50.0,
                budgetMax = 80.0,
                preferredLocation = "Sarjapur East",
                bhk = "Villa Plot",
                sizeRequirementSqFt = "1500 sq.ft",
                purpose = "Investment",
                requirementNotes = "Clear title BMRDA plot for 5-year investment.",
                status = LeadStatus.NEW_LEAD,
                priority = LeadPriority.WARM,
                assignedSalespersonId = "USR-004", // Rahul Verma
                salesManagerId = "USR-003",
                createdAt = System.currentTimeMillis() - 3600000L * 3,
                lastRemark = "Fresh lead from MagicBricks inquiry campaign."
            ),
            Lead(
                id = "NX-1005",
                name = "Suresh Chandran",
                mobile = "9844055667",
                whatsapp = "9844055667",
                email = "suresh.c@wipro.com",
                source = "Referral",
                interestedProjectId = "NX-PRJ-01",
                propertyType = PropertyType.APARTMENT,
                budgetMin = 110.0,
                budgetMax = 150.0,
                preferredLocation = "Hoskote Road",
                bhk = "3 BHK",
                sizeRequirementSqFt = "1800 sq.ft",
                purpose = "End Use",
                requirementNotes = "Wife works in Whitefield, son studying in Deens Academy.",
                status = LeadStatus.BOOKING,
                priority = LeadPriority.HOT,
                assignedSalespersonId = "USR-004", // Rahul Verma
                salesManagerId = "USR-003",
                createdAt = System.currentTimeMillis() - 86400000L * 14,
                lastContactedAt = System.currentTimeMillis() - 86400000L * 2,
                lastRemark = "Unit T1-1204 booked. Booking token received. Agreement in progress."
            ),
            Lead(
                id = "NX-1006",
                name = "Arun & Divya Menon",
                mobile = "9731234567",
                whatsapp = "9731234567",
                email = "arun.menon@cisco.com",
                source = "Meta Ads",
                interestedProjectId = "NX-PRJ-02",
                propertyType = PropertyType.VILLA,
                budgetMin = 150.0,
                budgetMax = 200.0,
                preferredLocation = "Hoskote",
                bhk = "4 BHK",
                sizeRequirementSqFt = "2800 sq.ft",
                purpose = "End Use",
                requirementNotes = "Looking for gated villa near highway.",
                status = LeadStatus.QUALIFIED,
                priority = LeadPriority.WARM,
                assignedSalespersonId = "USR-005", // Ananya Iyer
                salesManagerId = "USR-003",
                createdAt = System.currentTimeMillis() - 86400000L * 5,
                lastContactedAt = System.currentTimeMillis() - 86400000L * 2,
                lastRemark = "Qualified customer. Budget verified with HDFC pre-approved loan."
            )
        )
        leadDao.insertLeads(seedLeads)

        // 6. Lead Activities
        val seedActivities = listOf(
            LeadActivity(
                leadId = "NX-1001",
                userId = "USR-004",
                userName = "Rahul Verma",
                type = ActivityType.CALL,
                title = "Outbound Call (06:45)",
                description = "Customer discussed 3 BHK unit options at Pinnacle Towers. Requested site visit.",
                durationSeconds = 405,
                callOutcome = "Connected - Interested"
            ),
            LeadActivity(
                leadId = "NX-1001",
                userId = "USR-004",
                userName = "Rahul Verma",
                type = ActivityType.PROJECT_SHARED,
                title = "Project Shared on WhatsApp",
                description = "Brochure PDF & Price Matrix shared to 9845123456.",
                projectId = "NX-PRJ-01"
            ),
            LeadActivity(
                leadId = "NX-1001",
                userId = "USR-004",
                userName = "Rahul Verma",
                type = ActivityType.SITE_VISIT_PLANNED,
                title = "Site Visit Scheduled",
                description = "Site inspection scheduled for 4:30 PM with Family."
            ),
            LeadActivity(
                leadId = "NX-1005",
                userId = "USR-004",
                userName = "Rahul Verma",
                type = ActivityType.BOOKING_CREATED,
                title = "Booking Initiated - Unit T1-1204",
                description = "Booking amount of ₹5.00 Lakhs received. Total Deal Value: ₹118.00 Lakhs."
            )
        )
        seedActivities.forEach { activityDao.insertActivity(it) }

        // 7. Follow-ups
        val seedFollowUps = listOf(
            FollowUp(
                leadId = "NX-1001",
                leadName = "Karthik Sundaram",
                customerPhone = "9845123456",
                projectId = "NX-PRJ-01",
                projectName = "City Nexa Pinnacle Towers",
                salespersonId = "USR-004",
                salespersonName = "Rahul Verma",
                dueDate = todayStr,
                dueTime = "04:30 PM",
                type = FollowUpType.SITE_VISIT,
                notes = "Receive customer at Experience Pavilion. Show 1520 sq.ft sample flat."
            ),
            FollowUp(
                leadId = "NX-1002",
                leadName = "Deepak & Shilpa Roy",
                customerPhone = "9740987654",
                projectId = "NX-PRJ-02",
                projectName = "Nexa Boulevard Luxury Villas",
                salespersonId = "USR-004",
                salespersonName = "Rahul Verma",
                dueDate = todayStr,
                dueTime = "11:00 AM",
                type = FollowUpType.NEGOTIATION,
                notes = "Finalize price sheet with 2% festival waiver approved by Vikram."
            ),
            FollowUp(
                leadId = "NX-1003",
                leadName = "Dr. Manjunath Swamy",
                customerPhone = "9886001122",
                projectId = "NX-PRJ-01",
                projectName = "City Nexa Pinnacle Towers",
                salespersonId = "USR-004",
                salespersonName = "Rahul Verma",
                dueDate = todayStr,
                dueTime = "02:00 PM",
                type = FollowUpType.CALL,
                notes = "Follow up on brochure sent yesterday. Check if 2 BHK or 3 BHK preferred."
            ),
            FollowUp(
                leadId = "NX-1006",
                leadName = "Arun & Divya Menon",
                customerPhone = "9731234567",
                projectId = "NX-PRJ-02",
                projectName = "Nexa Boulevard Luxury Villas",
                salespersonId = "USR-005",
                salespersonName = "Ananya Iyer",
                dueDate = todayStr,
                dueTime = "05:00 PM",
                type = FollowUpType.WHATSAPP,
                notes = "Send payment schedule and bank loan approval documents."
            )
        )
        seedFollowUps.forEach { followUpDao.insertFollowUp(it) }

        // 8. Site Visits
        val seedSiteVisits = listOf(
            SiteVisit(
                leadId = "NX-1001",
                leadName = "Karthik Sundaram",
                customerPhone = "9845123456",
                projectId = "NX-PRJ-01",
                projectName = "City Nexa Pinnacle Towers",
                visitDate = todayStr,
                visitTime = "04:30 PM",
                salespersonId = "USR-004",
                salespersonName = "Rahul Verma",
                numberOfVisitors = 3,
                status = SiteVisitStatus.CONFIRMED,
                remarks = "Customer coming with spouse and father.",
                nextAction = "Confirm closing offer on spot.",
                cabRequired = false
            ),
            SiteVisit(
                leadId = "NX-1002",
                leadName = "Deepak & Shilpa Roy",
                customerPhone = "9740987654",
                projectId = "NX-PRJ-02",
                projectName = "Nexa Boulevard Luxury Villas",
                visitDate = "2026-09-27",
                visitTime = "03:00 PM",
                salespersonId = "USR-004",
                salespersonName = "Rahul Verma",
                numberOfVisitors = 2,
                status = SiteVisitStatus.COMPLETED,
                remarks = "Walked through Villa V-12 and V-28. Liked V-12 east facing.",
                nextAction = "Move to deal negotiation.",
                cabRequired = true
            )
        )
        seedSiteVisits.forEach { siteVisitDao.insertSiteVisit(it) }

        // 9. Bookings & Commission
        val seedBookings = listOf(
            Booking(
                id = "BK-901",
                leadId = "NX-1005",
                customerName = "Suresh Chandran",
                customerPhone = "9844055667",
                projectId = "NX-PRJ-01",
                projectName = "City Nexa Pinnacle Towers",
                unitNumber = "T1-1204",
                configuration = "3 BHK + 2T (1520 sq.ft)",
                salespersonId = "USR-004",
                salespersonName = "Rahul Verma",
                bookingDate = "2026-09-24",
                totalDealValueLakhs = 118.0,
                bookingAmountPaidLakhs = 5.0,
                bookingStatus = BookingStatus.BOOKING_CONFIRMED,
                agreementStatus = "Draft Sent for Signatures",
                commissionPercent = 2.0,
                commissionAmount = 2.36,
                commissionStatus = CommissionStatus.APPROVED,
                expectedCommissionDate = "2026-10-15",
                receivedCommissionLakhs = 0.0
            )
        )
        seedBookings.forEach { bookingDao.insertBooking(it) }

        // 10. Audit Logs
        val seedAudit = listOf(
            AuditLog(
                userId = "USR-002",
                userName = "Pooja Sharma",
                userRole = "Admin",
                action = "SYSTEM_INITIALIZED",
                recordType = "System",
                recordId = "SYS-01",
                previousValue = "",
                newValue = "City Nexa Networks Enterprise CRM Ready"
            )
        )
        seedAudit.forEach { auditDao.insertAuditLog(it) }
    }
}
