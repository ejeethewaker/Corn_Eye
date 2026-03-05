// Register Screen
// New farmer registration form with account creation via Firebase Auth.
package com.corneye.app.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.corneye.app.R
import com.corneye.app.data.FirebaseHelper
import com.corneye.app.navigation.Screen
import com.corneye.app.ui.theme.*
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener

@Composable
fun RegisterScreen(navController: NavController) {
    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(GoldenBackground)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Status bar spacer - same as all other screens
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(GoldenBackground)
                    .background(StatusBarGold)
                    .windowInsetsPadding(WindowInsets.statusBars)
            )

            // Card centered in remaining space with scroll for tall content
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                // White card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp, vertical = 28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Logo
                            Image(
                                painter = painterResource(id = R.drawable.logo),
                                contentDescription = "CornEye Logo",
                                modifier = Modifier.size(140.dp)
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "Create Account",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )

                            Spacer(modifier = Modifier.height(20.dp))

                            // Full Name
                            Text(
                                text = "Full Name",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 6.dp)
                            )
                            TextField(
                                value = fullName,
                                onValueChange = { fullName = it; errorMessage = null },
                                placeholder = { Text("Enter your full name", color = TextHint, fontSize = 13.sp) },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 14.sp),
                                modifier = Modifier.fillMaxWidth().height(52.dp),
                                colors = TextFieldDefaults.colors(
                                    unfocusedContainerColor = GoldenBackground,
                                    focusedContainerColor = GoldenBackground,
                                    unfocusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                                    focusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                                    cursorColor = BrownButton
                                )
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            // Email Address
                            Text(
                                text = "Email Address",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 6.dp)
                            )
                            TextField(
                                value = email,
                                onValueChange = { email = it; errorMessage = null },
                                placeholder = { Text("Enter your email", color = TextHint, fontSize = 13.sp) },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 14.sp),
                                modifier = Modifier.fillMaxWidth().height(52.dp),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                                colors = TextFieldDefaults.colors(
                                    unfocusedContainerColor = GoldenBackground,
                                    focusedContainerColor = GoldenBackground,
                                    unfocusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                                    focusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                                    cursorColor = BrownButton
                                )
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            // Phone Number
                            Text(
                                text = "Phone Number",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 6.dp)
                            )
                            TextField(
                                value = phoneNumber,
                                onValueChange = { phoneNumber = it; errorMessage = null },
                                placeholder = { Text("Enter your phone number", color = TextHint, fontSize = 13.sp) },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 14.sp),
                                modifier = Modifier.fillMaxWidth().height(52.dp),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                colors = TextFieldDefaults.colors(
                                    unfocusedContainerColor = GoldenBackground,
                                    focusedContainerColor = GoldenBackground,
                                    unfocusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                                    focusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                                    cursorColor = BrownButton
                                )
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            // Password
                            Text(
                                text = "Password",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 6.dp)
                            )
                            TextField(
                                value = password,
                                onValueChange = { password = it; errorMessage = null },
                                placeholder = { Text("Enter your password", color = TextHint, fontSize = 13.sp) },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 14.sp),
                                modifier = Modifier.fillMaxWidth().height(52.dp),
                                trailingIcon = {
                                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                        Icon(
                                            if (passwordVisible) Icons.Default.Visibility
                                            else Icons.Default.VisibilityOff,
                                            contentDescription = "Toggle password",
                                            tint = TextSecondary
                                        )
                                    }
                                },
                                visualTransformation = if (passwordVisible) VisualTransformation.None
                                else PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                colors = TextFieldDefaults.colors(
                                    unfocusedContainerColor = GoldenBackground,
                                    focusedContainerColor = GoldenBackground,
                                    unfocusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                                    focusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                                    cursorColor = BrownButton
                                )
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            // Confirm Password
                            Text(
                                text = "Confirm Password",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 6.dp)
                            )
                            TextField(
                                value = confirmPassword,
                                onValueChange = { confirmPassword = it; errorMessage = null },
                                placeholder = { Text("Confirm your password", color = TextHint, fontSize = 13.sp) },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 14.sp),
                                modifier = Modifier.fillMaxWidth().height(52.dp),
                                trailingIcon = {
                                    IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                                        Icon(
                                            if (confirmPasswordVisible) Icons.Default.Visibility
                                            else Icons.Default.VisibilityOff,
                                            contentDescription = "Toggle password",
                                            tint = TextSecondary
                                        )
                                    }
                                },
                                visualTransformation = if (confirmPasswordVisible) VisualTransformation.None
                                else PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                colors = TextFieldDefaults.colors(
                                    unfocusedContainerColor = GoldenBackground,
                                    focusedContainerColor = GoldenBackground,
                                    unfocusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                                    focusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                                    cursorColor = BrownButton
                                )
                            )

                            // Error message
                            if (errorMessage != null) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = errorMessage!!,
                                    color = StatusError,
                                    fontSize = 13.sp,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            // Create Account button
                            Button(
                                onClick = {
                                    val trimmedName = fullName.trim()
                                    val trimmedEmail = email.trim().lowercase()
                                    val trimmedPhone = phoneNumber.trim()

                                    when {
                                        trimmedName.isBlank() || trimmedEmail.isBlank() || password.isBlank() || trimmedPhone.isBlank() -> {
                                            errorMessage = "Please fill in all fields"
                                        }
                                        !android.util.Patterns.EMAIL_ADDRESS.matcher(trimmedEmail).matches() ||
                                                !trimmedEmail.matches(Regex("^[a-zA-Z0-9._%+\\-]+@[a-zA-Z0-9.\\-]+\\.[a-zA-Z]{2,}$")) -> {
                                            errorMessage = "Email format: yourname@email.com"
                                        }
                                        !trimmedPhone.matches(Regex("^[0-9]{11}$")) -> {
                                            errorMessage = "Phone number must be exactly 11 digits"
                                        }
                                        password.length < 6 -> {
                                            errorMessage = "Password must be at least 6 characters"
                                        }
                                        password != confirmPassword -> {
                                            errorMessage = "Passwords do not match"
                                        }
                                        else -> {
                                            isLoading = true
                                            errorMessage = null

                                            // Check if email already exists
                                            FirebaseHelper.farmersRef().orderByChild("email_address").equalTo(trimmedEmail)
                                                .addListenerForSingleValueEvent(object : ValueEventListener {
                                                    override fun onDataChange(snapshot: DataSnapshot) {
                                                        if (snapshot.exists()) {
                                                            isLoading = false
                                                            errorMessage = "Email already registered"
                                                            return
                                                        }

                                                        val farmerId = FirebaseHelper.farmersRef().push().key ?: run {
                                                            isLoading = false
                                                            errorMessage = "Registration failed. Please try again."
                                                            return
                                                        }
                                                        val farmerData = mapOf(
                                                            "farmer_id" to farmerId,
                                                            "fullname" to trimmedName,
                                                            "email_address" to trimmedEmail,
                                                            "phone_num" to trimmedPhone,
                                                            "farm_location" to "",
                                                            "farm_area" to "",
                                                            "password" to password,
                                                            "createdAt" to System.currentTimeMillis(),
                                                            "status" to "active"
                                                        )

                                                        FirebaseHelper.farmersRef().child(farmerId).setValue(farmerData)
                                                            .addOnSuccessListener {
                                                                // Push admin notification for new farmer registration
                                                                val notifId = FirebaseHelper.notificationsRef().push().key
                                                                if (notifId != null) {
                                                                    val notifData = mapOf(
                                                                        "notif_id"    to notifId,
                                                                        "farmer_id"   to farmerId,
                                                                        "notif_title" to "New User Registered",
                                                                        "notif_message" to "$trimmedName joined the platform as a farmer",
                                                                        "notif_type"  to "new_farmer",
                                                                        "timestamp"   to System.currentTimeMillis(),
                                                                        "is_read"     to false
                                                                    )
                                                                    FirebaseHelper.notificationsRef().child(notifId).setValue(notifData)
                                                                }
                                                                isLoading = false
                                                                navController.navigate(Screen.AccountCreated.route) {
                                                                    popUpTo(Screen.Register.route) { inclusive = true }
                                                                }
                                                            }
                                                            .addOnFailureListener {
                                                                isLoading = false
                                                                errorMessage = "Registration failed. Please try again."
                                                            }
                                                    }

                                                    override fun onCancelled(error: DatabaseError) {
                                                        isLoading = false
                                                        errorMessage = "Connection error. Please try again."
                                                    }
                                                })
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp),
                                shape = RoundedCornerShape(26.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = BrownButton),
                                enabled = !isLoading,
                                elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                            ) {
                                if (isLoading) {
                                    CircularProgressIndicator(
                                        color = White,
                                        modifier = Modifier.size(24.dp),
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Text(
                                        text = "Create account",
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = White
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(18.dp))

                            // Login link
                            Row(
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "Already have an account? ",
                                    fontSize = 13.sp,
                                    color = TextSecondary
                                )
                                Text(
                                    text = "Login here",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = BrownButton,
                                    modifier = Modifier.clickable {
                                        navController.navigate(Screen.Login.route)
                                    }
                                )
                            }
                        }

                        // Back arrow overlaid on top-left of card
                        IconButton(
                            onClick = { navController.popBackStack() },
                            modifier = Modifier
                                .padding(12.dp)
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(TextPrimary)
                                .align(Alignment.TopStart)
                        ) {
                            Icon(
                                Icons.Default.ArrowBack,
                                contentDescription = "Back",
                                tint = White
                            )
                        }
                    }
                }
            }
        }
    }
}
