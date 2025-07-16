package com.project.sharedcustodycalendar

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.telecom.Connection.RttModifyStatus
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.project.sharedcustodycalendar.objects.FamilyDataHolder
import com.project.sharedcustodycalendar.utils.CalendarStorageUtils
import java.text.SimpleDateFormat
import java.util.*

class DashboardActivity : AppCompatActivity() {

    private var currentChildIndex = 0

    private lateinit var calendarView: CalendarView
    private lateinit var todayTextView: TextView
    private lateinit var childrenList: LinearLayout
    private lateinit var addChildButton: Button
    private lateinit var modifyButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        // Bind views
        todayTextView = findViewById(R.id.todayTextView)
        calendarView = findViewById(R.id.calendarView)
        childrenList = findViewById(R.id.childrenList)
        addChildButton = findViewById(R.id.addChildButton)
        modifyButton = findViewById(R.id.modifyButton)

        modifyButton.setOnClickListener {
            startActivity(Intent(this, CalendarActivity::class.java))
        }

        //load data
        CalendarStorageUtils.loadLocally(this)

        // Set today's date
        val today = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault())
        todayTextView.text = "Today is ${dateFormat.format(today.time)}"
        calendarView.date = today.timeInMillis

        // Handle add button
        addChildButton.setOnClickListener {
            startActivity(Intent(this, ChildIdActivity::class.java))
        }

        // Show the first child
        showCurrentChild()
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
                showChildSelector()
            }
        }
        childrenList.addView(childView)
    }


    private fun showChildSelector() {
        val family = FamilyDataHolder.familyData
        val allChildren = family?.children ?: emptyList()

        if (allChildren.isEmpty()) {
            Toast.makeText(this, "No children available. Please add a child first.", Toast.LENGTH_SHORT).show()
            return
        }

        val names = allChildren.map { it.childName }.toTypedArray()

        AlertDialog.Builder(this)
            .setTitle("Select a child")
            .setItems(names) { _, which ->
                val selectedChild = allChildren[which]
                family.setActiveChild(selectedChild.childID)
                showCurrentChild()
            }
            .show()
    }


}
