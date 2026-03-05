// Manage Subscription Screen
// Shows the farmer's active subscription tier with renewal and cancellation options.
package com.corneye.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavController
import com.corneye.app.data.FirebaseHelper
import com.corneye.app.data.UserPreferences
import com.corneye.app.navigation.Screen
import com.corneye.app.ui.theme.*

@Composable
fun ManageSubscriptionScreen(navController: NavController) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(4) }
    val userId by UserPreferences.getUserId(context).collectAsState(initial = "")

    var planName by remember { mutableStateOf("Free Plan") }
    var planPrice by remember { mutableStateOf("₱0.00 / month") }
    var renewalDate by remember { mutableStateOf("N/A") }
    var paymentMethodName by remember { mutableStateOf("GCash") }
    var paymentMethodMasked by remember { mutableStateOf("") }
    var subscriptionStatus by remember { mutableStateOf("active") }
    var features by remember { mutableStateOf(listOf("10 scans per month", "Basic detection only", "Community support")) }
    var isLoading by remember { mutableStateOf(true) }
    var showPaymentModal by remember { mutableStateOf(false) }

    // Load subscription data from Firebase
    LaunchedEffect(userId) {
        if (userId.isNotEmpty()) {
            FirebaseHelper.farmersRef().child(userId).child("subscription").get()
                .addOnSuccessListener { snapshot ->
                    if (snapshot.exists()) {
                        planName = snapshot.child("active_plan").getValue(String::class.java) ?: "Free Plan"
                        val price = snapshot.child("plan_price").getValue(Long::class.java)?.toInt() ?: 0
                        planPrice = "₱${price}.00 / month"
                        renewalDate = snapshot.child("renewal_date_text").getValue(String::class.java) ?: "N/A"
                        paymentMethodName = snapshot.child("payment_method").getValue(String::class.java) ?: "None"
                        paymentMethodMasked = "••••${(1000..9999).random()}"
                        subscriptionStatus = snapshot.child("subscription_status").getValue(String::class.java) ?: "active"

                        // Set features based on plan
                        features = when {
                            planName.contains("Premium", ignoreCase = true) -> listOf("Unlimited scans", "Advanced AI + Expert review", "Priority support", "Full features", "Detailed reports")
                            planName.contains("Basic", ignoreCase = true) -> listOf("100 scans per month", "Standard AI detection", "Email support", "Scan history", "Detailed reports")
                            else -> listOf("10 scans per month", "Basic detection only", "Community support")
                        }
                    }
                    isLoading = false
                }
                .addOnFailureListener {
                    isLoading = false
                }
        } else {
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
                        text = "Manage Subscription",
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
                // Plan info card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = White),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        // ACTIVE badge
                        Box(
                            modifier = Modifier
                                .background(
                                    if (subscriptionStatus == "active") GreenPrimary else StatusError,
                                    RoundedCornerShape(8.dp)
                                )
                                .padding(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = subscriptionStatus.uppercase(),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = White
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = planName,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = planPrice,
                            fontSize = 16.sp,
                            color = TextSecondary,
                            modifier = Modifier.padding(top = 4.dp)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Next billing date:",
                            fontSize = 14.sp,
                            color = TextSecondary
                        )
                        Text(
                            text = renewalDate,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Current Features
                Text(
                    text = "Current Features",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = White),
                    elevation = CardDefaults.cardElevation(1.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        features.forEach { feature ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(vertical = 6.dp)
                            ) {
                                Icon(
                                    Icons.Filled.CheckCircle,
                                    contentDescription = null,
                                    tint = GreenPrimary,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = feature,
                                    fontSize = 15.sp,
                                    color = TextPrimary
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Payment Method
                Text(
                    text = "Payment Method",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = White),
                    elevation = CardDefaults.cardElevation(1.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF2196F3)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "G",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = White
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = paymentMethodName,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                            Text(
                                text = paymentMethodMasked,
                                fontSize = 13.sp,
                                color = TextHint
                            )
                        }
                        Text(
                            text = "Change",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2196F3),
                            modifier = Modifier.clickable { showPaymentModal = true }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                // Upgrade Plan button
                Button(
                    onClick = { navController.navigate(Screen.Subscription.route) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BrownButton)
                ) {
                    Text("Upgrade Plan", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = White)
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Cancel Subscription button
                Button(
                    onClick = {
                        if (userId.isNotEmpty()) {
                            FirebaseHelper.farmersRef().child(userId).child("subscription").child("subscription_status").setValue("cancelled")
                            subscriptionStatus = "cancelled"
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BrownButton)
                ) {
                    Text("Cancel Subscription", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = White)
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // Payment Method Selection Modal
    if (showPaymentModal) {
        AlertDialog(
            onDismissRequest = { showPaymentModal = false },
            title = {
                Text(
                    text = "Select Payment Method",
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            },
            text = {
                Column {
                    listOf("GCash", "Maya", "Credit / Debit Card", "Cash on Delivery").forEach { method ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    paymentMethodName = method
                                    if (userId.isNotEmpty()) {
                                        FirebaseHelper.farmersRef()
                                            .child(userId)
                                            .child("subscription")
                                            .child("payment_method")
                                            .setValue(method)
                                    }
                                    showPaymentModal = false
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = paymentMethodName == method,
                                onClick = null
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = method, fontSize = 16.sp, color = TextPrimary)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showPaymentModal = false }) {
                    Text("Cancel", color = Color(0xFF2196F3))
                }
            }
        )
    }
}
