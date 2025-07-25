package com.project.sharedcustodycalendar.views

import android.content.Context
import android.graphics.*
import android.view.View
import com.project.sharedcustodycalendar.objects.FamilyDataHolder

class TriangleToggleCell(
    context: Context,
    private val index: Int,
    private val morningSchedule: MutableList<Int>,
    private val eveningSchedule: MutableList<Int>,
    private val totalDays: Int,
    private val cellViews: List<View> = emptyList(), // Optional, for cross-updates
) : View(context) {

    private var showNumber = false
    private var isInCalendarActivity = false
    private var isViewer = false
    private var year = -1
    private var monthId = -1
    private val textPaint = Paint().apply {
        color = Color.BLACK
        textSize = 36f * resources.displayMetrics.density  // adjust as needed
        isAntiAlias = true
        textAlign = Paint.Align.RIGHT
    }
    private val paintTop = Paint().apply { style = Paint.Style.FILL }
    private val paintBottom = Paint().apply { style = Paint.Style.FILL }
    private val borderPaint = Paint().apply {
        color = Color.BLACK
        style = Paint.Style.STROKE
        strokeWidth = 4f
    }

    init {
        if (!isViewer) {
            setOnClickListener {
                val newValue = 1 - eveningSchedule[index]
                eveningSchedule[index] = newValue
                if (index + 1 >= morningSchedule.size) {
                    if (!isInCalendarActivity) {
                        morningSchedule[(index + 1) % totalDays] = newValue
                    }
                } else {
                    morningSchedule[(index + 1) % totalDays] = newValue
                }

                invalidate()
                cellViews.getOrNull((index + 1) % totalDays)?.invalidate()

                if (isInCalendarActivity) {
                    val activeChild = FamilyDataHolder.familyData.activeChild
                    activeChild?.changeParentNight(year.toString(), monthId, index + 1, newValue)
                }
            }
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val parents = FamilyDataHolder.familyData.activeChild?.parents ?: return

        val morningColor = morningSchedule[index].let {
            if (it >= 0) Color.parseColor(parents[morningSchedule[index]].color)
            else Color.WHITE
        }
        val eveningColor = eveningSchedule[index].let {
            if (it >= 0) Color.parseColor(parents[eveningSchedule[index]].color)
            else Color.WHITE
        }

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
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), borderPaint)

        if (showNumber) {
            val day = index + 1
            // Draw at top‑right corner, with 8dp padding
            val padding = 8 * resources.displayMetrics.density
            val x = width - padding
            // Use font metrics to position text a little down from top
            val y = textPaint.textSize + padding
            canvas.drawText(day.toString(), x, y, textPaint)
        }
    }

    fun isCalendarActive(year: Int, monthId: Int) {
        showNumber = true
        invalidate()
        isInCalendarActivity = true
        this.year = year
        this.monthId = monthId
    }

    fun isViewer(year: Int, monthId: Int){
        showNumber = true
        invalidate()
        isViewer = true
        this.year = year
        this.monthId = monthId
        setOnClickListener(null)
    }

    fun updateMonthYear (year: Int, monthId: Int) {
        this.year = year
        this.monthId = monthId
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val widthSize = MeasureSpec.getSize(widthMeasureSpec)
        setMeasuredDimension(widthSize, widthSize) // Make square
    }
}
