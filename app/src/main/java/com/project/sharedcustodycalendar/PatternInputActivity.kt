package com.project.sharedcustodycalendar

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.os.Bundle
import android.view.View
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

import com.project.sharedcustodycalendar.objects.FamilyDataHolder
import com.project.sharedcustodycalendar.utils.FirebaseUtils
import com.project.sharedcustodycalendar.utils.CalendarUIUtils
import com.project.sharedcustodycalendar.views.TriangleToggleCell

class PatternInputActivity : AppCompatActivity() {

    private lateinit var weekCountInput: EditText
    private lateinit var generateButton: Button
    private lateinit var calendarGrid: LinearLayout
    private lateinit var legendLayout: LinearLayout
    private lateinit var saveButton: Button
    private val cellViews = mutableListOf<TriangleToggleCell>()

    private var numberOfWeeks: Int = 0
    private val morningSchedule = MutableList(28) { 0 }  // color at start of day
    private val eveningSchedule = MutableList(28) { 0 }  // color at end of day

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pattern)

        val titleTextView = findViewById<TextView>(R.id.titleTextView)
        val activeChildName = FamilyDataHolder.familyData.activeChild?.childName ?: "Unknown"
        titleTextView.text = "New Calendar for $activeChildName"

        weekCountInput = findViewById(R.id.weekCountInput)
        generateButton = findViewById(R.id.generateButton)
        calendarGrid = findViewById(R.id.calendarGrid)
        legendLayout = findViewById(R.id.legendLayout)

        // Create and add the Save button programmatically below calendarGrid
        saveButton = Button(this).apply {
            text = "Save"
            isEnabled = false // Disabled until a pattern is generated
            setOnClickListener {
                FamilyDataHolder.familyData.setSchedulePatternForActiveChild(eveningSchedule.subList(0, numberOfWeeks * 7).toList())
                FirebaseUtils.saveActiveChild()

                // Start CalendarActivity
                startActivity(Intent(this@PatternInputActivity, CalendarActivity::class.java))
                finish()
            }
        }
        (calendarGrid.parent as LinearLayout).addView(saveButton)

        generateButton.setOnClickListener {
            numberOfWeeks = weekCountInput.text.toString().toIntOrNull() ?: 0
            if (numberOfWeeks in 1..4) {
                CalendarUIUtils.drawLegend(this@PatternInputActivity, legendLayout)
                drawCalendarGrid()
                saveButton.isEnabled = true
            } else {
                Toast.makeText(this, "Please enter a number between 1 and 4", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun drawCalendarGrid() {
        calendarGrid.removeAllViews()
        cellViews.clear()

        // Header Row
        val daysOfWeek = listOf("S", "M", "T", "W", "T", "F", "S")
        val headerRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        daysOfWeek.forEach {
            val label = TextView(this).apply {
                text = it
                gravity = android.view.Gravity.CENTER
                setPadding(8, 8, 8, 8)
                layoutParams = LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f)
            }
            headerRow.addView(label)
        }
        calendarGrid.addView(headerRow)

        // Calendar Cells
        for (row in 0 until numberOfWeeks) {
            val weekRow = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            }
            for (col in 0 until 7) {
                val index = row * 7 + col
                val cell = TriangleToggleCell(this, index, morningSchedule, eveningSchedule, numberOfWeeks*7, cellViews)
                val cellParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                cell.layoutParams = cellParams
                cellViews.add(cell)
                weekRow.addView(cell)
            }
            calendarGrid.addView(weekRow)
        }
    }

    private val borderPaint = Paint().apply {
        color = Color.BLACK
        style = Paint.Style.STROKE
        strokeWidth = 4f  // adjust thickness if needed
    }

}
