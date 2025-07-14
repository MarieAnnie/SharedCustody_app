package com.project.sharedcustodycalendar

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.project.sharedcustodycalendar.objects.FamilyDataHolder

class CalendarActivity :  AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_calendar)

        val titleTextView = findViewById<TextView>(R.id.titleTextView)
        val activeChildName = FamilyDataHolder.familyData.activeChild?.childName ?: "Unknown"
        val activeChildToken = FamilyDataHolder.familyData.activeChild?.childID ?: "Unknown"
        titleTextView.text = "New Calendar for $activeChildName ($activeChildToken)"
    }

}