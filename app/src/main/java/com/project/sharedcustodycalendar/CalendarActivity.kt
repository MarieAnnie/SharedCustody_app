package com.project.sharedcustodycalendar

import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.CalendarView
import android.widget.GridLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.project.sharedcustodycalendar.objects.Child
import com.project.sharedcustodycalendar.objects.FamilyDataHolder
import com.project.sharedcustodycalendar.utils.CalendarUIUtils
import com.project.sharedcustodycalendar.views.TriangleToggleCell
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class CalendarActivity :  AppCompatActivity() {
    private lateinit var titleTextView: TextView
    private lateinit var legendLayout: LinearLayout
    private lateinit var calendarGrid: LinearLayout
    private lateinit var calendarView: CalendarView


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_calendar)

        titleTextView = findViewById(R.id.titleTextView)
        legendLayout = findViewById(R.id.legendLayout)
        calendarGrid = findViewById(R.id.calendarGrid)
        calendarView = findViewById(R.id.calendarView)

        // TODO deal with the null case of child
        val activeChild = FamilyDataHolder.familyData.activeChild!!
        val activeChildName = activeChild?.childName ?: "Unknown"
        val activeChildToken = activeChild?.childID ?: ""

        titleTextView.text = "New Calendar for $activeChildName ($activeChildToken)"

        // draw legend
        CalendarUIUtils.drawLegend(this@CalendarActivity, legendLayout)

        // Show today's date
        val today = Calendar.getInstance()
        calendarView.date = today.timeInMillis
        calendarView.visibility = View.GONE


        // Init calendar (initialize if missing)
        val year = today.get(Calendar.YEAR)
        val month = today.get(Calendar.MONTH) + 1
        activeChild.initializeCalendar(year, month, 0)
        if (activeChild == null) {
            Log.e("CalendarActivity", "Active child is null")
        }


        // TODO : change month
        calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->
            val actualMonth = month + 1  // CalendarView uses 0-indexed months
            drawCalendarGrid(actualMonth, year, activeChild)
        }
    }


        fun drawCalendarGrid(monthIdx: Int, year: Int, activeChild: Child) {
            val calendar = Calendar.getInstance()
            calendar.set(Calendar.YEAR, year)
            calendar.set(Calendar.MONTH, monthIdx -1)
            calendar.set(Calendar.DAY_OF_MONTH, 1)

            val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
            val startDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK) //

            val parent0EveningSchedule = activeChild.getParent0EveningSchedule(year.toString(), monthIdx)
            var eveningSchedule = MutableList(daysInMonth) { 1 }
            var morningSchedule = MutableList(daysInMonth) { 0 }
            morningSchedule[0] = activeChild.getStartingParent(year.toString(), monthIdx)

            for (i in parent0EveningSchedule){
                eveningSchedule[i] = 0
            }
            for (i in 0 until eveningSchedule.size - 1){
                morningSchedule[i+1] = eveningSchedule [i]
            }

            // Clear existing views
            calendarGrid.removeAllViews()

            // Add blank cells before the first day
            for (i in 1 until startDayOfWeek) {
                val emptyView = TextView(calendarGrid.context)
                calendarGrid.addView(emptyView)
            }

            // Add triangle toggle cells
            for (day in 1..daysInMonth) {
                val index = day - 1
                val cell = TriangleToggleCell(
                    context = this,
                    index = index,
                    morningSchedule = morningSchedule,
                    eveningSchedule = eveningSchedule,
                    totalDays = daysInMonth,
                    cellViews = mutableListOf()  // shared list declared in Activity if needed
                )

                val size = resources.displayMetrics.widthPixels / 7
                val params = GridLayout.LayoutParams().apply {
                    width = size
                    height = size
                    setMargins(4, 4, 4, 4)
                }

                cell.layoutParams = params
                calendarGrid.addView(cell)

            }

        }


        // TODO regenerate with other parent (set morning[0] to 1) and regenerate calendar with 1 based on the current month
        /*titleTextView.text = "New Calendar for $activeChildName ($activeChildToken)"

        calendarView = findViewById(R.id.calendarView)

        // TODO show text "click on the parent's name to change who has the child on the first day of the month"
        // TODO show legend (see code)
        // TODO add toggle option on the legend

        val today = Calendar.getInstance()
        calendarView.date = today.timeInMillis

        val activeChild = FamilyDataHolder.familyData.activeChild
        val year = today.get(Calendar.YEAR)
        val month = today.get(Calendar.MONTH) + 1

        activeChild.initializeCalendar(year,month, 0)

        //TODO : overlaps the triangles
        // if the month wasn't initialized leave it white, consider other months than current one
        // TODO add toggle option. when clicking on a day change the evening color and the next morning color

        // TODO 2026

        //TODO cycle on the schedule pattern*/



        // For each month in val activeChild = FamilyDataHolder.familyData?.activeChild ?: return -1


    }

