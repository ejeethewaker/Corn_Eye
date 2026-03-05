// Invalid Scan Screen
// Shown when the captured image is not a recognisable corn leaf.
package com.corneye.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.ImageNotSupported
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.corneye.app.navigation.Screen
import com.corneye.app.ui.theme.*

@Composable
fun InvalidScanScreen(navController: NavController, reason: String = "not_corn") {
    val isUnclear = reason == "unclear"
    val iconTint = if (isUnclear) StatusWarning else StatusError
    val title = if (isUnclear) "Unclear Result" else "Invalid Image"
    val description = if (isUnclear)
        "The scan result was inconclusive. The image may show a healthy leaf, or the photo quality wasn't clear enough for a confident diagnosis."
    else
        "The image you provided does not appear to be a corn leaf. Please take a clear, close-up photo of an actual corn leaf for accurate diagnosis."
    val tips = if (isUnclear) listOf(
        "Make sure the leaf fills most of the frame",
        "Ensure good lighting — avoid shadows on the leaf",
        "Take a close-up of a single leaf surface",
        "If the leaf looks healthy, it likely is — try scanning again"
    ) else listOf(
        "Use a clear, well-lit photo",
        "Make sure the leaf fills most of the frame",
        "Avoid blurry or dark images",
        "Scan only corn (maize) leaves"
    )
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
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
            Text(
                text = "Scan Result",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                modifier = Modifier.align(Alignment.Center)
            )
        }

        // Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(iconTint.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (isUnclear) Icons.Default.HelpOutline else Icons.Default.ImageNotSupported,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(52.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = title,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = description,
                fontSize = 15.sp,
                color = TextSecondary,
                textAlign = TextAlign.Center,
                lineHeight = 22.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = GoldenBackground)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = if (isUnclear) "Tips for a clearer result:" else "Tips for a good scan:",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    tips.forEach { tip ->
                        Row(modifier = Modifier.padding(vertical = 2.dp)) {
                            Text("• ", fontSize = 13.sp, color = TextPrimary, fontWeight = FontWeight.Bold)
                            Text(tip, fontSize = 13.sp, color = TextPrimary, lineHeight = 18.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

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
                Text("Try Again", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
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
    }
}
