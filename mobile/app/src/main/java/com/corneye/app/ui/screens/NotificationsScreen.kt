// Notifications Screen
// Lists admin-sent notifications and updates received by the farmer.
package com.corneye.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.firebase.database.ServerValue
import com.corneye.app.data.FirebaseHelper
import com.corneye.app.data.UserPreferences
import com.corneye.app.navigation.Screen
import com.corneye.app.ui.theme.*

data class NotificationItem(
    val id: String,
    val title: String,
    val timeAgo: String,
    val description: String,
    val actionLabel: String,
    val type: NotificationType,
    val isRead: Boolean = false,
    val analysisId: String = "",
    val analysisLabel: String = "",
    val confidenceScore: Float = 0f,
    val date: String = "",
    val time: String = ""
)

enum class NotificationType {
    SCAN_COMPLETE, DISEASE_DETECTED
}

@Composable
fun NotificationsScreen(navController: NavController) {
    val context = LocalContext.current
    val userId by UserPreferences.getUserId(context).collectAsState(initial = "")
    var selectedTab by remember { mutableIntStateOf(3) }
    var selectedFilter by remember { mutableIntStateOf(0) } // 0=All, 1=Unread

    var notifications by remember { mutableStateOf<List<NotificationItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    // Load notifications from Firebase
    LaunchedEffect(userId) {
        if (userId.isNotEmpty()) {
            FirebaseHelper.notificationsRef().get()
                .addOnSuccessListener { snapshot ->
                    val items = mutableListOf<NotificationItem>()
                    val dateFormat = java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault())
                    val timeFormat = java.text.SimpleDateFormat("h:mm a", java.util.Locale.getDefault())
                    snapshot.children.forEach { child ->
                        val notifUserId = child.child("farmer_id").getValue(String::class.java) ?: ""
                        if (notifUserId == userId) {
                            val typeStr = child.child("notif_type").getValue(String::class.java) ?: "scan_healthy"
                            val timeScanned = child.child("time_scanned").getValue(Long::class.java) ?: 0L
                            val dateObj = if (timeScanned > 0) java.util.Date(timeScanned) else java.util.Date()
                            items.add(
                                NotificationItem(
                                    id = child.key ?: "",
                                    title = child.child("notif_title").getValue(String::class.java) ?: "",
                                    timeAgo = if (timeScanned > 0) getTimeAgo(timeScanned) else "",
                                    description = child.child("notif_message").getValue(String::class.java) ?: "",
                                    actionLabel = if (typeStr == "scan_disease") "View Details →" else "View Results →",
                                    type = if (typeStr == "scan_disease") NotificationType.DISEASE_DETECTED else NotificationType.SCAN_COMPLETE,
                                    isRead = child.child("is_read").getValue(Boolean::class.java) ?: false,
                                    analysisId = child.child("analysis_id").getValue(String::class.java) ?: "",
                                    analysisLabel = child.child("analysis_label").getValue(String::class.java) ?: "",
                                    confidenceScore = child.child("confidence_score").getValue(Float::class.java) ?: 0f,
                                    date = dateFormat.format(dateObj),
                                    time = timeFormat.format(dateObj)
                                )
                            )
                        }
                    }
                    notifications = items.sortedByDescending { it.id }
                    isLoading = false
                }
                .addOnFailureListener { isLoading = false }
        }
    }

    val filteredNotifications = when (selectedFilter) {
        1 -> notifications.filter { !it.isRead }
        else -> notifications
    }

    val allCount = notifications.size
    val unreadCount = notifications.count { !it.isRead }

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
                        1 -> navController.navigate(Screen.Profile.route)
                        2 -> navController.navigate(Screen.Scan.route)
                        3 -> { /* Already here */ }
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
            // Status bar background
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(GoldenBackground)
                    .background(StatusBarGold)
                    .windowInsetsPadding(WindowInsets.statusBars)
            )

            // Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(GoldenBackground)
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
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
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Notification",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }
            }

            // Filter tabs - outside the golden header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Background)
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                NotificationFilterChip("All ($allCount)", selectedFilter == 0) { selectedFilter = 0 }
                NotificationFilterChip("Unread ($unreadCount)", selectedFilter == 1) { selectedFilter = 1 }
            }

            if (filteredNotifications.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Filled.Notifications,
                        contentDescription = null,
                        tint = TextHint,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No Notifications",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "You're all caught up!",
                        fontSize = 14.sp,
                        color = TextHint,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    // TODAY section
                    item {
                        Text(
                            text = "TODAY",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextSecondary,
                            letterSpacing = 1.sp,
                            modifier = Modifier.padding(bottom = 10.dp, start = 4.dp)
                        )
                    }

                    items(filteredNotifications) { notification ->
                        NotificationCard(notification = notification, navController = navController)
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationFilterChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (isSelected) StatusWarning else White)
            .border(1.dp, if (isSelected) StatusWarning else DividerColor, RoundedCornerShape(20.dp))
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (isSelected) White else TextPrimary
        )
    }
}

