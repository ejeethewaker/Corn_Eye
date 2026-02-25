package com.corneye.app.navigation

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Login : Screen("login")
    object Register : Screen("register")
    object AccountCreated : Screen("account_created")
    object Home : Screen("home")
    object Scan : Screen("scan")
    object Result : Screen("result/{diseaseName}/{confidence}") {
        fun createRoute(diseaseName: String, confidence: Float): String =
            "result/$diseaseName/$confidence"
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
}
