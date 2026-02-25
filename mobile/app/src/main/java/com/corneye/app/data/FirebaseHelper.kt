package com.corneye.app.data

import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase

object FirebaseHelper {
    val database: FirebaseDatabase by lazy {
        Firebase.database("https://corneye-ec181-default-rtdb.asia-southeast1.firebasedatabase.app")
    }

    // Farmer
    fun farmersRef() = database.getReference("farmers")

    // LoginSession
    fun loginSessionsRef() = database.getReference("login_sessions")

    // Notification
    fun notificationsRef() = database.getReference("notifications")

    // CornImage
    fun cornImagesRef() = database.getReference("corn_images")

    // CornDisease
    fun cornDiseasesRef() = database.getReference("corn_diseases")

    // AnalysisResult
    fun analysisResultsRef() = database.getReference("analysis_results")

    // Payment
    fun paymentsRef() = database.getReference("payments")

    // SubscriptionPlan
    fun subscriptionPlansRef() = database.getReference("subscription_plans")

    // Legacy references (kept for compatibility)
    fun usersRef() = farmersRef()
    fun adminsRef() = database.getReference("admins")
    fun scansRef() = analysisResultsRef()
}
