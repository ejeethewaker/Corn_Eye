// Disease Detail Screen
// Full information on a specific disease: symptoms, causes, and treatment.
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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.corneye.app.navigation.Screen
import com.corneye.app.ui.theme.*

@Composable
fun DiseaseDetailScreen(
    navController: NavController,
    diseaseName: String
) {
    val disease = cornDiseases.find { it.name == diseaseName } ?: return
    var selectedTab by remember { mutableIntStateOf(-1) }

    val riskBorderColor = when (disease.riskLevel) {
        "High Risk" -> Color(0xFFC62828)
        "Medium Risk" -> Color(0xFFE6A817)
        "Low Risk" -> Color(0xFF00897B)
        else -> TextSecondary
    }
    val riskBgColor = when (disease.riskLevel) {
        "High Risk" -> Color(0xFFFFEBEE)
        "Medium Risk" -> Color(0xFFFFF8E1)
        "Low Risk" -> Color(0xFFE0F2F1)
        else -> Color(0xFFF5F5F5)
    }
    val riskTextColor = when (disease.riskLevel) {
        "High Risk" -> Color(0xFFC62828)
        "Medium Risk" -> Color(0xFFE65100)
        "Low Risk" -> Color(0xFF00695C)
        else -> TextSecondary
    }
    val riskIcon = when (disease.riskLevel) {
        "High Risk" -> "⚠"
        "Low Risk" -> "✓"
        else -> ""
    }
    val riskLabel = when (disease.riskLevel) {
        "High Risk" -> "HIGH RISK"
        "Medium Risk" -> "MEDIUM RISK"
        "Low Risk" -> "LOW RISK"
        else -> disease.riskLevel.uppercase()
    }
    val typeBorderColor = when (disease.type) {
        "Fungal" -> Color(0xFFC62828)
        "Bacterial" -> GreenPrimary
        "Oomycete" -> Color(0xFF6A1B9A)
        "Viral" -> Color(0xFFE65100)
        else -> TextSecondary
    }
    val typeBgColor = when (disease.type) {
        "Fungal" -> White
        "Bacterial" -> White
        "Oomycete" -> White
        "Viral" -> White
        else -> White
    }
    val descriptionTitleColor = when (disease.type) {
        "Bacterial" -> Color(0xFF00897B)
        else -> BrownButton
    }
    val descriptionBgColor = when (disease.riskLevel) {
        "High Risk" -> Color(0xFFFFEBEE)
        "Medium Risk" -> Color(0xFFFFF8E1)
        "Low Risk" -> Color(0xFFE0F2F1)
        else -> Color(0xFFF5F5F5)
    }

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
            // Status bar background - pinned outside scroll
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(GoldenBackground)
                    .background(StatusBarGold)
                    .windowInsetsPadding(WindowInsets.statusBars)
            )

            // Scrollable content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {

            // Golden header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(GoldenBackground)
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Back button
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color.Black)
                            .clickable { navController.popBackStack() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Text(
                        text = disease.name,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }
            }

            // Disease image area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(GreenPrimary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                // Fallback icon (shown behind if image not loaded)
                Icon(
                    Icons.Default.Eco,
                    contentDescription = null,
                    tint = GreenPrimary.copy(alpha = 0.2f),
                    modifier = Modifier.size(100.dp)
                )
                // Real disease photo
                AsyncImage(
                    model = disease.imageUrl,
                    contentDescription = disease.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(16.dp))
                )

                // Risk badge + Type badge stacked at bottom center inside the image
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Risk badge
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = riskBgColor,
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, riskBorderColor)
                    ) {
                        Text(
                            text = if (riskIcon.isNotEmpty()) "$riskIcon $riskLabel" else riskLabel,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = riskTextColor,
                            modifier = Modifier.padding(horizontal = 28.dp, vertical = 10.dp)
                        )
                    }
                    // Type badge
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = typeBgColor,
                        border = androidx.compose.foundation.BorderStroke(1.dp, typeBorderColor)
                    ) {
                        Text(
                            text = "${disease.type} Disease",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = typeBorderColor,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Description section
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = descriptionBgColor),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Text(
                        text = "Description",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = descriptionTitleColor
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Split description by paragraphs
                    val paragraphs = disease.fullDescription.split("\n\n")
                    paragraphs.forEachIndexed { index, paragraph ->
                        Text(
                            text = paragraph,
                            fontSize = 15.sp,
                            color = TextPrimary,
                            lineHeight = 24.sp
                        )
                        if (index < paragraphs.lastIndex) {
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            } // end inner scroll Column
        }
    }
}
