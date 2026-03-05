// Full Report Screen
// Detailed disease report with symptoms, causes, and treatment recommendations.
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.corneye.app.navigation.Screen
import com.corneye.app.ui.theme.*

@Composable
fun FullReportScreen(
    navController: NavController,
    scanId: String,
    diseaseName: String,
    confidence: Float,
    date: String,
    time: String
) {
    val isHealthy = diseaseName.equals("Healthy", ignoreCase = true)
    val badgeColor = if (isHealthy) StatusActive else StatusError
    val confidencePercent = (confidence * 100).toInt()
    val diseaseData = remember(diseaseName) { getFullReportDiseaseData(diseaseName) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
    ) {
        // Status bar - pinned outside scroll
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

        // Top bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(GoldenBackground)
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            IconButton(
                onClick = { navController.popBackStack() },
                modifier = Modifier
                    .align(Alignment.CenterStart)
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
            Text(
                text = "Full Report",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                modifier = Modifier.align(Alignment.Center)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Sample info card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = White),
            elevation = CardDefaults.cardElevation(2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "\uD83C\uDF3E", fontSize = 20.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Corn Leaf Sample",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }
                    Box(
                        modifier = Modifier
                            .background(
                                color = badgeColor.copy(alpha = 0.12f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (isHealthy) "✅" else "⚠\uFE0F",
                                fontSize = 11.sp
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isHealthy) "Healthy" else "Detected",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = badgeColor
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.CalendarToday,
                        contentDescription = null,
                        tint = TextSecondary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "$date • $time",
                        fontSize = 13.sp,
                        color = TextSecondary
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = DividerColor, thickness = 1.dp)
                Spacer(modifier = Modifier.height(12.dp))

                // Diagnosis result
                OutlinedCard(
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.outlinedCardColors(containerColor = Color.White),
                    border = CardDefaults.outlinedCardBorder().copy(brush = SolidColor(badgeColor))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
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

                // Confidence bar
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Confidence Score",
                        fontSize = 13.sp,
                        color = TextSecondary
                    )
                    Text(
                        text = "$confidencePercent%",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { confidence },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = badgeColor,
                    trackColor = badgeColor.copy(alpha = 0.15f)
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Disease information sections
        if (isHealthy) {
            FullReportSection(emoji = "✅", title = "Great News!") {
                FullReportBulletCard(
                    items = listOf(
                        "Your corn plant appears healthy with no signs of disease",
                        "Continue regular monitoring every 1–2 weeks",
                        "Maintain proper irrigation and row spacing",
                        "Apply preventive care during humid weather",
                        "Keep field clean of debris and weeds"
                    ),
                    color = StatusActive
                )
            }
        } else {
            FullReportSection(emoji = "⚠\uFE0F", title = "Causes") {
                FullReportBulletCard(items = diseaseData.causes, color = StatusWarning)
            }
            FullReportSection(emoji = "\uD83D\uDD2C", title = "Symptoms") {
                FullReportBulletCard(items = diseaseData.symptoms, color = StatusWarning)
            }
            FullReportSection(emoji = "\uD83D\uDEE1\uFE0F", title = "Prevention Methods") {
                FullReportBulletCard(items = diseaseData.prevention, color = GreenPrimary)
            }
            FullReportSection(emoji = "\uD83D\uDC8A", title = "Recommended Treatment") {
                diseaseData.treatments.forEachIndexed { index, treatment ->
                    FullReportTreatmentCard(number = index + 1, title = treatment.title, steps = treatment.steps)
                    if (index < diseaseData.treatments.lastIndex) Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Action button
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
            onClick = { navController.popBackStack() },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .height(52.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = GreenPrimary)
        ) {
            Icon(Icons.Default.ArrowBack, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Back to History", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        }

        Spacer(modifier = Modifier.height(32.dp))
        } // end inner scroll Column
    }
}

@Composable
private fun FullReportSection(
    emoji: String,
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
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
private fun FullReportBulletCard(items: List<String>, color: Color) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.07f)),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
            items.forEach { item ->
                Row(modifier = Modifier.padding(vertical = 3.dp)) {
                    Text("• ", fontSize = 14.sp, color = color, fontWeight = FontWeight.Bold)
                    Text(item, fontSize = 14.sp, color = TextPrimary, lineHeight = 20.sp)
                }
            }
        }
    }
}

@Composable
private fun FullReportTreatmentCard(number: Int, title: String, steps: List<String>) {
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
                    Text("• ", fontSize = 13.sp, color = GreenPrimary)
                    Text(step, fontSize = 13.sp, color = TextPrimary, lineHeight = 18.sp)
                }
            }
        }
    }
}

private data class FullReportTreatmentItem(val title: String, val steps: List<String>)
private data class FullReportDiseaseData(
    val causes: List<String>,
    val symptoms: List<String>,
    val prevention: List<String>,
    val treatments: List<FullReportTreatmentItem>
)

