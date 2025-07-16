package com.project.sharedcustodycalendar

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.project.sharedcustodycalendar.objects.CalendarParameters
import com.project.sharedcustodycalendar.objects.Child
import com.project.sharedcustodycalendar.objects.FamilyDataHolder
import com.project.sharedcustodycalendar.utils.CalendarStorageUtils
import com.project.sharedcustodycalendar.utils.CalendarUIUtils
import com.project.sharedcustodycalendar.views.TriangleToggleCell
import java.text.SimpleDateFormat
import java.util.*

class DashboardActivity : AppCompatActivity() {


    private lateinit var legendLayout: LinearLayout
    private lateinit var prevMonthBtn: Button
    private lateinit var nextMonthBtn: Button

    private lateinit var todayTextView: TextView
    private lateinit var childrenList: LinearLayout
    private lateinit var addChildButton: Button
    private lateinit var deleteChildButton: Button
    private lateinit var modifyButton: Button

    private lateinit var params: CalendarParameters

    private var activeChild: Child? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        // Bind views
        legendLayout = findViewById(R.id.legendLayout)
        prevMonthBtn = findViewById(R.id.prevMonthBtn)
        nextMonthBtn = findViewById(R.id.nextMonthBtn)

        todayTextView = findViewById(R.id.todayTextView)
        childrenList = findViewById(R.id.childrenList)
        addChildButton = findViewById(R.id.addChildButton)
        deleteChildButton = findViewById(R.id.deleteChildButton)
        modifyButton = findViewById(R.id.modifyButton)

        modifyButton.setOnClickListener {
            startActivity(Intent(this, CalendarActivity::class.java))
        }

        //load data
        CalendarStorageUtils.loadLocally(this)


        activeChild = FamilyDataHolder.familyData.activeChild

        // Set today's date
        val today = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault())
        todayTextView.text = "Today is ${dateFormat.format(today.time)}"

        // Handle add button
        addChildButton.setOnClickListener {
            startActivity(Intent(this, ChildIdActivity::class.java))
        }
        deleteChildButton.setOnClickListener {
            showChildSelector(::deleteChild)
            CalendarStorageUtils.saveLocally(this)
        }

        // Show the first child
        showCurrentChild()

        // Draw Calendar
        // Show today's date
        // Init calendar (initialize if missing)

        params = CalendarParameters(
            year = today.get(Calendar.YEAR),
            month = today.get(Calendar.MONTH) + 1,
            monthLabelView = findViewById(R.id.monthLabelView),
            cellViews = mutableListOf<TriangleToggleCell>(),
            calendarGrid = findViewById(R.id.calendarGrid),
            activeChild = FamilyDataHolder.familyData.activeChild!!,
            context = this,
            isViewer = true
        )


        prevMonthBtn.setOnClickListener { CalendarUIUtils.shiftMonth(-1, params) }
        nextMonthBtn.setOnClickListener { CalendarUIUtils.shiftMonth(+1, params) }

        CalendarUIUtils.updateHeaderAndGrid( params )

        // draw legend
        CalendarUIUtils.drawLegend(this@DashboardActivity, legendLayout)

        // Add header days of the week
        CalendarUIUtils.addHeader(findViewById(R.id.headerRow), this)

        CalendarUIUtils.drawCalendarGrid(params)

        // TODO deal with the null case of child
        val activeChildName = activeChild?.childName ?: "Unknown"
    }

    private fun showCurrentChild() {
        childrenList.removeAllViews()
        val family = FamilyDataHolder.familyData

        val name = family?.activeChild?.childName ?: "No child selected"
        val childView = TextView(this).apply {
            text = name
            textSize = 18f
            setPadding(24, 32, 24, 32)
            setBackgroundColor(Color.LTGRAY)
            setTextColor(Color.BLACK)
            textAlignment = TextView.TEXT_ALIGNMENT_CENTER
            setOnClickListener {
                showChildSelector(::selectChild)
            }
        }
        childrenList.addView(childView)
    }


    private fun showChildSelector(fn: (Array<String>, List<Child>, FamilyDataHolder.FamilyData)-> Unit) {
        val family = FamilyDataHolder.familyData
        val allChildren = family.children ?: emptyList()

        if (allChildren.isEmpty()) {
            Toast.makeText(this, "No children available. Please add a child first.", Toast.LENGTH_SHORT).show()
            return
        }

        val names = allChildren.map { it.childName }.toTypedArray()

        fn (names, allChildren, family)
    }

    private fun selectChild(names: Array<String>, allChildren: List<Child>, family: FamilyDataHolder.FamilyData) {
        AlertDialog.Builder(this)
            .setTitle("Select a child")
            .setItems(names) { _, which ->
                val selectedChild = allChildren[which]
                family.setActiveChild(selectedChild.childID)
                showCurrentChild()
            }
            .show()
    }

    private fun deleteChild(names: Array<String>, allChildren: List<Child>, family: FamilyDataHolder.FamilyData) {
        AlertDialog.Builder(this)
            .setTitle("Select a child")
            .setItems(names) { _, which ->
                val selectedChild = allChildren[which]
                family.deleteChild(selectedChild.childID)
                showCurrentChild()
            }
            .show()
    }

}
