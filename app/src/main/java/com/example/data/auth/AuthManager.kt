package com.example.data.auth

import com.example.data.model.User
import com.example.data.model.UserRole
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AuthManager {

    // Default to Rahul Verma (Sales Executive) as requested: "Priority 1. Mobile experience for Sales Executives"
    private val defaultUser = User(
        id = "USR-004",
        name = "Rahul Verma",
        email = "rahul.sales@citynexa.com",
        phone = "9880112266",
        role = UserRole.SALES_EXECUTIVE,
        managerId = "USR-003",
        designation = "Senior Relationship Executive"
    )

    private val _currentUser = MutableStateFlow<User?>(defaultUser)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    fun setCurrentUser(user: User?) {
        _currentUser.value = user
    }

    fun logout() {
        _currentUser.value = null
    }

    // Role-based permission checks
    fun canManageUsers(): Boolean {
        val role = _currentUser.value?.role ?: return false
        return role == UserRole.SUPER_ADMIN || role == UserRole.ADMIN
    }

    fun canManageMasterProjects(): Boolean {
        val role = _currentUser.value?.role ?: return false
        return role == UserRole.SUPER_ADMIN || role == UserRole.ADMIN
    }

    fun canManageMasterTemplates(): Boolean {
        val role = _currentUser.value?.role ?: return false
        return role == UserRole.SUPER_ADMIN || role == UserRole.ADMIN
    }

    fun canBulkImport(): Boolean {
        val role = _currentUser.value?.role ?: return false
        return role == UserRole.SUPER_ADMIN || role == UserRole.ADMIN || role == UserRole.SALES_MANAGER
    }

    fun canExportReports(): Boolean {
        val role = _currentUser.value?.role ?: return false
        return role == UserRole.SUPER_ADMIN || role == UserRole.ADMIN || role == UserRole.SALES_MANAGER
    }

    fun canViewAllLeads(): Boolean {
        val role = _currentUser.value?.role ?: return false
        return role == UserRole.SUPER_ADMIN || role == UserRole.ADMIN || role == UserRole.SALES_MANAGER
    }

    fun canViewAuditLogs(): Boolean {
        val role = _currentUser.value?.role ?: return false
        return role == UserRole.SUPER_ADMIN || role == UserRole.ADMIN
    }

    fun canAssignLeads(): Boolean {
        val role = _currentUser.value?.role ?: return false
        return role == UserRole.SUPER_ADMIN || role == UserRole.ADMIN || role == UserRole.SALES_MANAGER
    }
}
