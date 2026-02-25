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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.corneye.app.navigation.Screen
import com.corneye.app.ui.theme.*

data class NotificationItem(
    val id: String,
    val title: String,
    val timeAgo: String,
    val description: String,
    val actionLabel: String,
    val type: NotificationType,
    val isRead: Boolean = false
)

enum class NotificationType {
    SCAN_COMPLETE, DISEASE_DETECTED
}

@Composable
fun NotificationsScreen(navController: NavController) {
    var selectedTab by remember { mutableIntStateOf(3) }
    var selectedFilter by remember { mutableIntStateOf(0) } // 0=All, 1=Unread

    val notifications = remember {
        listOf(
            NotificationItem(
                id = "1",
                title = "Scan Complete",
                timeAgo = "2 hours ago",
                description = "Your corn leaf scan has been successfully analyzed.",
                actionLabel = "View Results →",
                type = NotificationType.SCAN_COMPLETE,
                isRead = false
            ),
            NotificationItem(
                id = "2",
                title = "Disease Detected",
                timeAgo = "5 hours ago",
                description = "Gray Leaf Spot detected. Check the result.",
                actionLabel = "View Details →",
                type = NotificationType.DISEASE_DETECTED,
                isRead = false
            ),
            NotificationItem(
                id = "3",
                title = "Scan Complete",
                timeAgo = "1 day ago",
                description = "Your corn leaf scan has been successfully analyzed.",
                actionLabel = "View Results →",
                type = NotificationType.SCAN_COMPLETE,
                isRead = true
            ),
            NotificationItem(
                id = "4",
                title = "Disease Detected",
                timeAgo = "2 days ago",
                description = "Common Rust detected. Check the result.",
                actionLabel = "View Details →",
                type = NotificationType.DISEASE_DETECTED,
                isRead = true
            ),
        )
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
                        1 -> navController.navigate(Screen.History.route)
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
                        NotificationCard(notification = notification)
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
fun NotificationCard(notification: NotificationItem) {
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
        colors = CardDefaults.cardColors(containerColor = White),
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
                        .clickable { /* Handle action */ }
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
