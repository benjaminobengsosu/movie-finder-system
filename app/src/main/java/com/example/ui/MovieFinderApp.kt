package com.example.ui

import com.example.data.CREATOR_ADMIN_EMAIL
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.MovieMatch
import com.example.data.api.TmdbMovie
import com.example.ui.theme.*
import java.io.File
import android.app.Activity
import android.speech.RecognizerIntent
import android.media.MediaMetadataRetriever
import android.graphics.Bitmap
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@Composable
fun MovieFinderApp(viewModel: MovieViewModel) {
    val context = LocalContext.current
    var showAuthDialog by remember { mutableStateOf(false) }
    var showAdminPanel by remember { mutableStateOf(false) }
    var showSupportDialog by remember { mutableStateOf(false) }

    // Load saved identity & preferences
    LaunchedEffect(Unit) {
        viewModel.loadUserIdentity(context)
    }

    val systemInDark = isSystemInDarkTheme()
    val isDark = when (viewModel.appearancePreference) {
        "Light" -> false
        "Dark" -> true
        else -> systemInDark
    }

    LaunchedEffect(isDark) {
        viewModel.isDarkMode = isDark
    }

    MyApplicationTheme(darkTheme = isDark) {
        Box(modifier = Modifier.fillMaxSize()) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                if (!viewModel.isAuthInitialized) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFF12131A)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(68.dp)
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(GlassSlateCard)
                                    .border(1.dp, ElectricCyan.copy(alpha = 0.4f), RoundedCornerShape(18.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Movie,
                                    contentDescription = "Movie Finder",
                                    tint = ElectricCyan,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                            CircularProgressIndicator(
                                color = ElectricCyan,
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp
                            )
                        }
                    }
                } else if (!viewModel.isLoggedIn && !viewModel.isGuestUser) {
                    AuthScreen(viewModel = viewModel)
                } else if (viewModel.isFirstTimeUser) {
                    OnboardingScreen(viewModel = viewModel)
                } else if (showAdminPanel) {
                    AdminControlPanelScreen(viewModel = viewModel, onClose = { showAdminPanel = false })
                } else {
                    BoxWithConstraints {
                        val isWideScreen = maxWidth > 800.dp

                        if (isWideScreen) {
                            WideScreenLayout(
                                viewModel = viewModel,
                                context = context,
                                onProfileClick = { showAuthDialog = true }
                            )
                        } else {
                            CompactMobileLayout(
                                viewModel = viewModel,
                                context = context,
                                onProfileClick = { showAuthDialog = true },
                                onAdminClick = { showAdminPanel = true },
                                onSupportClick = { showSupportDialog = true }
                            )
                        }
                    }
                }

                if (showAuthDialog) {
                    FirebaseSyncDialog(
                        viewModel = viewModel,
                        onDismiss = { showAuthDialog = false },
                        onAdminOpen = {
                            showAuthDialog = false
                            showAdminPanel = true
                        },
                        onSupportOpen = {
                            showAuthDialog = false
                            showSupportDialog = true
                        }
                    )
                }

                if (showSupportDialog) {
                    SupportTicketSubmissionDialog(
                        viewModel = viewModel,
                        onDismiss = { showSupportDialog = false }
                    )
                }
            }

            // Subtle Custom Toast Notification overlay
            viewModel.toastMessage?.let { msg ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .align(Alignment.TopCenter),
                    contentAlignment = Alignment.TopCenter
                ) {
                    SubtleToast(
                        message = msg,
                        onDismiss = { viewModel.dismissToast() }
                    )
                }
            }
        }
    }
}

@Composable
fun SubtleToast(
    message: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(message) {
        visible = true
        kotlinx.coroutines.delay(2600)
        visible = false
        kotlinx.coroutines.delay(300) // wait for fade out
        onDismiss()
    }

    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
        modifier = modifier
    ) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = if (message.startsWith("Error") || message.startsWith("Failed") || message.contains("fail", ignoreCase = true)) {
                    Color(0xFFBA1A1A) // Elegant Material red for error
                } else {
                    Color(0xFF6359E9) // Brand violet for success
                }
            ),
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
            modifier = Modifier
                .padding(horizontal = 24.dp, vertical = 16.dp)
                .fillMaxWidth(0.9f)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = if (message.startsWith("Error") || message.startsWith("Failed") || message.contains("fail", ignoreCase = true)) {
                        Icons.Filled.Warning
                    } else {
                        Icons.Filled.CheckCircle
                    },
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = message,
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f, fill = false)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompactMobileLayout(
    viewModel: MovieViewModel,
    context: Context,
    onProfileClick: () -> Unit,
    onAdminClick: () -> Unit = {},
    onSupportClick: () -> Unit = {}
) {
    var selectedTab by remember { mutableStateOf(0) }

    Scaffold(
        topBar = {
            Column {
                CinemaAppBar(
                    viewModel = viewModel,
                    onProfileClick = onProfileClick,
                    onAdminClick = onAdminClick,
                    onSupportClick = onSupportClick
                )

                // Remote Maintenance Mode Banner
                if (viewModel.remoteMaintenanceMode) {
                    Surface(
                        color = Color(0xFFD50000),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("⚠️", fontSize = 12.sp)
                            Text(
                                text = "MAINTENANCE MODE: Remote features being configured by Creator Admin.",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        },
        bottomBar = {
            NavigationBar(
                modifier = Modifier.testTag("bottom_nav_bar"),
                containerColor = GeometricNavBack,
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Filled.Search, contentDescription = "Scene Analyzer Tab") },
                    label = { Text("Analyzer") },
                    modifier = Modifier.testTag("tab_analyzer"),
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.White,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                        unselectedIconColor = Color(0xFF8C90A6),
                        unselectedTextColor = Color(0xFF8C90A6)
                    )
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Filled.Favorite, contentDescription = "Personal Watchlist Tab") },
                    label = { Text("Watchlist") },
                    modifier = Modifier.testTag("tab_watchlist"),
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.White,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                        unselectedIconColor = Color(0xFF8C90A6),
                        unselectedTextColor = Color(0xFF8C90A6)
                    )
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Filled.Star, contentDescription = "Discover & History Tab") },
                    label = { Text("Discover") },
                    modifier = Modifier.testTag("tab_discover"),
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.White,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                        unselectedIconColor = Color(0xFF8C90A6),
                        unselectedTextColor = Color(0xFF8C90A6)
                    )
                )
                if (viewModel.isAdmin) {
                    NavigationBarItem(
                        selected = selectedTab == 3,
                        onClick = { selectedTab = 3 },
                        icon = { Icon(Icons.Filled.Settings, contentDescription = "Creator Admin Panel Tab") },
                        label = { Text("Admin") },
                        modifier = Modifier.testTag("tab_creator_admin"),
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color.Black,
                            selectedTextColor = Color(0xFFFFB300),
                            indicatorColor = Color(0xFFFFB300),
                            unselectedIconColor = Color(0xFFFFB300),
                            unselectedTextColor = Color(0xFFFFB300)
                        )
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
            AnimatedContent(
                targetState = selectedTab,
                transitionSpec = {
                    if (targetState > initialState) {
                        slideInHorizontally { width -> width } + fadeIn() togetherWith
                                slideOutHorizontally { width -> -width } + fadeOut()
                    } else {
                        slideInHorizontally { width -> -width } + fadeIn() togetherWith
                                slideOutHorizontally { width -> width } + fadeOut()
                    }
                }
            ) { targetTab ->
                when (targetTab) {
                    0 -> AnalyzerScreen(viewModel, context)
                    1 -> WatchlistScreen(viewModel, context)
                    2 -> DiscoverScreen(viewModel, context)
                    3 -> AdminControlPanelScreen(viewModel, onClose = { selectedTab = 0 })
                }
            }
        }
    }
}

