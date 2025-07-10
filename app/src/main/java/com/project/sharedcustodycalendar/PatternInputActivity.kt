package com.project.sharedcustodycalendar

import android.content.Context
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

    private var numberOfWeeks: Int = 0
    private val schedule = MutableList(28) { 0 } // Default to parent 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pattern)

        weekCountInput = findViewById(R.id.weekCountInput)
        generateButton = findViewById(R.id.generateButton)
        calendarGrid = findViewById(R.id.calendarGrid)
        legendLayout = findViewById(R.id.legendLayout)

        generateButton.setOnClickListener {
            numberOfWeeks = weekCountInput.text.toString().toIntOrNull() ?: 0
            if (numberOfWeeks in 1..4) {
                drawLegend()
                drawCalendarGrid()
            } else {
                Toast.makeText(this, "Please enter a number between 1 and 4", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun drawLegend() {
        legendLayout.removeAllViews()
        val parents = FamilyDataHolder.familyData?.parents ?: return

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
        val parents = FamilyDataHolder.familyData?.parents ?: return

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
            val weekRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            for (col in 0 until 7) {
                val index = row * 7 + col
                val cell = TriangleToggleCell(this, index)
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
                schedule[index] = 1 - schedule[index] // Toggle between 0 and 1
                val prevIndex = (index - 1 + numberOfWeeks * 7) % (numberOfWeeks * 7)
                invalidate()
                findViewById<View>(index)?.invalidate()
                findViewById<View>(prevIndex)?.invalidate()
            }
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val parents = FamilyDataHolder.familyData?.parents ?: return

            val prevIndex = (index - 1 + numberOfWeeks * 7) % (numberOfWeeks * 7)
            val prevParent = schedule[prevIndex]
            val currentParent = schedule[index]

            paintTop.color = Color.parseColor(parents[prevParent].color)
            paintBottom.color = Color.parseColor(parents[currentParent].color)

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
        }

        override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
            val size = MeasureSpec.getSize(widthMeasureSpec) / 7
            setMeasuredDimension(size, size)
        }
    }
}