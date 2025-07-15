package com.project.sharedcustodycalendar

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.view.Gravity
import android.widget.Button

import android.widget.GridLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.project.sharedcustodycalendar.objects.Child
import com.project.sharedcustodycalendar.objects.FamilyDataHolder
import com.project.sharedcustodycalendar.utils.CalendarUIUtils
import com.project.sharedcustodycalendar.views.TriangleToggleCell
import com.project.sharedcustodycalendar.utils.CalendarStorageUtils
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale


class CalendarActivity :  AppCompatActivity() {
    private lateinit var titleTextView: TextView
    private lateinit var legendLayout: LinearLayout
    private lateinit var calendarGrid: GridLayout

    private lateinit var monthLabelView: TextView
    private lateinit var prevMonthBtn: Button
    private lateinit var nextMonthBtn: Button
    private lateinit var saveButton: Button

    private lateinit var activeChild: Child
    private var year : Int = 0
    private var month : Int = 0


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_calendar)
        activeChild = FamilyDataHolder.familyData.activeChild!!

        titleTextView = findViewById(R.id.titleTextView)
        legendLayout = findViewById(R.id.legendLayout)
        calendarGrid = findViewById(R.id.calendarGrid)

        monthLabelView = findViewById(R.id.monthLabelView)
        prevMonthBtn = findViewById(R.id.prevMonthBtn)
        nextMonthBtn = findViewById(R.id.nextMonthBtn)
        saveButton = findViewById(R.id.saveButton)

        prevMonthBtn.setOnClickListener { shiftMonth(-1) }
        nextMonthBtn.setOnClickListener { shiftMonth(+1) }

        saveButton.setOnClickListener {
            CalendarStorageUtils.saveLocally(this)
            startActivity(Intent(this, DashboardActivity::class.java))
        }

        // Show today's date
        val today = Calendar.getInstance()

        // Init calendar (initialize if missing)
        year = today.get(Calendar.YEAR)
        month = today.get(Calendar.MONTH) + 1
        updateHeaderAndGrid()

        // TODO deal with the null case of child
        val activeChildName = activeChild?.childName ?: "Unknown"
        val activeChildToken = activeChild?.childID ?: ""

        titleTextView.text = "New Calendar for $activeChildName ($activeChildToken)"

        // draw legend
        CalendarUIUtils.drawLegend(this@CalendarActivity, legendLayout)

        // Add header days of the week
        val headerRow = findViewById<LinearLayout>(R.id.headerRow)
        headerRow.removeAllViews()
        val daysOfWeek = listOf("S", "M", "T", "W", "T", "F", "S")
        daysOfWeek.forEach { day ->
            val tv = TextView(this).apply {
                text = day
                gravity = Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f)
            }
            headerRow.addView(tv)
        }

        activeChild.initializeCalendar(year, month, 0)

        if (activeChild == null) {
            Log.e("CalendarActivity", "Active child is null")
        } else {
            Log.d("CalendarActivity", "print parent0 evenings : ${activeChild.getParent0EveningSchedule(year.toString(), month)}")
        }
        // initial draw for current month
        drawCalendarGrid(month, year, activeChild)

        // TODO : change month
    }


        fun drawCalendarGrid(monthIdx: Int, year: Int, activeChild: Child) {
            val calendar = Calendar.getInstance()
            calendar.set(Calendar.YEAR, year)
            calendar.set(Calendar.MONTH, monthIdx -1)
            calendar.set(Calendar.DAY_OF_MONTH, 1)

            val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
            val startDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK) //

            val parent0EveningSchedule = activeChild.getParent0EveningSchedule(year.toString(), monthIdx)
            val hasMonthData = activeChild.years[year.toString()]?.any { it.monthId == monthIdx } == true

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

            val morningSchedule = if (hasMonthData) {
                MutableList(daysInMonth) { 0 }.apply {
                    this[0] = activeChild.getStartingParent(year.toString(), monthIdx)
                    for (i in 0 until size - 1) this[i + 1] = eveningSchedule[i]
                }
            } else {
                MutableList(daysInMonth) { -1 }
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
                cell.drawNumbers()

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

    private fun shiftMonth(delta: Int) {
        month += delta
        if (month < 1) { month = 12; year-- }
        else if (month > 12) { month = 1; year++ }
        updateHeaderAndGrid()
    }

    private fun updateHeaderAndGrid() {
        // Then when you want to set the label:
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR,  year)
            set(Calendar.MONTH, month - 1)  // Calendar.MONTH is zero‑based
            set(Calendar.DAY_OF_MONTH, 1)   // to avoid rolling into previous/next month
        }

        val formatter = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
        monthLabelView.text = formatter.format(cal.time)
        drawCalendarGrid(month, year, activeChild)
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
    // TODO doesn't toggle morning
    // TODO center legend
    // TODO allign on the same line month and <>
    // TODO add save bottom
    // Generate calendar from this month



        // For each month in val activeChild = FamilyDataHolder.familyData?.activeChild ?: return -1


    }

