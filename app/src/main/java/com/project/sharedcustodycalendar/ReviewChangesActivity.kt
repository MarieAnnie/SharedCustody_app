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

    private lateinit var changesContainer: LinearLayout
    private lateinit var activeChild: Child
    private lateinit var parentColors: Map<Int, Int>
    private var currentParent: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reviewchanges)
        changesContainer = findViewById(R.id.changesContainer)

        val backToDashboardButton = findViewById<Button>(R.id.backToDashboardButton)
        val approveAllButton = findViewById<Button>(R.id.approveAllButton)
        val saveButton = findViewById<Button>(R.id.save)

        backToDashboardButton.setOnClickListener {
            startActivity(Intent(this, DashboardActivity::class.java))
        }

        activeChild = FamilyDataHolder.familyData.activeChild!!
        currentParent = User.userData.childPermissions[activeChild.childID] ?: -1

        parentColors = mapOf(
            1 to Color.parseColor(activeChild.parents[0].color),
            2 to Color.parseColor(activeChild.parents[1].color)
        )

        val changes = getAllPendingChanges(activeChild)

        // Split into categories
        val myDecisions = changes.filter { it.isApproved() || it.isRejected() }
        val toApprove = changes.filter { it.isPending() && it.proposedByParent != currentParent }
        val myPending = changes.filter { it.isPending() && it.proposedByParent == currentParent }

        if (myDecisions.isEmpty() && toApprove.isEmpty() && myPending.isEmpty()) {
            val upToDateView = TextView(this).apply {
                text = "You are up to date!"
                setTextColor(Color.GRAY)
                textSize = 16f
                gravity = Gravity.CENTER
                setPadding(0, 32, 0, 32)
            }
            changesContainer.addView(upToDateView)
        } else {
            addSection("My Change Decisions", myDecisions, SectionType.MY_DECISIONS)
            addSection("Changes to Approve", toApprove, SectionType.TO_APPROVE)
            addSection("My Pending Changes", myPending, SectionType.MY_PENDING)
        }

        // Save button
        saveButton.setOnClickListener {
            activeChild.officialCalendar.values.flatten().forEach { month -> month.applyChanges() }
            FirebaseUtils.saveActiveChild()
            CalendarStorageUtils.saveLocally(this)
            startActivity(Intent(this, DashboardActivity::class.java))
        }

        // Approve all button
        approveAllButton.setOnClickListener {
            toApprove.forEach { if (it.isPending()) it.approveChange() }
            myDecisions.forEach { if (it.isApproved() || it.isRejected()) it.toBeDeleted() }
            FirebaseUtils.saveActiveChild()
            Toast.makeText(this, "All changes processed", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getAllPendingChanges(child: Child): List<PendingChanges> {
        return child.officialCalendar.values.flatten()
            .flatMap { it.changes }
            .filter { it.forCurrentParent() }
    }

    private enum class SectionType { MY_DECISIONS, TO_APPROVE, MY_PENDING }

    private fun addSection(title: String, items: List<PendingChanges>, type: SectionType) {
        if (items.isEmpty()) return

        changesContainer.addView(createTitle(title))

        items.forEach { change ->
            val backgroundColor = when (type) {
                SectionType.MY_DECISIONS -> when {
                    change.isApproved() -> Color.parseColor("#4CAF50")
                    change.isRejected() -> Color.parseColor("#F44336")
                    else -> Color.WHITE
                }
                SectionType.TO_APPROVE, SectionType.MY_PENDING -> parentColors[change.newParent] ?: Color.LTGRAY
            }

            val card = createCard(backgroundColor)
            card.addView(createRequestDateText(change))
            card.addView(createMainTextDate(change))

            // Add buttons
            val buttonRow = when (type) {
                SectionType.MY_DECISIONS -> createButtonRow(listOf(
                    createButton("Comfirm", "#9C27B0") {
                        change.toBeDeleted()
                        Toast.makeText(this, "Acknowledged", Toast.LENGTH_SHORT).show()
                        changesContainer.removeView(card)
                    }
                ))
                SectionType.TO_APPROVE -> createButtonRow(listOf(
                    createButton("✔ Approve", "#4CAF50") {
                        change.approveChange()
                        Toast.makeText(this, "Approved", Toast.LENGTH_SHORT).show()
                        changesContainer.removeView(card)
                    },
                    createButton("✖ Reject", "#F44336") {
                        change.rejectChange()
                        Toast.makeText(this, "Rejected", Toast.LENGTH_SHORT).show()
                        changesContainer.removeView(card)
                    }
                ))
                SectionType.MY_PENDING -> createButtonRow(listOf(
                    createButton("✖ Cancel", "#F44336") {
                        change.toBeDeleted()
                        Toast.makeText(this, "Deleted request", Toast.LENGTH_SHORT).show()
                        changesContainer.removeView(card)
                    }
                ))
            }

            card.addView(buttonRow)
            changesContainer.addView(card)
        }
    }

    private fun createButtonRow(buttons: List<Button>): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.END
            setPadding(0, 16, 0, 0)
            buttons.forEach { addView(it) }
        }
    }

    private fun createTitle(title: String) = TextView(this).apply {
        text = title
        textSize = 18f
        setPadding(0, 16, 0, 8)
    }

    private fun createCard(color: Int) = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(16, 16, 16, 16)
        setBackgroundColor(color)
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { setMargins(0, 0, 0, 32) }
    }

    private fun createRequestDateText(change: PendingChanges) = TextView(this).apply {
        val formatter = SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault())
        text = "Requested on: ${formatter.format(Date(change.timeStamp))}"
        setTextColor(Color.GRAY)
        textSize = 12f
        gravity = Gravity.END
    }

    private fun createMainTextDate(change: PendingChanges) = TextView(this).apply {
        text = "Date: ${change.night}/${change.monthId}/${change.year}"
        setTextColor(Color.BLACK)
        textSize = 16f
    }

    private fun createButton(text: String, bgColor: String, onClick: () -> Unit) = Button(this).apply {
        this.text = text
        setBackgroundColor(Color.parseColor(bgColor))
        setTextColor(Color.WHITE)
        setOnClickListener { onClick() }
    }
}



