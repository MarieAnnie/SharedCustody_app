package com.project.sharedcustodycalendar


import android.content.Intent
import android.os.Bundle
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

    private var isEditMode = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pattern)

        // 1 Determine mode FIRST
        isEditMode = intent.getStringExtra("MODE") == "EDIT"

        // 2 Bind views
        val titleTextView = findViewById<TextView>(R.id.titleTextView)
        weekCountInput = findViewById(R.id.weekCountInput)
        generateButton = findViewById(R.id.generateButton)
        calendarGrid = findViewById(R.id.calendarGrid)
        legendLayout = findViewById(R.id.legendLayout)

        // 3 Apply mode-dependent UI rules
        weekCountInput.isEnabled = !isEditMode

        val activeChildName =
            FamilyDataHolder.familyData.activeChild?.childName ?: "Unknown"

        titleTextView.text = if (isEditMode)
            "Edit Pattern for $activeChildName"
        else
            "New Calendar for $activeChildName"

        // 4 Create Save button
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

        // 5 Generate button
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

        // 6 Load pattern ONLY after views exist
        if (isEditMode) {
            loadExistingPattern()
        }
    }

    private fun loadExistingPattern() {
        val existingPattern =
            FamilyDataHolder.familyData.activeChild?.schedulePattern ?: return

        numberOfWeeks = existingPattern.size / 7
        weekCountInput.setText(numberOfWeeks.toString())

        // Copy into evening schedule (your saved source of truth)
        for (i in existingPattern.indices) {
            eveningSchedule[i] = existingPattern[i]
        }

        CalendarUIUtils.drawLegend(this, legendLayout)
        drawCalendarGrid()

        // Tell cells to refresh from schedule
        cellViews.forEach { it.refresh() }

        saveButton.isEnabled = true
        generateButton.isEnabled = false
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
                val cell = TriangleToggleCell(
                    context = this,
                    index =index,
                    morningSchedule = morningSchedule,
                    eveningSchedule = eveningSchedule,
                    totalDays = numberOfWeeks*7,
                    cellViews= cellViews)
                val cellParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                cell.layoutParams = cellParams
                cellViews.add(cell)
                weekRow.addView(cell)
            }
            calendarGrid.addView(weekRow)
        }
    }

}
