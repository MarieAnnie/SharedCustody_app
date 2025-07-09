package com.project.sharedcustodycalendar.utils

import android.util.Log
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.google.firebase.auth.ActionCodeSettings

object AuthUtils {
    private const val TAG = "AuthUtils"

    fun sendSignInLink(email: String) {
        val actionCodeSettings = ActionCodeSettings.newBuilder()
            .setUrl("https://www.example.com/finishSignUp?cartId=1234")
            .setHandleCodeInApp(true)
            .setIOSBundleId("com.example.ios")
            .setAndroidPackageName("com.example.android", true, "12")
            .build()

        Firebase.auth.sendSignInLinkToEmail(email, actionCodeSettings)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d(TAG, "Email link sent.")
                } else {
                    Log.w(TAG, "Email link failed", task.exception)
                }
            }
    }

}