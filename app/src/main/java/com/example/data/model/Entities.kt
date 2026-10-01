package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class UserRole(val displayName: String, val level: Int) {
    SUPER_ADMIN("MD / Super Admin", 4),
    ADMIN("Admin", 3),
    SALES_MANAGER("Sales Manager", 2),
    SALES_EXECUTIVE("Sales Executive", 1)
}

@Entity(tableName = "users")
data class User(
    @PrimaryKey val id: String,
    val name: String,
    val email: String,
    val phone: String,
    val role: UserRole,
    val managerId: String? = null,
    val isActive: Boolean = true,
    val designation: String = "",
    val avatarUrl: String = ""
)

enum class LeadPriority {
    HOT, WARM, COLD
}

enum class LeadStatus(val label: String, val order: Int) {
    NEW_LEAD("New Lead", 1),
    ASSIGNED("Assigned", 2),
    CONTACTED("Contacted", 3),
    QUALIFIED("Qualified", 4),
    PROJECT_SHARED("Project Shared", 5),
    SITE_VISIT_PLANNED("Site Visit Planned", 6),
    SITE_VISIT_DONE("Site Visit Done", 7),
    NEGOTIATION("Negotiation", 8),
    BOOKING("Booking", 9),
    CLOSED_LOST("Closed / Lost", 10)
}

enum class PropertyType(val label: String) {
    APARTMENT("Apartment"),
    VILLA("Villa / Row House"),
    PLOT("Residential Plot"),
    COMMERCIAL("Commercial / Retail"),
    PENTHOUSE("Luxury Penthouse"),
    RESALE("Resale Property")
}

@Entity(tableName = "leads")
data class Lead(
    @PrimaryKey val id: String, // e.g. NX-1001
    val name: String,
    val mobile: String,
    val whatsapp: String,
    val email: String,
    val alternativeNumber: String = "",
    val source: String, // Meta Ads, Google Ads, 99acres, MagicBricks, Referral, etc.
    val interestedProjectId: String? = null,
    val propertyType: PropertyType = PropertyType.APARTMENT,
    val budgetMin: Double, // in Lakhs/Crores, e.g. 80.0
    val budgetMax: Double, // e.g. 150.0 (in Lakhs)
    val preferredLocation: String,
    val bhk: String, // "1 BHK", "2 BHK", "3 BHK", "4 BHK", "Villa Plot"
    val sizeRequirementSqFt: String = "1200 - 1800 sq.ft",
    val purpose: String = "End Use", // "End Use" or "Investment"
    val requirementNotes: String = "",
    val status: LeadStatus = LeadStatus.NEW_LEAD,
    val priority: LeadPriority = LeadPriority.WARM,
    val assignedSalespersonId: String,
    val salesManagerId: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val lastContactedAt: Long? = null,
    val nextFollowUpDate: String = "", // YYYY-MM-DD
    val nextFollowUpTime: String = "", // HH:mm
    val lastRemark: String = "",
    val isDeleted: Boolean = false
)

enum class ActivityType {
    CALL, REMARK_ADDED, STATUS_CHANGED, PROJECT_SHARED, FOLLOW_UP_CREATED, SITE_VISIT_PLANNED, SITE_VISIT_UPDATED, BOOKING_CREATED, EMAIL_SENT, WHATSAPP_SENT, NOTE
}

@Entity(tableName = "lead_activities")
data class LeadActivity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val leadId: String,
    val userId: String,
    val userName: String,
    val type: ActivityType,
    val title: String,
    val description: String,
    val timestamp: Long = System.currentTimeMillis(),
    val durationSeconds: Int = 0,
    val callOutcome: String? = null,
    val projectId: String? = null,
    val metadata: String = ""
)

enum class FollowUpType(val label: String) {
    CALL("Call"),
    WHATSAPP("WhatsApp"),
    MEETING("Meeting"),
    SITE_VISIT("Site Visit"),
    NEGOTIATION("Negotiation"),
    PAYMENT("Payment"),
    BOOKING("Booking")
}

enum class FollowUpStatus {
    PENDING, COMPLETED, RESCHEDULED, CANCELLED
}

@Entity(tableName = "follow_ups")
data class FollowUp(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val leadId: String,
    val leadName: String,
    val customerPhone: String,
    val projectId: String? = null,
    val projectName: String? = null,
    val salespersonId: String,
    val salespersonName: String,
    val dueDate: String, // YYYY-MM-DD
    val dueTime: String, // HH:mm
    val type: FollowUpType,
    val notes: String,
    val status: FollowUpStatus = FollowUpStatus.PENDING,
    val createdAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null
)

