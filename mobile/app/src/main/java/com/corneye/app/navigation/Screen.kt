// Screen Routes
// Sealed class defining all navigation route constants used in the nav graph.
package com.corneye.app.navigation

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Login : Screen("login")
    object Register : Screen("register")
    object AccountCreated : Screen("account_created")
    object Home : Screen("home")
    object Scan : Screen("scan")
    object Analyzing : Screen("analyzing/{diseaseName}/{confidence}/{imageUri}") {
        fun createRoute(diseaseName: String, confidence: Float, imageUri: String): String =
            "analyzing/${java.net.URLEncoder.encode(diseaseName, "UTF-8")}/$confidence/${java.net.URLEncoder.encode(imageUri, "UTF-8")}"
    }
    object Result : Screen("result/{diseaseName}/{confidence}/{imageUri}") {
        fun createRoute(diseaseName: String, confidence: Float, imageUri: String): String =
            "result/${java.net.URLEncoder.encode(diseaseName, "UTF-8")}/$confidence/${java.net.URLEncoder.encode(imageUri, "UTF-8")}"
    }
    object InvalidScan : Screen("invalid_scan/{reason}") {
        fun createRoute(reason: String = "not_corn") = "invalid_scan/$reason"
    }
    object FullReport : Screen("full_report/{scanId}/{diseaseName}/{confidence}/{date}/{time}") {
        fun createRoute(scanId: String, diseaseName: String, confidence: Float, date: String, time: String): String =
            "full_report/${java.net.URLEncoder.encode(scanId, "UTF-8")}/${java.net.URLEncoder.encode(diseaseName, "UTF-8")}/$confidence/${java.net.URLEncoder.encode(date, "UTF-8")}/${java.net.URLEncoder.encode(time, "UTF-8")}"
    }
    object History : Screen("history")
    object Profile : Screen("profile")
    object EditProfile : Screen("edit_profile")
    object Notifications : Screen("notifications")
    object Settings : Screen("settings")
    object DiseaseList : Screen("disease_list")
    object DiseaseDetail : Screen("disease_detail/{diseaseName}") {
        fun createRoute(diseaseName: String): String =
            "disease_detail/${java.net.URLEncoder.encode(diseaseName, "UTF-8")}"
    }
    object ChangePassword : Screen("change_password")
    object ManageSubscription : Screen("manage_subscription")
    object FAQ : Screen("faq")
    object PrivacyPolicy : Screen("privacy_policy")
    object Subscription : Screen("subscription")
    object Payment : Screen("payment/{planName}/{planPrice}") {
        fun createRoute(planName: String, planPrice: Int): String =
            "payment/${java.net.URLEncoder.encode(planName, "UTF-8")}/$planPrice"
    }
    object SubscriptionSuccess : Screen("subscription_success/{planName}/{planPrice}/{paymentMethod}") {
        fun createRoute(planName: String, planPrice: Int, paymentMethod: String): String =
            "subscription_success/${java.net.URLEncoder.encode(planName, "UTF-8")}/$planPrice/${java.net.URLEncoder.encode(paymentMethod, "UTF-8")}"
    }
    object ForgotPassword : Screen("forgot_password")
    object Otp : Screen("otp/{email}") {
        fun createRoute(email: String): String =
            "otp/${java.net.URLEncoder.encode(email, "UTF-8")}"
    }
    object SetNewPassword : Screen("set_new_password/{email}") {
        fun createRoute(email: String): String =
            "set_new_password/${java.net.URLEncoder.encode(email, "UTF-8")}"
    }
    object PasswordReset : Screen("password_reset")
    object PasswordSuccess : Screen("password_success")
}
