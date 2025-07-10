package com.project.sharedcustodycalendar

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import androidx.appcompat.app.AppCompatActivity

class ConfigNewCalendarActivity :  AppCompatActivity() {

    private lateinit var parent1Name: EditText
    private lateinit var parent1Color: Spinner
    private lateinit var parent2Name: EditText
    private lateinit var parent2Color: Spinner
    private lateinit var continueButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_config_new_calendar)

        parent1Name = findViewById(R.id.parent1Name)
        parent1Color = findViewById(R.id.parent1Color)
        parent2Name = findViewById(R.id.parent2Name)
        parent2Color = findViewById(R.id.parent2Color)
        continueButton = findViewById(R.id.continue_button)


        val colors = listOf("Red", "Blue", "Green", "Purple")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, colors)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        parent1Color.adapter = adapter
        parent2Color.adapter = adapter

    }
}