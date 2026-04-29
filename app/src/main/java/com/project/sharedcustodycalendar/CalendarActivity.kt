package com.project.sharedcustodycalendar

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button

import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import android.app.DatePickerDialog
import com.project.sharedcustodycalendar.objects.CalendarParameters
import com.project.sharedcustodycalendar.objects.Child
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
    private lateinit var modifyButton : Button
    private lateinit var regenerateButton : Button

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
        modifyButton = findViewById(R.id.modifyPatternButton)
        regenerateButton = findViewById(R.id.regenerateCalendarButton)

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
            //User.addChildPermission(params.activeChild?.childID ?: "000000",0)
            val child = params.activeChild ?: return@setOnClickListener
            Log.i("CalendarActivity", "is loop on?: : ${params.activeChild?.parentConfirmed}" )
            if (params.activeChild?.parentConfirmed == true) {
                Log.INFO
                child.getCalendarChanges()
                //params.activeChild?.resolvePendingChanges()
                child.deleteModifiedCalendar()
            }
            else {
                applyModifiedCalendarToOfficial(child) // ← copy modified → official
            }
            FirebaseUtils.saveActiveChild()
            startActivity(Intent(this, DashboardActivity::class.java))
        }

        modifyButton.setOnClickListener {
            val intent = Intent(this, PatternInputActivity::class.java)
            intent.putExtra("MODE", "EDIT")
            startActivity(intent)
        }

        regenerateButton.setOnClickListener {

            val today = Calendar.getInstance()

            val year = today.get(Calendar.YEAR)
            val month = today.get(Calendar.MONTH)   // 0-based for DatePicker
            val day = today.get(Calendar.DAY_OF_MONTH)

            val datePicker = DatePickerDialog(
                this@CalendarActivity,
                { _, selectedYear, selectedMonth, selectedDay ->

                    // Convert month back to 1-based for your logic
                    val regenMonth = selectedMonth + 1

                    params.activeChild?.regenerateCalendarFromDate(
                        selectedYear,
                        regenMonth,
                        selectedDay
                    )

                    FirebaseUtils.saveActiveChild()
                    startActivity(Intent(this, DashboardActivity::class.java))
                    finish()
                },
                year,
                month,
                day
            )

            // Optional but VERY recommended:
            // prevent regenerating in the past
            datePicker.datePicker.minDate = today.timeInMillis

            datePicker.show()
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

    fun applyModifiedCalendarToOfficial(child: Child) {
        val modified = child.modifiedCalendar
        modified.forEach { (year, months) ->
            months.forEach { month ->
                child.setOrUpdateMonth(year, month)
            }
        }
        child.deleteModifiedCalendar() // optional after copying
    }


 }

