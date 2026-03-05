// Result Screen
// Shows the disease classification result with confidence score after a scan.
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.corneye.app.navigation.Screen
import com.corneye.app.ui.theme.*
import java.net.URLDecoder

@Composable
fun ResultScreen(
    navController: NavController,
    diseaseName: String,
    confidence: Float,
    imageUriEncoded: String = ""
) {
    val imageUri = remember {
        try { URLDecoder.decode(imageUriEncoded, "UTF-8") } catch (e: Exception) { imageUriEncoded }
    }
    val isHealthy = diseaseName.equals("Healthy", ignoreCase = true)
    val badgeColor = if (isHealthy) StatusActive else StatusError
    val confidencePercent = (confidence * 100).toInt()
    val diseaseData = remember(diseaseName) { getDiseaseData(diseaseName) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .verticalScroll(rememberScrollState())
    ) {
        // Status bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(GoldenBackground)
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
                onClick = { navController.navigate(Screen.Home.route) { popUpTo(Screen.Home.route) { inclusive = true } } },
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color.Black)
            ) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = White, modifier = Modifier.size(20.dp))
            }
            Text(
                text = "Diagnosis Report",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                modifier = Modifier.align(Alignment.Center)
            )
        }

        // Scanned leaf image
        if (imageUri.isNotEmpty()) {
            AsyncImage(
                model = imageUri,
                contentDescription = "Scanned leaf",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp),
                contentScale = ContentScale.Crop
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Disease badge
        OutlinedCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.outlinedCardColors(containerColor = Color.White),
            border = CardDefaults.outlinedCardBorder().copy(brush = SolidColor(badgeColor))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    if (isHealthy) Icons.Default.CheckCircle else Icons.Default.Warning,
                    contentDescription = null,
                    tint = badgeColor,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isHealthy) "Healthy Corn Detected" else "$diseaseName Detected",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = badgeColor
                )
            }
        }

        // Confidence
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Confidence: ", fontSize = 13.sp, color = TextSecondary)
            Text("$confidencePercent%", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
        }
        Spacer(modifier = Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = { confidence },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = badgeColor,
            trackColor = badgeColor.copy(alpha = 0.15f)
        )

        Spacer(modifier = Modifier.height(20.dp))

        if (isHealthy) {
            ReportSection(emoji = "✅", title = "Great News!") {
                BulletCard(items = listOf(
                    "Your corn plant appears healthy",
                    "Continue regular monitoring",
                    "Maintain proper irrigation and spacing",
                    "Apply preventive care during humid weather"
                ))
            }
        } else {
            ReportSection(emoji = "\u26A0\uFE0F", title = "Causes") {
                BulletCard(items = diseaseData.causes)
            }
            ReportSection(emoji = "\uD83D\uDD2C", title = "Symptoms") {
                BulletCard(items = diseaseData.symptoms)
            }
            ReportSection(emoji = "\uD83D\uDEE1\uFE0F", title = "Prevention Methods") {
                BulletCard(items = diseaseData.prevention)
            }
            ReportSection(emoji = "\uD83D\uDC8A", title = "Recommended Treatment") {
                diseaseData.treatments.forEachIndexed { index, treatment ->
                    TreatmentCard(number = index + 1, title = treatment.title, steps = treatment.steps)
                    if (index < diseaseData.treatments.lastIndex) Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = { navController.navigate(Screen.Scan.route) { popUpTo(Screen.Home.route) } },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
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
            onClick = { navController.navigate(Screen.Home.route) { popUpTo(Screen.Home.route) { inclusive = true } } },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .height(52.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = GreenPrimary)
        ) {
            Icon(Icons.Default.Home, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Back to Home", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun ReportSection(emoji: String, title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(emoji, fontSize = 20.sp)
            Spacer(modifier = Modifier.width(8.dp))
            Text(title, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
        }
        Spacer(modifier = Modifier.height(8.dp))
        content()
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun BulletCard(items: List<String>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = GoldenBackground),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
            items.forEach { item ->
                Row(modifier = Modifier.padding(vertical = 2.dp)) {
                    Text("• ", fontSize = 14.sp, color = TextPrimary, fontWeight = FontWeight.Bold)
                    Text(item, fontSize = 14.sp, color = TextPrimary, lineHeight = 20.sp)
                }
            }
        }
    }
}

@Composable
private fun TreatmentCard(number: Int, title: String, steps: List<String>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFD3F0D5)),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
            Text(
                text = "$number. $title",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = GreenPrimary
            )
            Spacer(modifier = Modifier.height(6.dp))
            steps.forEach { step ->
                Row(modifier = Modifier.padding(vertical = 2.dp)) {
                    Text("• ", fontSize = 13.sp, color = TextPrimary)
                    Text(step, fontSize = 13.sp, color = TextPrimary, lineHeight = 18.sp)
                }
            }
        }
    }
}

