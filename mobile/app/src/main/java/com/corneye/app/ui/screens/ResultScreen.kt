package com.corneye.app.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.corneye.app.navigation.Screen
import com.corneye.app.ui.theme.*

@Composable
fun ResultScreen(
    navController: NavController,
    diseaseName: String,
    confidence: Float
) {
    val isHealthy = diseaseName == "Healthy"
    val statusColor = if (isHealthy) StatusActive else StatusError
    val confidencePercent = (confidence * 100).toInt()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .verticalScroll(rememberScrollState())
    ) {
        // Status bar background
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(StatusBarGold)
                .windowInsetsPadding(WindowInsets.statusBars)
        )

        // Top bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(GoldenBackground)
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            IconButton(
                onClick = { navController.popBackStack() },
                modifier = Modifier.align(Alignment.CenterStart)
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color.Black)
            ) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = White, modifier = Modifier.size(20.dp))
            }
            Text(
                text = "Scan Result",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                modifier = Modifier.align(Alignment.Center)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Result card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(4.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Status icon
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(statusColor.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        if (isHealthy) Icons.Default.CheckCircle else Icons.Default.Warning,
                        contentDescription = null,
                        tint = statusColor,
                        modifier = Modifier.size(44.dp)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = if (isHealthy) "Your corn is healthy!" else "Disease Detected",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = diseaseName,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = statusColor
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Confidence bar
                Text(
                    text = "Confidence: $confidencePercent%",
                    fontSize = 14.sp,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = confidence,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = statusColor,
                    trackColor = statusColor.copy(alpha = 0.15f)
                )
            }
        }

        if (!isHealthy) {
            Spacer(modifier = Modifier.height(24.dp))

            // Recommendations
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Text(
                        text = "Recommendations",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    val recommendations = getRecommendations(diseaseName)
                    recommendations.forEach { (icon, text) ->
                        RecommendationItem(icon = icon, text = text)
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Action buttons
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
        ) {
            Button(
                onClick = {
                    navController.navigate(Screen.Scan.route) {
                        popUpTo(Screen.Home.route)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary)
            ) {
                Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Scan Again", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = GreenPrimary)
            ) {
                Icon(Icons.Default.Home, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Back to Home", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
fun RecommendationItem(icon: ImageVector, text: String) {
    Row(verticalAlignment = Alignment.Top) {
        Icon(
            icon,
            contentDescription = null,
            tint = GreenPrimary,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = text,
            fontSize = 14.sp,
            color = TextPrimary,
            lineHeight = 20.sp
        )
    }
}

private fun getRecommendations(disease: String): List<Pair<ImageVector, String>> {
    return when (disease) {
        "Northern Leaf Blight" -> listOf(
            Icons.Default.Spa to "Apply foliar fungicides such as azoxystrobin or pyraclostrobin",
            Icons.Default.Loop to "Practice crop rotation with non-host crops",
            Icons.Default.Delete to "Remove and destroy infected plant debris",
            Icons.Default.Grass to "Use resistant corn hybrids for future planting"
        )
        "Common Rust" -> listOf(
            Icons.Default.Spa to "Apply fungicides at first sign of pustules",
            Icons.Default.Schedule to "Plant early to avoid peak rust conditions",
            Icons.Default.Grass to "Choose rust-resistant corn varieties",
            Icons.Default.Visibility to "Monitor fields weekly during humid weather"
        )
        "Gray Leaf Spot" -> listOf(
            Icons.Default.Spa to "Apply strobilurin-based fungicides preventively",
            Icons.Default.Air to "Improve air circulation by adjusting plant spacing",
            Icons.Default.Loop to "Rotate with soybeans or other non-host crops",
            Icons.Default.WaterDrop to "Avoid overhead irrigation when possible"
        )
        else -> listOf(
            Icons.Default.CheckCircle to "Continue regular monitoring of your crops",
            Icons.Default.WaterDrop to "Maintain proper irrigation practices"
        )
    }
}
