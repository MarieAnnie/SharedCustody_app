package com.project.sharedcustodycalendar

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import java.text.SimpleDateFormat
import java.util.*

class DashboardActivity : AppCompatActivity() {

    private val children = listOf("Alice", "Ben", "Chloe") // sample children
    private var currentChildIndex = 0

    private lateinit var calendarView: CalendarView
    private lateinit var todayTextView: TextView
    private lateinit var childrenList: LinearLayout
    private lateinit var addChildButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        // Bind views
        todayTextView = findViewById(R.id.todayTextView)
        calendarView = findViewById(R.id.calendarView)
        childrenList = findViewById(R.id.childrenList)
        addChildButton = findViewById(R.id.addChildButton)

        // Set today's date
        val today = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault())
        todayTextView.text = "Today is ${dateFormat.format(today.time)}"
        calendarView.date = today.timeInMillis

        // Handle add button
        addChildButton.setOnClickListener {
            startActivity(Intent(this, ChildIdActivity::class.java))
        }

        // Show the first child
        showCurrentChild()
    }

    private fun showCurrentChild() {
        childrenList.removeAllViews()

        val name = children[currentChildIndex]
        val childView = TextView(this).apply {
            text = name
            textSize = 18f
            setPadding(24, 32, 24, 32)
            setBackgroundColor(Color.LTGRAY)
            setTextColor(Color.BLACK)
            textAlignment = TextView.TEXT_ALIGNMENT_CENTER
            setOnClickListener {
                cycleChild()
            }
        }

        childrenList.addView(childView)
    }

    private fun cycleChild() {
        currentChildIndex = (currentChildIndex + 1) % children.size
        showCurrentChild()
    }
}
