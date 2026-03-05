// Disease Library Screen
// Browse all corn diseases detectable by the app with brief descriptions.
package com.corneye.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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

data class CornDisease(
    val name: String,
    val type: String,
    val riskLevel: String,
    val shortDescription: String,
    val fullDescription: String,
    val imageUrl: String = ""
)

val cornDiseases = listOf(
    CornDisease(
        name = "Northern Leaf Blight",
        type = "Fungal",
        riskLevel = "High Risk",
        shortDescription = "Long gray-green elliptical lesions on leaves, rapid spread in humid conditions.",
        fullDescription = "Northern Leaf Blight (NLB), caused by Exserohilum turcicum, is a major fungal disease of corn. It appears as long, narrow, tan to grayish-green elliptical lesions (1–6 inches) on leaves.\n\nUnder humid, cool conditions the lesions expand rapidly and coalesce, causing severe defoliation. Yield losses up to 50% can occur in susceptible hybrids when disease develops before tasseling.\n\nManagement: Use resistant hybrids, rotate crops, apply fungicides (triazoles or strobilurins) at early disease onset.",
        imageUrl = "https://inaturalist-open-data.s3.amazonaws.com/photos/96642712/medium.jpg"
    ),
    CornDisease(
        name = "Common Rust",
        type = "Fungal",
        riskLevel = "Medium Risk",
        shortDescription = "Brick-red to orange-brown pustules scattered on both leaf surfaces.",
        fullDescription = "Common Rust, caused by Puccinia sorghi, is a widespread fungal disease that affects corn worldwide. Cinnamon-brown to brick-red pustules appear on both upper and lower leaf surfaces, releasing masses of powdery spores.\n\nThe pathogen spreads via wind-blown spores and thrives under cool (60–77°F), humid conditions. While usually not severe in tropical climates, heavy infections on susceptible sweet corn or seed corn can significantly reduce yield.\n\nManagement: Plant resistant hybrids, apply fungicides (propiconazole, azoxystrobin) when pustules first appear.",
        imageUrl = "https://inaturalist-open-data.s3.amazonaws.com/photos/224464294/medium.jpeg"
    ),
    CornDisease(
        name = "Gray Leaf Spot",
        type = "Fungal",
        riskLevel = "High Risk",
        shortDescription = "Rectangular gray-tan lesions bordered by leaf veins, reducing photosynthesis.",
        fullDescription = "Gray Leaf Spot (GLS), caused by Cercospora zeae-maydis, is one of the most yield-limiting corn diseases worldwide. Lesions are rectangular, gray to tan, and run parallel between leaf veins giving them a distinctive blocky appearance.\n\nThe disease thrives under prolonged high relative humidity (≥90%) and temperatures of 75–85°F. Severe infection causes premature death of leaves and significant yield reductions.\n\nManagement: Plant resistant hybrids, use crop rotation, apply foliar fungicides at VT/R1 growth stage.",
        imageUrl = "https://inaturalist-open-data.s3.amazonaws.com/photos/88462941/medium.jpg"
    )
)


@Composable
fun DiseaseListScreen(navController: NavController) {
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
                    .background(GoldenBackground)
                    .background(StatusBarGold)
                    .windowInsetsPadding(WindowInsets.statusBars)
            )

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
                        text = "Disease List",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }
            }

            // Disease list
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(cornDiseases) { disease ->
                    DiseaseCard(
                        disease = disease,
                        onClick = {
                            navController.navigate(Screen.DiseaseDetail.createRoute(disease.name))
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun DiseaseCard(
    disease: CornDisease,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Disease image
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(GreenPrimary.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                // Fallback icon visible if image fails to load
                Icon(
                    Icons.Default.Eco,
                    contentDescription = null,
                    tint = GreenPrimary.copy(alpha = 0.4f),
                    modifier = Modifier.size(40.dp)
                )
                // Disease photo drawn on top
                AsyncImage(
                    model = disease.imageUrl,
                    contentDescription = disease.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(100.dp)
                        .clip(RoundedCornerShape(12.dp))
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                // Disease name
                Text(
                    text = disease.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Type and Risk badges
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Type badge
                    TypeBadge(type = disease.type)
                    // Risk badge
                    RiskBadge(riskLevel = disease.riskLevel)
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Short description
                Text(
                    text = disease.shortDescription,
                    fontSize = 13.sp,
                    color = TextSecondary,
                    lineHeight = 18.sp
                )
            }
        }
    }
}

@Composable
fun TypeBadge(type: String) {
    val bgColor = when (type) {
        "Fungal" -> Color(0xFFE8F5E9)
        "Bacterial" -> Color(0xFFE3F2FD)
        "Oomycete" -> Color(0xFFF3E5F5)
        "Viral" -> Color(0xFFFFF3E0)
        else -> Color(0xFFF5F5F5)
    }
    val textColor = when (type) {
        "Fungal" -> GreenPrimary
        "Bacterial" -> Color(0xFF1565C0)
        "Oomycete" -> Color(0xFF6A1B9A)
        "Viral" -> Color(0xFFE65100)
        else -> TextSecondary
    }

    Surface(
        shape = RoundedCornerShape(6.dp),
        color = bgColor
    ) {
        Text(
            text = type,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = textColor,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun RiskBadge(riskLevel: String) {
    val bgColor = when (riskLevel) {
        "High Risk" -> Color(0xFFFFEBEE)
        "Medium Risk" -> Color(0xFFFFF8E1)
        "Low Risk" -> Color(0xFFE0F2F1)
        else -> Color(0xFFF5F5F5)
    }
    val textColor = when (riskLevel) {
        "High Risk" -> Color(0xFFC62828)
        "Medium Risk" -> Color(0xFFE65100)
        "Low Risk" -> Color(0xFF00695C)
        else -> TextSecondary
    }
    val icon = when (riskLevel) {
        "High Risk" -> "⚠"
        "Low Risk" -> "✓"
        else -> ""
    }

    Surface(
        shape = RoundedCornerShape(6.dp),
        color = bgColor
    ) {
        Text(
            text = if (icon.isNotEmpty()) "$icon $riskLevel" else riskLevel,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = textColor,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}
