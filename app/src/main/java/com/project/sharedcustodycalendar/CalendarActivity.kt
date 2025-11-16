package com.project.sharedcustodycalendar

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button

import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.project.sharedcustodycalendar.model.User
import com.project.sharedcustodycalendar.objects.CalendarParameters
import com.project.sharedcustodycalendar.objects.FamilyDataHolder
import com.project.sharedcustodycalendar.utils.CalendarUIUtils
import com.project.sharedcustodycalendar.views.TriangleToggleCell
import com.project.sharedcustodycalendar.utils.CalendarStorageUtils
import com.project.sharedcustodycalendar.utils.FirebaseUtils
import java.util.Calendar

class CalendarActivity :  AppCompatActivity() {
    private lateinit var legendLayout: LinearLayout

    private lateinit var prevMonthBtn: Button
    private lateinit var nextMonthBtn: Button
    private lateinit var saveButton: Button

    private lateinit var params: CalendarParameters


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_calendar)

        var titleTextView: TextView = findViewById(R.id.titleTextView)
        var childTextView: TextView = findViewById(R.id.childID_textView)
        var viewerTokenTextView: TextView = findViewById(R.id.viewerToken_textView)
        legendLayout = findViewById(R.id.legendLayout)

        prevMonthBtn = findViewById(R.id.prevMonthBtn)
        nextMonthBtn = findViewById(R.id.nextMonthBtn)
        saveButton = findViewById(R.id.saveButton)

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

        saveButton.setOnClickListener {
            CalendarStorageUtils.saveLocally(this)
            User.addChildPermission(params.activeChild?.childID ?: "000000",0)
            if (params.activeChild?.parentConfirmed == true) {
                params.activeChild?.getCalendarChanges()
                //params.activeChild?.resolvePendingChanges()
                params.activeChild?.deleteModifiedCalendar()
                FirebaseUtils.saveActiveChild()
            }
            Log.d("CalendarActivity", "Original nights: ${params.activeChild?.officialCalendar?.get("2025")?.get(5)?.parent0_nights}")
            Log.d("CalendarActivity", "Modified nights: ${params.activeChild?.modifiedCalendar?.get("2025")?.get(5)?.parent0_nights}")
            FirebaseUtils.saveActiveChild()
            startActivity(Intent(this, DashboardActivity::class.java))
        }

        prevMonthBtn.setOnClickListener { CalendarUIUtils.shiftMonth(-1, params) }
        nextMonthBtn.setOnClickListener { CalendarUIUtils.shiftMonth(+1, params) }

        CalendarUIUtils.updateHeaderAndGrid( params)

        // TODO deal with the null case of child
        val activeChildName = params.activeChild?.childName ?: "Unknown"
        val activeChildToken = params.activeChild?.childID ?: ""

        titleTextView.text = "Calendar for $activeChildName"
        childTextView.text = "Child Token (token allows to edit): $activeChildToken"
        viewerTokenTextView.text = "Viewer Token (token only allows to view ): ${params.activeChild?.getViewerToken()}"

        // draw legend
        CalendarUIUtils.drawLegend(this@CalendarActivity, legendLayout)

        // Add header days of the week
        CalendarUIUtils.addHeader(findViewById(R.id.headerRow), this)

        params.activeChild?.initializeCalendar(params.year, params.month, 0)
        params.activeChild?.createModifiedCalendar()

        // initial draw for current month
        CalendarUIUtils.drawCalendarGrid(params)
    }

 }

