package com.project.sharedcustodycalendar

import android.content.Intent
import android.os.Bundle
import android.widget.Button

import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.project.sharedcustodycalendar.objects.CalendarParameters
import com.project.sharedcustodycalendar.objects.FamilyDataHolder
import com.project.sharedcustodycalendar.utils.CalendarUIUtils
import com.project.sharedcustodycalendar.views.TriangleToggleCell
import com.project.sharedcustodycalendar.utils.CalendarStorageUtils
import java.util.Calendar

class CalendarActivity :  AppCompatActivity() {
    private lateinit var titleTextView: TextView
    private lateinit var legendLayout: LinearLayout

    private lateinit var prevMonthBtn: Button
    private lateinit var nextMonthBtn: Button
    private lateinit var saveButton: Button

    private lateinit var params: CalendarParameters


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_calendar)

        titleTextView = findViewById(R.id.titleTextView)
        legendLayout = findViewById(R.id.legendLayout)

        prevMonthBtn = findViewById(R.id.prevMonthBtn)
        nextMonthBtn = findViewById(R.id.nextMonthBtn)
        saveButton = findViewById(R.id.saveButton)

        saveButton.setOnClickListener {
            CalendarStorageUtils.saveLocally(this)
            startActivity(Intent(this, DashboardActivity::class.java))
        }

        // Show today's date
        val today = Calendar.getInstance()

        // Init calendar (initialize if missing)
        params = CalendarParameters(
            year = today.get(Calendar.YEAR),
            month = today.get(Calendar.MONTH) + 1,
            monthLabelView = findViewById(R.id.monthLabelView),
            cellViews = mutableListOf<TriangleToggleCell>(),
            calendarGrid = findViewById(R.id.calendarGrid),
            activeChild = FamilyDataHolder.familyData.activeChild!!,
            context = this,
            isCalendarActivity = true
        )

        prevMonthBtn.setOnClickListener { CalendarUIUtils.shiftMonth(-1, params) }
        nextMonthBtn.setOnClickListener { CalendarUIUtils.shiftMonth(+1, params) }

        CalendarUIUtils.updateHeaderAndGrid( params)

        // TODO deal with the null case of child
        val activeChildName = params.activeChild?.childName ?: "Unknown"
        val activeChildToken = params.activeChild?.childID ?: ""

        titleTextView.text = "Calendar for $activeChildName ($activeChildToken)"

        // draw legend
        CalendarUIUtils.drawLegend(this@CalendarActivity, legendLayout)

        // Add header days of the week
        CalendarUIUtils.addHeader(findViewById(R.id.headerRow), this)

        params.activeChild?.initializeCalendar(params.year, params.month, 0)

        // initial draw for current month
        CalendarUIUtils.drawCalendarGrid(params)
    }

 }

