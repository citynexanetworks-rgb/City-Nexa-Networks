package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.example.data.model.*

class Converters {
    @TypeConverter
    fun fromUserRole(value: UserRole): String = value.name

    @TypeConverter
    fun toUserRole(value: String): UserRole = try {
        UserRole.valueOf(value)
    } catch (e: Exception) {
        UserRole.SALES_EXECUTIVE
    }

    @TypeConverter
    fun fromLeadPriority(value: LeadPriority): String = value.name

    @TypeConverter
    fun toLeadPriority(value: String): LeadPriority = try {
        LeadPriority.valueOf(value)
    } catch (e: Exception) {
        LeadPriority.WARM
    }

    @TypeConverter
    fun fromLeadStatus(value: LeadStatus): String = value.name

    @TypeConverter
    fun toLeadStatus(value: String): LeadStatus = try {
        LeadStatus.valueOf(value)
    } catch (e: Exception) {
        LeadStatus.NEW_LEAD
    }

    @TypeConverter
    fun fromPropertyType(value: PropertyType): String = value.name

    @TypeConverter
    fun toPropertyType(value: String): PropertyType = try {
        PropertyType.valueOf(value)
    } catch (e: Exception) {
        PropertyType.APARTMENT
    }

    @TypeConverter
    fun fromActivityType(value: ActivityType): String = value.name

    @TypeConverter
    fun toActivityType(value: String): ActivityType = try {
        ActivityType.valueOf(value)
    } catch (e: Exception) {
        ActivityType.NOTE
    }

    @TypeConverter
    fun fromFollowUpType(value: FollowUpType): String = value.name

    @TypeConverter
    fun toFollowUpType(value: String): FollowUpType = try {
        FollowUpType.valueOf(value)
    } catch (e: Exception) {
        FollowUpType.CALL
    }

    @TypeConverter
    fun fromFollowUpStatus(value: FollowUpStatus): String = value.name

    @TypeConverter
    fun toFollowUpStatus(value: String): FollowUpStatus = try {
        FollowUpStatus.valueOf(value)
    } catch (e: Exception) {
        FollowUpStatus.PENDING
    }

    @TypeConverter
    fun fromUnitStatus(value: UnitStatus): String = value.name

    @TypeConverter
    fun toUnitStatus(value: String): UnitStatus = try {
        UnitStatus.valueOf(value)
    } catch (e: Exception) {
        UnitStatus.AVAILABLE
    }

    @TypeConverter
    fun fromSiteVisitStatus(value: SiteVisitStatus): String = value.name

    @TypeConverter
    fun toSiteVisitStatus(value: String): SiteVisitStatus = try {
        SiteVisitStatus.valueOf(value)
    } catch (e: Exception) {
        SiteVisitStatus.PLANNED
    }

    @TypeConverter
    fun fromBookingStatus(value: BookingStatus): String = value.name

    @TypeConverter
    fun toBookingStatus(value: String): BookingStatus = try {
        BookingStatus.valueOf(value)
    } catch (e: Exception) {
        BookingStatus.BOOKING_CONFIRMED
    }

    @TypeConverter
    fun fromCommissionStatus(value: CommissionStatus): String = value.name

    @TypeConverter
    fun toCommissionStatus(value: String): CommissionStatus = try {
        CommissionStatus.valueOf(value)
    } catch (e: Exception) {
        CommissionStatus.PENDING
    }
}

@Database(
    entities = [
        User::class,
        Lead::class,
        LeadActivity::class,
        FollowUp::class,
        Project::class,
        ProjectInventory::class,
        SiteVisit::class,
        Booking::class,
        WhatsAppTemplate::class,
        EmailTemplate::class,
        AuditLog::class,
        AppNotification::class
    ],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class CityNexaDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun leadDao(): LeadDao
    abstract fun leadActivityDao(): LeadActivityDao
    abstract fun followUpDao(): FollowUpDao
    abstract fun projectDao(): ProjectDao
    abstract fun projectInventoryDao(): ProjectInventoryDao
    abstract fun siteVisitDao(): SiteVisitDao
    abstract fun bookingDao(): BookingDao
    abstract fun templateDao(): TemplateDao
    abstract fun auditLogDao(): AuditLogDao
    abstract fun notificationDao(): NotificationDao

    companion object {
        @Volatile
        private var INSTANCE: CityNexaDatabase? = null

        fun getDatabase(context: Context): CityNexaDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    CityNexaDatabase::class.java,
                    "city_nexa_crm_db"
                ).fallbackToDestructiveMigration(true).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
