// Change Password Screen
// Form to update the current account password with validation.
package com.corneye.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavController
import com.corneye.app.data.FirebaseHelper
import com.corneye.app.data.UserPreferences
import com.corneye.app.navigation.Screen
import com.corneye.app.ui.theme.*

@Composable
fun ChangePasswordScreen(navController: NavController) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(4) }
    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var showCurrentPassword by remember { mutableStateOf(false) }
    var showNewPassword by remember { mutableStateOf(false) }
    var showConfirmPassword by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }
    val userId by UserPreferences.getUserId(context).collectAsState(initial = "")

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
                        text = "Change Password",
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
                    .padding(horizontal = 20.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Lock icon
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(GoldenBackground),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.Lock,
                        contentDescription = null,
                        tint = White,
                        modifier = Modifier.size(36.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Update Your Password",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Enter your current and new password",
                    fontSize = 14.sp,
                    color = TextSecondary
                )

                Spacer(modifier = Modifier.height(28.dp))

                // Current Password
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Current Password",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    OutlinedTextField(
                        value = currentPassword,
                        onValueChange = { currentPassword = it },
                        placeholder = { Text("Enter current password", color = TextPrimary) },
                        visualTransformation = if (showCurrentPassword) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { showCurrentPassword = !showCurrentPassword }) {
                                Icon(
                                    if (showCurrentPassword) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                    contentDescription = "Toggle password visibility",
                                    tint = TextSecondary
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.White,
                            unfocusedBorderColor = Color.White,
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White
                        ),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                // New Password
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "New Password",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    OutlinedTextField(
                        value = newPassword,
                        onValueChange = { newPassword = it },
                        placeholder = { Text("Enter new password", color = TextPrimary) },
                        visualTransformation = if (showNewPassword) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { showNewPassword = !showNewPassword }) {
                                Icon(
                                    if (showNewPassword) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                    contentDescription = "Toggle password visibility",
                                    tint = TextSecondary
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.White,
                            unfocusedBorderColor = Color.White,
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White
                        ),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Confirm New Password
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Confirm New Password",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it },
                        placeholder = { Text("Re-enter new password", color = TextPrimary) },
                        visualTransformation = if (showConfirmPassword) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { showConfirmPassword = !showConfirmPassword }) {
                                Icon(
                                    if (showConfirmPassword) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                    contentDescription = "Toggle password visibility",
                                    tint = TextSecondary
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.White,
                            unfocusedBorderColor = Color.White,
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White
                        ),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Password requirements
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Password must contain:",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    PasswordRequirement("At least 8 characters", newPassword.length >= 8)
                    PasswordRequirement("One uppercase letter", newPassword.any { it.isUpperCase() })
                    PasswordRequirement("One number or special character", newPassword.any { it.isDigit() || !it.isLetterOrDigit() })
                }

                Spacer(modifier = Modifier.height(28.dp))

                // Error message
                if (errorMessage != null) {
                    Text(
                        text = errorMessage!!,
                        color = StatusError,
                        fontSize = 13.sp,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                    )
                }

                // Success message
                if (successMessage != null) {
                    Text(
                        text = successMessage!!,
                        color = GreenPrimary,
                        fontSize = 13.sp,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                    )
                }

                // Update Password button
                Button(
                    onClick = {
                        errorMessage = null
                        successMessage = null

                        when {
                            currentPassword.isBlank() || newPassword.isBlank() || confirmPassword.isBlank() -> {
                                errorMessage = "Please fill in all fields"
                            }
                            newPassword.length < 8 -> {
                                errorMessage = "New password must be at least 8 characters"
                            }
                            !newPassword.any { it.isUpperCase() } -> {
                                errorMessage = "New password must contain an uppercase letter"
                            }
                            !newPassword.any { it.isDigit() || !it.isLetterOrDigit() } -> {
                                errorMessage = "New password must contain a number or special character"
                            }
                            newPassword != confirmPassword -> {
                                errorMessage = "New passwords do not match"
                            }
                            userId.isEmpty() -> {
                                errorMessage = "User session not found. Please log in again."
                            }
                            else -> {
                                isLoading = true
                                // Verify current password from Firebase
                                FirebaseHelper.farmersRef().child(userId).get()
                                    .addOnSuccessListener { snapshot ->
                                        val storedPassword = snapshot.child("password").getValue(String::class.java) ?: ""
                                        if (storedPassword != currentPassword) {
                                            isLoading = false
                                            errorMessage = "Current password is incorrect"
                                        } else {
                                            // Update password in Firebase
                                            FirebaseHelper.farmersRef().child(userId).child("password").setValue(newPassword)
                                                .addOnSuccessListener {
                                                    isLoading = false
                                                    successMessage = "Password updated successfully!"
                                                    currentPassword = ""
                                                    newPassword = ""
                                                    confirmPassword = ""
                                                }
                                                .addOnFailureListener {
                                                    isLoading = false
                                                    errorMessage = "Failed to update password. Please try again."
                                                }
                                        }
                                    }
                                    .addOnFailureListener {
                                        isLoading = false
                                        errorMessage = "Connection error. Please try again."
                                    }
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BrownButton),
                    enabled = !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            color = White,
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Update Password", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = White)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Cancel button
                Button(
                    onClick = { navController.popBackStack() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = GoldenBackground)
                ) {
                    Text("Cancel", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = White)
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun PasswordRequirement(text: String, isMet: Boolean) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 2.dp, horizontal = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(if (isMet) GreenPrimary else TextHint)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            fontSize = 13.sp,
            color = if (isMet) GreenPrimary else TextSecondary
        )
    }
}
