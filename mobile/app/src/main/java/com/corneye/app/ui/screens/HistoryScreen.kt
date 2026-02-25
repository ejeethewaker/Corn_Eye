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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.corneye.app.navigation.Screen
import com.corneye.app.ui.theme.*

data class ScanHistoryItem(
    val id: String,
    val sampleName: String,
    val diseaseName: String,
    val confidence: Float,
    val date: String,
    val time: String,
    val isHealthy: Boolean
)

@Composable
fun HistoryScreen(navController: NavController) {
    val historyItems = remember {
        listOf(
            ScanHistoryItem("1", "Corn Leaf Sample", "Gray Leaf Spot Detected", 0.92f, "Feb 16, 2026", "2:15 PM", false),
            ScanHistoryItem("2", "Corn Leaf Sample", "Common Rust Detected", 0.87f, "Feb 15, 2026", "11:30 AM", false),
        )
    }

    var selectedTab by remember { mutableIntStateOf(1) }
    var selectedFilter by remember { mutableIntStateOf(0) } // 0=All, 1=Healthy, 2=Diseased

    val filteredItems = when (selectedFilter) {
        1 -> historyItems.filter { it.isHealthy }
        2 -> historyItems.filter { !it.isHealthy }
        else -> historyItems
    }

    val allCount = historyItems.size
    val healthyCount = historyItems.count { it.isHealthy }
    val diseasedCount = historyItems.count { !it.isHealthy }

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
                        1 -> { /* Already on History */ }
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
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = White, modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Scan History",
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
                HistoryFilterChip("All ($allCount)", selectedFilter == 0) { selectedFilter = 0 }
                HistoryFilterChip("Healthy ($healthyCount)", selectedFilter == 1) { selectedFilter = 1 }
                HistoryFilterChip("Diseased ($diseasedCount)", selectedFilter == 2) { selectedFilter = 2 }
            }

            if (filteredItems.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.History,
                            contentDescription = null,
                            tint = TextHint,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No scan history yet",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Start scanning to see your history here",
                            fontSize = 14.sp,
                            color = TextSecondary
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredItems) { item ->
                        HistoryCard(item = item)
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryFilterChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (isSelected) GreenPrimary else White)
            .border(1.dp, if (isSelected) GreenPrimary else DividerColor, RoundedCornerShape(20.dp))
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
fun HistoryCard(item: ScanHistoryItem) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Top row: sample name + badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "\uD83C\uDF3E", fontSize = 18.sp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = item.sampleName,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }
                // Status badge
                Box(
                    modifier = Modifier
                        .background(
                            color = if (item.isHealthy) StatusActive.copy(alpha = 0.12f) else StatusError.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (item.isHealthy) "✅" else "⚠\uFE0F",
                            fontSize = 11.sp
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (item.isHealthy) "Healthy" else "Detected",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (item.isHealthy) StatusActive else StatusError
                        )
                    }
                }
            }

            // Date + time
            Text(
                text = "${item.date} • ${item.time}",
                fontSize = 13.sp,
                color = TextSecondary,
                modifier = Modifier.padding(top = 4.dp, bottom = 10.dp)
            )

            // Disease name highlight bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = if (item.isHealthy) StatusActive.copy(alpha = 0.08f) else StatusWarning.copy(alpha = 0.10f),
                        shape = RoundedCornerShape(8.dp)
                    )
                    .border(
                        width = 0.dp,
                        color = Color.Transparent,
                        shape = RoundedCornerShape(8.dp)
                    )
            ) {
                Row(modifier = Modifier.padding(12.dp)) {
                    Box(
                        modifier = Modifier
                            .width(3.dp)
                            .height(18.dp)
                            .background(
                                if (item.isHealthy) StatusActive else StatusWarning,
                                RoundedCornerShape(2.dp)
                            )
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = item.diseaseName,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (item.isHealthy) StatusActive else StatusError
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // View Full Report link
            Text(
                text = "View Full Report →",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = StatusWarning,
                modifier = Modifier.clickable { /* Navigate to report */ }
            )
        }
    }
}
