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
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.CheckCircle
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
import com.corneye.app.navigation.Screen
import com.corneye.app.ui.theme.*

data class CornDisease(
    val name: String,
    val type: String,
    val riskLevel: String,
    val shortDescription: String,
    val fullDescription: String
)

val cornDiseases = listOf(
    CornDisease(
        name = "Northern Leaf Blight",
        type = "Fungal",
        riskLevel = "High Risk",
        shortDescription = "Long gray-green elliptical lesions on leaves, rapid spread",
        fullDescription = "Northern Leaf Blight, also known as Turcicum Leaf Blight, is a devastating fungal disease that primarily affects corn plants. It manifests as long, narrow tan or grayish-green elliptical lesions on leaves, which can rapidly expand under favorable conditions.\n\nThe disease thrives in humid environments and can cause severe defoliation, significantly reducing crop yield and quality."
    ),
    CornDisease(
        name = "Common Leaf Rust",
        type = "Fungal",
        riskLevel = "Medium Risk",
        shortDescription = "Orange-brown pustules scattered on leaf surfaces",
        fullDescription = "Common Rust is a widespread fungal disease affecting corn crops worldwide. It appears as small, circular to elongated pustules that are cinnamon-brown to orange-brown in color.\n\nThese pustules break through the leaf surface and release masses of powdery spores. While generally not as severe as other leaf diseases, heavy infections can significantly reduce photosynthetic capacity and yield."
    ),
    CornDisease(
        name = "Gray Leaf Spot",
        type = "Fungal",
        riskLevel = "Medium Risk",
        shortDescription = "Rectangular gray spots parallel to leaf veins",
        fullDescription = "Gray Leaf Spot is a serious foliar disease of corn characterized by distinctive rectangular gray to tan lesions that develop parallel to the leaf veins.\n\nThis fungal pathogen can cause extensive defoliation in susceptible hybrids, particularly under prolonged periods of high humidity and warm temperatures. The disease has become increasingly problematic in continuous corn production systems."
    ),
    CornDisease(
        name = "Bacterial Leaf Streak",
        type = "Bacterial",
        riskLevel = "Low Risk",
        shortDescription = "Long narrow orange streaks between leaf veins",
        fullDescription = "Bacterial Leaf Streak is an emerging bacterial disease of corn that produces long, narrow orange to brown streaks on the leaves. The disease is spread by wind-driven rain and can develop rapidly during periods of warm, wet weather.\n\nWhile typically not as devastating as fungal leaf diseases, it can reduce photosynthetic area and overall plant health in susceptible hybrids."
    ),
    CornDisease(
        name = "Southern Leaf Blight",
        type = "Fungal",
        riskLevel = "High Risk",
        shortDescription = "Large tan lesions with dark borders on leaves",
        fullDescription = "Southern Leaf Blight is a highly destructive fungal disease that can cause severe yield losses in susceptible corn hybrids. The disease produces large tan to brown lesions with distinct dark reddish-brown borders.\n\nIt spreads rapidly during warm, humid weather and can quickly defoliate entire fields. The disease gained notoriety during the 1970 corn blight epidemic."
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
            // Disease image placeholder
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        when {
                            disease.name.contains("Northern") -> Color(0xFF5D7A3A)
                            disease.name.contains("Rust") -> Color(0xFF8B6914)
                            disease.name.contains("Gray") -> Color(0xFF6B8E5A)
                            disease.name.contains("Bacterial") -> Color(0xFF4A7A3A)
                            disease.name.contains("Southern") -> Color(0xFF7A6B3A)
                            else -> GreenPrimary
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Eco,
                    contentDescription = null,
                    tint = White.copy(alpha = 0.7f),
                    modifier = Modifier.size(48.dp)
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
        "Bacterial" -> Color(0xFFE8F5E9)
        else -> Color(0xFFE8F5E9)
    }
    val textColor = when (type) {
        "Fungal" -> GreenPrimary
        "Bacterial" -> GreenPrimary
        else -> GreenPrimary
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
