// Navigation Graph
// Defines the Compose navigation graph and wires all screen routes together.
package com.corneye.app.navigation
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.corneye.app.ui.screens.*

@Composable
fun CornEyeNavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Screen.Splash.route
    ) {
        composable(Screen.Splash.route) {
            SplashScreen(navController = navController)
        }

        composable(Screen.Login.route) {
            LoginScreen(navController = navController)
        }

        composable(Screen.Register.route) {
            RegisterScreen(navController = navController)
        }

        composable(Screen.AccountCreated.route) {
            AccountCreatedScreen(navController = navController)
        }

        composable(Screen.Home.route) {
            HomeScreen(navController = navController)
        }

        composable(Screen.Scan.route) {
            ScanScreen(navController = navController)
        }

        composable(
            route = Screen.InvalidScan.route,
            arguments = listOf(navArgument("reason") { type = NavType.StringType; defaultValue = "not_corn" })
        ) { backStackEntry ->
            val reason = backStackEntry.arguments?.getString("reason") ?: "not_corn"
            InvalidScanScreen(navController = navController, reason = reason)
        }

        composable(
            route = Screen.Analyzing.route,
            arguments = listOf(
                navArgument("diseaseName") { type = NavType.StringType },
                navArgument("confidence") { type = NavType.FloatType },
                navArgument("imageUri") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val diseaseName = java.net.URLDecoder.decode(
                backStackEntry.arguments?.getString("diseaseName") ?: "", "UTF-8"
            )
            val confidence = backStackEntry.arguments?.getFloat("confidence") ?: 0f
            val imageUri = backStackEntry.arguments?.getString("imageUri") ?: ""
            AnalyzingScreen(
                navController = navController,
                diseaseName = diseaseName,
                confidence = confidence,
                imageUriEncoded = imageUri
            )
        }

        composable(
            route = Screen.Result.route,
            arguments = listOf(
                navArgument("diseaseName") { type = NavType.StringType },
                navArgument("confidence") { type = NavType.FloatType },
                navArgument("imageUri") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val diseaseName = java.net.URLDecoder.decode(
                backStackEntry.arguments?.getString("diseaseName") ?: "", "UTF-8"
            )
            val confidence = backStackEntry.arguments?.getFloat("confidence") ?: 0f
            val imageUri = backStackEntry.arguments?.getString("imageUri") ?: ""
            ResultScreen(
                navController = navController,
                diseaseName = diseaseName,
                confidence = confidence,
                imageUriEncoded = imageUri
            )
        }

        composable(
            route = Screen.FullReport.route,
            arguments = listOf(
                navArgument("scanId") { type = NavType.StringType },
                navArgument("diseaseName") { type = NavType.StringType },
                navArgument("confidence") { type = NavType.FloatType },
                navArgument("date") { type = NavType.StringType },
                navArgument("time") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val scanId = java.net.URLDecoder.decode(backStackEntry.arguments?.getString("scanId") ?: "", "UTF-8")
            val diseaseName = java.net.URLDecoder.decode(backStackEntry.arguments?.getString("diseaseName") ?: "", "UTF-8")
            val confidence = backStackEntry.arguments?.getFloat("confidence") ?: 0f
            val date = java.net.URLDecoder.decode(backStackEntry.arguments?.getString("date") ?: "", "UTF-8")
            val time = java.net.URLDecoder.decode(backStackEntry.arguments?.getString("time") ?: "", "UTF-8")
            FullReportScreen(
                navController = navController,
                scanId = scanId,
                diseaseName = diseaseName,
                confidence = confidence,
                date = date,
                time = time
            )
        }

        composable(Screen.History.route) {
            HistoryScreen(navController = navController)
        }

        composable(Screen.Profile.route) {
            ProfileScreen(navController = navController)
        }

        composable(Screen.EditProfile.route) {
            EditProfileScreen(navController = navController)
        }

        composable(Screen.Notifications.route) {
            NotificationsScreen(navController = navController)
        }

        composable(Screen.Settings.route) {
            SettingsScreen(navController = navController)
        }

        composable(Screen.DiseaseList.route) {
            DiseaseListScreen(navController = navController)
        }

        composable(
            route = Screen.DiseaseDetail.route,
            arguments = listOf(
                navArgument("diseaseName") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val diseaseName = java.net.URLDecoder.decode(
                backStackEntry.arguments?.getString("diseaseName") ?: "", "UTF-8"
            )
            DiseaseDetailScreen(
                navController = navController,
                diseaseName = diseaseName
            )
        }

        composable(Screen.ChangePassword.route) {
            ChangePasswordScreen(navController = navController)
        }

        composable(Screen.ManageSubscription.route) {
            ManageSubscriptionScreen(navController = navController)
        }

        composable(Screen.FAQ.route) {
            FAQScreen(navController = navController)
        }

        composable(Screen.PrivacyPolicy.route) {
            PrivacyPolicyScreen(navController = navController)
        }

        composable(Screen.Subscription.route) {
            SubscriptionScreen(navController = navController)
        }

        composable(
            route = Screen.Payment.route,
            arguments = listOf(
                navArgument("planName") { type = NavType.StringType },
                navArgument("planPrice") { type = NavType.IntType }
            )
        ) { backStackEntry ->
            val planName = java.net.URLDecoder.decode(
                backStackEntry.arguments?.getString("planName") ?: "", "UTF-8"
            )
            val planPrice = backStackEntry.arguments?.getInt("planPrice") ?: 0
            PaymentScreen(
                navController = navController,
                planName = planName,
                planPrice = planPrice
            )
        }

        composable(
            route = Screen.SubscriptionSuccess.route,
            arguments = listOf(
                navArgument("planName") { type = NavType.StringType },
                navArgument("planPrice") { type = NavType.IntType },
                navArgument("paymentMethod") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val planName = java.net.URLDecoder.decode(
                backStackEntry.arguments?.getString("planName") ?: "", "UTF-8"
            )
            val planPrice = backStackEntry.arguments?.getInt("planPrice") ?: 0
            val paymentMethod = java.net.URLDecoder.decode(
                backStackEntry.arguments?.getString("paymentMethod") ?: "", "UTF-8"
            )
            SubscriptionSuccessScreen(
                navController = navController,
                planName = planName,
                planPrice = planPrice,
                paymentMethod = paymentMethod
            )
        }

        composable(Screen.ForgotPassword.route) {
            ForgotPasswordScreen(navController = navController)
        }

        composable(
            route = Screen.Otp.route,
            arguments = listOf(
                navArgument("email") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val email = java.net.URLDecoder.decode(
                backStackEntry.arguments?.getString("email") ?: "", "UTF-8"
            )
            OtpScreen(navController = navController, email = email)
        }

        composable(
            route = Screen.SetNewPassword.route,
            arguments = listOf(
                navArgument("email") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val email = java.net.URLDecoder.decode(
                backStackEntry.arguments?.getString("email") ?: "", "UTF-8"
            )
            SetNewPasswordScreen(navController = navController, email = email)
        }

        composable(Screen.PasswordReset.route) {
            PasswordResetScreen(navController = navController)
        }

        composable(Screen.PasswordSuccess.route) {
            PasswordSuccessScreen(navController = navController)
        }
    }
}
