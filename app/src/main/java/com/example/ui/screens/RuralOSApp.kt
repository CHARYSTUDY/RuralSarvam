package com.example.ui.screens

import android.widget.Toast
import android.speech.SpeechRecognizer
import android.speech.RecognizerIntent
import android.speech.RecognitionListener
import android.speech.tts.TextToSpeech
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.content.Intent
import android.os.Bundle
import java.util.Locale
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.compose.ui.draw.scale
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.RuralViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RuralOSApp(viewModel: RuralViewModel) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Observe Database & Viewmodel State
    val profile by viewModel.userProfile.collectAsStateWithLifecycle()
    val jobs by viewModel.jobs.collectAsStateWithLifecycle()
    val equipmentList by viewModel.equipmentList.collectAsStateWithLifecycle()
    val bookings by viewModel.bookings.collectAsStateWithLifecycle()
    val products by viewModel.products.collectAsStateWithLifecycle()
    val notices by viewModel.notices.collectAsStateWithLifecycle()

    val isOnline by viewModel.isOnline.collectAsStateWithLifecycle()
    val notifications by viewModel.inAppNotifications.collectAsStateWithLifecycle()

    // Navigation state
    var currentTab by remember { mutableStateOf("home") } // home, ai, market, services

    // Active sub-tabs
    var aiSubTab by remember { mutableStateOf("offline") } // agri, health, offline
    var marketSubTab by remember { mutableStateOf("produce") } // jobs, equipment, produce
    var servicesSubTab by remember { mutableStateOf("schemes") } // notices, schemes, learning, IoT

    // Dialog state
    var showProfileDialog by remember { mutableStateOf(false) }
    var showNotificationDialog by remember { mutableStateOf(false) }
    var showRoleSelector by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Agriculture,
                            contentDescription = "Logo",
                            tint = GreenPrimary,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "RuralOS",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = GreenPrimary
                                )
                            )
                            Text(
                                text = "Smart Rural Platform",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = MediumGrey
                                )
                            )
                        }
                    }
                },
                actions = {
                    // Online/Offline status toggler
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (isOnline) GreenLight else OrangeLight
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .clickable { viewModel.toggleOnlineMode() }
                            .padding(end = 8.dp)
                            .testTag("online_toggle")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(
                                        color = if (isOnline) GreenPrimary else OrangeAccent,
                                        shape = CircleShape
                                    )
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isOnline) "Online" else "Offline Caching",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isOnline) GreenPrimaryDark else OrangeAccent
                            )
                        }
                    }

                    // Notification bell
                    IconButton(
                        onClick = { showNotificationDialog = true },
                        modifier = Modifier.testTag("notification_button")
                    ) {
                        BadgedBox(
                            badge = {
                                if (notifications.isNotEmpty()) {
                                    Badge { Text(notifications.size.toString()) }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = "Alerts"
                            )
                        }
                    }

                    // User Profile Icon
                    IconButton(
                        onClick = { showProfileDialog = true },
                        modifier = Modifier.testTag("profile_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccountCircle,
                            contentDescription = "Profile",
                            tint = GreenPrimary,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                windowInsets = WindowInsets.navigationBars
            ) {
                NavigationBarItem(
                    selected = currentTab == "home",
                    onClick = { currentTab = "home" },
                    icon = { Icon(Icons.Default.Dashboard, contentDescription = "Home") },
                    label = { Text("Home", fontSize = 11.sp) },
                    modifier = Modifier.testTag("nav_home")
                )
                NavigationBarItem(
                    selected = currentTab == "ai",
                    onClick = { currentTab = "ai" },
                    icon = { Icon(Icons.Default.SmartToy, contentDescription = "AI") },
                    label = { Text("AI Assist", fontSize = 11.sp) },
                    modifier = Modifier.testTag("nav_ai")
                )
                NavigationBarItem(
                    selected = currentTab == "market",
                    onClick = { currentTab = "market" },
                    icon = { Icon(Icons.Default.ShoppingBag, contentDescription = "Market") },
                    label = { Text("Cooperative", fontSize = 11.sp) },
                    modifier = Modifier.testTag("nav_market")
                )
                NavigationBarItem(
                    selected = currentTab == "services",
                    onClick = { currentTab = "services" },
                    icon = { Icon(Icons.Default.Widgets, contentDescription = "Services") },
                    label = { Text("Services", fontSize = 11.sp) },
                    modifier = Modifier.testTag("nav_services")
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Offline Mode Banner Indicator
            AnimatedVisibility(
                visible = !isOnline,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(OrangeLight)
                        .padding(vertical = 4.dp, horizontal = 16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudOff,
                            contentDescription = "Offline Mode",
                            tint = OrangeAccent,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Working Offline. Data cached locally & will sync when network returns.",
                            color = OrangeAccent,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // Screen Switching Engine
            Box(modifier = Modifier.weight(1f)) {
                when (currentTab) {
                    "home" -> DashboardScreen(
                        viewModel = viewModel,
                        profile = profile,
                        notices = notices,
                        onNavigate = { tab, subTab ->
                            currentTab = tab
                            if (tab == "ai") aiSubTab = subTab
                            if (tab == "market") marketSubTab = subTab
                            if (tab == "services") servicesSubTab = subTab
                        },
                        onChangeRole = { showRoleSelector = true }
                    )
                    "ai" -> AIScreen(
                        viewModel = viewModel,
                        activeSubTab = aiSubTab,
                        onSubTabChange = { aiSubTab = it }
                    )
                    "market" -> MarketplaceHubScreen(
                        viewModel = viewModel,
                        activeSubTab = marketSubTab,
                        onSubTabChange = { marketSubTab = it },
                        profile = profile,
                        jobs = jobs,
                        equipmentList = equipmentList,
                        bookings = bookings,
                        products = products
                    )
                    "services" -> ServicesScreen(
                        viewModel = viewModel,
                        activeSubTab = servicesSubTab,
                        onSubTabChange = { servicesSubTab = it },
                        profile = profile,
                        notices = notices
                    )
                }
            }
        }
    }

    // Modal Notifications Drawer Dialog
    if (showNotificationDialog) {
        Dialog(onDismissRequest = { showNotificationDialog = false }) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Smart Notifications",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                        )
                        IconButton(onClick = { showNotificationDialog = false }) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    if (notifications.isEmpty()) {
                        Text(
                            text = "No active alerts. All systems running smooth.",
                            color = MediumGrey,
                            modifier = Modifier.padding(vertical = 16.dp)
                        )
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.heightIn(max = 400.dp)
                        ) {
                            items(notifications) { alert ->
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = LightBackground
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Info,
                                            contentDescription = "Alert",
                                            tint = GreenPrimary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(text = alert, fontSize = 13.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal Role Selector Dialog
    if (showRoleSelector) {
        Dialog(onDismissRequest = { showRoleSelector = false }) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Switch User Experience",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "Switching profiles instantly reconfigures dashboards, tools, and local permissions.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MediumGrey,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    val rolesList = listOf(
                        Triple("Farmer", Icons.Default.Grass, GreenPrimary),
                        Triple("Laborer", Icons.Default.Engineering, OrangeAccent),
                        Triple("Equipment Owner", Icons.Default.Construction, BlueSecondary),
                        Triple("Panchayat/Authority", Icons.Default.AccountBalance, Color(0xFF9C27B0)),
                        Triple("Healthcare Worker", Icons.Default.LocalHospital, Color(0xFF009688)),
                        Triple("Admin", Icons.Default.AdminPanelSettings, Color(0xFF37474F))
                    )

                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(rolesList) { (roleName, icon, color) ->
                            Card(
                                onClick = {
                                    viewModel.switchUserRole(roleName)
                                    showRoleSelector = false
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("role_select_$roleName"),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (profile?.role == roleName) color.copy(alpha = 0.15f) else LightBackground
                                ),
                                border = if (profile?.role == roleName) ButtonDefaults.outlinedButtonBorder else null
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = roleName,
                                        tint = color,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Text(
                                        text = roleName,
                                        fontWeight = FontWeight.Bold,
                                        color = if (profile?.role == roleName) color else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal Profile Editor Dialog
    if (showProfileDialog) {
        val p = profile ?: UserProfile()
        var editName by remember { mutableStateOf(p.name) }
        var editMobile by remember { mutableStateOf(p.mobileNumber) }
        var editVillage by remember { mutableStateOf(p.village) }
        var editDistrict by remember { mutableStateOf(p.district) }
        var editState by remember { mutableStateOf(p.state) }
        var editLanguage by remember { mutableStateOf(p.preferredLanguage) }
        var editOccupation by remember { mutableStateOf(p.occupation) }

        Dialog(onDismissRequest = { showProfileDialog = false }) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                LazyColumn(modifier = Modifier.padding(20.dp)) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "My Digital Profile",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                            )
                            IconButton(onClick = { showProfileDialog = false }) {
                                Icon(Icons.Default.Close, contentDescription = "Close")
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))

                        // Avatar Card
                        Card(
                            colors = CardDefaults.cardColors(containerColor = GreenLight),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(64.dp)
                                        .background(GreenPrimary, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = if (editName.isNotEmpty()) editName.first().uppercase() else "R",
                                        color = Color.White,
                                        fontSize = 28.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Column {
                                    Text(
                                        text = p.name,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp,
                                        color = GreenPrimaryDark
                                    )
                                    Text(
                                        text = "Role: ${p.role}",
                                        fontSize = 13.sp,
                                        color = MediumGrey
                                    )
                                    Text(
                                        text = "Location: ${p.gpsLocation}",
                                        fontSize = 11.sp,
                                        color = MediumGrey
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))

                        // Language Switcher
                        Text(
                            text = "Preferred Indian Language",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(vertical = 8.dp)
                        ) {
                            val languages = listOf("English", "Hindi", "Telugu", "Tamil", "Marathi")
                            languages.forEach { lang ->
                                FilterChip(
                                    selected = editLanguage == lang,
                                    onClick = { editLanguage = lang },
                                    label = { Text(lang, fontSize = 12.sp) }
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))

                        // Input fields
                        OutlinedTextField(
                            value = editName,
                            onValueChange = { editName = it },
                            label = { Text("Full Name") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .testTag("edit_name_field")
                        )

                        OutlinedTextField(
                            value = editMobile,
                            onValueChange = { editMobile = it },
                            label = { Text("Mobile Number") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        )

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = editVillage,
                                onValueChange = { editVillage = it },
                                label = { Text("Village") },
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(vertical = 4.dp)
                            )
                            OutlinedTextField(
                                value = editDistrict,
                                onValueChange = { editDistrict = it },
                                label = { Text("District") },
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(vertical = 4.dp)
                            )
                        }

                        OutlinedTextField(
                            value = editState,
                            onValueChange = { editState = it },
                            label = { Text("State") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        )

                        OutlinedTextField(
                            value = editOccupation,
                            onValueChange = { editOccupation = it },
                            label = { Text("Occupation / Main Activity") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Save buttons
                        Button(
                            onClick = {
                                viewModel.updateProfile(
                                    name = editName,
                                    mobile = editMobile,
                                    village = editVillage,
                                    district = editDistrict,
                                    state = editState,
                                    language = editLanguage,
                                    occupation = editOccupation
                                )
                                showProfileDialog = false
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("save_profile_button"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Save Profile Changes", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// SCREEN 1: DASHBOARD (HOME)
// ==========================================
@Composable
fun DashboardScreen(
    viewModel: RuralViewModel,
    profile: UserProfile?,
    notices: List<VillageNotice>,
    onNavigate: (String, String) -> Unit,
    onChangeRole: () -> Unit
) {
    val currentRole = profile?.role ?: "Farmer"
    val context = LocalContext.current

    // Observe stats simulation
    val soilPercent by viewModel.soilMoisture.collectAsStateWithLifecycle()
    val weatherTemp by viewModel.weatherTemp.collectAsStateWithLifecycle()
    val carbonSaved by viewModel.carbonSaved.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { Spacer(modifier = Modifier.height(12.dp)) }

        // Role Welcome Header Card
        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    // Decorative organic background pattern
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                            .background(
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        GreenPrimary.copy(alpha = 0.08f),
                                        Color.Transparent
                                    )
                                )
                            )
                    )

                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Column {
                                Text(
                                    text = "Namaste,",
                                    style = MaterialTheme.typography.titleMedium.copy(color = MediumGrey)
                                )
                                Text(
                                    text = profile?.name ?: "User",
                                    style = MaterialTheme.typography.headlineMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(top = 4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Place,
                                        contentDescription = "Location",
                                        tint = GreenPrimary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "${profile?.village}, ${profile?.district}",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }

                            // Role Switcher button
                            Button(
                                onClick = onChangeRole,
                                colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier.testTag("switch_role_button")
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.SwapHoriz,
                                        contentDescription = "Switch Role",
                                        modifier = Modifier.size(16.dp),
                                        tint = Color.White
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = currentRole,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                        }

                        // Role based specific statistics summary inside the card
                        Spacer(modifier = Modifier.height(16.dp))
                        Divider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f))
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            when (currentRole) {
                                "Farmer" -> {
                                    StatSummaryItem("Land Holdings", "2.5 Acres", Icons.Default.Terrain, GreenPrimary)
                                    StatSummaryItem("Next Sowing", "Paddy (July)", Icons.Default.Grass, OrangeAccent)
                                    StatSummaryItem("Active Bookings", "1 Pending", Icons.Default.DateRange, BlueSecondary)
                                }
                                "Laborer" -> {
                                    StatSummaryItem("Total Earnings", "₹${profile?.earnings}", Icons.Default.CurrencyRupee, GreenPrimary)
                                    StatSummaryItem("Jobs Completed", "8 Clean Jobs", Icons.Default.CheckCircle, OrangeAccent)
                                    StatSummaryItem("My Ratings", "4.9 ★", Icons.Default.Star, Color(0xFFFBC02D))
                                }
                                "Equipment Owner" -> {
                                    StatSummaryItem("My Machinery", "2 Registered", Icons.Default.Agriculture, BlueSecondary)
                                    StatSummaryItem("Rental Income", "₹4,800/mo", Icons.Default.MonetizationOn, GreenPrimary)
                                    StatSummaryItem("Active Renters", "Tractor Booked", Icons.Default.Assignment, Color(0xFF9C27B0))
                                }
                                "Panchayat/Authority" -> {
                                    StatSummaryItem("Village Population", "2,400", Icons.Default.People, Color(0xFF9C27B0))
                                    StatSummaryItem("Active Notices", "3 Published", Icons.Default.Announcement, OrangeAccent)
                                    StatSummaryItem("Resolved Requests", "45 approved", Icons.Default.VerifiedUser, GreenPrimary)
                                }
                                "Healthcare Worker" -> {
                                    StatSummaryItem("Village BP Average", "125/82", Icons.Default.Favorite, Color(0xFF009688))
                                    StatSummaryItem("Patients Seen", "14 this week", Icons.Default.Person, GreenPrimary)
                                    StatSummaryItem("Next Health Camp", "Saturday", Icons.Default.CalendarMonth, OrangeAccent)
                                }
                                "Admin" -> {
                                    StatSummaryItem("Total Users", "148 villagers", Icons.Default.Group, Color(0xFF37474F))
                                    StatSummaryItem("Active Bookings", "12 items", Icons.Default.List, GreenPrimary)
                                    StatSummaryItem("API Health Status", "Online 100%", Icons.Default.Dns, BlueSecondary)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Live Sensors & IoT Telemetry Panel (Feature 11 / IoT Integration)
        item {
            Text(
                text = "Village Climate & Soil Telemetry (IoT)",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.padding(bottom = 4.dp)
            )

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                border = ButtonDefaults.outlinedButtonBorder,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .background(Color(0xFF4CAF50), CircleShape)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Live IoT Feed",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF4CAF50)
                            )
                        }

                        IconButton(
                            onClick = { viewModel.updateWeatherData() },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Refresh sensors",
                                tint = GreenPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.WaterDrop, contentDescription = "Moisture", tint = BlueSecondary)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Soil Moisture", fontSize = 11.sp, color = MediumGrey)
                            Text("$soilPercent%", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.Thermostat, contentDescription = "Temperature", tint = OrangeAccent)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Air Temp", fontSize = 11.sp, color = MediumGrey)
                            Text("$weatherTemp°C", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.Eco, contentDescription = "Carbon Saved", tint = GreenPrimary)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("CO₂ Saved", fontSize = 11.sp, color = MediumGrey)
                            Text(String.format("%.1f kg", carbonSaved), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Soil specific actions
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { viewModel.triggerSoilSensorIrrigation() },
                            colors = ButtonDefaults.buttonColors(containerColor = BlueLight),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(vertical = 4.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Opacity, contentDescription = "Water", tint = BlueSecondary, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Trigger Drip", fontSize = 11.sp, color = BlueSecondary, fontWeight = FontWeight.Bold)
                            }
                        }

                        Button(
                            onClick = {
                                viewModel.recycleResidue(100)
                                Toast.makeText(context, "Simulated recycling of 100Kg crop residue into compost!", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = GreenLight),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(vertical = 4.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Recycling, contentDescription = "Recycle", tint = GreenPrimary, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Compost residue", fontSize = 11.sp, color = GreenPrimary, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // Feature 10: One-Tap Emergency Quick Access Row
        item {
            Text(
                text = "One-Tap Emergency Contacts",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.padding(bottom = 4.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                EmergencyButton("Ambulance", Icons.Default.LocalHospital, Color(0xFFE53935), context)
                EmergencyButton("Vet Doctor", Icons.Default.Pets, Color(0xFFFB8C00), context)
                EmergencyButton("Police / Fire", Icons.Default.Shield, Color(0xFF1976D2), context)
                EmergencyButton("Support Desk", Icons.Default.Phone, Color(0xFF4CAF50), context)
            }
        }

        // Shortcuts Grid to Other deep features
        item {
            Text(
                text = "Deep Digital Modules Explorer",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.padding(bottom = 4.dp)
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    ShortcutCard("AI Agri Doctor", Icons.Default.Spoke, GreenPrimary, "ai", "agri", onNavigate, Modifier.weight(1f))
                    ShortcutCard("AI Health Diagnostic", Icons.Default.Vaccines, Color(0xFF009688), "ai", "health", onNavigate, Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    ShortcutCard("Offline Assistant", Icons.Default.NetworkWifi3Bar, OrangeAccent, "ai", "offline", onNavigate, Modifier.weight(1f))
                    ShortcutCard("Labour Market", Icons.Default.Group, Color(0xFFE53935), "market", "jobs", onNavigate, Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    ShortcutCard("Equipments Rental", Icons.Default.Construction, BlueSecondary, "market", "equipment", onNavigate, Modifier.weight(1f))
                    ShortcutCard("Crops Marketplace", Icons.Default.Storefront, Color(0xFF9C27B0), "market", "produce", onNavigate, Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    ShortcutCard("Government Schemes", Icons.Default.AccountBalance, Color(0xFF3F51B5), "services", "schemes", onNavigate, Modifier.weight(1f))
                    ShortcutCard("Village Notice Board", Icons.Default.Announcement, Color(0xFFFF5722), "services", "notices", onNavigate, Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    ShortcutCard("Learning Centre", Icons.Default.School, Color(0xFF00796B), "services", "learning", onNavigate, Modifier.weight(1f))
                    ShortcutCard("Sustainability Hub", Icons.Default.Eco, Color(0xFF558B2F), "services", "IoT", onNavigate, Modifier.weight(1f))
                }
            }
        }

        // Live Notices highlights
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Village Notice Board Highlights",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = "View all",
                    color = GreenPrimary,
                    fontSize = 12.sp,
                    modifier = Modifier.clickable { onNavigate("services", "notices") }
                )
            }
        }

        if (notices.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Text(
                        text = "No notices published yet.",
                        modifier = Modifier.padding(16.dp),
                        color = MediumGrey
                    )
                }
            }
        } else {
            items(notices.take(2)) { notice ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = ButtonDefaults.outlinedButtonBorder
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = notice.category,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = OrangeAccent,
                                modifier = Modifier
                                    .background(OrangeLight, RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                            Text(
                                text = notice.publisher,
                                fontSize = 11.sp,
                                color = MediumGrey
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = notice.title,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = notice.content,
                            fontSize = 13.sp,
                            color = MediumGrey
                        )
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }
    }
}

@Composable
fun StatSummaryItem(label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color) {
    Column(horizontalAlignment = Alignment.Start) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = icon, contentDescription = label, tint = color, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(text = label, fontSize = 11.sp, color = MediumGrey, fontWeight = FontWeight.SemiBold)
        }
        Text(text = value, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 2.dp))
    }
}

@Composable
fun RowScope.EmergencyButton(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color, context: android.content.Context) {
    Card(
        modifier = Modifier
            .weight(1f)
            .clickable {
                Toast
                    .makeText(context, "Calling $label emergency services: 108/112 (Simulated)", Toast.LENGTH_LONG)
                    .show()
            },
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.08f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(color, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = label, tint = Color.White, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = label,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = color,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
        }
    }
}

@Composable
fun ShortcutCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    tab: String,
    subTab: String,
    onNavigate: (String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = { onNavigate(tab, subTab) },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = ButtonDefaults.outlinedButtonBorder,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(color.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = title, tint = color, modifier = Modifier.size(18.dp))
            }
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
        }
    }
}

// ==========================================
// SCREEN 2: AI ASSISTANTS (TABBED INTERFACE)
// ==========================================
@Composable
fun AIScreen(
    viewModel: RuralViewModel,
    activeSubTab: String,
    onSubTabChange: (String) -> Unit
) {
    val context = LocalContext.current

    // Chat states
    val agriChat by viewModel.agriChatMessages.collectAsStateWithLifecycle()
    val healthChat by viewModel.healthChatMessages.collectAsStateWithLifecycle()
    val offlineChat by viewModel.offlineChatMessages.collectAsStateWithLifecycle()

    val isAgriLoading by viewModel.isAgriLoading.collectAsStateWithLifecycle()
    val isHealthLoading by viewModel.isHealthLoading.collectAsStateWithLifecycle()
    val isOfflineLoading by viewModel.isOfflineLoading.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize()) {
        // Tab Headers
        TabRow(
            selectedTabIndex = when (activeSubTab) {
                "agri" -> 0
                "health" -> 1
                else -> 2
            },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Tab(
                selected = activeSubTab == "agri",
                onClick = { onSubTabChange("agri") },
                text = { Text("Crop Expert", fontSize = 12.sp) },
                icon = { Icon(Icons.Default.Grass, contentDescription = "Agri") }
            )
            Tab(
                selected = activeSubTab == "health",
                onClick = { onSubTabChange("health") },
                text = { Text("Wellness", fontSize = 12.sp) },
                icon = { Icon(Icons.Default.Favorite, contentDescription = "Health") }
            )
            Tab(
                selected = activeSubTab == "offline",
                onClick = { onSubTabChange("offline") },
                text = { Text("Offline AI", fontSize = 12.sp) },
                icon = { Icon(Icons.Default.WifiOff, contentDescription = "Offline Chat") }
            )
        }

        when (activeSubTab) {
            "agri" -> AIChatPanel(
                messages = agriChat,
                isLoading = isAgriLoading,
                placeholder = "Ask about crop diseases, pest solutions, or best fertilizers...",
                onSendMessage = { viewModel.sendChatMessage(it, "agri") },
                onClear = { viewModel.clearChat("agri") },
                hasImageFeature = true,
                onSimulateImage = { label ->
                    viewModel.sendChatMessage("Diagnose disease: My crop leaves show $label", "agri")
                }
            )
            "health" -> AIChatPanel(
                messages = healthChat,
                isLoading = isHealthLoading,
                placeholder = "Ask about diabetes management, child health, blood pressure diet...",
                onSendMessage = { viewModel.sendChatMessage(it, "health") },
                onClear = { viewModel.clearChat("health") },
                disclaimer = "⚠️ DISCLAIMER: This assistant provides basic rural health awareness and first aid information. It is NOT a substitute for professional medical diagnosis or treatment by a licensed doctor.",
                hasCalculatorFeature = true,
                onCalculateBMI = { w, h ->
                    val mH = h / 100.0
                    val bmi = w / (mH * mH)
                    val status = when {
                        bmi < 18.5 -> "Underweight"
                        bmi < 25.0 -> "Healthy Weight"
                        else -> "Overweight"
                    }
                    viewModel.sendChatMessage("My calculated BMI is ${String.format("%.2f", bmi)} ($status). Height: ${h}cm, Weight: ${w}kg. Give nutrition advice.", "health")
                }
            )
            "offline" -> AIChatPanel(
                messages = offlineChat,
                isLoading = isOfflineLoading,
                placeholder = "Ask offline questions about schemes, financial literacy, or village life...",
                onSendMessage = { viewModel.sendChatMessage(it, "offline") },
                onClear = { viewModel.clearChat("offline") }
            )
        }
    }
}

@Composable
fun AIChatPanel(
    messages: List<ChatMessage>,
    isLoading: Boolean,
    placeholder: String,
    onSendMessage: (String) -> Unit,
    onClear: () -> Unit,
    disclaimer: String? = null,
    hasImageFeature: Boolean = false,
    onSimulateImage: ((String) -> Unit)? = null,
    hasCalculatorFeature: Boolean = false,
    onCalculateBMI: ((Double, Double) -> Unit)? = null
) {
    val context = LocalContext.current
    var inputText by remember { mutableStateOf("") }
    var isRecordingSimulated by remember { mutableStateOf(false) }
    var showBmiCalc by remember { mutableStateOf(false) }

    // TextToSpeech Offline State
    var isTtsReady by remember { mutableStateOf(false) }
    var autoSpeakEnabled by remember { mutableStateOf(false) }
    val tts = remember {
        var ttsInstance: TextToSpeech? = null
        try {
            ttsInstance = TextToSpeech(context) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    isTtsReady = true
                    val hindiResult = ttsInstance?.setLanguage(Locale("hi", "IN"))
                    if (hindiResult == TextToSpeech.LANG_MISSING_DATA || hindiResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                        ttsInstance?.setLanguage(Locale.US)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        ttsInstance
    }

    // Auto-Speak trigger on new AI response
    LaunchedEffect(messages.size) {
        if (autoSpeakEnabled && messages.isNotEmpty()) {
            val lastMsg = messages.last()
            if (lastMsg.sender != "user" && isTtsReady) {
                tts?.speak(lastMsg.text, TextToSpeech.QUEUE_FLUSH, null, null)
            }
        }
    }

    // Release TTS Resource
    DisposableEffect(Unit) {
        onDispose {
            tts?.stop()
            tts?.shutdown()
        }
    }

    // SpeechRecognizer Offline State
    var isListeningSpeech by remember { mutableStateOf(false) }
    val speechRecognizer = remember {
        try {
            if (SpeechRecognizer.isRecognitionAvailable(context)) {
                SpeechRecognizer.createSpeechRecognizer(context)
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            try {
                speechRecognizer?.destroy()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun speakText(text: String) {
        if (isTtsReady && tts != null) {
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
            Toast.makeText(context, "🔊 Speaking answer...", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Text-to-speech is initializing...", Toast.LENGTH_SHORT).show()
        }
    }

    fun stopSpeaking() {
        if (isTtsReady && tts != null) {
            tts.stop()
        }
    }

    val recognitionListener = remember {
        object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                isListeningSpeech = true
                Toast.makeText(context, "🎤 Listening... Speak now", Toast.LENGTH_SHORT).show()
            }
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {
                isListeningSpeech = false
            }
            override fun onError(error: Int) {
                isListeningSpeech = false
                Toast.makeText(context, "🎤 Offline voice fallback: Try holding mic button. Presetting query for you!", Toast.LENGTH_LONG).show()
                inputText = "Suggest high-yield water-efficient crops for dry regions"
            }

            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    inputText = matches[0]
                    Toast.makeText(context, "🎤 Recognized: '$inputText'", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        }
    }

    fun startListening() {
        if (speechRecognizer == null) {
            Toast.makeText(context, "Speech Recognizer is loading or unavailable. Using instant voice fallback.", Toast.LENGTH_LONG).show()
            inputText = "What is the organic fertilizer recipe?"
            return
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        }

        try {
            speechRecognizer.setRecognitionListener(recognitionListener)
            speechRecognizer.startListening(intent)
            isListeningSpeech = true
        } catch (e: Exception) {
            Toast.makeText(context, "Voice recognizer error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    val requestPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            startListening()
        } else {
            Toast.makeText(context, "Microphone permission is required for voice recognition.", Toast.LENGTH_SHORT).show()
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        if (disclaimer != null) {
            Box(
                modifier = Modifier
                    .background(Color(0xFFFFF3E0))
                    .padding(8.dp)
            ) {
                Text(text = disclaimer, fontSize = 11.sp, color = Color(0xFFE65100), fontWeight = FontWeight.Bold)
            }
        }

        // Action controls (Pre-seeded queries / Simulators)
        if (hasImageFeature && onSimulateImage != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "Upload leaf image:",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.CenterVertically)
                )
                Button(
                    onClick = { onSimulateImage("Yellow spots and dried margins (Paddy Blast)") },
                    colors = ButtonDefaults.buttonColors(containerColor = GreenLight),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text("Paddy Blast leaf", fontSize = 10.sp, color = GreenPrimary)
                }
                Button(
                    onClick = { onSimulateImage("Reddish-brown powder pustules (Wheat Rust)") },
                    colors = ButtonDefaults.buttonColors(containerColor = GreenLight),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text("Wheat Rust leaf", fontSize = 10.sp, color = GreenPrimary)
                }
            }
        }

        if (hasCalculatorFeature && onCalculateBMI != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("BMI Quick Checker:", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Button(
                    onClick = { showBmiCalc = !showBmiCalc },
                    colors = ButtonDefaults.buttonColors(containerColor = BlueLight)
                ) {
                    Text(if (showBmiCalc) "Hide Calculator" else "Open BMI Calculator", fontSize = 10.sp, color = BlueSecondary)
                }
            }

            if (showBmiCalc) {
                var weightStr by remember { mutableStateOf("65") }
                var heightStr by remember { mutableStateOf("170") }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = LightBackground)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = weightStr,
                                onValueChange = { weightStr = it },
                                label = { Text("Weight (kg)", fontSize = 11.sp) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = heightStr,
                                onValueChange = { heightStr = it },
                                label = { Text("Height (cm)", fontSize = 11.sp) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = {
                                val w = weightStr.toDoubleOrNull() ?: 65.0
                                val h = heightStr.toDoubleOrNull() ?: 170.0
                                onCalculateBMI(w, h)
                                showBmiCalc = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Submit to Health Assistant AI", fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // Voice assistance panel (Auto-Speak answers)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp)
                .background(GreenLight, RoundedCornerShape(8.dp))
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.VolumeUp,
                    contentDescription = "Voice Output",
                    tint = GreenPrimary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Auto-Speak AI Answers (Hindi/English)",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = GreenPrimaryDark
                )
            }
            Switch(
                checked = autoSpeakEnabled,
                onCheckedChange = { 
                    autoSpeakEnabled = it
                    if (!it) stopSpeaking()
                    Toast.makeText(context, if (it) "Auto-Speak enabled 🔊" else "Auto-Speak disabled 🔇", Toast.LENGTH_SHORT).show()
                },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = GreenPrimary,
                    uncheckedThumbColor = Color.White,
                    uncheckedTrackColor = LightGrey
                ),
                modifier = Modifier.scale(0.8f).testTag("autospeak_switch")
            )
        }

        // Messages area
        Box(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp)
        ) {
            if (messages.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.SmartToy,
                        contentDescription = "Robot",
                        tint = GreenPrimary.copy(alpha = 0.4f),
                        modifier = Modifier.size(72.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "I am your RuralOS intelligent assistant.\nType questions or select from options above.",
                        color = MediumGrey,
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item { Spacer(modifier = Modifier.height(12.dp)) }
                    items(messages) { msg ->
                        val isUser = msg.sender == "user"
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (!isUser) {
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                                    ),
                                    shape = RoundedCornerShape(
                                        topStart = 16.dp,
                                        topEnd = 16.dp,
                                        bottomStart = 0.dp,
                                        bottomEnd = 16.dp
                                    ),
                                    modifier = Modifier.widthIn(max = 260.dp)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text(
                                            text = msg.text,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            fontSize = 13.sp,
                                            lineHeight = 18.sp
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                IconButton(
                                    onClick = { speakText(msg.text) },
                                    modifier = Modifier.size(32.dp).testTag("speak_button_${msg.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.VolumeUp,
                                        contentDescription = "Read aloud",
                                        tint = GreenPrimary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            } else {
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = GreenPrimary
                                    ),
                                    shape = RoundedCornerShape(
                                        topStart = 16.dp,
                                        topEnd = 16.dp,
                                        bottomStart = 16.dp,
                                        bottomEnd = 0.dp
                                    ),
                                    modifier = Modifier.widthIn(max = 280.dp)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text(
                                            text = msg.text,
                                            color = Color.White,
                                            fontSize = 13.sp,
                                            lineHeight = 18.sp
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (isLoading) {
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Start
                            ) {
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                                    ),
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("AI is responding...", fontSize = 12.sp, color = MediumGrey)
                                    }
                                }
                            }
                        }
                    }
                    item { Spacer(modifier = Modifier.height(12.dp)) }
                }
            }
        }

        // Input bottom bar
        Card(
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            shape = RoundedCornerShape(0.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(8.dp)
                    .imePadding()
                    .windowInsetsPadding(WindowInsets.navigationBars),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Clear chat
                IconButton(onClick = onClear) {
                    Icon(Icons.Default.DeleteSweep, contentDescription = "Clear Chat", tint = MediumGrey)
                }

                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    placeholder = { Text(if (isListeningSpeech) "🎤 Listening... Speak clearly now" else placeholder, fontSize = 12.sp) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("chat_input"),
                    shape = RoundedCornerShape(24.dp),
                    maxLines = 3,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = {
                        if (inputText.isNotBlank()) {
                            onSendMessage(inputText)
                            inputText = ""
                        }
                    })
                )

                Spacer(modifier = Modifier.width(4.dp))

                // Offline Real-time Voice Assistant mic
                IconButton(
                    onClick = {
                        if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                            if (isListeningSpeech) {
                                try {
                                    speechRecognizer?.stopListening()
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                                isListeningSpeech = false
                            } else {
                                startListening()
                            }
                        } else {
                            requestPermissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
                        }
                    },
                    modifier = Modifier.testTag("voice_mic")
                ) {
                    Icon(
                        imageVector = if (isListeningSpeech) Icons.Default.MicOff else Icons.Default.Mic,
                        contentDescription = "Mic",
                        tint = if (isListeningSpeech) OrangeAccent else GreenPrimary
                    )
                }

                // Send button
                IconButton(
                    onClick = {
                        if (inputText.isNotBlank()) {
                            onSendMessage(inputText)
                            inputText = ""
                        }
                    },
                    modifier = Modifier.testTag("send_chat_button")
                ) {
                    Icon(Icons.Default.Send, contentDescription = "Send", tint = GreenPrimary)
                }
            }
        }
    }
}

// ==========================================
// SCREEN 3: COOPERATIVE & MARKETPLACE HUB
// ==========================================
@Composable
fun MarketplaceHubScreen(
    viewModel: RuralViewModel,
    activeSubTab: String,
    onSubTabChange: (String) -> Unit,
    profile: UserProfile?,
    jobs: List<LaborJob>,
    equipmentList: List<EquipmentItem>,
    bookings: List<EquipmentBooking>,
    products: List<MarketplaceProduct>
) {
    val context = LocalContext.current
    var showAddJobDialog by remember { mutableStateOf(false) }
    var showAddEquipmentDialog by remember { mutableStateOf(false) }
    var showAddProductDialog by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(
            selectedTabIndex = when (activeSubTab) {
                "jobs" -> 0
                "equipment" -> 1
                else -> 2
            }
        ) {
            Tab(
                selected = activeSubTab == "jobs",
                onClick = { onSubTabChange("jobs") },
                text = { Text("Labor", fontSize = 11.sp) },
                icon = { Icon(Icons.Default.Group, contentDescription = "Labor") }
            )
            Tab(
                selected = activeSubTab == "equipment",
                onClick = { onSubTabChange("equipment") },
                text = { Text("Rentals", fontSize = 11.sp) },
                icon = { Icon(Icons.Default.Construction, contentDescription = "Rentals") }
            )
            Tab(
                selected = activeSubTab == "produce",
                onClick = { onSubTabChange("produce") },
                text = { Text("Produce", fontSize = 11.sp) },
                icon = { Icon(Icons.Default.Storefront, contentDescription = "Produce") }
            )
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp)
        ) {
            when (activeSubTab) {
                "jobs" -> LaborSubPanel(
                    viewModel = viewModel,
                    jobs = jobs,
                    profile = profile,
                    onAddClick = { showAddJobDialog = true }
                )
                "equipment" -> EquipmentSubPanel(
                    viewModel = viewModel,
                    equipmentList = equipmentList,
                    bookings = bookings,
                    profile = profile,
                    onAddClick = { showAddEquipmentDialog = true }
                )
                "produce" -> ProduceSubPanel(
                    viewModel = viewModel,
                    products = products,
                    profile = profile,
                    onAddClick = { showAddProductDialog = true }
                )
            }
        }
    }

    // Modal to Add Job
    if (showAddJobDialog) {
        var title by remember { mutableStateOf("") }
        var type by remember { mutableStateOf("Harvesting") }
        var date by remember { mutableStateOf("2026-07-20") }
        var time by remember { mutableStateOf("08:00 AM - 04:00 PM") }
        var workers by remember { mutableStateOf("4") }
        var wage by remember { mutableStateOf("450") }
        var location by remember { mutableStateOf("Main Field, Pipili") }

        Dialog(onDismissRequest = { showAddJobDialog = false }) {
            Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surface, modifier = Modifier.padding(16.dp)) {
                LazyColumn(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        Text("Post Labor Requirement", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                    item {
                        OutlinedTextField(
                            value = title,
                            onValueChange = { title = it },
                            label = { Text("Job Title (e.g. Sowing help)") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("job_title_input")
                        )
                    }
                    item {
                        Text("Work Type", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            val types = listOf("Sowing", "Harvesting", "Weeding", "Spraying")
                            types.forEach { t ->
                                FilterChip(selected = type == t, onClick = { type = t }, label = { Text(t, fontSize = 11.sp) })
                            }
                        }
                    }
                    item {
                        OutlinedTextField(value = date, onValueChange = { date = it }, label = { Text("Date") }, modifier = Modifier.fillMaxWidth())
                    }
                    item {
                        OutlinedTextField(value = time, onValueChange = { time = it }, label = { Text("Timings") }, modifier = Modifier.fillMaxWidth())
                    }
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = workers,
                                onValueChange = { workers = it },
                                label = { Text("No. of workers") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = wage,
                                onValueChange = { wage = it },
                                label = { Text("Wage / Day (₹)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("job_wage_input")
                            )
                        }
                    }
                    item {
                        OutlinedTextField(value = location, onValueChange = { location = it }, label = { Text("Location") }, modifier = Modifier.fillMaxWidth())
                    }
                    item {
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = {
                                if (title.isNotBlank()) {
                                    viewModel.postLaborJob(
                                        title = title,
                                        workType = type,
                                        date = date,
                                        time = time,
                                        workers = workers.toIntOrNull() ?: 4,
                                        wage = wage.toDoubleOrNull() ?: 450.0,
                                        location = location
                                    )
                                    showAddJobDialog = false
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("job_submit_button")
                        ) {
                            Text("Post Job Vacancy", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    // Modal to Register Rental Equipment
    if (showAddEquipmentDialog) {
        var name by remember { mutableStateOf("") }
        var type by remember { mutableStateOf("Tractor") }
        var rate by remember { mutableStateOf("1000") }
        var desc by remember { mutableStateOf("") }

        Dialog(onDismissRequest = { showAddEquipmentDialog = false }) {
            Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surface, modifier = Modifier.padding(16.dp)) {
                LazyColumn(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        Text("Register Rental Machinery", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                    item {
                        OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Machinery Name") }, modifier = Modifier.fillMaxWidth().testTag("equip_name_input"))
                    }
                    item {
                        Text("Machinery Category", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        val types = listOf("Tractor", "Drone", "Harvester", "Rotavator", "Sprayer")
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            types.take(3).forEach { t ->
                                FilterChip(selected = type == t, onClick = { type = t }, label = { Text(t, fontSize = 11.sp) })
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            types.drop(3).forEach { t ->
                                FilterChip(selected = type == t, onClick = { type = t }, label = { Text(t, fontSize = 11.sp) })
                            }
                        }
                    }
                    item {
                        OutlinedTextField(
                            value = rate,
                            onValueChange = { rate = it },
                            label = { Text("Rental Rate / Day (₹)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    item {
                        OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text("Machinery Specifications & Status") }, modifier = Modifier.fillMaxWidth())
                    }
                    item {
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = {
                                if (name.isNotBlank()) {
                                    viewModel.registerEquipment(
                                        name = name,
                                        type = type,
                                        rate = rate.toDoubleOrNull() ?: 1000.0,
                                        description = desc
                                    )
                                    showAddEquipmentDialog = false
                                }
                            },
                            modifier = Modifier.fillMaxWidth().testTag("equip_submit_button")
                        ) {
                            Text("Register Equipment", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    // Modal to Sell Produce Product
    if (showAddProductDialog) {
        var name by remember { mutableStateOf("") }
        var category by remember { mutableStateOf("Crops") }
        var quantity by remember { mutableStateOf("100 Kg") }
        var price by remember { mutableStateOf("40") }
        var desc by remember { mutableStateOf("") }

        Dialog(onDismissRequest = { showAddProductDialog = false }) {
            Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surface, modifier = Modifier.padding(16.dp)) {
                LazyColumn(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        Text("List Produce For Sale", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                    item {
                        OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Produce Name (e.g. Basmati Rice)") }, modifier = Modifier.fillMaxWidth().testTag("prod_name_input"))
                    }
                    item {
                        Text("Category", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        val cats = listOf("Crops", "Vegetables", "Fruits", "Dairy", "Poultry")
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            cats.take(3).forEach { c ->
                                FilterChip(selected = category == c, onClick = { category = c }, label = { Text(c, fontSize = 11.sp) })
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            cats.drop(3).forEach { c ->
                                FilterChip(selected = category == c, onClick = { category = c }, label = { Text(c, fontSize = 11.sp) })
                            }
                        }
                    }
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(value = quantity, onValueChange = { quantity = it }, label = { Text("Available Quantity") }, modifier = Modifier.weight(1f))
                            OutlinedTextField(
                                value = price,
                                onValueChange = { price = it },
                                label = { Text("Price / Unit (₹)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    item {
                        OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text("Product Description / Details") }, modifier = Modifier.fillMaxWidth())
                    }
                    item {
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = {
                                if (name.isNotBlank()) {
                                    viewModel.addMarketplaceProduct(
                                        name = name,
                                        category = category,
                                        quantity = quantity,
                                        price = price.toDoubleOrNull() ?: 40.0,
                                        description = desc
                                    )
                                    showAddProductDialog = false
                                }
                            },
                            modifier = Modifier.fillMaxWidth().testTag("prod_submit_button")
                        ) {
                            Text("List Produce", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LaborSubPanel(
    viewModel: RuralViewModel,
    jobs: List<LaborJob>,
    profile: UserProfile?,
    onAddClick: () -> Unit
) {
    val isFarmer = profile?.role in listOf("Farmer", "Admin")

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Labor Marketplace Requirements", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            if (isFarmer) {
                Button(
                    onClick = onAddClick,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
                    modifier = Modifier.testTag("post_job_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Post")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Post Job", fontSize = 12.sp)
                }
            }
        }

        if (jobs.isEmpty()) {
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                Text("No active job postings. Please check back later.")
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(jobs) { job ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = ButtonDefaults.outlinedButtonBorder
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(
                                    text = job.workType,
                                    color = GreenPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    modifier = Modifier
                                        .background(GreenLight, RoundedCornerShape(4.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                                Text(
                                    text = job.status,
                                    color = if (job.status == "OPEN") OrangeAccent else Color.Gray,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(job.title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text("Posted by: ${job.farmerName}", fontSize = 12.sp, color = MediumGrey)

                            Spacer(modifier = Modifier.height(8.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                Column {
                                    Text("Daily Wage", fontSize = 11.sp, color = MediumGrey)
                                    Text("₹${job.dailyWage}", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = GreenPrimary)
                                }
                                Column {
                                    Text("Required Workers", fontSize = 11.sp, color = MediumGrey)
                                    Text("${job.numWorkersNeeded} helpers", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                }
                                Column {
                                    Text("Scheduled Date", fontSize = 11.sp, color = MediumGrey)
                                    Text(job.date, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                }
                            }

                            if (job.workersApplied.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Applied workers: ${job.workersApplied}", fontSize = 11.sp, color = MediumGrey, fontWeight = FontWeight.SemiBold)
                            }

                            if (job.status == "OPEN") {
                                Spacer(modifier = Modifier.height(12.dp))
                                if (profile?.role == "Laborer") {
                                    Button(
                                        onClick = { viewModel.applyOrAcceptJob(job) },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("Accept Job & Record Earnings", fontWeight = FontWeight.Bold)
                                    }
                                } else if (isFarmer) {
                                    OutlinedButton(
                                        onClick = { viewModel.deleteJob(job) },
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("Cancel Requirements")
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

@Composable
fun EquipmentSubPanel(
    viewModel: RuralViewModel,
    equipmentList: List<EquipmentItem>,
    bookings: List<EquipmentBooking>,
    profile: UserProfile?,
    onAddClick: () -> Unit
) {
    val isOwner = profile?.role in listOf("Equipment Owner", "Admin")
    val isAuthority = profile?.role in listOf("Panchayat/Authority", "Admin")

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        // Rent Machinery Register bar
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Cooperative Machinery Rentals", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                if (isOwner) {
                    Button(onClick = onAddClick, colors = ButtonDefaults.buttonColors(containerColor = BlueSecondary)) {
                        Icon(Icons.Default.Add, contentDescription = "Add")
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("List Machine", fontSize = 11.sp)
                    }
                }
            }
        }

        // List bookings if Authority or Farmer
        if (bookings.isNotEmpty()) {
            item {
                Text("Machinery Booking Operations", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MediumGrey)
            }
            items(bookings) { b ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = LightBackground)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(b.equipmentName, fontWeight = FontWeight.Bold)
                            Text(
                                text = b.status,
                                color = when (b.status) {
                                    "APPROVED" -> GreenPrimary
                                    "REJECTED" -> Color.Red
                                    else -> OrangeAccent
                                },
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                        Text("Booked by: ${b.farmerName} for ${b.durationDays} days", fontSize = 12.sp, color = MediumGrey)
                        Text("Total Estimated Cost: ₹${b.totalCost} (Subsidized)", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)

                        if (isAuthority && b.status == "PENDING") {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { viewModel.approveBooking(b, true) },
                                    colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Approve", fontSize = 11.sp)
                                }
                                Button(
                                    onClick = { viewModel.approveBooking(b, false) },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Reject", fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Available equipment
        item {
            Text("Registered Agricultural Machinery", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MediumGrey)
        }

        if (equipmentList.isEmpty()) {
            item {
                Text("No machinery registered currently. Please check cooperative directories.")
            }
        } else {
            items(equipmentList) { eq ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = ButtonDefaults.outlinedButtonBorder
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(
                                text = eq.type,
                                color = BlueSecondary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                modifier = Modifier
                                    .background(BlueLight, RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            Text(
                                text = if (eq.availability) "AVAILABLE" else "BOOKED",
                                color = if (eq.availability) GreenPrimary else Color.Gray,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(eq.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text("Specs: ${eq.description}", fontSize = 12.sp, color = MediumGrey)
                        Text("Owner: ${eq.ownerName}", fontSize = 11.sp, color = MediumGrey)

                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Rental Charges", fontSize = 10.sp, color = MediumGrey)
                                Text("₹${eq.dailyRate} / Day", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = BlueSecondary)
                            }

                            if (eq.availability && !isOwner) {
                                Button(
                                    onClick = { viewModel.bookEquipment(eq, "2026-07-22", 3) },
                                    colors = ButtonDefaults.buttonColors(containerColor = BlueSecondary)
                                ) {
                                    Text("Book (3 Days)", fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
        item { Spacer(modifier = Modifier.height(24.dp)) }
    }
}

@Composable
fun ProduceSubPanel(
    viewModel: RuralViewModel,
    products: List<MarketplaceProduct>,
    profile: UserProfile?,
    onAddClick: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Organic Produce Cooperative", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Button(onClick = onAddClick, colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary)) {
                Icon(Icons.Default.Add, contentDescription = "Add")
                Spacer(modifier = Modifier.width(4.dp))
                Text("List Produce", fontSize = 11.sp)
            }
        }

        if (products.isEmpty()) {
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                Text("No products listed in directory.")
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(products) { p ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = ButtonDefaults.outlinedButtonBorder
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(
                                    text = p.category,
                                    color = Color(0xFF9C27B0),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    modifier = Modifier
                                        .background(Color(0xFFF3E5F5), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                                Text(
                                    text = p.quantity,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = MediumGrey
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(p.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text(p.description, fontSize = 12.sp, color = MediumGrey)

                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("Direct Price", fontSize = 10.sp, color = MediumGrey)
                                    Text("₹${p.price} / Kg", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF9C27B0))
                                }

                                if (p.sellerName != profile?.name) {
                                    Button(
                                        onClick = { viewModel.buyMarketplaceProduct(p) },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF9C27B0))
                                    ) {
                                        Text("Buy Produce", fontSize = 11.sp)
                                    }
                                } else {
                                    Text("My Listing", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = GreenPrimary)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// SCREEN 4: SERVICES PANEL (SCHEMES, NOTICES, LESSONS, IoT)
// ==========================================
@Composable
fun ServicesScreen(
    viewModel: RuralViewModel,
    activeSubTab: String,
    onSubTabChange: (String) -> Unit,
    profile: UserProfile?,
    notices: List<VillageNotice>
) {
    var showAddNoticeDialog by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(
            selectedTabIndex = when (activeSubTab) {
                "notices" -> 0
                "schemes" -> 1
                "learning" -> 2
                else -> 3
            }
        ) {
            Tab(
                selected = activeSubTab == "notices",
                onClick = { onSubTabChange("notices") },
                text = { Text("Notices", fontSize = 10.sp) },
                icon = { Icon(Icons.Default.Announcement, contentDescription = "Notices") }
            )
            Tab(
                selected = activeSubTab == "schemes",
                onClick = { onSubTabChange("schemes") },
                text = { Text("Schemes", fontSize = 10.sp) },
                icon = { Icon(Icons.Default.AccountBalance, contentDescription = "Schemes") }
            )
            Tab(
                selected = activeSubTab == "learning",
                onClick = { onSubTabChange("learning") },
                text = { Text("Learn", fontSize = 10.sp) },
                icon = { Icon(Icons.Default.School, contentDescription = "Learn") }
            )
            Tab(
                selected = activeSubTab == "IoT",
                onClick = { onSubTabChange("IoT") },
                text = { Text("Sustainability", fontSize = 10.sp) },
                icon = { Icon(Icons.Default.Eco, contentDescription = "Sustainability") }
            )
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp)
        ) {
            when (activeSubTab) {
                "notices" -> NoticeBoardSubPanel(
                    viewModel = viewModel,
                    notices = notices,
                    profile = profile,
                    onAddClick = { showAddNoticeDialog = true }
                )
                "schemes" -> GovernmentSchemesSubPanel(
                    viewModel = viewModel,
                    profile = profile
                )
                "learning" -> LearningCenterSubPanel()
                "IoT" -> SustainabilitySubPanel(viewModel = viewModel)
            }
        }
    }

    // Modal to Publish Notice
    if (showAddNoticeDialog) {
        var title by remember { mutableStateOf("") }
        var category by remember { mutableStateOf("Water Supply") }
        var content by remember { mutableStateOf("") }

        Dialog(onDismissRequest = { showAddNoticeDialog = false }) {
            Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surface, modifier = Modifier.padding(16.dp)) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Publish Village Notice", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Notice Title") }, modifier = Modifier.fillMaxWidth().testTag("notice_title_input"))

                    Text("Notice Category", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    val cats = listOf("Water Supply", "Electricity Outage", "Weather Alert", "Health Camp")
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        cats.take(2).forEach { c ->
                            FilterChip(selected = category == c, onClick = { category = c }, label = { Text(c, fontSize = 10.sp) })
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        cats.drop(2).forEach { c ->
                            FilterChip(selected = category == c, onClick = { category = c }, label = { Text(c, fontSize = 10.sp) })
                        }
                    }

                    OutlinedTextField(value = content, onValueChange = { content = it }, label = { Text("Notice Content Details") }, modifier = Modifier.fillMaxWidth().height(100.dp))

                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = {
                            if (title.isNotBlank()) {
                                viewModel.addNotice(title, category, content)
                                showAddNoticeDialog = false
                            }
                        },
                        modifier = Modifier.fillMaxWidth().testTag("notice_submit_button")
                    ) {
                        Text("Publish Notice", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun NoticeBoardSubPanel(
    viewModel: RuralViewModel,
    notices: List<VillageNotice>,
    profile: UserProfile?,
    onAddClick: () -> Unit
) {
    val canPublish = profile?.role in listOf("Panchayat/Authority", "Admin")

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Gram Panchayat Notice Board", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            if (canPublish) {
                Button(onClick = onAddClick, colors = ButtonDefaults.buttonColors(containerColor = OrangeAccent)) {
                    Icon(Icons.Default.Add, contentDescription = "Add")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Publish Notice", fontSize = 11.sp)
                }
            }
        }

        if (notices.isEmpty()) {
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                Text("No bulletins currently published.")
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(notices) { notice ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = ButtonDefaults.outlinedButtonBorder
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(
                                    text = notice.category,
                                    color = OrangeAccent,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    modifier = Modifier
                                        .background(OrangeLight, RoundedCornerShape(4.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                                if (canPublish) {
                                    IconButton(
                                        onClick = { viewModel.deleteNotice(notice) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(notice.title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(notice.content, fontSize = 13.sp, color = MediumGrey, lineHeight = 18.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Published by: ${notice.publisher}", fontSize = 11.sp, color = MediumGrey)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun GovernmentSchemesSubPanel(
    viewModel: RuralViewModel,
    profile: UserProfile?
) {
    var searchStr by remember { mutableStateOf("") }
    val matchedSchemes = viewModel.getEligibleSchemes(profile).filter {
        it["title"]?.lowercase()?.contains(searchStr.lowercase()) == true ||
        it["category"]?.lowercase()?.contains(searchStr.lowercase()) == true
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(
            value = searchStr,
            onValueChange = { searchStr = it },
            placeholder = { Text("Search schemes (e.g. PM-Kisan, Housing...)") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))
        Card(
            colors = CardDefaults.cardColors(containerColor = GreenLight),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "✨ RuralOS AI Schemes Matcher matched ${matchedSchemes.size} schemes based on your digital profile [Role: ${profile?.role}].",
                modifier = Modifier.padding(12.dp),
                fontSize = 12.sp,
                color = GreenPrimaryDark,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(matchedSchemes) { scheme ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = ButtonDefaults.outlinedButtonBorder
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(
                                text = scheme["category"] ?: "",
                                color = BlueSecondary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                modifier = Modifier
                                    .background(BlueLight, RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(scheme["title"] ?: "", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text(scheme["description"] ?: "", fontSize = 12.sp, color = MediumGrey, modifier = Modifier.padding(vertical = 4.dp))

                        Spacer(modifier = Modifier.height(8.dp))
                        Card(
                            colors = CardDefaults.cardColors(containerColor = LightBackground),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row {
                                    Text("Eligibility: ", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = GreenPrimary)
                                    Text(scheme["eligibility"] ?: "", fontSize = 11.sp)
                                }
                                Row(modifier = Modifier.padding(top = 2.dp)) {
                                    Text("Benefits: ", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = GreenPrimary)
                                    Text(scheme["benefits"] ?: "", fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LearningCenterSubPanel() {
    val lessons = listOf(
        mapOf("title" to "Introduction to Multi-Cropping & Rotation", "category" to "Organic Farming", "duration" to "12 mins", "desc" to "Learn how planting mustard with wheat preserves nitrogen in the soil and doubles revenue."),
        mapOf("title" to "Setting up UPI Payments securely", "category" to "Digital Payments", "duration" to "8 mins", "desc" to "A visual guide on accepting QR code payments directly into your cooperative bank account safely."),
        mapOf("title" to "Subsidies for Solar Water Pump Setups", "category" to "Modern Farming", "duration" to "15 mins", "desc" to "How to register under the Kusum Scheme to set up a solar pump with 90% government subsidy."),
        mapOf("title" to "Basics of Rural Micro-Loans & SHGs", "category" to "Financial Literacy", "duration" to "10 mins", "desc" to "Learn how cooperative Self-Help Groups can pull resources to access direct commercial credit.")
    )

    var playingTitle by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.School, contentDescription = "Learn", tint = GreenPrimary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Cooperative Learning Center (Offline Lessons)", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }

        items(lessons) { les ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = ButtonDefaults.outlinedButtonBorder
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(
                            text = les["category"] ?: "",
                            color = OrangeAccent,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            modifier = Modifier
                                .background(OrangeLight, RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                        Text(les["duration"] ?: "", fontSize = 11.sp, color = MediumGrey)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(les["title"] ?: "", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text(les["desc"] ?: "", fontSize = 12.sp, color = MediumGrey, modifier = Modifier.padding(vertical = 4.dp))

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Simulated play/pause button
                        Button(
                            onClick = {
                                if (playingTitle == les["title"]) {
                                    playingTitle = null
                                } else {
                                    playingTitle = les["title"]
                                    Toast.makeText(context, "🔊 Playing voice lesson: ${les["title"]}", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (playingTitle == les["title"]) Color.Red else GreenPrimary
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (playingTitle == les["title"]) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = "Play",
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(if (playingTitle == les["title"]) "Pause Lesson" else "Listen Voice", fontSize = 11.sp)
                            }
                        }

                        // Simulated PDF download
                        OutlinedButton(
                            onClick = {
                                Toast.makeText(context, "📥 Downloaded PDF booklet for ${les["title"]}! Safe for offline reading.", Toast.LENGTH_LONG).show()
                            },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Download, contentDescription = "PDF", modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Offline PDF", fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }
        item { Spacer(modifier = Modifier.height(24.dp)) }
    }
}

@Composable
fun SustainabilitySubPanel(viewModel: RuralViewModel) {
    val context = LocalContext.current
    var inputKilos by remember { mutableStateOf("100") }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Eco, contentDescription = "Eco", tint = GreenPrimary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Eco-Sustainability & Carbon Tracker", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }

        // Carbon Footprint Progress Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = GreenLight),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "My Green Carbon Savings",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = GreenPrimaryDark
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Earn carbon credits by recycling crop residue, using solar power, and organic tilling.",
                        fontSize = 11.sp,
                        color = MediumGrey
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    val carbonVal by viewModel.carbonSaved.collectAsStateWithLifecycle()
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Text("Active Level: Green Hero", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text("${String.format("%.1f", carbonVal)} kg CO₂ Saved", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = GreenPrimary)
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { (carbonVal.toFloat() / 500f).coerceIn(0.1f, 1f) },
                        color = GreenPrimary,
                        trackColor = LightGrey,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(12.dp)
                            .clip(RoundedCornerShape(6.dp))
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Goal: 500 kg CO₂ to claim Panchayat Bio-Compost Subsidy Voucher", fontSize = 10.sp, color = MediumGrey)
                }
            }
        }

        // Compost residue recycler calculator
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = ButtonDefaults.outlinedButtonBorder
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Organic Biomass Converter", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text("Convert crop tilling residue (stubble/leaves) to organic compost instead of burning. Avoid toxic field burning!", fontSize = 12.sp, color = MediumGrey, modifier = Modifier.padding(vertical = 4.dp))

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = inputKilos,
                            onValueChange = { inputKilos = it },
                            label = { Text("Residue amount (Kg)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )

                        Button(
                            onClick = {
                                val k = inputKilos.toIntOrNull() ?: 100
                                viewModel.recycleResidue(k)
                                Toast.makeText(context, "Eco-Compost calculated! Added to your carbon credits profile.", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
                            modifier = Modifier.weight(1.2f)
                        ) {
                            Text("Convert to Compost", fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // Green energy awareness links
        item {
            Text("Sustainability Action Pillars", fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CheckCircle, contentDescription = "Check", tint = GreenPrimary, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Plastic Waste Recycling: Hand over household plastics at cooperative collection centers.", fontSize = 11.sp)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CheckCircle, contentDescription = "Check", tint = GreenPrimary, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Water Conservation: Build small recharge wells to guide rainwater back to local aquifers.", fontSize = 11.sp)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CheckCircle, contentDescription = "Check", tint = GreenPrimary, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Renewable Solar Power: Register pumps under Kusm plan to generate surplus clean power income.", fontSize = 11.sp)
                    }
                }
            }
        }
        item { Spacer(modifier = Modifier.height(24.dp)) }
    }
}
