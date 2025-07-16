package com.project.sharedcustodycalendar.utils

import android.content.Context
import android.graphics.Color
import android.view.Gravity
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.GridLayout
import android.widget.LinearLayout
import android.widget.TextView
import com.project.sharedcustodycalendar.objects.CalendarParameters
import com.project.sharedcustodycalendar.objects.FamilyDataHolder
import com.project.sharedcustodycalendar.views.TriangleToggleCell
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.ceil

object CalendarUIUtils {
    fun drawLegend(context: Context, legendLayout: LinearLayout) {
        legendLayout.removeAllViews()
        val parents = FamilyDataHolder.familyData.activeChild?.parents ?: return

        parents.forEach { parent ->
            val item = TextView(context).apply {
                text = parent.name
                setPadding(16, 0, 16, 0)
                setBackgroundColor(Color.parseColor(parent.color))
                setTextColor(Color.WHITE)
            }
            legendLayout.addView(item)
        }
    }

    fun updateHeaderAndGrid( params: CalendarParameters,) {
        // Then when you want to set the label:

        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR,  params.year)
            set(Calendar.MONTH, params.month - 1)  // Calendar.MONTH is zero‑based
            set(Calendar.DAY_OF_MONTH, 1)   // to avoid rolling into previous/next month
        }

        val formatter = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
        params.monthLabelView.text = formatter.format(cal.time)
        drawCalendarGrid(params)
    }

    fun shiftMonth( delta: Int, params: CalendarParameters ) {
        params.month += delta
        if (params.month < 1) { params.month = 12; params.year-- }
        else if (params.month > 12) { params.month = 1; params.year++ }
        updateHeaderAndGrid( params)

        if (params.cellViews.isNotEmpty()) {
            params.cellViews.forEach { it.updateMonthYear(params.year, params.month) }
        }
    }

    fun addHeader(headerRow: LinearLayout, context: Context) {
        headerRow.removeAllViews()
        val daysOfWeek = listOf("S", "M", "T", "W", "T", "F", "S")
        daysOfWeek.forEach { day ->
            val tv = TextView(context).apply {
                text = day
                gravity = Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f)
            }
            headerRow.addView(tv)
        }
    }

    fun drawCalendarGrid(params : CalendarParameters) {

        val calendar = Calendar.getInstance()
        calendar.set(Calendar.YEAR, params.year)
        calendar.set(Calendar.MONTH, params.month -1)
        calendar.set(Calendar.DAY_OF_MONTH, 1)

        val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
        val startDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK) //
        var hasMonthData = false
        var parent0EveningSchedule = listOf<Int>()

        val totalCells = startDayOfWeek - 1 + daysInMonth
        val rows = ceil(totalCells / 7.0).toInt()
        params.calendarGrid.rowCount = rows

        val activeChild = params.activeChild
        if (activeChild != null) {
            parent0EveningSchedule = activeChild.getParent0EveningSchedule(params.year.toString(), params.month)
            hasMonthData = activeChild.years[params.year.toString()]?.any { it.monthId == params.month } == true
        }

        // If no data, fill with -1 (meaning “blank”)
        val eveningSchedule = if (hasMonthData) {
            MutableList(daysInMonth) { 1 }.apply {
                parent0EveningSchedule.forEach { day ->
                    if (day in 1..daysInMonth) this[day - 1] = 0
                }
            }
        } else {
            MutableList(daysInMonth) { -1 }
        }

        val morningSchedule = if (hasMonthData && activeChild != null) {
            MutableList(daysInMonth) { 0 }.apply {
                this[0] = activeChild.getStartingParent(params.year.toString(), params.month)
                for (i in 0 until daysInMonth - 1) this[i + 1] = eveningSchedule[i]
            }
        } else {
            MutableList(daysInMonth) { -1 }
        }

        // Clear existing views
        params.calendarGrid.removeAllViews()
        params.cellViews.clear()

        // Add blank cells before the first day
        for (i in 1 until startDayOfWeek) {
            val emptyView = TextView(params.calendarGrid.context)
            params.calendarGrid.addView(emptyView)
        }

        // Add triangle toggle cells
        for (day in 1..daysInMonth) {
            val index = day - 1
            val cell = TriangleToggleCell(
                context = params.context,
                index = index,
                morningSchedule = morningSchedule,
                eveningSchedule = eveningSchedule,
                totalDays = daysInMonth,
                cellViews = params.cellViews  // shared list declared in Activity if needed
            )
            if (params.isCalendarActivity) {
                cell.isCalendarActive(params.year, params.month)
            } else if (params.isViewer) {
                cell.isViewer(params.year, params.month)
            }

            val density = params.context.resources.displayMetrics.density
            val totalHorizontalPadding = (8 * 2 * density).toInt()
            val screenWidth = params.context.resources.displayMetrics.widthPixels - totalHorizontalPadding
            val marginPx = (4 * density).toInt()
            val cellSize = (screenWidth - marginPx * 7) / 7

            val layoutParams = GridLayout.LayoutParams().apply {
                width = cellSize
                height = cellSize
                setMargins(4, 4, 4, 4)
            }
            cell.layoutParams = layoutParams
            params.calendarGrid.addView(cell)
            params.cellViews.add(cell)

        }

    }
}
