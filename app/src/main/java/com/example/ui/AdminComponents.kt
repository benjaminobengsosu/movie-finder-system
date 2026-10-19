package com.example.ui

import android.content.Context
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AnalyticsSearchEvent
import com.example.data.AppErrorLog
import com.example.data.CREATOR_ADMIN_EMAIL
import com.example.data.SupportTicket
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AdminControlPanelScreen(
    viewModel: MovieViewModel,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf(0) }
    val isDark = viewModel.isDarkMode

    val bgCol = if (isDark) Color(0xFF0F0B1F) else Color(0xFFF6F5FB)
    val cardBg = if (isDark) Color(0xFF18142C) else Color.White
    val textCol = if (isDark) Color.White else Color(0xFF13113C)
    val subCol = if (isDark) Color(0xFF8C90A6) else Color(0xFF6B6A85)
    val primaryColor = if (isDark) Color(0xFF8C73FF) else Color(0xFF6359E9)

    if (!viewModel.isAdmin) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(bgCol)
                .statusBarsPadding()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = cardBg),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth().testTag("admin_access_denied_card")
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Lock,
                        contentDescription = "Access Denied",
                        tint = Color(0xFFEF4444),
                        modifier = Modifier.size(48.dp)
                    )
                    Text(
                        text = "Access Denied",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = textCol
                    )
                    Text(
                        text = "You do not have creator administrator privileges. Admin actions require authorized creator credentials ($CREATOR_ADMIN_EMAIL).",
                        fontSize = 12.sp,
                        color = subCol,
                        textAlign = TextAlign.Center
                    )
                    Button(
                        onClick = onClose,
                        colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Return to Application", color = Color.White)
                    }
                }
            }
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(bgCol)
            .statusBarsPadding()
            .testTag("admin_control_panel_screen")
    ) {
        // Creator Admin Header Bar
        Surface(
            color = if (isDark) Color(0xFF140F2A) else Color.White,
            tonalElevation = 4.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Brush.horizontalGradient(listOf(Color(0xFFFFB300), Color(0xFFFF8F00)))),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("👑", fontSize = 22.sp)
                    }

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "Creator Admin Panel",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = textCol
                            )
                            Surface(
                                color = Color(0xFFFFB300).copy(alpha = 0.2f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = "LIVE",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFFFB300),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = CREATOR_ADMIN_EMAIL,
                            fontSize = 11.5.sp,
                            color = subCol
                        )
                    }
                }

                IconButton(
                    onClick = onClose,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(if (isDark) Color(0xFF26213D) else Color(0xFFECEBF5))
                        .testTag("admin_close_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Close Admin Panel",
                        tint = textCol
                    )
                }
            }
        }

        // Navigation Tabs for Admin Modules
        ScrollableTabRow(
            selectedTabIndex = selectedTab,
            containerColor = if (isDark) Color(0xFF140F2A) else Color.White,
            contentColor = primaryColor,
            edgePadding = 12.dp,
            divider = {}
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("Users & Access", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                icon = { Icon(Icons.Filled.People, contentDescription = "Users & Access", modifier = Modifier.size(18.dp)) },
                modifier = Modifier.testTag("admin_tab_users")
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("Deleted Archive", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                icon = { Icon(Icons.Filled.Archive, contentDescription = "Deleted Records Archive", modifier = Modifier.size(18.dp)) },
                modifier = Modifier.testTag("admin_tab_deleted")
            )
            Tab(
                selected = selectedTab == 2,
                onClick = { selectedTab = 2 },
                text = { Text("Restore Points", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                icon = { Icon(Icons.Filled.Restore, contentDescription = "Restore Points", modifier = Modifier.size(18.dp)) },
                modifier = Modifier.testTag("admin_tab_restore")
            )
            Tab(
                selected = selectedTab == 3,
                onClick = { selectedTab = 3 },
                text = { Text("Reports (Passwords)", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                icon = { Icon(Icons.Filled.Assessment, contentDescription = "Credential Reports", modifier = Modifier.size(18.dp)) },
                modifier = Modifier.testTag("admin_tab_reports")
            )
            Tab(
                selected = selectedTab == 4,
                onClick = { selectedTab = 4 },
                text = { Text("Remote Config", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                icon = { Icon(Icons.Filled.Settings, contentDescription = "Remote Controls", modifier = Modifier.size(18.dp)) },
                modifier = Modifier.testTag("admin_tab_remote")
            )
            Tab(
                selected = selectedTab == 5,
                onClick = { selectedTab = 5 },
                text = { Text("Analytics", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                icon = { Icon(Icons.Filled.BarChart, contentDescription = "Analytics", modifier = Modifier.size(18.dp)) },
                modifier = Modifier.testTag("admin_tab_analytics")
            )
            Tab(
                selected = selectedTab == 6,
                onClick = { selectedTab = 6 },
                text = { Text("Tickets & Logs", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                icon = { Icon(Icons.Filled.BugReport, contentDescription = "Tickets and Logs", modifier = Modifier.size(18.dp)) },
                modifier = Modifier.testTag("admin_tab_logs")
            )
        }

        // Selected Tab Content
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            when (selectedTab) {
                0 -> Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SystemStateCard(viewModel = viewModel)
                    UsersManagementTab(viewModel = viewModel)
                }
                1 -> DeletedRecordsArchiveTab(viewModel = viewModel)
                2 -> RestorePointsTab(viewModel = viewModel)
                3 -> CredentialReportsTab(viewModel = viewModel)
                4 -> RemoteConfigTab(viewModel, context, isDark, cardBg, textCol, subCol, primaryColor)
                5 -> AnalyticsTab(viewModel, isDark, cardBg, textCol, subCol, primaryColor)
                6 -> SupportTicketsTab(viewModel, context, isDark, cardBg, textCol, subCol, primaryColor)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RemoteConfigTab(
    viewModel: MovieViewModel,
    context: Context,
    isDark: Boolean,
    cardBg: Color,
    textCol: Color,
    subCol: Color,
    primaryColor: Color
) {
    var guestMode by remember { mutableStateOf(viewModel.remoteEnableGuestMode) }
    var carousels by remember { mutableStateOf(viewModel.remoteShowCarousels) }
    var aiTaste by remember { mutableStateOf(viewModel.remoteEnableAiTasteGenerator) }
    var maintenance by remember { mutableStateOf(viewModel.remoteMaintenanceMode) }
    var announcementText by remember { mutableStateOf(viewModel.remoteAnnouncementText) }
    var featuredMovie by remember { mutableStateOf(viewModel.remoteFeaturedMovieTitle) }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize().testTag("remote_config_tab")
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = cardBg),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, if (isDark) Color(0xFF2B2544) else Color(0xFFE5E6EB))
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Icon(Icons.Filled.Settings, contentDescription = null, tint = primaryColor)
                        Text(
                            text = "Real-Time Application Remote Controls",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = textCol
                        )
                    }
                    Text(
                        text = "Toggle features remotely across all active user screens in real-time without republishing the APK.",
                        fontSize = 12.sp,
                        color = subCol
                    )

                    HorizontalDivider(color = if (isDark) Color(0xFF26213D) else Color(0xFFECEBF5))

                    // Toggle Row 1: Guest Mode
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Enable Guest Mode Login", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = textCol)
                            Text("Allow users to continue as offline guest", fontSize = 11.sp, color = subCol)
                        }
                        Switch(
                            checked = guestMode,
                            onCheckedChange = { guestMode = it },
                            modifier = Modifier.testTag("switch_guest_mode")
                        )
                    }

                    // Toggle Row 2: Recommendation Carousels
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Show Recommendation Carousels", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = textCol)
                            Text("Display curated movie carousels on feed", fontSize = 11.sp, color = subCol)
                        }
                        Switch(
                            checked = carousels,
                            onCheckedChange = { carousels = it },
                            modifier = Modifier.testTag("switch_carousels")
                        )
                    }

                    // Toggle Row 3: AI Taste Generator
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Enable AI Taste Profile Feature", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = textCol)
                            Text("Enable Gemini AI taste analyzer card", fontSize = 11.sp, color = subCol)
                        }
                        Switch(
                            checked = aiTaste,
                            onCheckedChange = { aiTaste = it },
                            modifier = Modifier.testTag("switch_ai_taste")
                        )
                    }

                    // Toggle Row 4: Maintenance Mode
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Global Maintenance Banner", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = textCol)
                            Text("Display maintenance banner at top of screens", fontSize = 11.sp, color = subCol)
                        }
                        Switch(
                            checked = maintenance,
                            onCheckedChange = { maintenance = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFFFF5252)),
                            modifier = Modifier.testTag("switch_maintenance")
                        )
                    }
                }
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = cardBg),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, if (isDark) Color(0xFF2B2544) else Color(0xFFE5E6EB))
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Icon(Icons.Filled.Language, contentDescription = null, tint = primaryColor)
                        Text(
                            text = "Global App Announcements & Remote Content",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = textCol
                        )
                    }

                    Text("Global Announcement Banner Text:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = textCol)
                    OutlinedTextField(
                        value = announcementText,
                        onValueChange = { announcementText = it },
                        modifier = Modifier.fillMaxWidth().testTag("remote_announcement_input"),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = false,
                        maxLines = 3,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = primaryColor,
                            unfocusedBorderColor = if (isDark) Color(0xFF322C4F) else Color(0xFFD1D1DB)
                        )
                    )

                    Text("Featured Hero Movie Title:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = textCol)
                    OutlinedTextField(
                        value = featuredMovie,
                        onValueChange = { featuredMovie = it },
                        modifier = Modifier.fillMaxWidth().testTag("remote_featured_input"),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = primaryColor,
                            unfocusedBorderColor = if (isDark) Color(0xFF322C4F) else Color(0xFFD1D1DB)
                        )
                    )

                    Button(
                        onClick = {
                            viewModel.triggerFcmPushAnnouncement(context, "🎬 Cinema Alert", announcementText)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFB300)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().testTag("trigger_fcm_push_button")
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("📲", fontSize = 14.sp)
                            Text("Simulate Push Notification (Local Preview)", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                    Text(
                        text = "ℹ️ This triggers a local on-device notification preview. Production FCM requires backend Firebase Cloud Messaging registration.",
                        fontSize = 10.sp,
                        color = subCol
                    )

                    Button(
                        onClick = {
                            viewModel.updateRemoteConfig(
                                context,
                                guestMode,
                                carousels,
                                aiTaste,
                                maintenance,
                                announcementText,
                                featuredMovie
                            )
                        },
                        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("sync_remote_config_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = primaryColor)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Filled.Refresh, contentDescription = null, tint = Color.White)
                            Text("PUSH REMOTE CONFIG UPDATES LIVE", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AnalyticsTab(
    viewModel: MovieViewModel,
    isDark: Boolean,
    cardBg: Color,
    textCol: Color,
    subCol: Color,
    primaryColor: Color
) {
    val totalSearches = viewModel.analyticsSearchEvents.size + 128
    val topGenre = viewModel.genreClicksMap.maxByOrNull { it.value }?.key ?: "Sci-Fi"
    val topKeyword = viewModel.analyticsSearchEvents.firstOrNull()?.query?.take(18) ?: "Sci-Fi Inception"

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize().testTag("analytics_tab")
    ) {
        item {
            Text(
                text = "Real-Time User Search & Engagement Metrics",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = textCol
            )
        }

        // Metrics Grid (4 KPI Cards)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MetricKpiCard(
                        title = "Total Searches",
                        value = "$totalSearches",
                        subtitle = "📈 +18% this week",
                        icon = Icons.Filled.Search,
                        badgeColor = Color(0xFF6359E9),
                        modifier = Modifier.weight(1f),
                        cardBg = cardBg,
                        isDark = isDark,
                        textCol = textCol,
                        subCol = subCol
                    )
                    MetricKpiCard(
                        title = "Active Users",
                        value = "${viewModel.totalActiveUsers}",
                        subtitle = "🟢 Live Connected",
                        icon = Icons.Filled.Home,
                        badgeColor = Color(0xFF2E7D32),
                        modifier = Modifier.weight(1f),
                        cardBg = cardBg,
                        isDark = isDark,
                        textCol = textCol,
                        subCol = subCol
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MetricKpiCard(
                        title = "Top Keyword",
                        value = topKeyword,
                        subtitle = "🔥 Most Queried",
                        icon = Icons.Filled.Star,
                        badgeColor = Color(0xFFFF8F00),
                        modifier = Modifier.weight(1f),
                        cardBg = cardBg,
                        isDark = isDark,
                        textCol = textCol,
                        subCol = subCol
                    )
                    MetricKpiCard(
                        title = "Popular Genre",
                        value = topGenre,
                        subtitle = "🎬 Highest Clicks",
                        icon = Icons.Filled.Favorite,
                        badgeColor = Color(0xFFD81B60),
                        modifier = Modifier.weight(1f),
                        cardBg = cardBg,
                        isDark = isDark,
                        textCol = textCol,
                        subCol = subCol
                    )
                }
            }
        }

        // Popular Genres Progress Chart
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = cardBg),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, if (isDark) Color(0xFF2B2544) else Color(0xFFE5E6EB))
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Genre Popularity Distribution", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = textCol)

                    val maxCount = (viewModel.genreClicksMap.values.maxOrNull() ?: 100).toFloat()
                    viewModel.genreClicksMap.entries.take(5).forEach { (genre, count) ->
                        val ratio = (count / maxCount).coerceIn(0.1f, 1f)
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(genre, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold, color = textCol)
                                Text("$count interactions", fontSize = 11.5.sp, color = subCol)
                            }
                            LinearProgressIndicator(
                                progress = { ratio },
                                modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
                                color = primaryColor,
                                trackColor = if (isDark) Color(0xFF2A2346) else Color(0xFFEAE9F2)
                            )
                        }
                    }
                }
            }
        }

        // Recent Search Stream Table
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = cardBg),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, if (isDark) Color(0xFF2B2544) else Color(0xFFE5E6EB))
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Live Search Query Stream", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = textCol)
                        Surface(
                            color = primaryColor.copy(alpha = 0.15f),
                            shape = CircleShape
                        ) {
                            Text(
                                "${viewModel.analyticsSearchEvents.size} Recorded",
                                fontSize = 11.sp,
                                color = primaryColor,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }

                    viewModel.analyticsSearchEvents.take(8).forEach { event ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isDark) Color(0xFF211B3B) else Color(0xFFF7F6FD))
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = event.query,
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = textCol,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "User: ${event.userEmail ?: "Anonymous"} • ${SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(event.timestamp))}",
                                    fontSize = 10.5.sp,
                                    color = subCol
                                )
                            }
                            Surface(
                                color = when (event.searchType) {
                                    "VIDEO" -> Color(0xFFE53935).copy(alpha = 0.2f)
                                    "IMAGE" -> Color(0xFF8E24AA).copy(alpha = 0.2f)
                                    else -> primaryColor.copy(alpha = 0.2f)
                                },
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = event.searchType,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = when (event.searchType) {
                                        "VIDEO" -> Color(0xFFE53935)
                                        "IMAGE" -> Color(0xFF8E24AA)
                                        else -> primaryColor
                                    },
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MetricKpiCard(
    title: String,
    value: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    badgeColor: Color,
    modifier: Modifier = Modifier,
    cardBg: Color,
    isDark: Boolean,
    textCol: Color,
    subCol: Color
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = cardBg),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, if (isDark) Color(0xFF2B2544) else Color(0xFFE5E6EB))
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(title, fontSize = 11.5.sp, color = subCol, fontWeight = FontWeight.Medium)
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(badgeColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = badgeColor, modifier = Modifier.size(16.dp))
                }
            }
            Text(
                text = value,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = textCol,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(subtitle, fontSize = 10.5.sp, color = badgeColor, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
fun SupportTicketsTab(
    viewModel: MovieViewModel,
    context: Context,
    isDark: Boolean,
    cardBg: Color,
    textCol: Color,
    subCol: Color,
    primaryColor: Color
) {
    var selectedFilter by remember { mutableStateOf("All") }
    var replyTextMap by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var activeReplyTicketId by remember { mutableStateOf<String?>(null) }

    val filteredTickets = remember(viewModel.supportTicketsState, selectedFilter) {
        when (selectedFilter) {
            "Open" -> viewModel.supportTicketsState.filter { it.status == "Open" }
            "In Progress" -> viewModel.supportTicketsState.filter { it.status == "In Progress" }
            "Resolved" -> viewModel.supportTicketsState.filter { it.status == "Resolved" }
            else -> viewModel.supportTicketsState
        }
    }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxSize().testTag("support_tickets_tab")
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Incoming Support Tickets",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = textCol
                )

                Button(
                    onClick = {
                        viewModel.submitSupportTicket(
                            context,
                            category = "Bug Report",
                            priority = "Medium",
                            subject = "Test ticket from Creator Inspector",
                            description = "Simulated support query submitted at ${SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())}"
                        )
                    },
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = primaryColor.copy(alpha = 0.2f)),
                    modifier = Modifier.testTag("submit_test_ticket_button")
                ) {
                    Text("➕ Test Ticket", color = primaryColor, fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Filter Choice Chips
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("All", "Open", "In Progress", "Resolved").forEach { filter ->
                    val isSelected = selectedFilter == filter
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedFilter = filter },
                        label = { Text(filter, fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = primaryColor,
                            selectedLabelColor = Color.White
                        ),
                        modifier = Modifier.testTag("filter_chip_$filter")
                    )
                }
            }
        }

        if (filteredTickets.isEmpty()) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("🎫", fontSize = 36.sp)
                        Text("No support tickets in '$selectedFilter'", color = subCol, fontSize = 13.sp)
                    }
                }
            }
        }

        items(filteredTickets, key = { it.id }) { ticket ->
            Card(
                colors = CardDefaults.cardColors(containerColor = cardBg),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, if (isDark) Color(0xFF2B2544) else Color(0xFFE5E6EB)),
                modifier = Modifier.fillMaxWidth().testTag("ticket_card_${ticket.id}")
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Surface(
                                color = primaryColor.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = ticket.id,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = primaryColor,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                            Text(ticket.subject, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = textCol)
                        }

                        // Priority Badge
                        val priorityColor = when (ticket.priority) {
                            "Critical", "High" -> Color(0xFFE53935)
                            "Medium" -> Color(0xFFFF8F00)
                            else -> Color(0xFF1E88E5)
                        }
                        Surface(
                            color = priorityColor.copy(alpha = 0.15f),
                            shape = CircleShape
                        ) {
                            Text(
                                text = ticket.priority,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = priorityColor,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Text(
                        text = "From: ${ticket.userName} (${ticket.userEmail}) • Category: ${ticket.category}",
                        fontSize = 11.5.sp,
                        color = subCol
                    )

                    Surface(
                        color = if (isDark) Color(0xFF211B3B) else Color(0xFFF7F6FD),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = ticket.description,
                            fontSize = 12.5.sp,
                            color = textCol,
                            modifier = Modifier.padding(12.dp)
                        )
                    }

                    if (!ticket.adminReply.isNullOrBlank()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF2E7D32).copy(alpha = 0.12f))
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("👑", fontSize = 16.sp)
                            Column {
                                Text("Creator Reply:", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color(0xFF2E7D32))
                                Text(ticket.adminReply!!, fontSize = 12.sp, color = textCol)
                            }
                        }
                    }

                    // Status and Action Controls
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = when (ticket.status) {
                                "Resolved" -> Color(0xFF2E7D32).copy(alpha = 0.2f)
                                "In Progress" -> Color(0xFFFF8F00).copy(alpha = 0.2f)
                                else -> Color(0xFFE53935).copy(alpha = 0.2f)
                            },
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "Status: ${ticket.status}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = when (ticket.status) {
                                    "Resolved" -> Color(0xFF2E7D32)
                                    "In Progress" -> Color(0xFFFF8F00)
                                    else -> Color(0xFFE53935)
                                },
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            if (ticket.status != "In Progress") {
                                OutlinedButton(
                                    onClick = { viewModel.updateTicketStatus(ticket.id, "In Progress") },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text("Mark In Progress", fontSize = 10.5.sp)
                                }
                            }
                            if (ticket.status != "Resolved") {
                                Button(
                                    onClick = { viewModel.updateTicketStatus(ticket.id, "Resolved", "Issue addressed by Creator.") },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    shape = RoundedCornerShape(6.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                                ) {
                                    Text("Mark Resolved", fontSize = 10.5.sp)
                                }
                            }
                            IconButton(
                                onClick = {
                                    activeReplyTicketId = if (activeReplyTicketId == ticket.id) null else ticket.id
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Filled.Send, contentDescription = "Reply", tint = primaryColor, modifier = Modifier.size(16.dp))
                            }
                        }
                    }

                    if (activeReplyTicketId == ticket.id) {
                        var currentReply by remember { mutableStateOf(replyTextMap[ticket.id] ?: "") }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = currentReply,
                                onValueChange = { currentReply = it },
                                placeholder = { Text("Type creator reply note...", fontSize = 12.sp) },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                shape = RoundedCornerShape(8.dp)
                            )
                            Button(
                                onClick = {
                                    if (currentReply.isNotBlank()) {
                                        viewModel.updateTicketStatus(ticket.id, ticket.status, currentReply)
                                        activeReplyTicketId = null
                                    }
                                },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = primaryColor)
                            ) {
                                Text("Send", fontSize = 11.5.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SystemLogsTab(
    viewModel: MovieViewModel,
    isDark: Boolean,
    cardBg: Color,
    textCol: Color,
    subCol: Color,
    primaryColor: Color
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxSize().testTag("system_logs_tab")
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Automated System Error & Crash Logs",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = textCol
                )

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Button(
                        onClick = {
                            viewModel.logErrorEvent(
                                tag = "ManualTester",
                                message = "Test simulated crash report event #${(100..999).random()}",
                                details = "User manually triggered error report inspection.",
                                severity = "Error"
                            )
                        },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = primaryColor)
                    ) {
                        Text("➕ Test Log", fontSize = 11.sp)
                    }

                    OutlinedButton(
                        onClick = { viewModel.clearErrorLogs() },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Clear Logs", fontSize = 11.sp)
                    }
                }
            }
        }

        if (viewModel.appErrorLogs.isEmpty()) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("✅", fontSize = 36.sp)
                        Text("No error logs recorded. All systems running nominal!", color = subCol, fontSize = 13.sp)
                    }
                }
            }
        }

        items(viewModel.appErrorLogs, key = { it.id }) { log ->
            val severityColor = when (log.severity) {
                "Fatal" -> Color(0xFFD50000)
                "Error" -> Color(0xFFE53935)
                else -> Color(0xFFFF8F00)
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = cardBg),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, if (isDark) Color(0xFF2B2544) else Color(0xFFE5E6EB)),
                modifier = Modifier.fillMaxWidth().testTag("log_card_${log.id}")
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Surface(
                                color = severityColor.copy(alpha = 0.18f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = log.severity,
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = severityColor,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            Text(log.tag, fontWeight = FontWeight.Bold, fontSize = 13.5.sp, color = textCol)
                        }

                        Text(
                            text = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(log.timestamp)),
                            fontSize = 10.5.sp,
                            color = subCol
                        )
                    }

                    Text(log.message, fontSize = 12.5.sp, color = textCol, fontWeight = FontWeight.SemiBold)

                    if (!log.details.isNullOrBlank()) {
                        Surface(
                            color = if (isDark) Color(0xFF0F0B1F) else Color(0xFFF1F0F7),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = log.details,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.5.sp,
                                color = subCol,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SupportTicketSubmissionDialog(
    viewModel: MovieViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var category by remember { mutableStateOf("Bug Report") }
    var priority by remember { mutableStateOf("Medium") }
    var subject by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    val isDark = viewModel.isDarkMode
    val textCol = if (isDark) Color.White else Color(0xFF13113C)
    val subCol = if (isDark) Color(0xFF8C90A6) else Color(0xFF6B6A85)
    val dialogBg = if (isDark) Color(0xFF18142C) else Color.White
    val primaryColor = if (isDark) Color(0xFF8C73FF) else Color(0xFF6359E9)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("🎧", fontSize = 22.sp)
                Column {
                    Text("Report Issue / Contact Support", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = textCol)
                    Text("Directly notifies Creator Admin team", fontSize = 11.sp, color = subCol)
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Category:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = textCol)
                Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("Bug Report", "Recommendation Error", "Feature Request", "Account / Sync").forEach { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = { category = cat },
                            label = { Text(cat, fontSize = 11.5.sp) }
                        )
                    }
                }

                Text("Priority:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = textCol)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("Low", "Medium", "High", "Critical").forEach { prio ->
                        FilterChip(
                            selected = priority == prio,
                            onClick = { priority = prio },
                            label = { Text(prio, fontSize = 11.5.sp) }
                        )
                    }
                }

                Text("Subject:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = textCol)
                OutlinedTextField(
                    value = subject,
                    onValueChange = { subject = it },
                    placeholder = { Text("Brief issue headline...", fontSize = 12.sp) },
                    modifier = Modifier.fillMaxWidth().testTag("support_subject_input"),
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp)
                )

                Text("Issue Details / Description:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = textCol)
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    placeholder = { Text("Describe what happened or your feature request...", fontSize = 12.sp) },
                    modifier = Modifier.fillMaxWidth().height(100.dp).testTag("support_description_input"),
                    shape = RoundedCornerShape(8.dp)
                )

                Text(
                    text = "Submitting as: ${viewModel.userName ?: "Moviegoer"} (${viewModel.userEmail ?: "guest@moviefinder.com"})",
                    fontSize = 11.sp,
                    color = subCol
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (subject.isNotBlank() && description.isNotBlank()) {
                        viewModel.submitSupportTicket(context, category, priority, subject, description)
                        onDismiss()
                    } else {
                        viewModel.showToast("Please enter a subject and description!")
                    }
                },
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                modifier = Modifier.testTag("submit_ticket_confirm_button")
            ) {
                Text("Submit Support Ticket")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = subCol)
            }
        },
        containerColor = dialogBg,
        shape = RoundedCornerShape(16.dp)
    )
}