@Composable
fun WideScreenLayout(
    viewModel: MovieViewModel,
    context: Context,
    onProfileClick: () -> Unit,
    onAdminClick: () -> Unit = {},
    onSupportClick: () -> Unit = {}
) {
    var rightTab by remember { mutableStateOf(0) }

    Column(modifier = Modifier.fillMaxSize()) {
        CinemaAppBar(
            viewModel = viewModel,
            onProfileClick = onProfileClick,
            onAdminClick = onAdminClick,
            onSupportClick = onSupportClick
        )

        Row(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Left Panel - Input and Analyzer
            Box(
                modifier = Modifier
                    .weight(1.1f)
                    .fillMaxHeight()
                    .verticalScroll(rememberScrollState())
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Identify Movie Scene",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.semantics { contentDescription = "Section Identifier: Identify Film Scene" }
                    )
                    SceneInputsCard(viewModel, context)
                }
            }

            // Right Panel - History Table & Active Result Details
            Column(
                modifier = Modifier
                    .weight(1.3f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    listOf("Discoveries", "Watchlist", "Recommendations").forEachIndexed { idx, label ->
                        Button(
                            onClick = { rightTab = idx },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (rightTab == idx) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = if (rightTab == idx) Color.White else Color(0xFF8C90A6)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                            modifier = Modifier.weight(1f).height(38.dp)
                        ) {
                            Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                        }
                    }
                }

                if (viewModel.detectedMovie != null) {
                    DetectedMovieCard(
                        match = viewModel.detectedMovie!!,
                        context = context,
                        isDarkMode = viewModel.isDarkMode,
                        onClear = { viewModel.detectedMovie = null },
                        viewModel = viewModel
                    )
                }

                Box(modifier = Modifier.weight(1f)) {
                    when (rightTab) {
                        0 -> HistoryScreen(viewModel, context)
                        1 -> WatchlistScreen(viewModel, context)
                        2 -> DiscoverScreen(viewModel, context)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CinemaAppBar(
    viewModel: MovieViewModel,
    onProfileClick: () -> Unit,
    onAdminClick: () -> Unit = {},
    onSupportClick: () -> Unit = {}
) {
    val isDark = viewModel.isDarkMode
    
    val bgCol = if (isDark) MaterialTheme.colorScheme.background else Color(0xFFFBFBFE)
    val titleCol = if (isDark) Color(0xFFF5F6F8) else Color(0xFF12131A)
    val subCol = if (isDark) Color(0xFF8E93A4) else Color(0xFF6B7280)
    val logoBg = if (isDark) GlassSlateCard else Color(0xFFF0F1F5)
    val logoTint = if (isDark) ElectricCyan else Color(0xFF0284C7)
    val starBg = if (isDark) GlassSlateCard else Color.White
    val starBorder = if (isDark) GlassBorderLight else Color(0xFFE5E7EB)
    val starColor = if (isDark) ElectricCyan else Color(0xFF0284C7)

    TopAppBar(
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                // Film icon container
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(logoBg)
                        .border(1.dp, starBorder, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Movie,
                        contentDescription = "Logo",
                        tint = logoTint,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Movie Finder",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = titleCol,
                        letterSpacing = 0.2.sp
                    )
                    Text(
                        text = "Welcome, ${viewModel.userName ?: "Guest Moviegoer"}",
                        fontSize = 11.sp,
                        color = subCol,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 0.2.sp
                    )
                }
            }
        },
        actions = {
            // Support / Report Issue button
            IconButton(
                onClick = onSupportClick,
                modifier = Modifier.padding(end = 2.dp).testTag("appbar_support_button")
            ) {
                Icon(
                    imageVector = Icons.Filled.Info,
                    contentDescription = "Support & Reports",
                    tint = subCol,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Dedicated Settings & Developer Dashboard button
            IconButton(
                onClick = onProfileClick,
                modifier = Modifier.padding(end = 2.dp).testTag("appbar_settings_button")
            ) {
                Icon(
                    imageVector = Icons.Filled.Settings,
                    contentDescription = "Settings & Developer Dashboard",
                    tint = starColor,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Creator Admin Panel Badge Button
            if (viewModel.isAdmin) {
                Box(
                    modifier = Modifier
                        .padding(end = 8.dp)
                        .height(34.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Brush.horizontalGradient(listOf(Color(0xFFFFB300), Color(0xFFFF8F00))))
                        .clickable { onAdminClick() }
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                        .testTag("appbar_admin_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("👑", fontSize = 11.sp)
                        Text("ADMIN", color = Color.Black, fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Star button container matching screenshot exactly (acts as user profile and settings)
            Box(
                modifier = Modifier
                    .padding(end = 12.dp)
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(starBg)
                    .border(1.dp, starBorder, CircleShape)
                    .clickable { onProfileClick() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Star,
                    contentDescription = "User Identity & Premium Settings",
                    tint = starColor,
                    modifier = Modifier.size(18.dp)
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = bgCol
        ),
        modifier = Modifier.border(0.dp, Color.Transparent)
    )
}

enum class SceneInputMode { DESCRIBE, CAMERA, UPLOAD }

@Composable
fun AnalyzerScreen(viewModel: MovieViewModel, context: Context) {
    val history by viewModel.searchHistory.collectAsState()
    val isDark = viewModel.isDarkMode

    LaunchedEffect(Unit) {
        if (viewModel.trendingMoviesState.isEmpty()) {
            viewModel.fetchTrendingMovies()
        }
        if (viewModel.discoverMoviesState.isEmpty()) {
            viewModel.fetchDiscoverMovies()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Unified Omnibar & Mode Switcher Workspace
        SceneInputsCard(viewModel, context)

        // Bento Box: Cinema Taste Profile (Streamlined Collapsible Tray)
        CinemaTasteProfileBentoCard(viewModel = viewModel, context = context)

        // Active Identified Movie Card
        if (viewModel.detectedMovie != null) {
            DetectedMovieCard(
                match = viewModel.detectedMovie!!,
                context = context,
                isDarkMode = viewModel.isDarkMode,
                onClear = { viewModel.detectedMovie = null },
                viewModel = viewModel
            )
        }

        // Recent Scene Matches (Horizontally scrollable for instant recall)
        if (history.isNotEmpty()) {
            RecentMatchesHomeSection(history = history, viewModel = viewModel)
        }

        // Curated Discoveries (Curated trending cinematic titles)
        CuratedDiscoveriesHomeSection(viewModel = viewModel, context = context)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CinemaTasteProfileBentoCard(viewModel: MovieViewModel, context: Context) {
    var isExpanded by remember { mutableStateOf(false) }
    val isDark = viewModel.isDarkMode
    val cardBg = if (isDark) GlassSlateCard else Color.White
    val cardBorder = if (isDark) GlassBorderLight else Color(0xFFE5E7EB)
    val inputBg = if (isDark) Color(0xCC0E101A) else Color(0xFFF5F4FA)
    val textCol = if (isDark) Color(0xFFF3F4F6) else Color(0xFF12131A)
    val labelCol = if (isDark) Color(0xFF9CA3AF) else Color(0xFF6B7280)
    val accentGold = ElectricCyan

    val activeGenres = if (viewModel.onboardingGenres.isNotEmpty()) {
        viewModel.onboardingGenres
    } else {
        listOf("Sci-Fi", "Neo-Noir", "Psychological Thriller", "Cyberpunk", "Classic Cinema")
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, cardBorder, RoundedCornerShape(16.dp))
            .clickable { isExpanded = !isExpanded }
            .testTag("cinema_taste_profile_bento_card"),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .clip(RoundedCornerShape(7.dp))
                            .background(accentGold.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.AutoAwesome,
                            contentDescription = null,
                            tint = accentGold,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "CINEMA TASTE PROFILE",
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.5.sp,
                            color = accentGold,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = if (isExpanded) "Tap genres to auto-fill search" else "${activeGenres.size} active genre filters • Tap to expand",
                            fontSize = 9.5.sp,
                            color = labelCol
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(accentGold.copy(alpha = 0.12f))
                            .border(1.dp, accentGold.copy(alpha = 0.35f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 7.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "AI TUNED",
                            color = accentGold,
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                    }
                    Icon(
                        imageVector = if (isExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                        contentDescription = if (isExpanded) "Collapse taste profile" else "Expand taste profile",
                        tint = labelCol,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Quick Taste Profile Filter Chips (Collapsible Tray)
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                FlowRow(
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    activeGenres.forEach { genre ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = inputBg,
                            border = BorderStroke(1.dp, cardBorder),
                            modifier = Modifier.clickable {
                                viewModel.setSceneText(genre)
                            }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "#",
                                    fontSize = 10.sp,
                                    color = accentGold,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = genre,
                                    fontSize = 11.sp,
                                    color = textCol,
                                    fontWeight = FontWeight.Medium
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
fun SceneInputsCard(viewModel: MovieViewModel, context: Context) {
    val isDark = viewModel.isDarkMode
    val cardBg = if (isDark) GlassSlateCard else Color.White
    val cardBorder = if (isDark) GlassBorderLight else Color(0xFFE5E7EB)
    val inputBg = if (isDark) Color(0xCC0E101A) else Color(0xFFF5F4FA)
    val textCol = if (isDark) Color(0xFFF3F4F6) else Color(0xFF12131A)
    val labelCol = if (isDark) Color(0xFF9CA3AF) else Color(0xFF6B7280)
    val accentGold = ElectricCyan

    var currentMode by remember { mutableStateOf(SceneInputMode.DESCRIBE) }
    var showCameraScanner by remember { mutableStateOf(false) }

    if (showCameraScanner) {
        Dialog(
            onDismissRequest = { showCameraScanner = false },
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false
            )
        ) {
            CameraView(
                onFrameCaptured = { bitmap ->
                    viewModel.setCapturedFrame(bitmap)
                    showCameraScanner = false
                },
                onClose = { showCameraScanner = false }
            )
        }
    }

    val speechRecognizerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spokenText = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull() ?: ""
            if (spokenText.isNotBlank()) {
                val oldVal = viewModel.sceneDescription
                viewModel.setSceneText(if (oldVal.isBlank()) spokenText else "$oldVal $spokenText")
            }
        }
    }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            if (uri != null) {
                viewModel.selectMedia(context, uri, "IMAGE")
            }
        }
    )

    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            if (uri != null) {
                viewModel.selectMedia(context, uri, "VIDEO")
            }
        }
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, cardBorder, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Streamlined Mode Switcher (Floats directly at top of input card)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(inputBg)
                    .border(1.dp, cardBorder, RoundedCornerShape(10.dp))
                    .padding(3.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf(
                    Triple(SceneInputMode.DESCRIBE, "Describe", Icons.Filled.TextFields),
                    Triple(SceneInputMode.CAMERA, "Live Camera", Icons.Filled.PhotoCamera),
                    Triple(SceneInputMode.UPLOAD, "Upload Media", Icons.Filled.CloudUpload)
                ).forEach { (mode, label, icon) ->
                    val isSelected = currentMode == mode
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(32.dp)
                            .clip(RoundedCornerShape(7.dp))
                            .background(if (isSelected) accentGold else Color.Transparent)
                            .clickable { currentMode = mode }
                            .testTag(
                                when (mode) {
                                    SceneInputMode.DESCRIBE -> "mode_pill_describe"
                                    SceneInputMode.CAMERA -> "mode_pill_camera"
                                    SceneInputMode.UPLOAD -> "mode_pill_upload"
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = if (isSelected) Color(0xFF0F1015) else labelCol,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = label,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color(0xFF0F1015) else labelCol
                            )
                        }
                    }
                }
            }

            // Cross-Mode Attached Media Strip
            if (viewModel.thumbnailBitmap != null && currentMode == SceneInputMode.DESCRIBE) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(accentGold.copy(alpha = 0.08f))
                        .border(1.dp, accentGold.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Image(
                            bitmap = viewModel.thumbnailBitmap!!.asImageBitmap(),
                            contentDescription = "Attached frame",
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(6.dp)),
                            contentScale = ContentScale.Crop
                        )
                        Column {
                            Text(
                                text = when (viewModel.selectedMediaType) {
                                    "CAMERA_SCAN" -> "Attached: Live Camera Scan"
                                    "VIDEO" -> "Attached: Video Clip"
                                    else -> "Attached: Scene Image"
                                },
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = textCol
                            )
                            Text(
                                text = "Visual context will be analyzed together with your text",
                                fontSize = 9.5.sp,
                                color = labelCol
                            )
                        }
                    }
                    IconButton(
                        onClick = { viewModel.clearSelections() },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Remove attachment",
                            tint = labelCol,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            // Progressive Workspace (Only controls relevant to active mode appear)
            when (currentMode) {
                SceneInputMode.DESCRIBE -> {
                    // Single Unified Input Card Section
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = inputBg,
                        border = BorderStroke(1.dp, cardBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.Top
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Search,
                                    contentDescription = "Search Hub",
                                    tint = accentGold,
                                    modifier = Modifier.padding(top = 2.dp).size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                BasicTextField(
                                    value = viewModel.sceneDescription,
                                    onValueChange = {
                                        if (it.length <= 500) viewModel.setSceneText(it)
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = 44.dp, max = 80.dp)
                                        .testTag("omnibar_search_input")
                                        .testTag("unified_search_input")
                                        .testTag("description_text_input")
                                        .semantics { contentDescription = "Describe scene quotes, actors, vibes, or plot clues" },
                                    textStyle = TextStyle(
                                        fontSize = 13.sp,
                                        color = textCol,
                                        fontWeight = FontWeight.Normal,
                                        lineHeight = 18.sp
                                    ),
                                    cursorBrush = SolidColor(accentGold),
                                    decorationBox = { innerTextField ->
                                        if (viewModel.sceneDescription.isEmpty()) {
                                            Text(
                                                text = "Search dialogue quotes, actors, scenes, or vibe...",
                                                fontSize = 12.5.sp,
                                                color = labelCol.copy(alpha = 0.75f),
                                                lineHeight = 18.sp
                                            )
                                        }
                                        innerTextField()
                                    }
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    if (viewModel.sceneDescription.isNotEmpty()) {
                                        IconButton(
                                            onClick = { viewModel.setSceneText("") },
                                            modifier = Modifier.size(26.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.Close,
                                                contentDescription = "Clear input",
                                                tint = labelCol,
                                                modifier = Modifier.size(15.dp)
                                            )
                                        }
                                    }
                                    IconButton(
                                        onClick = {
                                            try {
                                                val speechIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                                    putExtra(RecognizerIntent.EXTRA_PROMPT, "Describe the scene out loud...")
                                                }
                                                speechRecognizerLauncher.launch(speechIntent)
                                            } catch (e: Exception) {
                                                Toast.makeText(context, "Speech recognizer not available", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        modifier = Modifier.size(28.dp).testTag("dictate_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Mic,
                                            contentDescription = "Dictate scene",
                                            tint = labelCol,
                                            modifier = Modifier.size(17.dp)
                                        )
                                    }
                                    IconButton(
                                        onClick = { currentMode = SceneInputMode.CAMERA },
                                        modifier = Modifier.size(28.dp).testTag("omnibar_camera_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.PhotoCamera,
                                            contentDescription = "One-tap live camera scan",
                                            tint = accentGold,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }

                            // Clean prompt chips grouped inside input container
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                listOf(
                                    "Sci-Fi Noir",
                                    "Mind-Bending",
                                    "Rainy Car Chase",
                                    "Spinning top on table",
                                    "Clock tower lightning",
                                    "Red vs blue pill"
                                ).forEach { prompt ->
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(cardBg)
                                            .border(1.dp, cardBorder, RoundedCornerShape(6.dp))
                                            .clickable { viewModel.setSceneText(prompt) }
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = prompt,
                                            fontSize = 10.5.sp,
                                            color = labelCol
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                SceneInputMode.CAMERA -> {
                    if (viewModel.thumbnailBitmap != null && viewModel.selectedMediaType == "CAMERA_SCAN") {
                        // Captured camera frame preview
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(170.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .border(1.dp, cardBorder, RoundedCornerShape(12.dp))
                        ) {
                            Image(
                                bitmap = viewModel.thumbnailBitmap!!.asImageBitmap(),
                                contentDescription = "Captured camera scan",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .padding(8.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color.Black.copy(alpha = 0.75f))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    "LIVE SCAN READY",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF00E676)
                                )
                            }
                            Row(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Button(
                                    onClick = { showCameraScanner = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.Black.copy(alpha = 0.7f)),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    modifier = Modifier.height(28.dp)
                                ) {
                                    Text("Retake", fontSize = 10.sp, color = Color.White)
                                }
                                Button(
                                    onClick = { viewModel.clearSelections() },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.Black.copy(alpha = 0.7f)),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    modifier = Modifier.height(28.dp)
                                ) {
                                    Text("Clear", fontSize = 10.sp, color = Color.White)
                                }
                            }
                        }
                    } else {
                        // Live camera preview area for scanning scenes directly from main screen
                        InlineCameraScanner(
                            onFrameCaptured = { bitmap ->
                                viewModel.setCapturedFrame(bitmap)
                            },
                            onExpandFullScreen = {
                                showCameraScanner = true
                            }
                        )
                    }
                }

                SceneInputMode.UPLOAD -> {
                    if (viewModel.thumbnailBitmap != null && (viewModel.selectedMediaType == "IMAGE" || viewModel.selectedMediaType == "VIDEO")) {
                        // Uploaded media preview
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(170.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .border(1.dp, cardBorder, RoundedCornerShape(12.dp))
                        ) {
                            Image(
                                bitmap = viewModel.thumbnailBitmap!!.asImageBitmap(),
                                contentDescription = "Uploaded media",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .padding(8.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color.Black.copy(alpha = 0.75f))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    if (viewModel.selectedMediaType == "VIDEO") "VIDEO ATTACHED" else "IMAGE ATTACHED",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = accentGold
                                )
                            }
                            IconButton(
                                onClick = { viewModel.clearSelections() },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(8.dp)
                                    .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(50))
                                    .size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = "Remove file",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    } else {
                        // Side-by-side upload cards
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Card(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(88.dp)
                                    .border(1.dp, cardBorder, RoundedCornerShape(12.dp))
                                    .clickable {
                                        imagePickerLauncher.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    }
                                    .testTag("upload_image_button")
                                    .testTag("omnibar_gallery_button"),
                                colors = CardDefaults.cardColors(containerColor = inputBg),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxSize().padding(8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Image,
                                        contentDescription = null,
                                        tint = accentGold,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Upload Image",
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = textCol
                                    )
                                    Text(
                                        text = "From gallery",
                                        fontSize = 9.5.sp,
                                        color = labelCol
                                    )
                                }
                            }

                            Card(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(88.dp)
                                    .border(1.dp, cardBorder, RoundedCornerShape(12.dp))
                                    .clickable {
                                        videoPickerLauncher.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                                        )
                                    }
                                    .testTag("upload_video_button"),
                                colors = CardDefaults.cardColors(containerColor = inputBg),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxSize().padding(8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Movie,
                                        contentDescription = null,
                                        tint = accentGold,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Upload Video",
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = textCol
                                    )
                                    Text(
                                        text = "Short clip",
                                        fontSize = 9.5.sp,
                                        color = labelCol
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Error display
            if (viewModel.errorMessage != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.error.copy(alpha = 0.1f))
                        .border(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                        .padding(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = viewModel.errorMessage!!,
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            // Primary Identify Scene Button
            Button(
                onClick = { viewModel.identifyMovie(context) },
                enabled = !viewModel.isLoading,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = accentGold,
                    contentColor = Color(0xFF12131A)
                ),
                contentPadding = PaddingValues(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .testTag("analyze_scene_button")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    if (viewModel.isLoading) {
                        CircularProgressIndicator(
                            color = Color(0xFF12131A),
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Finding movie...",
                            color = Color(0xFF12131A),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Filled.Search,
                            contentDescription = null,
                            tint = Color(0xFF12131A),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "FIND MOVIE",
                            color = Color(0xFF12131A),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RecentMatchesHomeSection(
    history: List<MovieMatch>,
    viewModel: MovieViewModel
) {
    val isDark = viewModel.isDarkMode
    val textCol = if (isDark) Color(0xFFF5F6F8) else Color(0xFF12131A)
    val labelCol = if (isDark) Color(0xFF8E93A4) else Color(0xFF6B7280)
    val cardBorder = if (isDark) GlassBorderLight else Color(0xFFE5E7EB)
    val cardBg = if (isDark) GlassSlateCard else Color.White
    val accentGold = ElectricCyan

    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth().testTag("recent_matches_home_section")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "RECENT SCENE MATCHES",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = accentGold,
                    letterSpacing = 0.8.sp
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(accentGold.copy(alpha = 0.15f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "${history.size}",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = accentGold
                    )
                }
            }
        }

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(history.take(8)) { match ->
                Card(
                    modifier = Modifier
                        .width(136.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .border(1.dp, cardBorder, RoundedCornerShape(16.dp))
                        .clickable { viewModel.detectedMovie = match },
                    colors = CardDefaults.cardColors(containerColor = cardBg),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(140.dp)
                                .background(Color(0xFF12131A)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (match.posterPath.isNotBlank()) {
                                AsyncImage(
                                    model = match.posterPath,
                                    contentDescription = match.title,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Filled.Movie,
                                    contentDescription = null,
                                    tint = accentGold.copy(alpha = 0.5f),
                                    modifier = Modifier.size(36.dp)
                                )
                            }

                            // Match score badge
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(6.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color.Black.copy(alpha = 0.75f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "${match.confidence}%",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = accentGold
                                )
                            }
                        }

                        Column(
                            modifier = Modifier.padding(8.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = match.title,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = textCol,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${match.year} • ${match.genre.split("/").firstOrNull()?.trim() ?: match.genre}",
                                fontSize = 10.sp,
                                color = labelCol,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CuratedDiscoveriesHomeSection(
    viewModel: MovieViewModel,
    context: Context
) {
    val trendingList = viewModel.trendingMoviesState
    val discoverList = viewModel.discoverMoviesState
    val isDark = viewModel.isDarkMode
    val accentGold = ElectricCyan

    var selectedSectionTab by remember { mutableStateOf(0) } // 0 = Trending, 1 = Curated
    var selectedMovieForDetail by remember { mutableStateOf<TmdbMovie?>(null) }

    val currentList = if (selectedSectionTab == 0) {
        if (trendingList.isNotEmpty()) trendingList else discoverList
    } else {
        if (discoverList.isNotEmpty()) discoverList else trendingList
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth().testTag("curated_discoveries_home_section")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "CURATED DISCOVERIES",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = accentGold,
                    letterSpacing = 0.8.sp
                )
            }

            // Tab switch between Trending and Curated Discovery
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                FilterChip(
                    selected = selectedSectionTab == 0,
                    onClick = {
                        selectedSectionTab = 0
                        if (trendingList.isEmpty()) viewModel.fetchTrendingMovies()
                    },
                    label = { Text("Trending", fontSize = 9.sp) },
                    shape = RoundedCornerShape(50.dp)
                )
                FilterChip(
                    selected = selectedSectionTab == 1,
                    onClick = {
                        selectedSectionTab = 1
                        if (discoverList.isEmpty()) viewModel.fetchDiscoverMovies()
                    },
                    label = { Text("Curated", fontSize = 9.sp) },
                    shape = RoundedCornerShape(50.dp)
                )
            }
        }

        if (viewModel.isTmdbLoading && currentList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f))
                    .padding(20.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), color = accentGold, strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Loading cinema titles from TMDB...", fontSize = 11.5.sp, color = accentGold)
                }
            }
        } else if (viewModel.tmdbErrorMessage != null && currentList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                    .padding(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Filled.Warning, contentDescription = null, tint = accentGold, modifier = Modifier.size(20.dp))
                        Text(
                            text = viewModel.tmdbErrorMessage!!,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                            lineHeight = 15.sp
                        )
                    }
                    Button(
                        onClick = {
                            viewModel.fetchTrendingMovies()
                            viewModel.fetchDiscoverMovies()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = accentGold, contentColor = Color(0xFF12131A)),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Text("Retry", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else if (currentList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f))
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Filled.Movie, contentDescription = null, tint = accentGold, modifier = Modifier.size(18.dp))
                    Text("No curated titles found.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    TextButton(onClick = {
                        viewModel.fetchTrendingMovies()
                        viewModel.fetchDiscoverMovies()
                    }) {
                        Text("Refresh", fontSize = 11.sp, color = accentGold, fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(currentList.take(15), key = { it.id }) { movie ->
                    TmdbMovieCardItem(
                        movie = movie,
                        onSaveWatchlist = {
                            val match = MovieMatch(
                                title = movie.displayTitle,
                                year = movie.displayYear,
                                director = "TMDB Curated",
                                imdbUrl = "https://www.themoviedb.org/movie/${movie.id}",
                                confidence = "High",
                                rationale = movie.overview?.take(140) ?: "Curated title discovered via TMDB cinema feed.",
                                actors = "Live TMDB Metadata",
                                genre = "Curated",
                                searchType = "TEXT",
                                isInWatchlist = true,
                                posterPath = movie.fullPosterUrl
                            )
                            viewModel.toggleWatchlist(match)
                        },
                        onOpenTmdb = {
                            selectedMovieForDetail = movie
                        }
                    )
                }
            }
        }
    }

    val movieToDisplay = selectedMovieForDetail
    if (movieToDisplay != null) {
        TmdbMovieDetailDialog(
            movie = movieToDisplay,
            viewModel = viewModel,
            context = context,
            onDismiss = { selectedMovieForDetail = null }
        )
    }
}

@Composable
fun DetectedMovieCard(
    match: MovieMatch,
    context: Context,
    isDarkMode: Boolean,
    onClear: () -> Unit,
    viewModel: MovieViewModel
) {
    val cardBorder = if (isDarkMode) Color(0xFF2D2D32) else Color(0xFFE5E7EB)
    val cardBg = if (isDarkMode) Color(0xFF1E1F26) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, cardBorder, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(
            containerColor = cardBg
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header showing Match Found badge & Clear cross
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "IDENTIFIED MATCH DETECTED",
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.5.sp
                    )
                }

                IconButton(
                    onClick = onClear,
                    modifier = Modifier.size(26.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Collapse matched film card",
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Movie Poster Cover Art
            val posterImageToDisplay = match.posterPath.takeIf { !it.isNullOrBlank() }
                ?: CuratedMoviesList.firstOrNull { it.title.equals(match.title, ignoreCase = true) }?.imageUrl
                ?: ""

            TmdbMoviePoster(
                posterUrl = posterImageToDisplay,
                title = match.title,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(16.dp))
            )

            // Big Film Title
            Column {
                Text(
                    text = match.title.uppercase(),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.5.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Year: ${match.year}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(text = "•", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(
                                when (match.confidence.lowercase()) {
                                    "high" -> Color(0xFF22C55E).copy(alpha = 0.15f)
                                    "medium" -> Color(0xFFF59E0B).copy(alpha = 0.15f)
                                    else -> Color(0xFFEF4444).copy(alpha = 0.15f)
                                }
                            )
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "${match.confidence.uppercase()} CONFIDENCE",
                            color = when (match.confidence.lowercase()) {
                                "high" -> Color(0xFF22C55E)
                                "medium" -> Color(0xFFF59E0B)
                                else -> Color(0xFFEF4444)
                            },
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }

            // Details metadata rows
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                DetailRow(label = "DIRECTOR DIRECT", value = match.director)
                DetailRow(label = "CAST / DRAMATISTS", value = match.actors)
                DetailRow(label = "CINEMATIC CATEGORY", value = match.genre)
                if (match.thematicElements.isNotEmpty()) {
                    DetailRow(label = "THEIME ELEMENTS", value = match.thematicElements)
                }
            }

            Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))

            // Analysis rationale
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "FINDER ARCHAEOLOGY REPORT",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 9.sp,
                    letterSpacing = 1.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = match.rationale,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
                    fontStyle = FontStyle.Italic,
                    lineHeight = 16.sp
                )
            }

            Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))

            // Action Triggers: IMDb, Watchlist, Share, Trailer
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Trailer Button - High contrast play action
                Button(
                    onClick = { watchMovieTrailer(context, match.title, match.year) },
                    modifier = Modifier
                        .weight(1.1f)
                        .height(44.dp)
                        .testTag("watch_trailer_button")
                        .semantics { contentDescription = "Watch official youtube movie trailer for ${match.title}" },
                    shape = RoundedCornerShape(50.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFEF4444), // Crimson/YouTube red color
                        contentColor = Color.White
                    ),
                    contentPadding = PaddingValues(horizontal = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text("TRAILER", fontSize = 9.5.sp, fontWeight = FontWeight.Bold)
                }

                // IMDb URI Action Trigger - Outlined layout matching HTML
                OutlinedButton(
                    onClick = {
                        val imdbIntent = Intent(Intent.ACTION_VIEW, Uri.parse(match.imdbUrl))
                        context.startActivity(imdbIntent)
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .testTag("imdb_link_button")
                        .semantics { contentDescription = "View details and trailers for ${match.title} on IMDb website" },
                    shape = RoundedCornerShape(50.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.primary
                    ),
                    contentPadding = PaddingValues(horizontal = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Star,
                        contentDescription = null,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text("IMDb", fontSize = 9.5.sp, fontWeight = FontWeight.Bold)
                }

                // Watchlist Save/Remove action selector - styled with premium cinema theme
                Button(
                    onClick = { viewModel.toggleWatchlist(match) },
                    modifier = Modifier
                        .weight(1.1f)
                        .height(44.dp)
                        .testTag("match_watchlist_button")
                        .semantics { contentDescription = "Toggle adding or removing ${match.title} from your personal watch priority list" },
                    shape = RoundedCornerShape(50.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (match.isInWatchlist) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = if (match.isInWatchlist) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    contentPadding = PaddingValues(horizontal = 4.dp)
                ) {
                    Icon(
                        imageVector = if (match.isInWatchlist) Icons.Filled.Check else Icons.Filled.Favorite,
                        contentDescription = null,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(if (match.isInWatchlist) "ADDED" else "WATCHLIST", fontSize = 9.5.sp, fontWeight = FontWeight.Bold)
                }

                // Share Action Trigger - High priority color matching HTML
                Button(
                    onClick = { shareMovieMatch(context, match) },
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .testTag("share_movie_button")
                        .semantics { contentDescription = "Share movie title, year and analysis link to other social platforms" },
                    shape = RoundedCornerShape(50.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary,
                        contentColor = MaterialTheme.colorScheme.onSecondary
                    ),
                    contentPadding = PaddingValues(horizontal = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Share,
                        contentDescription = null,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text("SHARE", fontSize = 9.5.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = label,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.width(100.dp),
            letterSpacing = 0.5.sp
        )
        Text(
            text = value,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun VideoThumbnailImage(
    mediaUriString: String?,
    modifier: Modifier = Modifier
) {
    var thumbnailBitmap by remember(mediaUriString) { mutableStateOf<Bitmap?>(null) }
    val context = LocalContext.current

    LaunchedEffect(mediaUriString) {
        if (!mediaUriString.isNullOrBlank()) {
            withContext(Dispatchers.IO) {
                try {
                    val uri = Uri.parse(mediaUriString)
                    val retriever = MediaMetadataRetriever()
                    retriever.setDataSource(context, uri)
                    val bitmap = retriever.getFrameAtTime(1000000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                    retriever.release()
                    thumbnailBitmap = bitmap
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    if (thumbnailBitmap != null) {
        Image(
            bitmap = thumbnailBitmap!!.asImageBitmap(),
            contentDescription = "Video frame capture",
            contentScale = ContentScale.Crop,
            modifier = modifier
        )
    } else {
        Box(
            modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.PlayArrow,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun GalleryItemCard(
    item: MovieMatch,
    viewModel: MovieViewModel,
    context: Context
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                onClick = { viewModel.detectedMovie = item },
                onClickLabel = "Expand details for ${item.title}"
            ),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(115.dp)
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                if (item.searchType.uppercase() == "VIDEO" && !item.mediaUri.isNullOrBlank()) {
                    VideoThumbnailImage(
                        mediaUriString = item.mediaUri,
                        modifier = Modifier.fillMaxSize()
                    )
                } else if (item.searchType.uppercase() == "IMAGE" && !item.mediaUri.isNullOrBlank()) {
                    AsyncImage(
                        model = Uri.parse(item.mediaUri),
                        contentDescription = "Scene picture frame capture",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    listOf(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.surface)
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(8.dp)
                        ) {
                            Text("🎬", fontSize = 22.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "STORY QUERY",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(
                            when (item.confidence.lowercase()) {
                                "high" -> Color(0xFF22C55E)
                                "medium" -> Color(0xFFF59E0B)
                                else -> Color(0xFFEF4444)
                            }
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = item.confidence.uppercase(),
                        color = Color.White,
                        fontSize = 7.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(6.dp)
                        .size(26.dp)
                        .clip(RoundedCornerShape(13.dp))
                        .background(Color.Black.copy(alpha = 0.7f))
                        .clickable { watchMovieTrailer(context, item.title, item.year) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = "Watch Trailer",
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            Column(
                modifier = Modifier.padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = item.title,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = "${item.year} | Dir: ${item.director}",
                    fontSize = 9.5.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { viewModel.toggleWatchlist(item) },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Favorite,
                            contentDescription = "Toggle Watchlist",
                            tint = if (item.isInWatchlist) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f),
                            modifier = Modifier.size(13.dp)
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        IconButton(
                            onClick = { shareMovieMatch(context, item) },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Share,
                                contentDescription = "Share Movie",
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                modifier = Modifier.size(12.dp)
                            )
                        }
                        IconButton(
                            onClick = { viewModel.deleteMatch(item) },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Delete,
                                contentDescription = "Remove Match",
                                tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HistoryScreen(viewModel: MovieViewModel, context: Context) {
    var isGalleryMode by remember { mutableStateOf(true) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        HistoryHeader(
            viewModel = viewModel,
            context = context,
            isGalleryMode = isGalleryMode,
            onToggleMode = { isGalleryMode = it }
        )
        HistoryList(
            viewModel = viewModel,
            context = context,
            isGalleryMode = isGalleryMode
        )
    }
}

@Composable
fun HistoryHeader(
    viewModel: MovieViewModel,
    context: Context,
    isGalleryMode: Boolean = true,
    onToggleMode: ((Boolean) -> Unit)? = null
) {
    val history by viewModel.searchHistory.collectAsState()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(20.dp)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)),
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "HISTORY REPORT",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "${history.size} matches registered",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (onToggleMode != null) {
                    IconButton(
                        onClick = { onToggleMode(true) },
                        modifier = Modifier
                            .size(36.dp)
                            .background(
                                if (isGalleryMode) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else Color.Transparent,
                                RoundedCornerShape(50)
                            )
                    ) {
                        Text("🖼️", fontSize = 14.sp)
                    }
                    IconButton(
                        onClick = { onToggleMode(false) },
                        modifier = Modifier
                            .size(36.dp)
                            .background(
                                if (!isGalleryMode) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else Color.Transparent,
                                RoundedCornerShape(50)
                            )
                    ) {
                        Text("📜", fontSize = 14.sp)
                    }
                    Spacer(modifier = Modifier.width(2.dp))
                }

                Button(
                    onClick = {
                        viewModel.generatePdfReport(context) { file ->
                            if (file != null) {
                                openOrSharePdf(context, file)
                            } else {
                                Toast.makeText(context, "Could not generate PDF report. Add search items first.", Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    modifier = Modifier
                        .height(36.dp)
                        .testTag("export_pdf_button")
                        .semantics { contentDescription = "Compile search history log and share as Adobe PDF Document" },
                    shape = RoundedCornerShape(50.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Share,
                        contentDescription = null,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("PDF EXPORT", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }

                IconButton(
                    onClick = { viewModel.clearSearchHistory() },
                    modifier = Modifier
                        .size(36.dp)
                        .background(MaterialTheme.colorScheme.error.copy(alpha = 0.12f), RoundedCornerShape(50))
                        .testTag("clear_history_button")
                        .semantics { contentDescription = "Discard all items in research registry history database" }
                ) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun HistoryList(
    viewModel: MovieViewModel,
    context: Context,
    isGalleryMode: Boolean = true
) {
    val history by viewModel.searchHistory.collectAsState()

    if (history.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Star,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                        modifier = Modifier.size(36.dp)
                    )
                }
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "NO CINEMATICS REGISTERED",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Discovered movies will be saved to your library. Start scanning above or explaining scenic segments.",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                    textAlign = TextAlign.Center,
                    lineHeight = 14.sp
                )
            }
        }
    } else {
        if (isGalleryMode) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                history.chunked(2).forEach { rowItems ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        rowItems.forEach { item ->
                            Box(modifier = Modifier.weight(1f)) {
                                GalleryItemCard(item, viewModel, context)
                            }
                        }
                        if (rowItems.size < 2) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("history_list"),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(history, key = { it.id }) { item ->
                    HistoryItemCard(item, viewModel, context)
                }
            }
        }
    }
}

@Composable
fun HistoryItemCard(
    item: MovieMatch,
    viewModel: MovieViewModel,
    context: Context
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("history_item_${item.id}")
            .clickable(
                onClick = { viewModel.detectedMovie = item },
                onClickLabel = "Expand details for ${item.title}"
            ),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Source icon
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when (item.searchType.uppercase()) {
                        "VIDEO" -> Icons.Filled.PlayArrow
                        "IMAGE" -> Icons.Filled.Home
                        else -> Icons.Filled.Search
                    },
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Identifications info
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = item.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "(${item.year})",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Dir: ${item.director}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                // small badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(
                            when (item.confidence.lowercase()) {
                                "high" -> Color(0xFF22C55E).copy(alpha = 0.12f)
                                "medium" -> Color(0xFFF59E0B).copy(alpha = 0.12f)
                                else -> Color(0xFFEF4444).copy(alpha = 0.12f)
                            }
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "${item.confidence} match".uppercase(),
                        color = when (item.confidence.lowercase()) {
                            "high" -> Color(0xFF22C55E)
                            "medium" -> Color(0xFFF59E0B)
                            else -> Color(0xFFEF4444)
                        },
                        fontSize = 7.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Quick actions sidepanel: Share, Trash
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Share
                IconButton(
                    onClick = { shareMovieMatch(context, item) },
                    modifier = Modifier
                        .size(36.dp)
                        .semantics { contentDescription = "Share movie match details for ${item.title}" }
                ) {
                    Icon(
                        imageVector = Icons.Filled.Share,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        modifier = Modifier.size(16.dp)
                    )
                }

                // Delete
                IconButton(
                    onClick = { viewModel.deleteMatch(item) },
                    modifier = Modifier
                        .size(36.dp)
                        .semantics { contentDescription = "Remove movie match details for ${item.title} from search history registry" }
                ) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

private fun watchMovieTrailer(context: Context, title: String, year: String) {
    try {
        val query = Uri.encode("$title $year official movie trailer")
        val youtubeUrl = "https://www.youtube.com/results?search_query=$query"
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(youtubeUrl))
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "No video app available to stream tailer.", Toast.LENGTH_LONG).show()
    }
}

private fun shareMovieMatch(context: Context, match: MovieMatch) {
    val shareBody = """
        🎬 Realized cinema identity match using Movie Finder!
        
        🎥 Film Title: ${match.title} (${match.year})
        🎬 Directed By: ${match.director}
        🍿 Class categories: ${match.genre}
        🎭 Credits list: ${match.actors}
        
        📄 Analysis Rationale:
        "${match.rationale}"
        
        🔗 Launch film database card: ${match.imdbUrl}
    """.trimIndent()

    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, "🎬 movie Match Solved!")
        putExtra(Intent.EXTRA_TEXT, shareBody)
    }
    context.startActivity(Intent.createChooser(intent, "Share with cinephiles via:"))
}

private fun openOrSharePdf(context: Context, file: File) {
    try {
        val fileUri: Uri = androidx.core.content.FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, fileUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Retrieve History Report PDF:"))
    } catch (e: Exception) {
        Toast.makeText(context, "Failure opening PDF document: ${e.message}", Toast.LENGTH_LONG).show()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WatchlistScreen(viewModel: MovieViewModel, context: Context) {
    val watchlist by viewModel.watchlistHistory.collectAsState()
    var filterStatus by remember { mutableStateOf("All") }
    var filterCategory by remember { mutableStateOf("All") }
    var activeEditItem by remember { mutableStateOf<MovieMatch?>(null) }

    val filteredList = watchlist.filter { item ->
        val matchesStatus = (filterStatus == "All" || item.watchlistStatus == filterStatus)
        val matchesCategory = (filterCategory == "All" || item.customListCategory == filterCategory)
        matchesStatus && matchesCategory
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Watchlist filtering headers
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(20.dp)),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Column {
                    Text(
                        text = "PERSONAL FILM WATCHLIST & CUSTOM LISTS",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "${watchlist.size} movies flagged across custom user lists",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }

                // Custom List Category Filter
                Text("Custom Lists:", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = CinemaGoldLight)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("All", "Must Watch", "Weekend Binge", "With Friends", "Top Favorites").forEach { cat ->
                        FilterChip(
                            selected = filterCategory == cat,
                            onClick = { filterCategory = cat },
                            label = { Text(cat, fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = CinemaGold,
                                selectedLabelColor = Color.Black
                            ),
                            shape = RoundedCornerShape(50.dp),
                            modifier = Modifier.testTag("filter_list_category_$cat")
                        )
                    }
                }

                // Watch Status Filter
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("All", "Plan to Watch", "Watching", "Completed").forEach { status ->
                        FilterChip(
                            selected = filterStatus == status,
                            onClick = { filterStatus = status },
                            label = { Text(status, fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            shape = RoundedCornerShape(50.dp)
                        )
                    }
                }
            }
        }

        if (filteredList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(24.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(RoundedCornerShape(50))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Favorite,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "EMPTY ARCHIVE PANEL",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Mark movies as 'WATCHLIST' on detected scene results or past items to populate this list.",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredList, key = { it.id }) { item ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
                            .clickable { activeEditItem = item },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.title.uppercase(),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "${item.year}  |  Dir: ${item.director}",
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = item.watchlistStatus.uppercase(),
                                        color = MaterialTheme.colorScheme.primary,
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                }
                            }

                            if (item.watchlistNotes.isNotEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
                                        .padding(8.dp)
                                ) {
                                    Text(
                                        text = "Notes: ${item.watchlistNotes}",
                                        fontSize = 9.5.sp,
                                        fontStyle = FontStyle.Italic,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    IconButton(
                                        onClick = {
                                            val imdbIntent = Intent(Intent.ACTION_VIEW, Uri.parse(item.imdbUrl))
                                            context.startActivity(imdbIntent)
                                        },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Star,
                                            contentDescription = "View IMDb page",
                                            tint = CinemaGoldLight,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }

                                    IconButton(
                                        onClick = { shareMovieMatch(context, item) },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Share,
                                            contentDescription = "Share Movie",
                                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                    // Edit details button
                                    Button(
                                        onClick = { activeEditItem = item },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                        ),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                                        modifier = Modifier.height(28.dp),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text("ORGANIZE", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                    }

                                    // Remove from watchlist
                                    IconButton(
                                        onClick = { viewModel.toggleWatchlist(item) },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Delete,
                                            contentDescription = "Remove from watchlist",
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Dialog to Organize Watchlist Notes & Status
    activeEditItem?.let { item ->
        var selectedStatus by remember { mutableStateOf(item.watchlistStatus) }
        var inputNotes by remember { mutableStateOf(item.watchlistNotes) }

        AlertDialog(
            shape = RoundedCornerShape(24.dp),
            onDismissRequest = { activeEditItem = null },
            title = {
                Text(
                    text = "ORGANIZE WATCHLIST ITEM",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 0.5.sp
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Configure details, tracking notes, and progress status for '${item.title}' in your watchlist.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "WATCHLIST STATUS",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            listOf("Plan to Watch", "Watching", "Completed").forEach { status ->
                                val isSelected = selectedStatus == status
                                Button(
                                    onClick = { selectedStatus = status },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                        contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                    ),
                                    modifier = Modifier.weight(1f).height(36.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Text(status, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "PERSONAL CINEMA NOTES",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        OutlinedTextField(
                            value = inputNotes,
                            onValueChange = { inputNotes = it },
                            placeholder = { Text("Add personal thoughts, rating details...", fontSize = 11.sp) },
                            textStyle = TextStyle(fontSize = 11.sp),
                            modifier = Modifier.fillMaxWidth(),
                            maxLines = 4,
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updateWatchlistDetails(item, selectedStatus, inputNotes)
                        activeEditItem = null
                    },
                    shape = RoundedCornerShape(50.dp)
                ) {
                    Text("SAVE CHANGES", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { activeEditItem = null },
                    shape = RoundedCornerShape(50.dp)
                ) {
                    Text("DISMISS", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

@Composable
fun DiscoverScreen(viewModel: MovieViewModel, context: Context) {
    val history by viewModel.searchHistory.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // TMDB Live Search & Filters Item
        item {
            TmdbLiveSearchSection(viewModel = viewModel, context = context)
        }

        // TMDB Live Trending Section Item
        item {
            TmdbTrendingSection(viewModel = viewModel, context = context)
        }

        // Share Taste Profile Card Item
        item {
            ShareTasteProfileCard(viewModel = viewModel, context = context)
        }

        // Recommendations Section
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(2.dp, CinemaGold.copy(alpha = 0.5f), RoundedCornerShape(24.dp)),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Header Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "CURATED DISCOVERY",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = CinemaGold,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = "Curated recommendations based on viewing history",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }

                        // Query recommendations trigger
                        Button(
                            onClick = { viewModel.fetchAIRecommendations() },
                            enabled = !viewModel.isRecommendationsLoading,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CinemaGold,
                                contentColor = Color.Black
                            ),
                            shape = RoundedCornerShape(50.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                            modifier = Modifier.height(34.dp).testTag("trigger_recommendations_button")
                        ) {
                            Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("DISCOVER", fontSize = 9.5.sp, fontWeight = FontWeight.Black)
                        }
                    }

                    Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))

                    if (viewModel.isRecommendationsLoading) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                CircularProgressIndicator(color = CinemaGoldLight)
                                Text(
                                    "CONSULTING CINEMA ENCYCLOPEDIA...",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CinemaGoldLight,
                                    letterSpacing = 1.sp
                                )
                            }
                        }
                    } else if (viewModel.recommendationsError != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Icon(Icons.Filled.Warning, contentDescription = null, tint = CinemaGoldLight, modifier = Modifier.size(32.dp))
                                Text(
                                    text = viewModel.recommendationsError!!,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    } else if (viewModel.recommendations.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(140.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Icon(Icons.Filled.Favorite, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f), modifier = Modifier.size(36.dp))
                                Text(
                                    text = "TAP DISCOVER TO INITIATE RECS",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                    letterSpacing = 0.8.sp
                                )
                                Text(
                                    text = "Your search history files will spark personalized cinematic suggestions.",
                                    fontSize = 9.5.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            viewModel.recommendations.forEach { recommendation ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(16.dp)),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)),
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.Top
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = recommendation.title.uppercase(),
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                Text(
                                                    text = "${recommendation.year}  |  Dir: ${recommendation.director}  |  Genre: ${recommendation.genre}",
                                                    fontSize = 9.5.sp,
                                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                                )
                                            }

                                            IconButton(
                                                onClick = {
                                                    val uri = if (recommendation.imdbUrl.startsWith("http")) recommendation.imdbUrl else "https://www.imdb.com"
                                                    val imdbIntent = Intent(Intent.ACTION_VIEW, Uri.parse(uri))
                                                    context.startActivity(imdbIntent)
                                                },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Filled.Star,
                                                    contentDescription = "IMDb page for ${recommendation.title}",
                                                    tint = CinemaGoldLight,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }
                                        }

                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                                                .padding(8.dp)
                                        ) {
                                            Text(
                                                text = "Recommendation reason: ${recommendation.rationale}",
                                                fontSize = 9.5.sp,
                                                fontStyle = FontStyle.Italic,
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                                            )
                                        }

                                        Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.05f))

                                        val feedback = viewModel.movieFeedbacks[recommendation.title]
                                        val thumbsVal = feedback?.first
                                        val starsVal = feedback?.second ?: 0

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                IconButton(
                                                    onClick = {
                                                        val newVal = if (thumbsVal == "LOVED") null else "LOVED"
                                                        viewModel.saveMovieFeedback(context, recommendation.title, newVal, starsVal)
                                                    },
                                                    modifier = Modifier.size(28.dp).testTag("loved_button_${recommendation.title.lowercase().replace(" ", "_")}")
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Filled.ThumbUp,
                                                        contentDescription = "Loved It",
                                                        tint = if (thumbsVal == "LOVED") Color(0xFF4CAF50) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }

                                                IconButton(
                                                    onClick = {
                                                        val newVal = if (thumbsVal == "NOT_INTERESTED") null else "NOT_INTERESTED"
                                                        viewModel.saveMovieFeedback(context, recommendation.title, newVal, starsVal)
                                                    },
                                                    modifier = Modifier.size(28.dp).testTag("not_interested_button_${recommendation.title.lowercase().replace(" ", "_")}")
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Filled.ThumbDown,
                                                        contentDescription = "Not Interested",
                                                        tint = if (thumbsVal == "NOT_INTERESTED") Color(0xFFF44336) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                            }

                                            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                                (1..5).forEach { starIndex ->
                                                    val isSelected = starIndex <= starsVal
                                                    Icon(
                                                        imageVector = Icons.Filled.Star,
                                                        contentDescription = "Rate $starIndex stars",
                                                        tint = if (isSelected) Color(0xFFFFB300) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f),
                                                        modifier = Modifier
                                                            .size(20.dp)
                                                            .clickable {
                                                                viewModel.saveMovieFeedback(context, recommendation.title, thumbsVal, starIndex)
                                                            }
                                                            .testTag("star_${starIndex}_${recommendation.title.lowercase().replace(" ", "_")}")
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // History Section Header
        item {
            HistoryHeader(viewModel, context)
        }

        // History Grid items list inline
        if (history.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Star,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                                modifier = Modifier.size(32.dp)
                             )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "NO HISTORIC RECORDS FOUND",
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Successfully identified scenes will be saved here.",
                            fontSize = 9.5.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(history, key = { it.id }) { item ->
                HistoryItemCard(item, viewModel, context)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FirebaseSyncDialog(
    viewModel: MovieViewModel,
    onDismiss: () -> Unit,
    onAdminOpen: () -> Unit = {},
    onSupportOpen: () -> Unit = {}
) {
    val context = LocalContext.current
    var customName by remember { mutableStateOf("") }
    var customEmail by remember { mutableStateOf("") }
    var newFavoriteInput by remember { mutableStateOf("") }

    val isDark = viewModel.isDarkMode
    val textColor = if (isDark) Color(0xFFF3F4F6) else Color(0xFF12131A)
    val labelColor = if (isDark) Color(0xFF9CA3AF) else Color(0xFF6B7280)
    val cardBg = if (isDark) GlassSlateCard else Color(0xFFF9FAFB)
    val borderCol = if (isDark) GlassBorderLight else Color(0xFFE5E7EB)
    val dialogBg = if (isDark) ObsidianDark else Color.White
    val primaryColor = if (isDark) ElectricCyan else Color(0xFF0284C7)

    // Language list for custom preference
    var selectedLanguage by remember { mutableStateOf("English") }
    val languages = listOf("English", "Español", "Français", "Deutsch", "日本語")

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = primaryColor, fontWeight = FontWeight.Bold)
            }
        },
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(primaryColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Settings, contentDescription = null, tint = primaryColor, modifier = Modifier.size(20.dp))
                }
                Column {
                    Text(
                        text = "Dashboard & Developer Hub",
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = textColor
                    )
                    Text(
                        text = "Settings, developer details & system architecture",
                        fontSize = 11.sp,
                        color = labelColor
                    )
                }
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // BENTO BOX: DEVELOPER DETAILS & SYSTEM ARCHITECTURE DASHBOARD
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, borderCol, RoundedCornerShape(16.dp))
                        .testTag("developer_dashboard_bento_card"),
                    colors = CardDefaults.cardColors(containerColor = cardBg),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(26.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(primaryColor.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Code,
                                    contentDescription = null,
                                    tint = primaryColor,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "DEVELOPER DETAILS",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.5.sp,
                                    color = primaryColor,
                                    letterSpacing = 0.5.sp
                                )
                                Text(
                                    text = "Lead engineer & system specifications",
                                    fontSize = 9.5.sp,
                                    color = labelColor
                                )
                            }
                        }

                        // Developer Profile Card
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isDark) Color(0xFF16171D) else Color.White,
                            border = BorderStroke(1.dp, borderCol),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(primaryColor.copy(alpha = 0.18f))
                                        .border(1.dp, primaryColor.copy(alpha = 0.45f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("BS", color = primaryColor, fontWeight = FontWeight.Black, fontSize = 13.sp)
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Benjamin Obeng Sosu", fontWeight = FontWeight.Bold, fontSize = 12.5.sp, color = textColor)
                                    Text("Lead Systems & AI Engineer", fontSize = 10.sp, color = labelColor)
                                    Text("benjaminobengsosu@gmail.com", fontSize = 9.5.sp, color = primaryColor)
                                }
                            }
                        }

                        // System Architecture Mini Bento Grid
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isDark) Color(0xFF16171D) else Color.White,
                                border = BorderStroke(1.dp, borderCol),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Text("AI ENGINE", fontSize = 8.5.sp, fontWeight = FontWeight.Bold, color = labelColor)
                                    Text("Gemini 3.5 Flash", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = textColor)
                                    Text("Multimodal Vision", fontSize = 9.sp, color = primaryColor)
                                }
                            }
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isDark) Color(0xFF16171D) else Color.White,
                                border = BorderStroke(1.dp, borderCol),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Text("CAMERA STACK", fontSize = 8.5.sp, fontWeight = FontWeight.Bold, color = labelColor)
                                    Text("CameraX 1.4", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = textColor)
                                    Text("Live Viewfinder", fontSize = 9.sp, color = primaryColor)
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isDark) Color(0xFF16171D) else Color.White,
                                border = BorderStroke(1.dp, borderCol),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Text("DATABASE", fontSize = 8.5.sp, fontWeight = FontWeight.Bold, color = labelColor)
                                    Text("Android Room", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = textColor)
                                    Text("Offline SQLite", fontSize = 9.sp, color = primaryColor)
                                }
                            }
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isDark) Color(0xFF16171D) else Color.White,
                                border = BorderStroke(1.dp, borderCol),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Text("DESIGN SYSTEM", fontSize = 8.5.sp, fontWeight = FontWeight.Bold, color = labelColor)
                                    Text("Frosted Glass UI", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = textColor)
                                    Text("Obsidian & Cyan", fontSize = 9.sp, color = primaryColor)
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(color = borderCol)

                // SECTION 1: PROFILE / FIREBASE
                Text(
                    text = "PROFILE & ACCOUNT SYNC",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 11.sp,
                    color = primaryColor,
                    letterSpacing = 0.5.sp
                )

                if (!viewModel.isLoggedIn) {
                    Text(
                        text = "Connect your account to backup your watchlist and Taste Profile settings securely in the cloud.",
                        fontSize = 11.sp,
                        color = labelColor
                    )

                    // Display custom credentials setup
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = customName,
                            onValueChange = { customName = it },
                            label = { Text("Display Name", fontSize = 11.sp, color = labelColor) },
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = TextStyle(fontSize = 12.5.sp, color = textColor),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = customEmail,
                            onValueChange = { customEmail = it },
                            label = { Text("Email Address", fontSize = 11.sp, color = labelColor) },
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = TextStyle(fontSize = 12.5.sp, color = textColor),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )

                        Button(
                            onClick = {
                                if (customName.isNotBlank() && customEmail.isNotBlank()) {
                                    viewModel.signInWithGoogle(context, customName, customEmail)
                                }
                            },
                            enabled = customName.isNotBlank() && customEmail.isNotBlank(),
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Connect Profile", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                } else {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = cardBg),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(primaryColor.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = (viewModel.userName?.firstOrNull()?.toString()?.uppercase() ?: "U"),
                                    color = primaryColor,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = viewModel.userName ?: "Authenticated User",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = textColor
                                )
                                Text(
                                    text = viewModel.userEmail ?: "",
                                    fontSize = 10.5.sp,
                                    color = labelColor
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFF4CAF50).copy(alpha = 0.12f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text("SYNCED", color = Color(0xFF4CAF50), fontSize = 8.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // Cloud Sync action row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { viewModel.syncWatchlistWithFirestore(context) },
                            enabled = !viewModel.isSyncing,
                            colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(if (viewModel.isSyncing) "Syncing..." else "Cloud Backup", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }

                        Button(
                            onClick = { viewModel.signOut(context) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFBA1A1A)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Sign Out", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }

                HorizontalDivider(color = borderCol)

                // SECTION: CREATOR ADMIN & SUPPORT
                Text(
                    text = "CREATOR ADMIN & SUPPORT",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 11.sp,
                    color = primaryColor,
                    letterSpacing = 0.5.sp
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (viewModel.isAdmin) {
                        Button(
                            onClick = {
                                onDismiss()
                                onAdminOpen()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFB300)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth().testTag("open_admin_panel_settings_button")
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("👑", fontSize = 14.sp)
                                Text("Open Creator Admin Control Panel", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    } else {
                        OutlinedButton(
                            onClick = {
                                viewModel.loginAsCreatorAdmin(context)
                                onDismiss()
                                onAdminOpen()
                            },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth().testTag("creator_admin_auth_shortcut")
                        ) {
                            Text("👑 Sign In as Creator Admin ($CREATOR_ADMIN_EMAIL)", fontSize = 11.sp, color = primaryColor)
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            onDismiss()
                            onSupportOpen()
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().testTag("open_support_ticket_button")
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("🎧", fontSize = 14.sp)
                            Text("Report Issue / Contact Support", fontSize = 12.sp, color = primaryColor, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                HorizontalDivider(color = borderCol)

                // SECTION 2: APPEARANCE PREFERENCES
                Text(
                    text = "APPEARANCE",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 11.sp,
                    color = primaryColor,
                    letterSpacing = 0.5.sp
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Choose App Theme style:",
                        fontSize = 11.sp,
                        color = textColor,
                        fontWeight = FontWeight.Bold
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("Light", "Dark", "System").forEach { themeMode ->
                            val isSelected = viewModel.appearancePreference == themeMode
                            Card(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        viewModel.saveAppearancePreference(context, themeMode)
                                        viewModel.showToast("Theme set to $themeMode")
                                    }
                                    .border(
                                        width = 1.dp,
                                        color = if (isSelected) primaryColor else borderCol,
                                        shape = RoundedCornerShape(10.dp)
                                    ),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) primaryColor.copy(alpha = 0.12f) else cardBg
                                ),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = themeMode,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) primaryColor else textColor
                                    )
                                }
                            }
                        }
                    }
                }

                Divider(color = borderCol)

                // SECTION 3: NOTIFICATIONS & LANGUAGE
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1.3f)) {
                        Text(
                            text = "Push Notifications",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = textColor
                        )
                        Text(
                            text = "Alert me on matches & releases",
                            fontSize = 10.5.sp,
                            color = labelColor
                        )
                    }
                    var notifyAllowed by remember { mutableStateOf(true) }
                    Switch(
                        checked = notifyAllowed,
                        onCheckedChange = {
                            notifyAllowed = it
                            viewModel.showToast(if (it) "Notifications Enabled!" else "Notifications Disabled")
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = primaryColor,
                            checkedTrackColor = primaryColor.copy(alpha = 0.4f)
                        )
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "App Language",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = textColor
                        )
                        Text(
                            text = "Localize theater interface",
                            fontSize = 10.5.sp,
                            color = labelColor
                        )
                    }

                    // Simple Language Selector Chips
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(cardBg)
                                .border(1.dp, borderCol, RoundedCornerShape(8.dp))
                                .clickable {
                                    val nextIndex = (languages.indexOf(selectedLanguage) + 1) % languages.size
                                    selectedLanguage = languages[nextIndex]
                                    viewModel.showToast("Language set to $selectedLanguage")
                                }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(Icons.Filled.Language, contentDescription = null, tint = primaryColor, modifier = Modifier.size(12.dp))
                                Text(selectedLanguage, fontSize = 11.sp, color = textColor, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                Divider(color = borderCol)

                // SECTION 4: FAVORITES LIST MANAGER
                Text(
                    text = "MY FAVORITE FILMS",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 11.sp,
                    color = primaryColor,
                    letterSpacing = 0.5.sp
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        OutlinedTextField(
                            value = newFavoriteInput,
                            onValueChange = { newFavoriteInput = it },
                            label = { Text("Add favorite movie", fontSize = 10.sp, color = labelColor) },
                            placeholder = { Text("e.g. Your Name", fontSize = 11.sp) },
                            modifier = Modifier.weight(1f),
                            textStyle = TextStyle(fontSize = 12.sp, color = textColor),
                            shape = RoundedCornerShape(10.dp),
                            singleLine = true
                        )

                        Button(
                            onClick = {
                                if (newFavoriteInput.isNotBlank()) {
                                    viewModel.onboardingFavoriteMovies = viewModel.onboardingFavoriteMovies + newFavoriteInput.trim()
                                    newFavoriteInput = ""
                                    viewModel.showToast("Favorite added!")
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Add", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }

                    // Chips layout of current favorite movies
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        viewModel.onboardingFavoriteMovies.forEach { fav ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(primaryColor.copy(alpha = 0.1f))
                                    .border(1.dp, primaryColor.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(fav, fontSize = 10.5.sp, color = textColor, fontWeight = FontWeight.Medium)
                                    Icon(
                                        imageVector = Icons.Filled.Delete,
                                        contentDescription = "Remove",
                                        tint = Color(0xFFBA1A1A),
                                        modifier = Modifier
                                            .size(12.dp)
                                            .clickable {
                                                viewModel.onboardingFavoriteMovies = viewModel.onboardingFavoriteMovies - fav
                                                viewModel.showToast("Removed favorite!")
                                            }
                                    )
                                }
                            }
                        }
                    }
                }

                Divider(color = borderCol)

                // SECTION 5: TASTE PROFILE MANAGER
                Text(
                    text = "FILM TASTE PROFILE",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 11.sp,
                    color = primaryColor,
                    letterSpacing = 1.sp
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = cardBg),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Current Personalized Genres:",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = textColor
                        )

                        Text(
                            text = if (viewModel.onboardingGenres.isEmpty()) "None selected" else viewModel.onboardingGenres.joinToString(),
                            fontSize = 10.5.sp,
                            color = labelColor
                        )

                        Text(
                            text = "Selected Theme Interests:",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = textColor
                        )

                        Text(
                            text = if (viewModel.onboardingInterests.isEmpty()) "None selected" else viewModel.onboardingInterests.joinToString(),
                            fontSize = 10.5.sp,
                            color = labelColor
                        )

                        Text(
                            text = "Content to Avoid:",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = textColor
                        )

                        Text(
                            text = if (viewModel.onboardingAvoidContent.isEmpty()) "None selected" else viewModel.onboardingAvoidContent.joinToString(),
                            fontSize = 10.5.sp,
                            color = Color(0xFFBA1A1A)
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Button(
                            onClick = {
                                viewModel.isFirstTimeUser = true
                                onDismiss()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.White)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("RE-RUN INITIAL ONBOARDING", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }

                Divider(color = borderCol)

                // SECTION 6: SCENE MATCHING ENGINE
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Scene Analysis Engine",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = textColor
                            )
                            Text(
                                text = "Select scene matching model",
                                fontSize = 10.5.sp,
                                color = labelColor
                            )
                        }
                        if (viewModel.isPremium) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(ElectricCyan.copy(alpha = 0.15f))
                                    .border(1.dp, ElectricCyan.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text("ACTIVE", color = ElectricCyan, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                            }
                        }
                    }

                    if (!viewModel.isPremium) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, ElectricCyan.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                .clickable { viewModel.togglePremium(context) },
                            colors = CardDefaults.cardColors(containerColor = ElectricCyan.copy(alpha = 0.05f)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(Icons.Filled.Tune, contentDescription = null, tint = ElectricCyan, modifier = Modifier.size(22.dp))
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Unlock Analysis Engine Selection",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = textColor
                                )
                                Text(
                                    text = "Click to upgrade and customize specialized scene matching models.",
                                    fontSize = 10.sp,
                                    color = labelColor,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf(
                                "gemini-3.5-flash" to "Standard • 3.5 Flash",
                                "gemini-3.1-pro-preview" to "Deep Analysis • 3.1 Pro",
                                "gemini-3.1-flash-lite" to "Rapid Scan • 3.1 Lite"
                            ).forEach { (modelKey, modelLabel) ->
                                val isSelected = viewModel.selectedModel == modelKey
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(34.dp)
                                        .clickable { viewModel.selectModel(context, modelKey) }
                                        .border(
                                            1.dp,
                                            if (isSelected) primaryColor else borderCol,
                                            RoundedCornerShape(8.dp)
                                        ),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isSelected) primaryColor.copy(alpha = 0.12f) else cardBg
                                    ),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        Text(
                                            text = modelLabel,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) primaryColor else textColor
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Divider(color = borderCol)

                // SYSTEM PIPELINE & ENGINE METRICS
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = cardBg),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Filled.Speed, contentDescription = null, tint = primaryColor, modifier = Modifier.size(14.dp))
                            Text("Engine & Storage Status", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = textColor)
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Vision Engine", fontSize = 10.5.sp, color = labelColor)
                            Text("Gemini 3.5 Flash", fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold, color = textColor)
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Watchlist Titles", fontSize = 10.5.sp, color = labelColor)
                            Text("${viewModel.watchlistHistory.value.size} Saved (SQLite Sync)", fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold, color = textColor)
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Local Database", fontSize = 10.5.sp, color = labelColor)
                            Text("Room SQLite Active", fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold, color = primaryColor)
                        }
                    }
                }

                Divider(color = borderCol)

                // SECTION 7: PRIVACY, SUPPORT & ABOUT
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = cardBg),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Filled.Info, contentDescription = null, tint = primaryColor, modifier = Modifier.size(14.dp))
                            Text("About Movie Finder", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = textColor)
                        }

                        Text(
                            text = "Company: Astute Business Consortium",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = textColor
                        )

                        Text(
                            text = "Support: benjaminobengsosu@gmail.com",
                            fontSize = 10.5.sp,
                            color = labelColor
                        )

                        Text(
                            text = "Privacy Policy: All scene recognition and search requests are processed securely. No personal watch details or media files are permanently stored outside your secure personal device and cloud databases.",
                            fontSize = 9.5.sp,
                            color = labelColor,
                            fontStyle = FontStyle.Italic
                        )
                    }
                }
            }
        },
        containerColor = dialogBg,
        shape = RoundedCornerShape(16.dp)
    )
}

@Composable
fun AuthScreen(viewModel: MovieViewModel) {
    val context = LocalContext.current
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var isSignUpMode by remember { mutableStateOf(false) }
    var isForgotMode by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(colors = listOf(Color(0xFF0F0B1F), Color(0xFF05030A)))),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Editorial Cinema Logo
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(GlassSlateCard)
                    .border(1.dp, ElectricCyan.copy(alpha = 0.4f), RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Movie,
                    contentDescription = "Movie Finder",
                    tint = ElectricCyan,
                    modifier = Modifier.size(32.dp)
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Movie Finder",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFF5F6F8),
                    letterSpacing = 0.5.sp
                )
                Text(
                    text = "Instant AI Scene & Film Identifier",
                    fontSize = 11.sp,
                    color = Color(0xFF8E93A4),
                    textAlign = TextAlign.Center,
                    letterSpacing = 0.3.sp
                )
            }

            // Input Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = GlassSlateCard),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, GlassBorderLight)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = when {
                            isForgotMode -> "Reset Password"
                            isSignUpMode -> "Create Account"
                            else -> "Sign In"
                        },
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color(0xFFF5F6F8),
                        letterSpacing = 0.2.sp
                    )

                    if (isSignUpMode && !isForgotMode) {
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("Display Name", color = Color(0xFF8E93A4), fontSize = 11.sp) },
                            modifier = Modifier.fillMaxWidth().testTag("auth_name_field"),
                            textStyle = TextStyle(color = Color.White, fontSize = 13.sp),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }

                    if (!isForgotMode || isSignUpMode) {
                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it },
                            label = { Text("Email", color = Color(0xFF8E93A4), fontSize = 11.sp) },
                            modifier = Modifier.fillMaxWidth().testTag("auth_email_field"),
                            textStyle = TextStyle(color = Color.White, fontSize = 13.sp),
                            shape = RoundedCornerShape(12.dp)
                        )

                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text("Password", color = Color(0xFF8E93A4), fontSize = 11.sp) },
                            modifier = Modifier.fillMaxWidth().testTag("auth_password_field"),
                            textStyle = TextStyle(color = Color.White, fontSize = 13.sp),
                            shape = RoundedCornerShape(12.dp)
                        )
                    } else {
                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it },
                            label = { Text("Email Address", color = Color(0xFF8E93A4), fontSize = 11.sp) },
                            modifier = Modifier.fillMaxWidth().testTag("auth_forgot_email_field"),
                            textStyle = TextStyle(color = Color.White, fontSize = 13.sp),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }

                    if (!isForgotMode && !isSignUpMode) {
                        Text(
                            text = "Forgot Password?",
                            color = ElectricCyan,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier
                                .clickable { isForgotMode = true }
                                .align(Alignment.End)
                        )
                    }

                    Button(
                        onClick = {
                            if (isForgotMode) {
                                viewModel.resetPassword(context, email)
                                isForgotMode = false
                            } else if (isSignUpMode) {
                                viewModel.registerWithEmailAndPassword(context, name, email, password)
                            } else {
                                viewModel.signInWithEmailAndPassword(context, email, password)
                            }
                        },
                        modifier = Modifier.fillMaxWidth().testTag("auth_primary_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ElectricCyan,
                            contentColor = Color(0xFF06080D)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = when {
                                isForgotMode -> "Send Reset Link"
                                isSignUpMode -> "Register Account"
                                else -> "Sign In"
                            },
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF06080D)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = when {
                                isForgotMode -> "Back to Sign In"
                                isSignUpMode -> "Already have an account? Sign In"
                                else -> "New to Movie Finder? Create Account"
                            },
                            color = Color(0xFF8C90A6),
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.clickable {
                                when {
                                    isForgotMode -> isForgotMode = false
                                    isSignUpMode -> isSignUpMode = false
                                    else -> isSignUpMode = true
                                }
                            }
                        )
                    }
                }
            }

            // Google Sign In, Creator Admin & Guest options
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("or connect securely", fontSize = 11.sp, color = Color(0xFF8C90A6))

                // Admin Sign In Button
                Button(
                    onClick = {
                        viewModel.loginAsCreatorAdmin(context)
                    },
                    modifier = Modifier.fillMaxWidth().testTag("creator_admin_auth_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = GlassSlateCard),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, ElectricCyan.copy(alpha = 0.5f))
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(vertical = 2.dp)
                    ) {
                        Icon(Icons.Filled.AdminPanelSettings, contentDescription = null, tint = ElectricCyan, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Sign In as Administrator", color = ElectricCyan, fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // Google button
                Button(
                    onClick = {
                        viewModel.signInWithGoogle(context, "Alex Mercer", "alex.mercer@gmail.com")
                    },
                    modifier = Modifier.fillMaxWidth().testTag("google_auth_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF181A22)),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFF222430))
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(vertical = 2.dp)
                    ) {
                        Icon(Icons.Filled.AccountCircle, contentDescription = null, tint = Color(0xFFF5F6F8), modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Sign In with Google", color = Color(0xFFF5F6F8), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                if (viewModel.remoteEnableGuestMode) {
                    TextButton(
                        onClick = { viewModel.continueAsGuest(context) },
                        modifier = Modifier.testTag("guest_mode_button")
                    ) {
                        Text(
                            text = "Continue as Guest",
                            color = Color(0xFF8E93A4),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    Text(
                        text = "🔒 Guest Mode disabled remotely by Admin",
                        fontSize = 11.sp,
                        color = Color(0xFFFF5252),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OnboardingScreen(viewModel: MovieViewModel) {
    val context = LocalContext.current
    var currentStep by remember { mutableStateOf(0) }

    // Onboarding preferences state
    val genresList = listOf("Action", "Adventure", "Comedy", "Romance", "Horror", "Sci-Fi", "Fantasy", "Mystery", "Crime", "Drama", "Anime", "Animation", "Documentary", "Family")
    val interestsList = listOf("Superheroes", "Space", "Sci-Fi & Cyberpunk", "Time Travel", "Magic", "Survival", "History", "Sports", "Technology", "Cars", "Music", "Military", "School Life", "Business")
    val avoidList = listOf("Horror", "Violence", "Romance", "Slow-paced movies", "Sad endings")

    var selectedGenres by remember { mutableStateOf(setOf<String>()) }
    var selectedInterests by remember { mutableStateOf(setOf<String>()) }
    var customFavoriteInput by remember { mutableStateOf("") }
    var favoriteMoviesList by remember { mutableStateOf(listOf<String>()) }
    var selectedAvoids by remember { mutableStateOf(setOf<String>()) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF12131A)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp)
        ) {
            // Steps indicator
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "TASTE PROFILE SETUP",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 11.sp,
                    color = ElectricCyan,
                    letterSpacing = 1.sp
                )

                Text(
                    text = "Step ${currentStep + 1} of 5",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF8E93A4)
                )
            }

            // Smooth linear step progress indicator
            LinearProgressIndicator(
                progress = { (currentStep + 1) / 5f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(50)),
                color = ElectricCyan,
                trackColor = Color(0xFF222430)
            )

            when (currentStep) {
                0 -> {
                    // Welcome onboarding splash screen
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.padding(vertical = 24.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(68.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .background(GlassSlateCard)
                                .border(1.dp, ElectricCyan.copy(alpha = 0.4f), RoundedCornerShape(18.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Movie,
                                contentDescription = "Movie Finder",
                                tint = ElectricCyan,
                                modifier = Modifier.size(32.dp)
                            )
                        }

                        Text(
                            text = "Movie Finder",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFF5F6F8),
                            textAlign = TextAlign.Center,
                            letterSpacing = 0.5.sp
                        )

                        Text(
                            text = "Personalize your movie profile. Build a tailored taste profile to discover films and match scenes with precision.",
                            fontSize = 12.5.sp,
                            color = Color(0xFF8E93A4),
                            textAlign = TextAlign.Center,
                            lineHeight = 18.sp
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = { currentStep = 1 },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ElectricCyan,
                                contentColor = Color(0xFF06080D)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().testTag("onboarding_start_button")
                        ) {
                            Text("Get Started", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF06080D))
                        }
                    }
                }

                1 -> {
                    // STEP 1: FAVORITE GENRES
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = "Select Your Favorite Genres",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Which categories of film make you lose track of time? Select multiple.",
                            fontSize = 11.5.sp,
                            color = Color(0xFF8C90A6)
                        )

                        FlowRow(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            genresList.forEach { gen ->
                                val isSelected = selectedGenres.contains(gen)
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(if (isSelected) ElectricCyan else GlassSlateCard)
                                        .border(
                                            1.dp,
                                            if (isSelected) ElectricCyan else Color(0xFF222430),
                                            RoundedCornerShape(20.dp)
                                        )
                                        .clickable {
                                            selectedGenres = if (isSelected) selectedGenres - gen else selectedGenres + gen
                                        }
                                        .padding(horizontal = 14.dp, vertical = 8.dp)
                                        .testTag("genre_chip_${gen.lowercase()}")
                                ) {
                                    Text(
                                        text = gen,
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) Color(0xFF06080D) else Color(0xFF8E93A4)
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Button(
                                onClick = { currentStep = 0 },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = GlassSlateCard,
                                    contentColor = Color(0xFFF5F6F8)
                                ),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, Color(0xFF222430))
                            ) {
                                Text("Back", color = Color(0xFFF5F6F8), fontSize = 12.sp)
                            }

                            Button(
                                onClick = { currentStep = 2 },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = ElectricCyan,
                                    contentColor = Color(0xFF06080D)
                                ),
                                shape = RoundedCornerShape(12.dp),
                                enabled = selectedGenres.isNotEmpty(),
                                modifier = Modifier.testTag("genres_next_button")
                            ) {
                                Text("Next Step", color = Color(0xFF06080D), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                2 -> {
                    // STEP 2: INTERESTS
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = "Themes & Interests",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFF5F6F8),
                            letterSpacing = 0.2.sp
                        )
                        Text(
                            text = "What elements do you look for in a great story? Select multiple.",
                            fontSize = 11.5.sp,
                            color = Color(0xFF8E93A4)
                        )

                        FlowRow(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            interestsList.forEach { interest ->
                                val isSelected = selectedInterests.contains(interest)
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(if (isSelected) ElectricCyan else GlassSlateCard)
                                        .border(
                                            1.dp,
                                            if (isSelected) ElectricCyan else Color(0xFF222430),
                                            RoundedCornerShape(20.dp)
                                        )
                                        .clickable {
                                            selectedInterests = if (isSelected) selectedInterests - interest else selectedInterests + interest
                                        }
                                        .padding(horizontal = 14.dp, vertical = 8.dp)
                                        .testTag("interest_chip_${interest.lowercase().replace(" ", "_")}")
                                ) {
                                    Text(
                                        text = interest,
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) Color(0xFF06080D) else Color(0xFF8E93A4)
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Button(
                                onClick = { currentStep = 1 },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = GlassSlateCard,
                                    contentColor = Color(0xFFF5F6F8)
                                ),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, Color(0xFF222430))
                            ) {
                                Text("Back", color = Color(0xFFF5F6F8), fontSize = 12.sp)
                            }

                            Button(
                                onClick = { currentStep = 3 },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = ElectricCyan,
                                    contentColor = Color(0xFF06080D)
                                ),
                                shape = RoundedCornerShape(12.dp),
                                enabled = selectedInterests.isNotEmpty(),
                                modifier = Modifier.testTag("interests_next_button")
                            ) {
                                Text("Next Step", color = Color(0xFF06080D), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                3 -> {
                    // STEP 3: FAVORITE MOVIES (3-10 user movies)
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = "Add Your All-Time Favorites",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFF5F6F8),
                            letterSpacing = 0.2.sp
                        )
                        Text(
                            text = "Add between 3 to 10 films you love. This list will tailor your recommendations and film curation.",
                            fontSize = 11.5.sp,
                            color = Color(0xFF8E93A4)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = customFavoriteInput,
                                onValueChange = { customFavoriteInput = it },
                                label = { Text("Movie title", color = Color(0xFF8E93A4)) },
                                modifier = Modifier.weight(1f).testTag("onboarding_movie_field"),
                                textStyle = TextStyle(color = Color.White, fontSize = 13.sp),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true
                            )

                            Button(
                                onClick = {
                                    if (customFavoriteInput.isNotBlank()) {
                                        favoriteMoviesList = favoriteMoviesList + customFavoriteInput.trim()
                                        customFavoriteInput = ""
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = ElectricCyan,
                                    contentColor = Color(0xFF06080D)
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.testTag("onboarding_add_movie_button")
                            ) {
                                Text("Add", color = Color(0xFF06080D), fontWeight = FontWeight.Bold)
                            }
                        }

                        // Curated suggestions
                        Text(
                            text = "Quick Suggestion: Tap to add instantly",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = ElectricCyan
                        )

                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf("Inception", "Interstellar", "Your Name", "The Matrix", "Spirited Away", "Dune", "Your Lie in April", "Demon Slayer").forEach { suggestion ->
                                val isAlreadyAdded = favoriteMoviesList.contains(suggestion)
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isAlreadyAdded) ElectricCyan else GlassSlateCard)
                                        .border(1.dp, if (isAlreadyAdded) ElectricCyan else Color(0xFF222430), RoundedCornerShape(8.dp))
                                        .clickable {
                                            if (!isAlreadyAdded) {
                                                favoriteMoviesList = favoriteMoviesList + suggestion
                                            }
                                        }
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text(suggestion, fontSize = 10.5.sp, color = if (isAlreadyAdded) Color(0xFF06080D) else Color(0xFF8E93A4))
                                }
                            }
                        }

                        Text(
                            text = "Movies added: ${favoriteMoviesList.size} (Minimum 3 recommended)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (favoriteMoviesList.size >= 3) Color(0xFF4CAF50) else ElectricCyan
                        )

                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            favoriteMoviesList.forEach { movie ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(GlassSlateCard)
                                        .border(1.dp, ElectricCyan.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text(movie, fontSize = 11.sp, color = Color(0xFFF5F6F8))
                                        Icon(
                                            imageVector = Icons.Filled.Delete,
                                            contentDescription = "Remove",
                                            tint = Color(0xFFBA1A1A),
                                            modifier = Modifier
                                                .size(12.dp)
                                                .clickable {
                                                    favoriteMoviesList = favoriteMoviesList - movie
                                                }
                                        )
                                    }
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Button(
                                onClick = { currentStep = 2 },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = GlassSlateCard,
                                    contentColor = Color(0xFFF5F6F8)
                                ),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, Color(0xFF222430))
                            ) {
                                Text("Back", color = Color(0xFFF5F6F8), fontSize = 12.sp)
                            }

                            Button(
                                onClick = { currentStep = 4 },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = ElectricCyan,
                                    contentColor = Color(0xFF06080D)
                                ),
                                shape = RoundedCornerShape(12.dp),
                                enabled = favoriteMoviesList.isNotEmpty(),
                                modifier = Modifier.testTag("favorites_next_button")
                            ) {
                                Text("Next Step", color = Color(0xFF06080D), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                4 -> {
                    // STEP 4: AVOID CONTENT
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = "What would you rather avoid?",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFF5F6F8),
                            letterSpacing = 0.2.sp
                        )
                        Text(
                            text = "Select styles or themes you prefer to exclude from your recommendations.",
                            fontSize = 11.5.sp,
                            color = Color(0xFF8E93A4)
                        )

                        FlowRow(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            avoidList.forEach { avoid ->
                                val isSelected = selectedAvoids.contains(avoid)
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(if (isSelected) Color(0xFFBA1A1A).copy(alpha = 0.3f) else Color(0xFF181A22))
                                        .border(
                                            1.dp,
                                            if (isSelected) Color(0xFFBA1A1A) else Color(0xFF222430),
                                            RoundedCornerShape(20.dp)
                                        )
                                        .clickable {
                                            selectedAvoids = if (isSelected) selectedAvoids - avoid else selectedAvoids + avoid
                                        }
                                        .padding(horizontal = 14.dp, vertical = 8.dp)
                                        .testTag("avoid_chip_${avoid.lowercase().replace(" ", "_")}")
                                ) {
                                    Text(
                                        text = avoid,
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) Color(0xFFFF897D) else Color(0xFF8E93A4)
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Button(
                                onClick = { currentStep = 3 },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF181A22),
                                    contentColor = Color(0xFFF5F6F8)
                                ),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, Color(0xFF222430))
                            ) {
                                Text("Back", color = Color(0xFFF5F6F8), fontSize = 12.sp)
                            }

                            Button(
                                onClick = {
                                    viewModel.completeOnboarding(
                                        context = context,
                                        genres = selectedGenres.toList(),
                                        interests = selectedInterests.toList(),
                                        movies = favoriteMoviesList,
                                        avoids = selectedAvoids.toList()
                                    )
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = ElectricCyan,
                                    contentColor = Color(0xFF06080D)
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.testTag("onboarding_finish_button")
                            ) {
                                Text("Complete Setup", color = Color(0xFF06080D), fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PersonalizedMovieRow(
    rowTitle: String,
    movies: List<CuratedMovie>,
    viewModel: MovieViewModel,
    context: Context
) {
    var selectedMovieDetail by remember { mutableStateOf<CuratedMovie?>(null) }
    val coroutineScope = rememberCoroutineScope()

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = rowTitle,
            fontSize = 15.sp,
            fontWeight = FontWeight.ExtraBold,
            color = if (viewModel.isDarkMode) Color.White else Color(0xFF13113C),
            modifier = Modifier.padding(horizontal = 4.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            movies.forEach { movie ->
                Card(
                    modifier = Modifier
                        .width(110.dp)
                        .clickable { selectedMovieDetail = movie }
                        .testTag("curated_movie_card_${movie.title.lowercase().replace(" ", "_")}"),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(150.dp)
                        ) {
                            TmdbMoviePoster(
                                posterUrl = movie.imageUrl,
                                title = movie.title,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        Text(
                            text = movie.title,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }

    if (selectedMovieDetail != null) {
        val detailMovie = selectedMovieDetail!!
        AlertDialog(
            onDismissRequest = { selectedMovieDetail = null },
            confirmButton = {
                TextButton(onClick = { selectedMovieDetail = null }) {
                    Text("Close", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                }
            },
            title = {
                Text(
                    text = detailMovie.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = if (viewModel.isDarkMode) Color.White else Color(0xFF13113C)
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.verticalScroll(rememberScrollState())
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .clip(RoundedCornerShape(14.dp))
                    ) {
                        AsyncImage(
                            model = detailMovie.imageUrl,
                            contentDescription = detailMovie.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    Text(
                        text = "${detailMovie.year}  |  Dir: ${detailMovie.director}  |  Genre: ${detailMovie.genre}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (viewModel.isDarkMode) Color(0xFF8C90A6) else Color(0xFF72718A)
                    )

                    Text(
                        text = detailMovie.rationale,
                        fontSize = 13.sp,
                        color = if (viewModel.isDarkMode) Color.White else Color(0xFF1C1B1F)
                    )

                    Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))

                    // Rating and feedback layout inside details dialog!
                    Text(
                        text = "Rate or Give Feedback:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (viewModel.isDarkMode) Color.White else Color(0xFF13113C)
                    )

                    val feedback = viewModel.movieFeedbacks[detailMovie.title]
                    val thumbsVal = feedback?.first
                    val starsVal = feedback?.second ?: 0

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Thumbs Up
                        IconButton(
                            onClick = {
                                val newVal = if (thumbsVal == "LOVED") null else "LOVED"
                                viewModel.saveMovieFeedback(context, detailMovie.title, newVal, starsVal)
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Filled.ThumbUp,
                                contentDescription = "Loved It",
                                tint = if (thumbsVal == "LOVED") Color(0xFF4CAF50) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                            )
                        }

                        // Thumbs Down
                        IconButton(
                            onClick = {
                                val newVal = if (thumbsVal == "NOT_INTERESTED") null else "NOT_INTERESTED"
                                viewModel.saveMovieFeedback(context, detailMovie.title, newVal, starsVal)
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Filled.ThumbDown,
                                contentDescription = "Not Interested",
                                tint = if (thumbsVal == "NOT_INTERESTED") Color(0xFFF44336) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                            )
                        }

                        Spacer(modifier = Modifier.weight(1f))

                        // Star ratings (1-5)
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            (1..5).forEach { starIndex ->
                                val isSelected = starIndex <= starsVal
                                Icon(
                                    imageVector = Icons.Filled.Star,
                                    contentDescription = "Rate $starIndex stars",
                                    tint = if (isSelected) Color(0xFFFFB300) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clickable {
                                            viewModel.saveMovieFeedback(context, detailMovie.title, thumbsVal, starIndex)
                                        }
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                // Add as an identified match to Watchlist database!
                                val dummyMatch = MovieMatch(
                                    title = detailMovie.title,
                                    year = detailMovie.year,
                                    director = detailMovie.director,
                                    imdbUrl = detailMovie.imdbUrl,
                                    confidence = "High",
                                    rationale = detailMovie.rationale,
                                    actors = "All Star Cast",
                                    genre = detailMovie.genre,
                                    searchType = "TEXT",
                                    isInWatchlist = true,
                                    watchlistStatus = "Plan to Watch",
                                    watchlistNotes = "Added from Curated Home Feed"
                                )
                                coroutineScope.launch {
                                    viewModel.repository.insertMatch(dummyMatch)
                                    viewModel.showToast("${detailMovie.title} added to Watchlist!")
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Watchlist", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }

                        Button(
                            onClick = {
                                val uri = Uri.parse(detailMovie.imdbUrl)
                                val intent = Intent(Intent.ACTION_VIEW, uri)
                                context.startActivity(intent)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFB300)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("IMDb Link", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                        }
                    }
                }
            },
            containerColor = if (viewModel.isDarkMode) Color(0xFF131524) else Color.White,
            shape = RoundedCornerShape(24.dp)
        )
    }
}

data class CuratedMovie(
    val title: String,
    val year: String,
    val director: String,
    val genre: String,
    val imageUrl: String,
    val imdbUrl: String,
    val rationale: String,
    val isAnime: Boolean = false
)

val CuratedMoviesList = listOf(
    CuratedMovie(
        title = "Inception",
        year = "2010",
        director = "Christopher Nolan",
        genre = "Sci-Fi / Thriller",
        imageUrl = "https://image.tmdb.org/t/p/w500/9gk7adHYeHCm0mS7A6Yv6o60ZST.jpg",
        imdbUrl = "https://www.imdb.com/title/tt1375666/",
        rationale = "A mind-bending heist thriller set inside the architecture of dreams."
    ),
    CuratedMovie(
        title = "Dune: Part Two",
        year = "2024",
        director = "Denis Villeneuve",
        genre = "Sci-Fi / Adventure",
        imageUrl = "https://image.tmdb.org/t/p/w500/czemb7GEg9TyR6QZ36P6gslSziH.jpg",
        imdbUrl = "https://www.imdb.com/title/tt15239678/",
        rationale = "The mythic journey of Paul Atreides continues in a spectacular visual masterpiece."
    ),
    CuratedMovie(
        title = "Spirited Away",
        year = "2001",
        director = "Hayao Miyazaki",
        genre = "Anime / Fantasy",
        imageUrl = "https://image.tmdb.org/t/p/w500/393mh146v061o6gMfwE6P066Fm3.jpg",
        imdbUrl = "https://www.imdb.com/title/tt0245429/",
        rationale = "An enchanting hand-drawn story of a girl lost in an immersive world of spirits and magic.",
        isAnime = true
    ),
    CuratedMovie(
        title = "Interstellar",
        year = "2014",
        director = "Christopher Nolan",
        genre = "Sci-Fi / Drama",
        imageUrl = "https://image.tmdb.org/t/p/w500/gEU2Qv6G37uBhYfrok7suAlavND.jpg",
        imdbUrl = "https://www.imdb.com/title/tt0816692/",
        rationale = "A spectacular cosmic odyssey exploring humanity's survival and the bounds of love across space and time."
    ),
    CuratedMovie(
        title = "Your Name",
        year = "2016",
        director = "Makoto Shinkai",
        genre = "Anime / Romance",
        imageUrl = "https://image.tmdb.org/t/p/w500/q786OJe8S0St8g8219YCBHgHMTe.jpg",
        imdbUrl = "https://www.imdb.com/title/tt5311514/",
        rationale = "A breathtaking fantasy romance following two high schoolers who magically swap bodies.",
        isAnime = true
    ),
    CuratedMovie(
        title = "The Dark Knight",
        year = "2008",
        director = "Christopher Nolan",
        genre = "Action / Crime",
        imageUrl = "https://image.tmdb.org/t/p/w500/qJ2tWvXgIM6uI6Cg6A2vX5gHGg4.jpg",
        imdbUrl = "https://www.imdb.com/title/tt0468569/",
        rationale = "A gritty, unforgettable battle of morals and chaos between Batman and the Joker."
    ),
    CuratedMovie(
        title = "Spider-Man: Across the Spider-Verse",
        year = "2023",
        director = "Kemp Powers",
        genre = "Animation / Action",
        imageUrl = "https://image.tmdb.org/t/p/w500/8Vtbi7SuCCto760fxe6ofn07gjC.jpg",
        imdbUrl = "https://www.imdb.com/title/tt9362722/",
        rationale = "An artistic and narrative triumph stretching across multiple parallel web-slinging universes."
    ),
    CuratedMovie(
        title = "Everything Everywhere All at Once",
        year = "2022",
        director = "Daniel Kwan",
        genre = "Sci-Fi / Comedy",
        imageUrl = "https://image.tmdb.org/t/p/w500/w36H0mALv7mYgYUiH6u7mZ00v7t.jpg",
        imdbUrl = "https://www.imdb.com/title/tt6710474/",
        rationale = "An insanely creative multiverse family drama packed with absurd comedy and heart-wrenching truths."
    ),
    CuratedMovie(
        title = "Demon Slayer: Mugen Train",
        year = "2020",
        director = "Haruo Sotozaki",
        genre = "Anime / Action",
        imageUrl = "https://image.tmdb.org/t/p/w500/h8g6Xg36b9nsS8668v976m8gS35.jpg",
        imdbUrl = "https://www.imdb.com/title/tt11030574/",
        rationale = "A thrilling high-stakes demon-slaying battle inside a cursed train.",
        isAnime = true
    ),
    CuratedMovie(
        title = "Suzume",
        year = "2022",
        director = "Makoto Shinkai",
        genre = "Anime / Fantasy",
        imageUrl = "https://image.tmdb.org/t/p/w500/v9SOf966pSu7uI08V96ofmI262A.jpg",
        imdbUrl = "https://www.imdb.com/title/tt16428456/",
        rationale = "A modern-day fantasy journey across Japan to close the supernatural portals causing disasters.",
        isAnime = true
    ),
    CuratedMovie(
        title = "A Silent Voice",
        year = "2016",
        director = "Naoko Yamada",
        genre = "Anime / Drama",
        imageUrl = "https://image.tmdb.org/t/p/w500/tu5Ct779GuY6Yv9vNhvkgGaPDvj.jpg",
        imdbUrl = "https://www.imdb.com/title/tt5323662/",
        rationale = "A profoundly touching drama of redemption, forgiveness, and human connection.",
        isAnime = true
    ),
    CuratedMovie(
        title = "Princess Mononoke",
        year = "1997",
        director = "Hayao Miyazaki",
        genre = "Anime / Adventure",
        imageUrl = "https://image.tmdb.org/t/p/w500/m78gRLg9Y6C03mIAsN2K8n7vB9D.jpg",
        imdbUrl = "https://www.imdb.com/title/tt0119698/",
        rationale = "A sweeping epic struggle between industrial civilization and the guardians of nature.",
        isAnime = true
    ),
    CuratedMovie(
        title = "The Matrix",
        year = "1999",
        director = "Lana Wachowski",
        genre = "Sci-Fi / Action",
        imageUrl = "https://image.tmdb.org/t/p/w500/f89U3wLgYv76K9SgWv7YqgT90gq.jpg",
        imdbUrl = "https://www.imdb.com/title/tt0133093/",
        rationale = "The revolutionary cyberpunk classic questioning the reality of our physical existence."
    ),
    CuratedMovie(
        title = "Blade Runner 2049",
        year = "2017",
        director = "Denis Villeneuve",
        genre = "Sci-Fi / Mystery",
        imageUrl = "https://image.tmdb.org/t/p/w500/gajva2L0YvjvS7URgJgJJmS26jg.jpg",
        imdbUrl = "https://www.imdb.com/title/tt1856101/",
        rationale = "A visually stunning cyberpunk detective story investigating the boundary of human souls."
    )
)
