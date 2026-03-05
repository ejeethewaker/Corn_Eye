// OTP Screen
// One-time password entry step for verifying the farmer's identity.
package com.corneye.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.corneye.app.data.FirebaseHelper
import com.corneye.app.navigation.Screen
import com.corneye.app.ui.theme.*

@Composable
fun OtpScreen(navController: NavController, email: String) {
    val otpLength = 5
    var otpValues by remember { mutableStateOf(List(otpLength) { "" }) }
    val focusRequesters = remember { List(otpLength) { FocusRequester() } }
    var isVerifying by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(GoldenBackground)
    ) {
        // Status bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(GoldenBackground)
                .background(StatusBarGold)
                .windowInsetsPadding(WindowInsets.statusBars)
                .align(Alignment.TopStart)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.height(64.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = White),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 32.dp)
                ) {
                    // Back button + title
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { navController.popBackStack() },
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(TextPrimary)
                        ) {
                            Icon(
                                Icons.Default.ArrowBack,
                                contentDescription = "Back",
                                tint = White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Enter OTP",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = "Please enter the OTP sent to your Email.",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextPrimary
                    )

                    Spacer(modifier = Modifier.height(28.dp))

                    // OTP boxes
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        for (i in 0 until otpLength) {
                            BasicTextField(
                                value = otpValues[i],
                                onValueChange = { newVal ->
                                    val digit = newVal.filter { it.isDigit() }.take(1)
                                    val newList = otpValues.toMutableList()
                                    newList[i] = digit
                                    otpValues = newList
                                    errorMessage = null
                                    if (digit.isNotEmpty() && i < otpLength - 1) {
                                        focusRequesters[i + 1].requestFocus()
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .border(
                                        width = 1.5.dp,
                                        color = if (otpValues[i].isNotEmpty()) BrownButton else DividerColor,
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    .background(White)
                                    .focusRequester(focusRequesters[i]),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                                textStyle = TextStyle(
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center,
                                    color = TextPrimary
                                ),
                                decorationBox = { innerTextField ->
                                    Box(contentAlignment = Alignment.Center) {
                                        innerTextField()
                                    }
                                }
                            )
                        }
                    }

                    if (errorMessage != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = errorMessage!!,
                            color = StatusError,
                            fontSize = 13.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(28.dp))

                    Button(
                        onClick = {
                            val otp = otpValues.joinToString("")
                            if (otp.length < otpLength) {
                                errorMessage = "Please enter all ${otpLength} digits"
                                return@Button
                            }
                            isVerifying = true
                            val safeKey = FirebaseHelper.emailToKey(email)
                            FirebaseHelper.passwordResetsRef()
                                .child(safeKey)
                                .child("otp")
                                .get()
                                .addOnSuccessListener { snapshot ->
                                    val storedOtp = snapshot.getValue(String::class.java) ?: ""
                                    if (otp == storedOtp) {
                                        isVerifying = false
                                        navController.navigate(Screen.SetNewPassword.createRoute(email))
                                    } else {
                                        isVerifying = false
                                        errorMessage = "Incorrect OTP. Please try again."
                                    }
                                }
                                .addOnFailureListener {
                                    isVerifying = false
                                    errorMessage = "Connection error. Please try again."
                                }
                        },
                        enabled = !isVerifying,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(26.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BrownButton),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                    ) {
                        if (isVerifying) {
                            CircularProgressIndicator(
                                color = White,
                                modifier = Modifier.size(22.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                text = "Verify Code",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = White
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Resend OTP
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Haven't got the otp yet? ",
                            fontSize = 13.sp,
                            color = TextSecondary
                        )
                        Text(
                            text = "Resend opt",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = BrownButton,
                            modifier = Modifier.clickable(
                                indication = null,
                                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                            ) {
                                val newOtp = (100000..999999).random().toString()
                                val safeKey = FirebaseHelper.emailToKey(email)
                                FirebaseHelper.passwordResetsRef()
                                    .child(safeKey)
                                    .child("otp")
                                    .setValue(newOtp)
                                errorMessage = null
                                otpValues = List(otpLength) { "" }
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
