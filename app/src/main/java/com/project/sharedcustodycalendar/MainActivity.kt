package com.project.sharedcustodycalendar

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import android.widget.Toast
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.FirebaseApp
import com.project.sharedcustodycalendar.model.User
import com.project.sharedcustodycalendar.utils.CalendarStorageUtils

class MainActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        FirebaseApp.initializeApp(this)

        auth = FirebaseAuth.getInstance()
        val currentUser = auth.currentUser

        if (currentUser != null) {
            User.setUserID()
            Log.d("MainActivity", "User is already signed in: ${currentUser.email}")
            // Go directly to Dashboard
            if (isInternetAvailable(this)) {
                CalendarStorageUtils.loadFromFirebaseAndCacheLocally(this) {
                    startActivity(Intent(this, DashboardActivity::class.java))
                    finish()
                }
            } else {
                CalendarStorageUtils.loadLocally(this)
                startActivity(Intent(this, DashboardActivity::class.java))
                finish()
            }

        } else {
            Log.d("MainActivity", "No user signed in, redirecting to LoginActivity")
            Toast.makeText(this, "Please log in", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    fun isInternetAvailable(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork ?: return false
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
            return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
        } else {
            @Suppress("DEPRECATION")
            val networkInfo = connectivityManager.activeNetworkInfo ?: return false
            @Suppress("DEPRECATION")
            return networkInfo.isConnected
        }
    }
}