private fun getFullReportDiseaseData(disease: String): FullReportDiseaseData = when (disease) {
    "Northern Leaf Blight" -> FullReportDiseaseData(
        causes = listOf(
            "Fungal infection caused by Exserohilum turcicum",
            "Cool temperatures (64–81°F) with high humidity",
            "Infected crop debris remaining in soil",
            "Overhead irrigation promoting prolonged leaf wetness"
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
            "Remove and destroy infected crop debris after harvest",
            "Avoid overhead irrigation",
            "Monitor fields during cool, humid periods"
        ),
        treatments = listOf(
            FullReportTreatmentItem("Fungicide Application", listOf(
                "Apply azoxystrobin or propiconazole at first sign",
                "Spray during early tassel stage",
                "Repeat every 14 days if conditions persist"
            )),
            FullReportTreatmentItem("Cultural Practices", listOf(
                "Remove severely infected leaves promptly",
                "Improve air circulation between rows",
                "Reduce irrigation frequency during humid periods"
            )),
            FullReportTreatmentItem("Organic Options", listOf(
                "Copper-based fungicide sprays",
                "Neem oil applied early in season"
            ))
        )
    )
    "Common Rust" -> FullReportDiseaseData(
        causes = listOf(
            "Fungal infection caused by Puccinia sorghi",
            "Spores spread by wind over long distances",
            "Cool temperatures (60–77°F) with high humidity",
            "Prolonged leaf wetness (6+ hours)"
        ),
        symptoms = listOf(
            "Small, powdery brick-red/brown pustules on both leaf surfaces",
            "Yellowing (chlorosis) around pustules",
            "Leaves may dry out and die in severe infections",
            "Reduced photosynthetic capacity leading to yield loss"
        ),
        prevention = listOf(
            "Plant rust-resistant corn varieties",
            "Plant early to avoid peak infection periods",
            "Monitor fields weekly during humid weather",
            "Maintain field sanitation after harvest"
        ),
        treatments = listOf(
            FullReportTreatmentItem("Fungicide Application", listOf(
                "Apply triazole-based fungicides at first pustule appearance",
                "Mancozeb or chlorothalonil as cost-effective alternatives",
                "Treat before tasseling for best results"
            )),
            FullReportTreatmentItem("Cultural Practices", listOf(
                "Remove heavily infected lower leaves",
                "Reduce plant density in future plantings",
                "Improve field drainage to reduce humidity"
            )),
            FullReportTreatmentItem("Organic Options", listOf(
                "Sulfur-based fungicide sprays",
                "Potassium bicarbonate solutions applied weekly"
            ))
        )
    )
    "Gray Leaf Spot" -> FullReportDiseaseData(
        causes = listOf(
            "Fungal infection caused by Cercospora zeae-maydis",
            "Warm and humid weather (75–85°F)",
            "Poor air circulation in dense planting",
            "Infected residue remaining in soil"
        ),
        symptoms = listOf(
            "Small rectangular gray/tan spots on leaves",
            "Lesions run parallel to leaf veins",
            "Yellowing (chlorosis) around spots",
            "Premature leaf death in severe cases",
            "Reduced photosynthesis capacity"
        ),
        prevention = listOf(
            "Plant resistant corn varieties",
            "Maintain proper plant spacing for air flow",
            "Rotate crops on a 2–3 year cycle",
            "Remove and destroy infected debris after harvest",
            "Avoid overhead irrigation",
            "Apply preventive fungicides early in season",
            "Monitor weather conditions and field humidity"
        ),
        treatments = listOf(
            FullReportTreatmentItem("Fungicide Application", listOf(
                "Apply azoxystrobin-based products at first sign",
                "Repeat applications every 14 days",
                "Propiconazole as an alternative option"
            )),
            FullReportTreatmentItem("Cultural Practices", listOf(
                "Remove severely infected leaves promptly",
                "Improve air circulation between rows",
                "Reduce leaf wetness duration through drip irrigation"
            )),
            FullReportTreatmentItem("Organic Options", listOf(
                "Copper-based fungicides applied preventively",
                "Biofungicides containing Bacillus subtilis"
            ))
        )
    )
    else -> FullReportDiseaseData(
        causes = listOf("Cause information not available for this diagnosis"),
        symptoms = listOf("Symptom information not available for this diagnosis"),
        prevention = listOf(
            "Consult a local agricultural extension officer",
            "Maintain regular crop monitoring every 1–2 weeks",
            "Follow standard corn farming best practices"
        ),
        treatments = listOf(
            FullReportTreatmentItem("General Recommendations", listOf(
                "Consult a licensed agronomist for specific treatment",
                "Document affected areas and symptoms for follow-up",
                "Isolate affected plants to prevent further spread"
            ))
        )
    )
}
