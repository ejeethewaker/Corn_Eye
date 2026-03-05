// Profile Screen
// Farmer account info, profile photo, and subscription status overview.
package com.corneye.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
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

@Composable
fun ProfileScreen(navController: NavController) {
    val context = LocalContext.current
    val userId by UserPreferences.getUserId(context).collectAsState(initial = "")

    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var farmSize by remember { mutableStateOf("") }
    var photoUrl by remember { mutableStateOf("") }
    var totalScans by remember { mutableIntStateOf(0) }
    var weeklyScans by remember { mutableIntStateOf(0) }
    var isLoading by remember { mutableStateOf(true) }

    // Load farmer profile from Firebase
    LaunchedEffect(userId) {
        if (userId.isNotEmpty()) {
            FirebaseHelper.farmersRef().child(userId).get()
                .addOnSuccessListener { snapshot ->
                    if (snapshot.exists()) {
                        fullName = snapshot.child("fullname").getValue(String::class.java) ?: ""
                        email = snapshot.child("email_address").getValue(String::class.java) ?: ""
                        phone = snapshot.child("phone_num").getValue(String::class.java) ?: ""
                        location = snapshot.child("farm_location").getValue(String::class.java) ?: ""
                        farmSize = snapshot.child("farm_area").getValue(String::class.java) ?: ""
                    }
                    isLoading = false
                }
                .addOnFailureListener { isLoading = false }

            // Load scan counts from analysis_results
            FirebaseHelper.analysisResultsRef().get()
                .addOnSuccessListener { snapshot ->
                    var total = 0
                    var weekly = 0
                    val oneWeekAgo = System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000L
                    snapshot.children.forEach { child ->
                        val scanUserId = child.child("farmer_id").getValue(String::class.java) ?: ""
                        if (scanUserId == userId) {
                            total++
                            val timeScanned = child.child("time_scanned").getValue(Long::class.java) ?: 0L
                            if (timeScanned >= oneWeekAgo) weekly++
                        }
                    }
                    totalScans = total
                    weeklyScans = weekly
                }
        }
    }

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

    val initials = fullName.split(" ")
        .mapNotNull { it.firstOrNull()?.uppercase() }
        .joinToString("")
        .take(2)

    val photoBytes = remember(photoUrl) {
        if (photoUrl.isNotEmpty()) {
            try { Base64.decode(photoUrl, Base64.DEFAULT) }
            catch (e: Exception) { null }
        } else null
    }

    var selectedTab by remember { mutableIntStateOf(-1) }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        bottomBar = {
            BottomNavBar(
                selectedTab = selectedTab,
                onTabSelected = { index ->
                    selectedTab = index
                    when (index) {
                        0 -> navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Home.route) { inclusive = true }
                        }
                        1 -> { /* Already here */ }
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
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            // Status bar background
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(GoldenBackground)
                    .background(StatusBarGold)
                    .windowInsetsPadding(WindowInsets.statusBars)
            )

            // Golden header with back button and title
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(GoldenBackground)
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { navController.popBackStack() },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color.Black)
                    ) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = "Profile",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }
            }

            // Divider
            HorizontalDivider(color = DividerColor, thickness = 1.dp)

            // Profile card area
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Background)
                    .padding(horizontal = 20.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Golden profile card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = GoldenBackground),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Avatar with black border ring
                        Box(contentAlignment = Alignment.Center) {
                            Box(
                                modifier = Modifier
                                    .size(100.dp)
                                    .clip(CircleShape)
                                    .background(GoldenBackground)
                                    .border(3.dp, Color.Black, CircleShape),
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
                                        text = initials.ifEmpty { "?" },
                                        fontSize = 42.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                }
                            }
                            // Edit icon on avatar
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(Color.White)
                                    .border(1.5.dp, Color.Black, CircleShape)
                                    .align(Alignment.BottomEnd)
                                    .clickable { navController.navigate(Screen.EditProfile.route) },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Edit,
                                    contentDescription = "Edit",
                                    tint = Color.Black,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = fullName.ifEmpty { "Loading..." },
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = email.ifEmpty { "" },
                            fontSize = 14.sp,
                            color = TextPrimary.copy(alpha = 0.7f)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Stats row inside card
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Total Scans
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .border(1.5.dp, Color.Black.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                    .background(Color.White.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                    .padding(vertical = 14.dp, horizontal = 16.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "$totalScans",
                                        fontSize = 22.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = "Total Scans",
                                        fontSize = 14.sp,
                                        color = TextPrimary.copy(alpha = 0.7f)
                                    )
                                }
                            }
                            // This Week
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .border(1.5.dp, Color.Black.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                    .background(Color.White.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                    .padding(vertical = 14.dp, horizontal = 16.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "$weeklyScans",
                                        fontSize = 22.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = "This Week",
                                        fontSize = 14.sp,
                                        color = TextPrimary.copy(alpha = 0.7f)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Account Information header
                Text(
                    text = "Account Information",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                )

                // Phone Number card
                AccountInfoCard(
                    icon = Icons.Default.Phone,
                    label = "Phone Number",
                    value = phone.ifEmpty { "Not set" }
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Location card
                AccountInfoCard(
                    icon = Icons.Default.LocationOn,
                    label = "Location",
                    value = location.ifEmpty { "Not set" }
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Farm Size card
                AccountInfoCard(
                    icon = Icons.Default.Agriculture,
                    label = "Farm Size",
                    value = farmSize.ifEmpty { "Not set" }
                )

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun AccountInfoCard(
    icon: ImageVector,
    label: String,
    value: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = GoldenBackground),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(GoldenBackground),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = TextPrimary,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = label,
                    fontSize = 13.sp,
                    color = TextPrimary.copy(alpha = 0.7f)
                )
                Text(
                    text = value,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }
        }
    }
}
