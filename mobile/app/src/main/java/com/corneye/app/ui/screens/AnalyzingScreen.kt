// Analyzing Screen
// Loading screen displayed while the on-device TFLite model processes the image.
package com.corneye.app.ui.screens

import android.content.Context
import android.net.Uri
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.corneye.app.data.FirebaseHelper
import com.corneye.app.data.UserPreferences
import com.corneye.app.navigation.Screen
import com.corneye.app.ui.theme.*
import kotlinx.coroutines.flow.first
import java.net.URLDecoder

@Composable
fun AnalyzingScreen(
    navController: NavController,
    diseaseName: String,
    confidence: Float,
    imageUriEncoded: String
) {
    val context = LocalContext.current
    val imageUri = remember {
        try { URLDecoder.decode(imageUriEncoded, "UTF-8") } catch (e: Exception) { imageUriEncoded }
    }

    // Animate progress from 0 to 100 over ~2.5 seconds, then navigate
    var progress by remember { mutableFloatStateOf(0f) }
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 2200, easing = LinearEasing),
        label = "progress"
    )

    // Step states: 0=pending, 1=active, 2=done
    var step1 by remember { mutableIntStateOf(0) }
    var step2 by remember { mutableIntStateOf(0) }
    var step3 by remember { mutableIntStateOf(0) }
    var step4 by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        // Step 1 complete immediately
        step1 = 2
        progress = 0.25f
        kotlinx.coroutines.delay(200)

        // Step 2
        step2 = 1
        kotlinx.coroutines.delay(500)
        step2 = 2
        progress = 0.55f

        // Step 3
        step3 = 1
        kotlinx.coroutines.delay(700)
        step3 = 2
        progress = 0.80f

        // Step 4
        step4 = 1
        kotlinx.coroutines.delay(600)
        step4 = 2
        progress = 1.0f

        kotlinx.coroutines.delay(400)

        // Reject low-confidence results — ambiguous or not a corn leaf
        if (confidence < 0.60f) {
            val reason = if (diseaseName == "Analysis Unavailable") "not_corn" else "unclear"
            navController.navigate(Screen.InvalidScan.createRoute(reason)) {
                popUpTo(Screen.Scan.route)
            }
            return@LaunchedEffect
        }

        // Save to Firebase then navigate
        saveAnalysisToFirebase(context, diseaseName, confidence)

        navController.navigate(
            Screen.Result.createRoute(diseaseName, confidence, imageUri)
        ) {
            popUpTo(Screen.Scan.route)
        }
    }

    val progressPercent = (animatedProgress * 100).toInt()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Status bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(GoldenBackground)
                .background(StatusBarGold)
                .windowInsetsPadding(WindowInsets.statusBars)
        )

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Analyzing Leaf...",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Please wait",
            fontSize = 14.sp,
            color = TextSecondary
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Captured image
        AsyncImage(
            model = imageUri,
            contentDescription = "Scanned leaf",
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .height(260.dp)
                .clip(RoundedCornerShape(12.dp)),
            contentScale = ContentScale.Crop
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Circular progress
        Box(contentAlignment = Alignment.Center) {
            CircularProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier.size(80.dp),
                strokeWidth = 6.dp,
                color = GoldenBackground,
                trackColor = GoldenBackground.copy(alpha = 0.25f)
            )
            Text(
                text = "$progressPercent%",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "AI Analysis in Progress",
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = BrownButton
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Thin progress bar
        LinearProgressIndicator(
            progress = { animatedProgress },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp)),
            color = BrownButton,
            trackColor = BrownButton.copy(alpha = 0.2f)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Steps card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = GoldenBackground),
            elevation = CardDefaults.cardElevation(0.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AnalysisStep(state = step1, text = "Image quality verified")
                AnalysisStep(state = step2, text = "CNN model processing")
                AnalysisStep(state = step3, text = "Identifying disease patterns")
                AnalysisStep(state = step4, text = "Generating recommendations")
            }
        }
    }
}

@Composable
private fun AnalysisStep(state: Int, text: String) {
    val dotColor = when (state) {
        2 -> Color(0xFF4CAF50)   // done — green
        1 -> GoldenBackground    // active — gold
        else -> Color(0xFFBBBBBB) // pending — grey
    }
    val textColor = if (state == 0) TextSecondary else TextPrimary
    val prefix = when (state) {
        2 -> "✓ "
        1 -> "↺ "
        else -> "○ "
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(dotColor)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = "$prefix$text",
            fontSize = 14.sp,
            color = textColor
        )
    }
}

private suspend fun saveAnalysisToFirebase(context: Context, label: String, confidence: Float) {
    val userId = UserPreferences.getUserId(context).first()

    if (userId.isEmpty()) return

    val resultId = FirebaseHelper.analysisResultsRef().push().key ?: return
    val isHealthy = label == "Healthy"
    val resultData = mapOf(
        "analysis_id"     to resultId,
        "image_id"        to "",
        "disease_id"      to "",
        "farmer_id"       to userId,
        "analysis_label"  to label,
        "confidence_score" to confidence,
        "time_scanned"    to System.currentTimeMillis()
    )
    FirebaseHelper.analysisResultsRef().child(resultId).setValue(resultData)
        .addOnFailureListener { e ->
            android.util.Log.e("AnalyzingScreen", "Failed to save scan: ${e.message}")
        }

    val notifId = FirebaseHelper.notificationsRef().push().key ?: return
    val notifMessage = if (isHealthy)
        "Your corn leaf scan shows a healthy plant with ${(confidence * 100).toInt()}% confidence."
    else
        "Disease detected: $label with ${(confidence * 100).toInt()}% confidence. Check recommendations."
    val timeNow = System.currentTimeMillis()
    val notifData = mapOf(
        "notif_id"          to notifId,
        "farmer_id"         to userId,
        "notif_title"       to if (isHealthy) "Healthy Scan Result" else "Disease Detected",
        "notif_message"     to notifMessage,
        "notif_type"        to if (isHealthy) "scan_healthy" else "scan_disease",
        "analysis_id"       to resultId,
        "analysis_label"    to label,
        "confidence_score"  to confidence,
        "time_scanned"      to timeNow,
        "is_read"           to false
    )
    FirebaseHelper.notificationsRef().child(notifId).setValue(notifData)
}