@Entity(tableName = "projects")
data class Project(
    @PrimaryKey val id: String, // NX-PRJ-01
    val name: String,
    val developer: String,
    val location: String,
    val address: String,
    val reraNumber: String,
    val projectType: PropertyType,
    val priceMinLakhs: Double, // in Lakhs (e.g. 75.0)
    val priceMaxLakhs: Double, // in Lakhs (e.g. 210.0)
    val totalUnits: Int,
    val availableUnits: Int,
    val configurations: String, // "2, 3 & 4 BHK Luxury Apartments"
    val sizeRangeSqFt: String, // "1150 - 2650 sq.ft"
    val plotSize: String = "8.5 Acres",
    val towers: Int = 4,
    val floors: Int = 22,
    val possessionDate: String = "Dec 2027",
    val projectStatus: String = "Under Construction", // Ready to Move, Under Construction, Pre-Launch
    val amenities: String, // comma-separated: Clubhouse, Infinity Pool, Gym, Tennis Court, Children Play Area, Jogging Track
    val imageDrawableRes: String = "img_project_tower_1790708814742",
    val brochurePdfName: String = "CityNexa_Brochure.pdf",
    val priceSheetPdfName: String = "CityNexa_PriceSheet.pdf",
    val videoUrl: String = "https://youtu.be/citynexa_walkthrough",
    val googleMapsLink: String = "https://maps.google.com/?q=City+Nexa+Residences",
    val nearbyLandmarks: String = "5 mins to Metro Station, 10 mins to IT Tech Park, Top International Schools within 3 km",
    val reraVerified: Boolean = true,
    val isResale: Boolean = false,
    val resaleOwnerName: String = "",
    val propertyAgeYears: Int = 0
)

enum class UnitStatus {
    AVAILABLE, HOLD, BOOKED, SOLD
}

@Entity(tableName = "project_inventory")
data class ProjectInventory(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val projectId: String,
    val unitNumber: String, // e.g. T1-1204
    val tower: String,
    val floor: Int,
    val configuration: String, // 3 BHK + 3T
    val sizeSqFt: Int,
    val priceLakhs: Double,
    val status: UnitStatus = UnitStatus.AVAILABLE,
    val facing: String = "East"
)

enum class SiteVisitStatus(val label: String) {
    PLANNED("Planned"),
    CONFIRMED("Confirmed"),
    COMPLETED("Completed"),
    RESCHEDULED("Rescheduled"),
    CANCELLED("Cancelled"),
    NO_SHOW("No Show")
}

@Entity(tableName = "site_visits")
data class SiteVisit(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val leadId: String,
    val leadName: String,
    val customerPhone: String,
    val projectId: String,
    val projectName: String,
    val visitDate: String, // YYYY-MM-DD
    val visitTime: String, // HH:mm
    val salespersonId: String,
    val salespersonName: String,
    val numberOfVisitors: Int = 2,
    val status: SiteVisitStatus = SiteVisitStatus.PLANNED,
    val remarks: String = "",
    val nextAction: String = "",
    val cabRequired: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

enum class BookingStatus(val label: String) {
    BOOKING_INITIATED("Booking Initiated"),
    BOOKING_CONFIRMED("Booking Confirmed"),
    AGREEMENT_PENDING("Agreement Pending"),
    AGREEMENT_COMPLETED("Agreement Completed"),
    PAYMENT_PENDING("Payment Pending"),
    PAYMENT_COMPLETED("Payment Completed"),
    CLOSED("Closed / Registered")
}

enum class CommissionStatus(val label: String) {
    PENDING("Pending"),
    INVOICE_RAISED("Invoice Raised"),
    APPROVED("Approved"),
    RECEIVED("Received")
}

@Entity(tableName = "bookings")
data class Booking(
    @PrimaryKey val id: String, // e.g. BK-901
    val leadId: String,
    val customerName: String,
    val customerPhone: String,
    val projectId: String,
    val projectName: String,
    val unitNumber: String,
    val configuration: String,
    val salespersonId: String,
    val salespersonName: String,
    val bookingDate: String, // YYYY-MM-DD
    val totalDealValueLakhs: Double, // e.g. 125.0 Lakhs
    val bookingAmountPaidLakhs: Double, // e.g. 5.0 Lakhs
    val bookingStatus: BookingStatus = BookingStatus.BOOKING_CONFIRMED,
    val paymentMode: String = "NEFT / Cheque",
    val paymentReference: String = "TXN-8874129",
    val agreementStatus: String = "Agreement Under Review",
    val commissionPercent: Double = 2.0, // 2%
    val commissionAmount: Double = 2.5, // 2.5 Lakhs
    val commissionStatus: CommissionStatus = CommissionStatus.PENDING,
    val expectedCommissionDate: String = "2026-10-30",
    val receivedCommissionLakhs: Double = 0.0,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "whatsapp_templates")
data class WhatsAppTemplate(
    @PrimaryKey val id: String,
    val title: String,
    val category: String, // Project Details, Follow-up, Not Connected, Not Interested, Site Visit Confirmation, Site Visit Reminder, Booking Follow-up, Thank You, Brochure Sharing
    val messageTemplate: String,
    val isSystemDefault: Boolean = true
)

@Entity(tableName = "email_templates")
data class EmailTemplate(
    @PrimaryKey val id: String,
    val title: String,
    val subject: String,
    val body: String
)

@Entity(tableName = "audit_logs")
data class AuditLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: String,
    val userName: String,
    val userRole: String,
    val action: String, // e.g. "LEAD_ASSIGNED", "STATUS_UPDATED", "PROJECT_SHARED", "BOOKING_CREATED", "BULK_IMPORT"
    val recordType: String, // "Lead", "Project", "Booking", "FollowUp", "SiteVisit"
    val recordId: String,
    val previousValue: String = "",
    val newValue: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "app_notifications")
data class AppNotification(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: String,
    val title: String,
    val message: String,
    val type: String, // "NEW_LEAD", "FOLLOW_UP_DUE", "SITE_VISIT", "BOOKING", "SYSTEM"
    val referenceId: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false
)
