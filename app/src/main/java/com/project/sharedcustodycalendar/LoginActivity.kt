package com.project.sharedcustodycalendar

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.ActionCodeSettings
import com.google.firebase.auth.FirebaseAuth

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var emailField: EditText
    private lateinit var sendLinkButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        auth = FirebaseAuth.getInstance()

        emailField = findViewById(R.id.email_field)
        sendLinkButton = findViewById(R.id.send_link_button)

        sendLinkButton.setOnClickListener {
            val email = emailField.text.toString().trim()

            if (email.isNotEmpty()) {
                val actionCodeSettings = ActionCodeSettings.newBuilder()
                    .setUrl("https://calendar-2d3hf8.web.app") // <-- Must match Firebase config
                    .setHandleCodeInApp(true)
                    .setAndroidPackageName(
                        "com.project.sharedcustodycalendar", // your app's package name
                        true, // installIfNotAvailable
                        "21"  // minimum Android SDK version
                    )
                    .build()

                auth.sendSignInLinkToEmail(email, actionCodeSettings)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            Log.d("LoginActivity", "Sign-in link sent to $email")
                            // Store email locally
                            getSharedPreferences("app", MODE_PRIVATE)
                                .edit()
                                .putString("email", email)
                                .apply()
                            Toast.makeText(this, "Link sent. Check your email.", Toast.LENGTH_SHORT).show()
                        } else {
                            Log.e("LoginActivity", "Failed to send link", task.exception)
                            Toast.makeText(this, "Failed to send link.", Toast.LENGTH_SHORT).show()
                        }
                    }
            } else {
                Toast.makeText(this, "Please enter your email", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
