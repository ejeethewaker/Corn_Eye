// Home Screen
// Main farmer dashboard with quick-scan button and recent scan summary.
package com.corneye.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.corneye.app.data.FirebaseHelper
import com.corneye.app.data.UserPreferences
import com.corneye.app.navigation.Screen
import com.corneye.app.ui.theme.*
import android.util.Base64
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class RecentScan(
    val id: String,
    val diseaseName: String,
    val confidence: Float,
    val timestamp: Long,
    val isHealthy: Boolean
)

@Composable
fun HomeScreen(navController: NavController) {
    val context = LocalContext.current
    val fullname by UserPreferences.getFullname(context).collectAsState(initial = "Farmer")
    val userId by UserPreferences.getUserId(context).collectAsState(initial = "")
    val firstName = fullname.split(" ").firstOrNull() ?: "Farmer"
    var selectedTab by remember { mutableIntStateOf(0) }

    var totalScans by remember { mutableIntStateOf(0) }
    var diseasedCount by remember { mutableIntStateOf(0) }
    var healthyCount by remember { mutableIntStateOf(0) }
    var recentScans by remember { mutableStateOf<List<RecentScan>>(emptyList()) }
    var photoUrl by remember { mutableStateOf("") }
    var selectedHistoryFilter by remember { mutableIntStateOf(0) }

    // Real-time listener so photo updates immediately after upload
    DisposableEffect(userId) {
        if (userId.isEmpty()) return@DisposableEffect onDispose {}
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                photoUrl = snapshot.getValue(String::class.java) ?: ""
            }
            override fun onCancelled(error: DatabaseError) {}
        }
        val ref = FirebaseHelper.farmersRef().child(userId).child("profile_photo_url")
        ref.addValueEventListener(listener)
        onDispose { ref.removeEventListener(listener) }
    }

    // Real-time listener for scan history and stats
    DisposableEffect(userId) {
        if (userId.isEmpty()) return@DisposableEffect onDispose {}
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val allScans = mutableListOf<RecentScan>()
                var total = 0
                var diseased = 0
                var healthy = 0
                snapshot.children.forEach { child ->
                    val scanUserId = child.child("farmer_id").getValue(String::class.java) ?: ""
                    if (scanUserId == userId) {
                        total++
                        val label = child.child("analysis_label").getValue(String::class.java) ?: "Unknown"
                        val isHealthyScan = label.equals("Healthy", ignoreCase = true)
                        if (isHealthyScan) healthy++ else diseased++
                        allScans.add(
                            RecentScan(
                                id = child.key ?: "",
                                diseaseName = label,
                                confidence = child.child("confidence_score").getValue(Float::class.java) ?: 0f,
                                timestamp = child.child("time_scanned").getValue(Long::class.java) ?: 0L,
                                isHealthy = isHealthyScan
                            )
                        )
                    }
                }
                totalScans = total
                diseasedCount = diseased
                healthyCount = healthy
                recentScans = allScans.sortedByDescending { it.timestamp }
            }
            override fun onCancelled(error: DatabaseError) {
                android.util.Log.e("HomeScreen", "Firebase scan history error: ${error.message} (code: ${error.code})")
            }
        }
        val ref = FirebaseHelper.analysisResultsRef()
        ref.addValueEventListener(listener)
        onDispose { ref.removeEventListener(listener) }
    }

    val photoBytes = remember(photoUrl) {
        if (photoUrl.isNotEmpty()) {
            try { Base64.decode(photoUrl, Base64.DEFAULT) }
            catch (e: Exception) { null }
        } else null
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        bottomBar = {
            BottomNavBar(
                selectedTab = selectedTab,
                onTabSelected = { index ->
                    selectedTab = index
                    when (index) {
                        0 -> { /* Already on Home */ }
                        1 -> navController.navigate(Screen.Profile.route)
                        2 -> navController.navigate(Screen.Scan.route)
                        3 -> navController.navigate(Screen.Notifications.route)
                        4 -> navController.navigate(Screen.Settings.route)
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Background)
                .padding(bottom = paddingValues.calculateBottomPadding())
        ) {
            // Status bar background - pinned outside scroll
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(GoldenBackground)
                    .background(StatusBarGold)
                    .windowInsetsPadding(WindowInsets.statusBars)
            )

            // Top header - Golden
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(GoldenBackground)
                    .clickable { navController.navigate(Screen.Profile.route) }
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Profile avatar
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Color.Black),
                        contentAlignment = Alignment.Center
                    ) {
                        if (photoBytes != null) {
                            AsyncImage(
                                model = photoBytes,
                                contentDescription = "Profile photo",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Text(
                                text = firstName.take(1).uppercase(),
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = White
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Hello, Farmer $firstName! 👋",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Monitor your corn health",
                            fontSize = 13.sp,
                            color = TextPrimary.copy(alpha = 0.7f)
                        )
                    }
                }
            }

            // Divider
            HorizontalDivider(color = DividerColor, thickness = 1.dp)

            // Content area with light background
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Background)
                    .verticalScroll(rememberScrollState())
            ) {
                Spacer(modifier = Modifier.height(16.dp))

            // Overview section
            Text(
                text = "Overview",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                modifier = Modifier.padding(horizontal = 20.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Stats cards row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    modifier = Modifier.weight(1f),
                    value = "$totalScans",
                    label = "Total Scans",
                    borderColor = Color(0xFF2196F3),
                    valueColor = Color(0xFF2196F3)
                )
                StatCard(
                    modifier = Modifier.weight(1f),
                    value = "$diseasedCount",
                    label = "Diseased",
                    borderColor = StatusError,
                    valueColor = StatusError
                )
                StatCard(
                    modifier = Modifier.weight(1f),
                    value = "$healthyCount",
                    label = "Healthy",
                    borderColor = GreenPrimary,
                    valueColor = GreenPrimary
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Scan Your Corn card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .clickable { navController.navigate(Screen.Scan.route) },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = GreenPrimary),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // QR/Scan icon
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(White.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.QrCodeScanner,
                            contentDescription = null,
                            tint = White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = "Scan Your Corn",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = White
                        )
                        Text(
                            text = "Detect diseases instantly",
                            fontSize = 13.sp,
                            color = White.copy(alpha = 0.85f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Disease List section
            Text(
                text = "Disease List",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                modifier = Modifier.padding(horizontal = 20.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // View Disease List button
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .border(3.dp, GreenPrimary, RoundedCornerShape(16.dp))
                    .clickable { navController.navigate(Screen.DiseaseList.route) },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = YellowAccent),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Leaf icon in dark circle
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(GreenDark),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Eco,
                            contentDescription = null,
                            tint = White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(
                            text = "View  Disease List",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = GreenDark
                        )
                        Text(
                            text = "Browse corn diseases",
                            fontSize = 12.sp,
                            color = GreenDark.copy(alpha = 0.8f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Scan History section header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Scan History",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = "See All",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = GreenPrimary,
                    modifier = Modifier.clickable { navController.navigate(Screen.History.route) }
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Filter chips
            val allCount = recentScans.size
            val histHealthyCount = recentScans.count { it.isHealthy }
            val histDiseasedCount = recentScans.count { !it.isHealthy }
            val filteredScans = when (selectedHistoryFilter) {
                1 -> recentScans.filter { it.isHealthy }
                2 -> recentScans.filter { !it.isHealthy }
                else -> recentScans
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                HistoryFilterChip("All ($allCount)", selectedHistoryFilter == 0) { selectedHistoryFilter = 0 }
                HistoryFilterChip("Healthy ($histHealthyCount)", selectedHistoryFilter == 1) { selectedHistoryFilter = 1 }
                HistoryFilterChip("Diseased ($histDiseasedCount)", selectedHistoryFilter == 2) { selectedHistoryFilter = 2 }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (filteredScans.isEmpty()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Background),
                    elevation = CardDefaults.cardElevation(1.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Outlined.CameraAlt,
                            contentDescription = null,
                            tint = TextHint,
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No Scans Yet",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Start scanning to track your plant's health",
                            fontSize = 13.sp,
                            color = TextHint,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
                filteredScans.forEach { scan ->
                    val date = if (scan.timestamp > 0) Date(scan.timestamp) else Date()
                    val histItem = ScanHistoryItem(
                        id = scan.id,
                        sampleName = "Corn Leaf Sample",
                        diseaseName = scan.diseaseName,
                        confidence = scan.confidence,
                        date = dateFormat.format(date),
                        time = timeFormat.format(date),
                        isHealthy = scan.isHealthy
                    )
                    Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)) {
                        HistoryCard(item = histItem, navController = navController)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            } // end content Column
        }
    }
}

@Composable
fun StatCard(
    modifier: Modifier = Modifier,
    value: String,
    label: String,
    borderColor: Color,
    valueColor: Color
) {
    Card(
        modifier = modifier
            .border(1.5.dp, borderColor, RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = White),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = valueColor
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                fontSize = 12.sp,
                color = TextSecondary
            )
        }
    }
}

@Composable
fun BottomNavBar(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(GoldenBackground)
            .navigationBarsPadding()
            .padding(top = 14.dp, bottom = 10.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val items = listOf(
                Triple(Icons.Filled.Home, "Home", 0),
                Triple(Icons.Filled.Person, "Profile", 1),
                Triple(Icons.Filled.QrCodeScanner, "Scan", 2),
                Triple(Icons.Filled.Notifications, "Notification", 3),
                Triple(Icons.Filled.Settings, "Settings", 4)
            )
            items.forEach { (icon, label, index) ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ) { onTabSelected(index) }
                ) {
                    Icon(
                        icon,
                        contentDescription = label,
                        tint = if (selectedTab == index) GreenPrimary else TextPrimary,
                        modifier = Modifier.size(34.dp)
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = label,
                        fontSize = 13.sp,
                        fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                        color = if (selectedTab == index) GreenPrimary else TextPrimary
                    )
                }
            }
        }
    }
}