private data class TreatmentItem(val title: String, val steps: List<String>)
private data class DiseaseData(
    val causes: List<String>,
    val symptoms: List<String>,
    val prevention: List<String>,
    val treatments: List<TreatmentItem>
)

private fun getDiseaseData(disease: String): DiseaseData = when (disease) {
    "Northern Leaf Blight" -> DiseaseData(
        causes = listOf(
            "Fungal infection caused by Exserohilum turcicum",
            "Cool temperatures (64–81°F) with high humidity",
            "Infected crop debris in soil",
            "Overhead irrigation promoting leaf wetness"
        ),
        symptoms = listOf(
            "Long, cigar-shaped gray-green lesions on leaves",
            "Tan/brown necrotic spots with wavy margins",
            "Yellowing of surrounding leaf tissue",
            "Premature leaf death in severe cases"
        ),
        prevention = listOf(
            "Plant resistant corn hybrids",
            "Rotate crops with non-host plants (2–3 year cycle)",
            "Remove and destroy infected crop debris",
            "Avoid overhead irrigation",
            "Monitor fields during cool, humid periods"
        ),
        treatments = listOf(
            TreatmentItem("Fungicide Application", listOf(
                "Apply azoxystrobin or propiconazole at first sign",
                "Spray during early tassel stage",
                "Repeat every 14 days if conditions persist"
            )),
            TreatmentItem("Cultural Practices", listOf(
                "Remove severely infected leaves",
                "Improve air circulation between rows",
                "Reduce irrigation frequency"
            )),
            TreatmentItem("Organic Options", listOf(
                "Copper-based fungicide sprays",
                "Neem oil applied early in season"
            ))
        )
    )
    "Common Rust" -> DiseaseData(
        causes = listOf(
            "Fungal infection caused by Puccinia sorghi",
            "Spores spread by wind over long distances",
            "Cool temperatures (60–77°F) with high humidity",
            "Prolonged leaf wetness (6+ hours)"
        ),
        symptoms = listOf(
            "Small, powdery brick-red/brown pustules on both leaf surfaces",
            "Yellowing (chlorosis) around pustules",
            "Leaves may dry out in severe infections",
            "Reduced photosynthetic capacity"
        ),
        prevention = listOf(
            "Plant rust-resistant corn varieties",
            "Plant early to avoid peak infection periods",
            "Monitor fields weekly during humid weather",
            "Maintain field sanitation after harvest"
        ),
        treatments = listOf(
            TreatmentItem("Fungicide Application", listOf(
                "Apply triazole-based fungicides at first pustule appearance",
                "Mancozeb or chlorothalonil as alternatives",
                "Treat before tasseling for best results"
            )),
            TreatmentItem("Cultural Practices", listOf(
                "Remove heavily infected lower leaves",
                "Reduce plant density in future plantings"
            )),
            TreatmentItem("Organic Options", listOf(
                "Sulfur-based fungicide sprays",
                "Potassium bicarbonate solutions"
            ))
        )
    )
    "Gray Leaf Spot" -> DiseaseData(
        causes = listOf(
            "Fungal infection caused by Cercospora zeae-maydis",
            "Warm and humid weather (75–85°F)",
            "Poor air circulation in dense planting",
            "Infected residue in soil"
        ),
        symptoms = listOf(
            "Small rectangular gray/tan spots",
            "Lesions run parallel to leaf veins",
            "Yellowing (chlorosis) around spots",
            "Premature leaf death in severe cases",
            "Reduced photosynthesis capacity"
        ),
        prevention = listOf(
            "Plant resistant corn varieties",
            "Maintain proper plant spacing",
            "Rotate crops (2–3 year cycle)",
            "Remove and destroy infected debris",
            "Avoid overhead irrigation",
            "Apply preventive fungicides early",
            "Monitor weather conditions"
        ),
        treatments = listOf(
            TreatmentItem("Fungicide Application", listOf(
                "Azoxystrobin-based products",
                "Apply at first sign of disease",
                "Repeat every 14 days if needed"
            )),
            TreatmentItem("Cultural Practices", listOf(
                "Remove severely infected leaves",
                "Improve air circulation",
                "Reduce leaf wetness duration"
            )),
            TreatmentItem("Organic Options", listOf(
                "Copper-based fungicides",
                "Biofungicides containing Bacillus subtilis"
            ))
        )
    )
    else -> DiseaseData(
        causes = listOf("Cause information not available for this diagnosis"),
        symptoms = listOf("Symptom information not available for this diagnosis"),
        prevention = listOf(
            "Consult a local agricultural extension officer",
            "Maintain regular crop monitoring",
            "Follow standard corn farming best practices"
        ),
        treatments = listOf(
            TreatmentItem("General Recommendations", listOf(
                "Consult an agronomist for specific treatment",
                "Document affected areas for follow-up"
            ))
        )
    )
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
