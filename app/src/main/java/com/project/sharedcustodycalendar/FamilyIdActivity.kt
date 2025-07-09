package com.project.sharedcustodycalendar

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class FamilyIdActivity :  AppCompatActivity() {

    private lateinit var familyIdField: EditText
    private lateinit var continueButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_family_id)

        familyIdField = findViewById(R.id.family_id_input)
        continueButton = findViewById(R.id.family_id_button)

        continueButton.setOnClickListener {
            val familyId = familyIdField.text.toString().trim()

            if (familyId.isNotEmpty()) {
                // TODO: Check against backend (Firebase or Flask)

                // For now: simulate result
                if (familyId.lowercase() == "testfamily") {
                    Toast.makeText(this, "Family ID found. Loading calendar...", Toast.LENGTH_SHORT).show()
                    // TODO: Navigate to CalendarActivity
                } else {
                    Toast.makeText(this, "Family ID not found. Creating new calendar...", Toast.LENGTH_SHORT).show()
                    // TODO: Navigate to CreateNewCalendarActivity
                }
            } else {
                Toast.makeText(this, "Please enter a Family ID.", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
