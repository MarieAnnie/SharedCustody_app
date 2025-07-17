package com.project.sharedcustodycalendar

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class ResetPasswordActivity : AppCompatActivity() {

    private lateinit var emailField: EditText
    private lateinit var resetButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_resetpassword)

        emailField = findViewById(R.id.email_field)
        resetButton = findViewById(R.id.reset_button)

        resetButton.setOnClickListener {
            val userEmail = emailField.text.toString().trim()

            FirebaseAuth.getInstance().sendPasswordResetEmail(userEmail)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Toast.makeText(this, "Reset email sent", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this, "Failed to send reset email", Toast.LENGTH_SHORT)
                            .show()
                    }
                }
        }
    }
}