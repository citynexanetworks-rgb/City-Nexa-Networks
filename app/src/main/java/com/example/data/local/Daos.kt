package com.example.data.local

import androidx.room.*
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM users ORDER BY name ASC")
    fun getAllUsers(): Flow<List<User>>

    @Query("SELECT * FROM users WHERE role = 'SALES_EXECUTIVE' AND isActive = 1")
    fun getSalesExecutives(): Flow<List<User>>

    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    suspend fun getUserById(id: String): User?

    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    suspend fun getUserByEmail(email: String): User?

    @Query("SELECT * FROM users WHERE phone = :phone LIMIT 1")
    suspend fun getUserByPhone(phone: String): User?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: User)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUsers(users: List<User>)

    @Update
    suspend fun updateUser(user: User)

    @Delete
    suspend fun deleteUser(user: User)
}

@Dao
interface LeadDao {
    @Query("SELECT * FROM leads WHERE isDeleted = 0 ORDER BY createdAt DESC")
    fun getAllLeads(): Flow<List<Lead>>

    @Query("SELECT * FROM leads WHERE assignedSalespersonId = :salespersonId AND isDeleted = 0 ORDER BY createdAt DESC")
    fun getLeadsBySalesperson(salespersonId: String): Flow<List<Lead>>

    @Query("SELECT * FROM leads WHERE salesManagerId = :managerId AND isDeleted = 0 ORDER BY createdAt DESC")
    fun getLeadsByManager(managerId: String): Flow<List<Lead>>

    @Query("SELECT * FROM leads WHERE id = :id LIMIT 1")
    suspend fun getLeadById(id: String): Lead?

    @Query("SELECT * FROM leads WHERE id = :id LIMIT 1")
    fun getLeadByIdFlow(id: String): Flow<Lead?>

    @Query("SELECT * FROM leads WHERE mobile = :phone OR whatsapp = :phone OR email = :email LIMIT 1")
    suspend fun findDuplicateLead(phone: String, email: String): Lead?

    @Query("SELECT COUNT(*) FROM leads WHERE isDeleted = 0")
    fun getLeadCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLead(lead: Lead)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLeads(leads: List<Lead>)

    @Update
    suspend fun updateLead(lead: Lead)

    @Query("UPDATE leads SET isDeleted = 1 WHERE id = :id")
    suspend fun softDeleteLead(id: String)
}

@Dao
interface LeadActivityDao {
    @Query("SELECT * FROM lead_activities WHERE leadId = :leadId ORDER BY timestamp DESC")
    fun getActivitiesForLead(leadId: String): Flow<List<LeadActivity>>

    @Query("SELECT * FROM lead_activities ORDER BY timestamp DESC LIMIT 200")
    fun getAllActivities(): Flow<List<LeadActivity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertActivity(activity: LeadActivity)
}

@Dao
interface FollowUpDao {
    @Query("SELECT * FROM follow_ups ORDER BY dueDate ASC, dueTime ASC")
    fun getAllFollowUps(): Flow<List<FollowUp>>

    @Query("SELECT * FROM follow_ups WHERE salespersonId = :salespersonId ORDER BY dueDate ASC, dueTime ASC")
    fun getFollowUpsForSalesperson(salespersonId: String): Flow<List<FollowUp>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFollowUp(followUp: FollowUp)

    @Update
    suspend fun updateFollowUp(followUp: FollowUp)

    @Query("UPDATE follow_ups SET status = :status, completedAt = :completedAt WHERE id = :id")
    suspend fun updateStatus(id: Long, status: FollowUpStatus, completedAt: Long?)
}

@Dao
interface ProjectDao {
    @Query("SELECT * FROM projects ORDER BY name ASC")
    fun getAllProjects(): Flow<List<Project>>

    @Query("SELECT * FROM projects WHERE id = :id LIMIT 1")
    suspend fun getProjectById(id: String): Project?

    @Query("SELECT * FROM projects WHERE id = :id LIMIT 1")
    fun getProjectByIdFlow(id: String): Flow<Project?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProject(project: Project)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProjects(projects: List<Project>)

    @Update
    suspend fun updateProject(project: Project)

    @Query("DELETE FROM projects WHERE id = :id")
    suspend fun deleteProjectById(id: String)
}

@Dao
interface ProjectInventoryDao {
    @Query("SELECT * FROM project_inventory WHERE projectId = :projectId ORDER BY tower ASC, floor ASC, unitNumber ASC")
    fun getInventoryForProject(projectId: String): Flow<List<ProjectInventory>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInventory(items: List<ProjectInventory>)

    @Update
    suspend fun updateInventory(item: ProjectInventory)
}

@Dao
interface SiteVisitDao {
    @Query("SELECT * FROM site_visits ORDER BY visitDate ASC, visitTime ASC")
    fun getAllSiteVisits(): Flow<List<SiteVisit>>

    @Query("SELECT * FROM site_visits WHERE salespersonId = :salespersonId ORDER BY visitDate ASC, visitTime ASC")
    fun getSiteVisitsForSalesperson(salespersonId: String): Flow<List<SiteVisit>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSiteVisit(siteVisit: SiteVisit)

    @Update
    suspend fun updateSiteVisit(siteVisit: SiteVisit)
}

@Dao
interface BookingDao {
    @Query("SELECT * FROM bookings ORDER BY createdAt DESC")
    fun getAllBookings(): Flow<List<Booking>>

    @Query("SELECT * FROM bookings WHERE salespersonId = :salespersonId ORDER BY createdAt DESC")
    fun getBookingsForSalesperson(salespersonId: String): Flow<List<Booking>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBooking(booking: Booking)

    @Update
    suspend fun updateBooking(booking: Booking)
}

@Dao
interface TemplateDao {
    @Query("SELECT * FROM whatsapp_templates ORDER BY category ASC")
    fun getAllWhatsAppTemplates(): Flow<List<WhatsAppTemplate>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWhatsAppTemplates(templates: List<WhatsAppTemplate>)

    @Update
    suspend fun updateWhatsAppTemplate(template: WhatsAppTemplate)

    @Query("SELECT * FROM email_templates ORDER BY title ASC")
    fun getAllEmailTemplates(): Flow<List<EmailTemplate>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEmailTemplates(templates: List<EmailTemplate>)
}

@Dao
interface AuditLogDao {
    @Query("SELECT * FROM audit_logs ORDER BY timestamp DESC LIMIT 300")
    fun getAllAuditLogs(): Flow<List<AuditLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAuditLog(log: AuditLog)
}

@Dao
interface NotificationDao {
    @Query("SELECT * FROM app_notifications WHERE userId = :userId OR userId = 'ALL' ORDER BY timestamp DESC")
    fun getNotificationsForUser(userId: String): Flow<List<AppNotification>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(notification: AppNotification)

    @Query("UPDATE app_notifications SET isRead = 1 WHERE id = :id")
    suspend fun markAsRead(id: Long)
}