@Composable
fun NotificationCard(notification: NotificationItem, navController: NavController? = null) {
    val iconTint: Color
    val iconBg: Color
    val icon: ImageVector
    val actionColor: Color

    when (notification.type) {
        NotificationType.SCAN_COMPLETE -> {
            iconTint = GreenPrimary
            iconBg = GreenPrimary.copy(alpha = 0.12f)
            icon = Icons.Default.CheckCircle
            actionColor = GreenPrimary
        }
        NotificationType.DISEASE_DETECTED -> {
            iconTint = StatusWarning
            iconBg = StatusWarning.copy(alpha = 0.12f)
            icon = Icons.Default.Warning
            actionColor = StatusWarning
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, DividerColor, RoundedCornerShape(14.dp)),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (notification.isRead) White else GoldenBackground.copy(alpha = 0.18f)
        ),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(26.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = notification.title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    // Read / Unread badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(
                                if (notification.isRead)
                                    Color(0xFFE0E0E0)
                                else
                                    StatusWarning
                            )
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = if (notification.isRead) "Read" else "New",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (notification.isRead) Color(0xFF757575) else White,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
                Text(
                    text = notification.timeAgo,
                    fontSize = 12.sp,
                    color = actionColor,
                    modifier = Modifier.padding(top = 2.dp)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = notification.description,
                    fontSize = 14.sp,
                    color = TextSecondary,
                    lineHeight = 19.sp
                )
                Spacer(modifier = Modifier.height(10.dp))

                // Action button
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(actionColor)
                        .clickable {
                            // Mark as read in Firebase
                            if (notification.id.isNotEmpty()) {
                                FirebaseHelper.notificationsRef()
                                    .child(notification.id)
                                    .child("is_read")
                                    .setValue(true)
                            }
                            if (notification.analysisLabel.isNotEmpty() && navController != null) {
                                navController.navigate(
                                    Screen.FullReport.createRoute(
                                        scanId = notification.analysisId,
                                        diseaseName = notification.analysisLabel,
                                        confidence = notification.confidenceScore,
                                        date = notification.date,
                                        time = notification.time
                                    )
                                )
                            }
                        }
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = notification.actionLabel,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = White
                    )
                }
            }
        }
    }
}

private fun getTimeAgo(timestamp: Long): String {
    if (timestamp <= 0) return ""
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    val minutes = diff / (1000 * 60)
    val hours = diff / (1000 * 60 * 60)
    val days = diff / (1000 * 60 * 60 * 24)
    return when {
        minutes < 1 -> "Just now"
        minutes < 60 -> "$minutes minutes ago"
        hours < 24 -> "$hours hours ago"
        days < 7 -> "$days days ago"
        else -> "${days / 7} weeks ago"
    }
}
