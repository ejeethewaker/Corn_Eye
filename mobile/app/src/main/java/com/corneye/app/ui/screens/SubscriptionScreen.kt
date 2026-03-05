// Subscription Plans Screen
// Displays available subscription tiers for farmers to choose from.
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
import com.corneye.app.data.FirebaseHelper
import com.corneye.app.navigation.Screen
import com.corneye.app.ui.theme.*

data class SubscriptionPlan(
    val name: String,
    val emoji: String,
    val price: Int,
    val features: List<String>,
    val badge: String? = null,
    val badgeColor: Color = GreenPrimary,
    val accentColor: Color = DividerColor
)

@Composable
fun SubscriptionScreen(navController: NavController) {
    var selectedTab by remember { mutableIntStateOf(4) }
    var selectedPlanIndex by remember { mutableIntStateOf(1) }
    var plans by remember { mutableStateOf<List<SubscriptionPlan>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    // Load subscription plans from Firebase
    LaunchedEffect(Unit) {
        FirebaseHelper.subscriptionPlansRef().get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.exists()) {
                    val loadedPlans = mutableListOf<SubscriptionPlan>()
                    for (child in snapshot.children) {
                        val name = child.child("plan_name").getValue(String::class.java) ?: ""
                        val emoji = child.child("emoji").getValue(String::class.java) ?: ""
                        val price = child.child("plan_price").getValue(Long::class.java)?.toInt() ?: 0
                        val featuresSnapshot = child.child("features")
                        val features = mutableListOf<String>()
                        for (f in featuresSnapshot.children) {
                            f.getValue(String::class.java)?.let { features.add(it) }
                        }
                        val badge = child.child("badge").getValue(String::class.java)
                        val badgeColorStr = child.child("badgeColor").getValue(String::class.java) ?: ""
                        val badgeColor = when (badgeColorStr) {
                            "green" -> GreenPrimary
                            "red" -> StatusError
                            else -> GreenPrimary
                        }
                        loadedPlans.add(SubscriptionPlan(name, emoji, price, features, badge, badgeColor))
                    }
                    if (loadedPlans.isNotEmpty()) {
                        plans = loadedPlans
                        // Select the second plan by default if available
                        selectedPlanIndex = if (loadedPlans.size > 1) 1 else 0
                    }
                }
                // If no plans from Firebase, fall back to defaults
                if (plans.isEmpty()) {
                    plans = listOf(
                        SubscriptionPlan("Free Plan", "\uD83D\uDCE6", 0, listOf("10 scans per month", "Basic detection only", "Community support")),
                        SubscriptionPlan("Basic Plan", "⭐", 99, listOf("100 scans per month", "Standard AI detection", "Email support"), "SELECTED", GreenPrimary),
                        SubscriptionPlan("Premium Plan", "\uD83D\uDC51", 199, listOf("Unlimited scans", "Advanced AI + Expert review", "Priority support", "Full features"), "BEST", StatusError)
                    )
                }
                isLoading = false
            }
            .addOnFailureListener {
                // Fall back to defaults on error
                plans = listOf(
                    SubscriptionPlan("Free Plan", "\uD83D\uDCE6", 0, listOf("10 scans per month", "Basic detection only", "Community support")),
                    SubscriptionPlan("Basic Plan", "⭐", 99, listOf("100 scans per month", "Standard AI detection", "Email support"), "SELECTED", GreenPrimary),
                    SubscriptionPlan("Premium Plan", "\uD83D\uDC51", 199, listOf("Unlimited scans", "Advanced AI + Expert review", "Priority support", "Full features"), "BEST", StatusError)
                )
                isLoading = false
            }
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
                        1 -> navController.navigate(Screen.Profile.route)
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
                        text = "Choose Your Plan",
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
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                Text(
                    text = "Select a subscription plan",
                    fontSize = 16.sp,
                    color = TextSecondary,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Plan cards
                plans.forEachIndexed { index, plan ->
                    PlanCard(
                        plan = plan,
                        isSelected = selectedPlanIndex == index,
                        onClick = { selectedPlanIndex = index }
                    )
                    if (index < plans.size - 1) {
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Subscribe button
                if (plans.isNotEmpty()) {
                    Button(
                        onClick = {
                            navController.navigate(
                                Screen.Payment.createRoute(
                                    planName = plans[selectedPlanIndex].name,
                                    planPrice = plans[selectedPlanIndex].price
                                )
                            )
                        },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BrownButton)
                ) {
                    Text(
                        text = "Subscribe to ${plans[selectedPlanIndex].name}",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = White
                    )
                }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Cancel anytime • Secure payment",
                    fontSize = 13.sp,
                    color = TextHint,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
fun PlanCard(
    plan: SubscriptionPlan,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val borderColor = if (isSelected) Color(0xFF1976D2) else plan.accentColor
    val backgroundColor = if (isSelected) Color(0xFFE3F2FD) else White

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = isSelected,
                        onClick = onClick,
                        colors = RadioButtonDefaults.colors(
                            selectedColor = Color(0xFF1976D2),
                            unselectedColor = TextHint
                        )
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${plan.emoji} ${plan.name}",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (plan.badge == "BEST") StatusWarning else TextPrimary
                    )
                }

                if (plan.badge != null) {
                    Box(
                        modifier = Modifier
                            .background(
                                color = plan.badgeColor,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = plan.badge,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = White
                        )
                    }
                }
            }

            Text(
                text = "₱${plan.price}",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                modifier = Modifier.padding(start = 48.dp, top = 4.dp, bottom = 8.dp)
            )

            plan.features.forEach { feature ->
                Text(
                    text = "• $feature",
                    fontSize = 14.sp,
                    color = TextSecondary,
                    modifier = Modifier.padding(start = 48.dp, bottom = 3.dp)
                )
            }
        }
    }
}
