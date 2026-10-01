package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("City Nexa CRM", appName)
    }

    @Test
    fun `lead pipeline status progression order`() {
        assertTrue(LeadStatus.NEW_LEAD.order < LeadStatus.CONTACTED.order)
        assertTrue(LeadStatus.CONTACTED.order < LeadStatus.QUALIFIED.order)
        assertTrue(LeadStatus.QUALIFIED.order < LeadStatus.SITE_VISIT_PLANNED.order)
        assertTrue(LeadStatus.SITE_VISIT_DONE.order < LeadStatus.BOOKING.order)
    }

    @Test
    fun `user role hierarchy verification`() {
        assertTrue(UserRole.SUPER_ADMIN.level > UserRole.ADMIN.level)
        assertTrue(UserRole.ADMIN.level > UserRole.SALES_MANAGER.level)
        assertTrue(UserRole.SALES_MANAGER.level > UserRole.SALES_EXECUTIVE.level)
    }
}
