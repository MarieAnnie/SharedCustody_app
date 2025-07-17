package com.project.sharedcustodycalendar

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import android.widget.Toast

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.FirebaseApp
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
            Log.d("MainActivity", "User is already signed in: ${currentUser.email}")
            // Go directly to Dashboard or FamilyId page
            CalendarStorageUtils.loadLocally(this)
            startActivity(Intent(this, DashboardActivity::class.java))
            finish()
        } else {
            Log.d("MainActivity", "No user signed in, redirecting to LoginActivity")
            Toast.makeText(this, "Please log in", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }
}
