package com.project.sharedcustodycalendar

import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.project.sharedcustodycalendar.objects.FamilyDataHolder

import com.project.sharedcustodycalendar.objects.Parent

class SetParentsActivity :  AppCompatActivity() {

    private lateinit var parent1NameField: EditText
    private lateinit var parent1ColorSpinner: Spinner
    private lateinit var parent2NameField: EditText
    private lateinit var parent2ColorSpinner: Spinner
    private lateinit var continueButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_set_parents)

        val titleTextView = findViewById<TextView>(R.id.titleTextView)
        val activeChildName = FamilyDataHolder.familyData.activeChild?.childName ?: "Unknown"
        titleTextView.text = "New Calendar for $activeChildName"

        parent1NameField = findViewById(R.id.parent1Name)
        parent1ColorSpinner = findViewById(R.id.parent1Color)
        parent2NameField = findViewById(R.id.parent2Name)
        parent2ColorSpinner = findViewById(R.id.parent2Color)
        continueButton = findViewById(R.id.continue_button)

        val colors = listOf("Red", "Blue", "Green", "Purple")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, colors)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        parent1ColorSpinner.adapter = adapter
        parent2ColorSpinner.adapter = adapter

        continueButton.setOnClickListener {
            val parentName1 = parent1NameField.text.toString().trim()
            val parentColor1 = parent1ColorSpinner.selectedItem.toString()
            val parentName2 = parent2NameField.text.toString().trim()
            val parentColor2 = parent2ColorSpinner.selectedItem.toString()

            // Validate input
            if (parentName1.isEmpty() || parentName2.isEmpty()) {
                Toast.makeText(this, "Please enter both parent names", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (parentColor1 == parentColor2) {
                Toast.makeText(this, "Please choose different colors for each parent", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (parentName1.equals(parentName2, ignoreCase = true)) {
                Toast.makeText(this, "Please use different names for each parent", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val parent1 = Parent(parentName1, parentColor1)
            val parent2 = Parent(parentName2, parentColor2)

            val parents = listOf(parent1, parent2)
            FamilyDataHolder.familyData.setParentsForActiveChild(parents)

            startActivity(Intent(this, PatternInputActivity::class.java))
        }
    }

}