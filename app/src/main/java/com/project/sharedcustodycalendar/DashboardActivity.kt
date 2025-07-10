package com.project.sharedcustodycalendar

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import java.text.SimpleDateFormat
import java.util.*

class DashboardActivity : AppCompatActivity() {

    private val children = listOf("Alice", "Ben", "Chloe")
    private var selectedChild: String? = null
    private val childButtons = mutableListOf<Button>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        val todayTextView = findViewById<TextView>(R.id.todayTextView)
        val calendarView = findViewById<CalendarView>(R.id.calendarView)
        val childrenListLayout = findViewById<LinearLayout>(R.id.childrenList)
        val addChildButton = findViewById<Button>(R.id.addChildButton)

        // Show today's date
        val today = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault())
        todayTextView.text = "Today is ${dateFormat.format(today.time)}"
        calendarView.date = today.timeInMillis

        // Add child buttons vertically
        for (child in children) {
            val childButton = Button(this).apply {
                text = child
                textSize = 16f
                gravity = Gravity.START
                setBackgroundColor(Color.LTGRAY)
                setOnClickListener {
                    selectChild(child)
                }
            }
            childrenListLayout.addView(childButton)
            childButtons.add(childButton)
        }

        // "+" button goes to FamilyIdActivity
        addChildButton.setOnClickListener {
            startActivity(Intent(this, FamilyIdActivity::class.java))
        }
    }

    private fun selectChild(name: String) {
        selectedChild = name
        Toast.makeText(this, "$name selected", Toast.LENGTH_SHORT).show()

        for (btn in childButtons) {
            if (btn.text == name) {
                btn.setBackgroundColor(Color.parseColor("#FFBB86FC")) // Highlight color
                btn.setTextColor(Color.WHITE)
            } else {
                btn.setBackgroundColor(Color.LTGRAY)
                btn.setTextColor(Color.BLACK)
            }
        }
    }
}
