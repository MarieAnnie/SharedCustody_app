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
                FamilyDataHolder.familyData.setSchedulePatternForActiveChild(eveningSchedule)

                // Start CalendarActivity
                startActivity(Intent(this@PatternInputActivity, CalendarActivity::class.java))
                finish()
            }
        }
        (calendarGrid.parent as LinearLayout).addView(saveButton)

        generateButton.setOnClickListener {
            numberOfWeeks = weekCountInput.text.toString().toIntOrNull() ?: 0
            if (numberOfWeeks in 1..4) {
                drawLegend()
                drawCalendarGrid()
                saveButton.isEnabled = true
            } else {
                Toast.makeText(this, "Please enter a number between 1 and 4", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun drawLegend() {
        legendLayout.removeAllViews()
        val parents = FamilyDataHolder.familyData.activeChild?.parents ?: return

        parents.forEach { parent ->
            val item = TextView(this).apply {
                text = parent.name
                setPadding(16, 0, 16, 0)
                setBackgroundColor(Color.parseColor(parent.color))
                setTextColor(Color.WHITE)
            }
            legendLayout.addView(item)
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
                val cell = TriangleToggleCell(this, index)
                val cellParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                cell.layoutParams = cellParams
                cellViews.add(cell)
                weekRow.addView(cell)
            }
            calendarGrid.addView(weekRow)
        }
    }

    inner class TriangleToggleCell(context: Context, val index: Int) : View(context) {
        private val paintTop = Paint().apply { style = Paint.Style.FILL }
        private val paintBottom = Paint().apply { style = Paint.Style.FILL }

        init {
            setOnClickListener {
                val totalDays = numberOfWeeks * 7
                // Toggle evening of current day and morning of next day (wrap around)
                val newValue = 1 - eveningSchedule[index]

                // Toggle between parent 0 and parent 1
                // Toggle based on current value
                eveningSchedule[index] = newValue
                morningSchedule[(index + 1) % totalDays] = newValue


                invalidate()
                cellViews.getOrNull((index + 1) % totalDays)?.invalidate()
            }
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val parents = FamilyDataHolder.familyData.activeChild?.parents ?: return

            val morningColor = Color.parseColor(parents[morningSchedule[index]].color)
            val eveningColor = Color.parseColor(parents[eveningSchedule[index]].color)

            paintTop.color = morningColor
            paintBottom.color = eveningColor

            val pathTop = Path().apply {
                moveTo(0f, 0f)
                lineTo(width.toFloat(), 0f)
                lineTo(0f, height.toFloat())
                close()
            }

            val pathBottom = Path().apply {
                moveTo(width.toFloat(), height.toFloat())
                lineTo(0f, height.toFloat())
                lineTo(width.toFloat(), 0f)
                close()
            }

            canvas.drawPath(pathTop, paintTop)
            canvas.drawPath(pathBottom, paintBottom)

            // Black border
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), borderPaint)
        }


        override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
            // Let width be dictated by layout weights, so height = width for square
            val widthSize = MeasureSpec.getSize(widthMeasureSpec)
            setMeasuredDimension(widthSize, widthSize)
        }

        private val borderPaint = Paint().apply {
            color = Color.BLACK
            style = Paint.Style.STROKE
            strokeWidth = 4f  // adjust thickness if needed
        }
    }
}
