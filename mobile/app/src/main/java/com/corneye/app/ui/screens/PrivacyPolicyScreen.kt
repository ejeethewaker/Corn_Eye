package com.corneye.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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

@Composable
fun PrivacyPolicyScreen(navController: NavController) {
    var selectedTab by remember { mutableIntStateOf(4) }

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
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Privacy Policy",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }
            }

            // Content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 20.dp)
            ) {
                // Introduction
                Text(
                    text = "CornEye values your privacy and is committed to protecting your personal information. This policy explains how we collect, use, and protect",
                    fontSize = 14.sp,
                    color = TextSecondary,
                    lineHeight = 20.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "↕ Scroll to read full policy ↕",
                    fontSize = 13.sp,
                    color = TextHint,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Section 1: Information We Collect
                PolicySectionHeader("\uD83D\uDCCB Information We Collect", Color(0xFF1565C0))

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = White),
                    elevation = CardDefaults.cardElevation(1.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        PolicyBulletBold("Personal Information:")
                        PolicyBulletSub("Name, email, contact number for account")
                        Spacer(modifier = Modifier.height(8.dp))
                        PolicyBulletBold("Uploaded Images:")
                        PolicyBulletSub("Corn leaf photos for disease analysis")
                        Spacer(modifier = Modifier.height(8.dp))
                        PolicyBulletBold("Device & Usage Data:")
                        PolicyBulletSub("Device type, location to improve experience")
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Section 2: How We Use Your Info
                PolicySectionHeader("\uD83D\uDCCB How We Use Your Info", GreenPrimary)

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = White),
                    elevation = CardDefaults.cardElevation(1.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        PolicyCheckItem("Provide accurate disease detection results")
                        PolicyCheckItem("Improve AI models for future accuracy")
                        PolicyCheckItem("Send alerts, recommendations, and updates")
                        PolicyCheckItem("Respond to support requests")
                        PolicyCheckItem("Maintain and secure your account")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Warning box
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFFFF9C4), RoundedCornerShape(8.dp))
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "\u26A0\uFE0F We never sell your personal information",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = GreenPrimary,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Section 3: Data Security
                PolicySectionHeader("\uD83D\uDD12 Data Security", StatusError)

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = White),
                    elevation = CardDefaults.cardElevation(1.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        PolicyEmojiItem("\uD83D\uDCE6", "We use modern encryption and secure servers")
                        PolicyEmojiItem("\uD83D\uDC65", "Only authorized personnel access your data")
                        PolicyEmojiItem("\u26A1", "Please protect your login credentials")
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Section 4: Image & Data Ownership
                PolicySectionHeader("\uD83D\uDCCB Image & Data Ownership", Color(0xFF7B1FA2))

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = White),
                    elevation = CardDefaults.cardElevation(1.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        PolicyEmojiItem("\uD83D\uDCF7", "You retain ownership of all captured images")
                        PolicyEmojiItem("\uD83E\uDDCA", "Images used for AI training (anonymously)")
                        PolicyEmojiItem("\uD83D\uDD12", "Your identity never linked without consent")
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun PolicySectionHeader(title: String, accentColor: Color) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                accentColor.copy(alpha = 0.15f),
                RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp)
            )
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Text(
            text = title,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = accentColor
        )
    }
}

@Composable
private fun PolicyBulletBold(text: String) {
    Text(
        text = "• $text",
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold,
        color = TextPrimary
    )
}

@Composable
private fun PolicyBulletSub(text: String) {
    Text(
        text = "   $text",
        fontSize = 13.sp,
        color = TextSecondary,
        modifier = Modifier.padding(start = 8.dp)
    )
}

@Composable
private fun PolicyCheckItem(text: String) {
    Row(
        verticalAlignment = Alignment.Top,
        modifier = Modifier.padding(vertical = 3.dp)
    ) {
        Text("✓ ", fontSize = 14.sp, color = GreenPrimary)
        Text(text, fontSize = 14.sp, color = TextSecondary)
    }
}

@Composable
private fun PolicyEmojiItem(emoji: String, text: String) {
    Row(
        verticalAlignment = Alignment.Top,
        modifier = Modifier.padding(vertical = 4.dp)
    ) {
        Text("$emoji ", fontSize = 14.sp)
        Text(text, fontSize = 14.sp, color = TextSecondary)
    }
}
