package com.project.sharedcustodycalendar

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.project.sharedcustodycalendar.model.User
import com.project.sharedcustodycalendar.objects.CalendarParameters
import com.project.sharedcustodycalendar.objects.Child
import com.project.sharedcustodycalendar.objects.FamilyDataHolder
import com.project.sharedcustodycalendar.objects.ChangeStatus
import com.project.sharedcustodycalendar.utils.CalendarStorageUtils
import com.project.sharedcustodycalendar.utils.CalendarUIUtils
import com.project.sharedcustodycalendar.utils.FirebaseUtils
import com.project.sharedcustodycalendar.views.TriangleToggleCell
import java.text.SimpleDateFormat
import java.util.*

class DashboardActivity : AppCompatActivity() {

    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var syncCalendarButton: Button
    private lateinit var reviewChangesButton: Button

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

    private fun onRefresh() {
        val activeChildId = FamilyDataHolder.familyData.activeChild?.childID
        if (activeChildId == null) {
            swipeRefreshLayout.isRefreshing = false
            Toast.makeText(this, "No active child to refresh", Toast.LENGTH_SHORT).show()
            return
        }

        swipeRefreshLayout.isRefreshing = true  // show spinner

        // Load the latest child data from Firebase
        FirebaseUtils.loadChild(activeChildId) { childFromFirebase ->
            if (childFromFirebase != null) {
                // Update the active child in FamilyDataHolder
                FamilyDataHolder.familyData.setActiveChild(childFromFirebase.childID)
                FamilyDataHolder.familyData.activeChild?.apply {
                    // Replace local data with fresh data
                    officialCalendar.clear()
                    officialCalendar.putAll(childFromFirebase.officialCalendar)
                    modifiedCalendar.clear()
                    modifiedCalendar.putAll(childFromFirebase.modifiedCalendar)
                }

                // Apply approved changes
                val activeChild = FamilyDataHolder.familyData.activeChild
                if (activeChild != null) {
                    DashboardActivity.CalendarChangeManager.applyAllApprovedChanges(activeChild)

                    // Optional: save to local cache
                    CalendarStorageUtils.saveLocally(this)

                    // Update calendar UI
                    val today = Calendar.getInstance()
                    params.year = today.get(Calendar.YEAR)
                    params.month = today.get(Calendar.MONTH) + 1
                    CalendarUIUtils.updateHeaderAndGrid(params)

                    // Update modify button visibility/click
                    setupModifyButton()
                }

            } else {
                Toast.makeText(this, "Failed to fetch latest child data", Toast.LENGTH_SHORT).show()
            }

            swipeRefreshLayout.isRefreshing = false  // stop spinner
        }
    }


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

        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout)
        syncCalendarButton = findViewById(R.id.sync)
        reviewChangesButton = findViewById(R.id.changes)

        //TODO
        swipeRefreshLayout.setOnRefreshListener {
            onRefresh()
        }

        // Optional: set refresh indicator colors
        swipeRefreshLayout.setColorSchemeResources(
            R.color.teal_200,
            R.color.purple_500,
            R.color.black
        )

        modifyButton.visibility = View.GONE

        activeChild = FamilyDataHolder.familyData.activeChild
        val activeChild = FamilyDataHolder.familyData.activeChild
        if (activeChild != null) {
            CalendarChangeManager.applyAllApprovedChanges(activeChild)
            FirebaseUtils.saveActiveChild()
        }

        setupModifyButton()

        // Set today's date
        val today = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault())
        todayTextView.text = "Today is ${dateFormat.format(today.time)}"

        // Sync Calendar Button
        syncCalendarButton.setOnClickListener {
            CalendarStorageUtils.syncAll()
            CalendarStorageUtils.saveLocally(this)
            recreate()
        }

        // Review Changes Button
        reviewChangesButton.setOnClickListener {
            startActivity(Intent(this, ReviewChangesActivity::class.java))
        }

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
            activeChild = FamilyDataHolder.familyData.activeChild,
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

    private fun hasPendingChangesToApprove(): Boolean {
        val currentParent = User.userData.childPermissions[FamilyDataHolder.familyData.activeChild?.childID] ?: return false
        val activeChild = FamilyDataHolder.familyData.activeChild ?: return false

        for ((_, months) in activeChild.officialCalendar) {
            for (month in months) {
                for (change in month.changes) {
                    if (change.isPending() && change.proposedByParent != currentParent) {
                        return true
                    }
                }
            }
        }
        return false
    }

    private fun setupModifyButton() {
        val child = FamilyDataHolder.familyData.activeChild
        val currentParent = User.userData.childPermissions[child?.childID]

        if (child != null && currentParent in listOf(0, 1)) {
            modifyButton.visibility = View.VISIBLE
            modifyButton.isEnabled = true
            modifyButton.setOnClickListener {
                if (hasPendingChangesToApprove()) {
                    // Redirect to review changes
                    startActivity(Intent(this, ReviewChangesActivity::class.java))
                } else {
                    // Redirect to editable calendar
                    startActivity(Intent(this, CalendarActivity::class.java))
                }
            }
        } else {
            modifyButton.visibility = View.GONE
            modifyButton.isEnabled = false
            modifyButton.setOnClickListener(null)
        }
    }


    object CalendarChangeManager {

        fun applyAllApprovedChanges(child: Child) {
            child.officialCalendar.values.flatten().forEach { month ->
                val list = month.changes.filter { it.status == ChangeStatus.APPROVED && !it.applied }
                list.forEach { change ->
                    month.applyChange(change)
                    change.applied = true
                }
                // Optionally remove deleted changes
                month.changes.removeAll { it.applied && it.isToBeDeleted()}
            }
        }
    }

}
