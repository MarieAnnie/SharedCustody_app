package com.project.sharedcustodycalendar

import android.content.Intent
import android.graphics.Color
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import java.text.SimpleDateFormat
import java.util.*
import android.os.Bundle
import android.widget.Button
import android.widget.Toast

import androidx.appcompat.app.AppCompatActivity
import com.project.sharedcustodycalendar.model.User
import com.project.sharedcustodycalendar.objects.Child
import com.project.sharedcustodycalendar.objects.FamilyDataHolder
import com.project.sharedcustodycalendar.objects.PendingChanges
import com.project.sharedcustodycalendar.utils.CalendarStorageUtils
import com.project.sharedcustodycalendar.utils.FirebaseUtils
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.iterator


class ReviewChangesActivity : AppCompatActivity() {

    private lateinit var changesContainer : LinearLayout
    private lateinit var activeChild : Child
    private lateinit var parentColors : Map<Int, Int>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reviewchanges)
        changesContainer = findViewById<LinearLayout>(R.id.changesContainer)

        val backToDashboardButton = findViewById<Button>(R.id.backToDashboardButton)
        val approveAllButton = findViewById<Button>(R.id.approveAllButton)
        val saveButton = findViewById<Button>(R.id.save)

        backToDashboardButton.setOnClickListener {
            startActivity(Intent(this, DashboardActivity::class.java))
        }


        activeChild = FamilyDataHolder.familyData.activeChild!!
        val currentParent = User.userData.childPermissions[activeChild?.childID]
        var changes = mutableListOf<PendingChanges>()

        parentColors = mapOf(
            1 to (activeChild?.parents[0]?.color ?: Color.parseColor("#FFFFFF")),
            2 to (activeChild?.parents[1]?.color ?: Color.parseColor("#FFFFFF"))
        ) as Map<Int, Int>

        if (activeChild == null) {
            // TODO handle null case
        } else {
            changes = getAllPendingChanges(activeChild)
        }

        // Split into categories

        val myChangeDecisions = changes.filter {
            (it.isApproved() || it.isRejected())
        }

        val changesToApprove = changes.filter {
            it.isPending() && it.proposedByParent != currentParent
        }

        val myPendingChanges = changes.filter {
            it.isPending() && it.proposedByParent == currentParent
        }

        // Add sections
        addMyDecisionsSection("My Change Decisions", myChangeDecisions)
        addToApproveSection("Changes to Approve", changesToApprove)
        addPendingChangesSection("My Pending Changes", myPendingChanges)

        //Save:
        saveButton.setOnClickListener {
            for (year in activeChild.years) {
                for (month in year.value) {
                    month.applyChanges()
                }
            }
            FirebaseUtils.saveActiveChild()
            CalendarStorageUtils.saveLocally(this)
            startActivity(Intent(this, DashboardActivity::class.java))
        }

        // Accept all:
        approveAllButton.setOnClickListener {
            for (change in changesToApprove) {
                if (change.isPending()){
                    change.approveChange()
                }
            }
            for (change in myChangeDecisions) {
                if (change.isApproved() || change.isRejected()){
                    change.toBeDeleted()
                }
            }
        }
    }
    // Function to create section

    fun getAllPendingChanges(activeChild: Child): MutableList<PendingChanges> {
        val changes = mutableListOf<PendingChanges>()
        for ((_, months) in activeChild.years) {
            for (month in months) {
                for (change in month.changes) {
                    if (change.forCurrentParent()) {
                        changes.add(change)
                    }
                }
            }
        }
        return changes
    }

    fun addMyDecisionsSection (
        title: String,
        items: List<PendingChanges>
    ) {
        val titleView = createTitle(title)
        changesContainer.addView(titleView)

        items.forEach { change ->
            val backgroundColor = if (change.isApproved()) {
                Color.parseColor("#4CAF50")
            } else if (change.isRejected()) {
                Color.parseColor("#F44336")
            } else {
                Color.WHITE
            }

            val cardLayout = createCard(backgroundColor)
            val requestDateText = createRequestDateText(change)
            val mainText = createMainTextDate(change)

            // "Read" button
            val readButton = createButton("Read", "#9C27B0") {
                change.toBeDeleted()
                Toast.makeText(this, "Acknowledged", Toast.LENGTH_SHORT).show()
                changesContainer.removeView(cardLayout)
            }

            // Add views
            cardLayout.addView(requestDateText)
            cardLayout.addView(mainText)
            cardLayout.addView(readButton)

            changesContainer.addView(cardLayout)
        }
    }

    // Function to create section
    fun addToApproveSection(
        title: String,
        items: List<PendingChanges>
    ) {
        val titleView = createTitle(title)
        changesContainer.addView(titleView)


        items.forEach { change ->
            val backgroundColor = parentColors[change.newParent] ?: Color.LTGRAY
            val cardLayout = createCard(backgroundColor)
            val requestDateText = createRequestDateText(change)
            val mainText = createMainTextDate(change)

            // Button row (horizontal layout)
            val buttonRow = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.END
                setPadding(0, 16, 0, 0)
            }

            // Approve button
            val approveButton = createButton("✔ Approve", "#4CAF50") {
                change.approveChange()
                Toast.makeText(this, "Approved", Toast.LENGTH_SHORT).show()
                changesContainer.removeView(cardLayout)
            }


            // Reject button
            val rejectButton = createButton("✖ Reject", "#F44336") {
                change.rejectChange()
                Toast.makeText(this, "Rejected", Toast.LENGTH_SHORT).show()
                changesContainer.removeView(cardLayout)
            }

            buttonRow.addView(approveButton)
            buttonRow.addView(rejectButton)

            cardLayout.addView(requestDateText)
            cardLayout.addView(mainText)
            cardLayout.addView(buttonRow)

            changesContainer.addView(cardLayout)
        }
    }

    fun addPendingChangesSection (
        title: String,
        items: List<PendingChanges>
    ) {
        val titleView = createTitle(title)
        changesContainer.addView(titleView)

        items.forEach { change ->
            val backgroundColor = parentColors[change.newParent] ?: Color.LTGRAY
            val cardLayout = createCard(backgroundColor)
            val requestDateText = createRequestDateText(change)
            val mainText = createMainTextDate(change)

            // Reject button
            val rejectButton = createButton("✖ Cancel", "#F44336") {
                change.toBeDeleted()
                Toast.makeText(this, "Delete request", Toast.LENGTH_SHORT).show()
                changesContainer.removeView(cardLayout)
            }

            // Add views
            cardLayout.addView(requestDateText)
            cardLayout.addView(mainText)
            cardLayout.addView(rejectButton)

            changesContainer.addView(cardLayout)
        }
    }

    fun createTitle(title: String): TextView {
        val titleView = TextView(this).apply {
            text = title
            textSize = 18f
            setPadding(0, 16, 0, 8)
        }
        return titleView
    }

    fun createCard(color: Int) : LinearLayout{
        val cardLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16, 16, 16, 16)
            setBackgroundColor(color)

            // LayoutParams for margins if needed
            val layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 0, 0, 32) // bottom margin between cards
            }
            this.layoutParams = layoutParams
        }
        return cardLayout
    }

    fun createRequestDateText(change: PendingChanges): TextView {
        val formatter = SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault())

        val requestDateText = TextView(this).apply {
            text = "Requested on: ${formatter.format(Date(change.timsStamp))}"
            setTextColor(Color.GRAY)
            textSize = 12f
            gravity = Gravity.END
        }
        return requestDateText
    }

    fun createMainTextDate(change: PendingChanges): TextView {
        val mainText = TextView(this).apply {
            text =
                "Date: ${change.night}/${change.monthId}/${change.year}".trimIndent()
            setTextColor(Color.BLACK)
            textSize = 16f
        }
        return mainText
    }

    fun createButton(text: String, bgColor: String, onClick: () -> Unit): Button {
        return Button(this).apply {
            this.text = text
            setBackgroundColor(Color.parseColor(bgColor))
            setTextColor(Color.WHITE)
            setOnClickListener {
                onClick()
            }
        }
    }
}


