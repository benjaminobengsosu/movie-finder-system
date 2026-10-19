package com.example.data

import java.io.Serializable

const val CREATOR_ADMIN_EMAIL = "benjaminobengsosu@gmail.com"

data class SupportTicket(
    val id: String = "TK-" + (1000..9999).random(),
    val timestamp: Long = System.currentTimeMillis(),
    val userEmail: String,
    val userName: String,
    val category: String, // "Bug Report", "Recommendation Error", "Feature Request", "Account / Sync"
    val priority: String, // "Low", "Medium", "High", "Critical"
    val subject: String,
    val description: String,
    var status: String = "Open", // "Open", "In Progress", "Resolved"
    var adminReply: String? = null,
    val deviceModel: String = "Android Device (Live Session)"
) : Serializable

data class AnalyticsSearchEvent(
    val id: String = "EV-" + (10000..99999).random(),
    val timestamp: Long = System.currentTimeMillis(),
    val query: String,
    val searchType: String, // "TEXT", "IMAGE", "VIDEO"
    val userEmail: String? = "guest@moviefinder.com"
) : Serializable

data class AppErrorLog(
    val id: String = "ERR-" + (1000..9999).random(),
    val timestamp: Long = System.currentTimeMillis(),
    val tag: String,
    val message: String,
    val details: String? = null,
    val severity: String = "Error" // "Warning", "Error", "Fatal"
) : Serializable

data class RemoteConfigData(
    var enableGuestMode: Boolean = true,
    var showCarousels: Boolean = true,
    var enableAiTasteGenerator: Boolean = true,
    var maintenanceMode: Boolean = false,
    var announcementText: String = "⚡ Welcome to Movie Finder Admin Edition! Dynamic Remote Config Active.",
    var featuredMovieTitle: String = "Interstellar"
) : Serializable
