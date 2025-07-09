package com.project.sharedcustodycalendar

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class MainActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        auth = FirebaseAuth.getInstance()

        val intentData: Uri? = intent?.data
        Log.d("MainActivity", "Intent data: $intentData")

        if (intentData != null && auth.isSignInWithEmailLink(intentData.toString())) {
            val email = getSharedPreferences("app", MODE_PRIVATE).getString("email", null)

            if (email == null) {
                Log.e("MainActivity", "No email found in SharedPreferences")
                Toast.makeText(this, "Missing email. Please login again.", Toast.LENGTH_LONG).show()
                startActivity(Intent(this, LoginActivity::class.java))
                finish()
                return
            }

            Log.d("MainActivity", "Attempting sign-in with email: $email")

            auth.signInWithEmailLink(email, intentData.toString())
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Log.d("MainActivity", "Successfully signed in with email link")
                        startActivity(Intent(this, FamilyIdActivity::class.java))
                        finish()
                    } else {
                        Log.e("MainActivity", "Sign-in failed", task.exception)
                        Toast.makeText(this, "Sign-in failed: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                        startActivity(Intent(this, LoginActivity::class.java))
                        finish()
                    }
                }

        } else {
            val currentUser = auth.currentUser
            if (currentUser != null) {
                Log.d("MainActivity", "User already signed in: ${currentUser.email}")
                startActivity(Intent(this, FamilyIdActivity::class.java))
            } else {
                Log.d("MainActivity", "No user signed in, going to login")
                startActivity(Intent(this, LoginActivity::class.java))
            }
            finish()
        }
    }
}
