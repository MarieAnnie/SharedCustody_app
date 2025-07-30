package com.project.sharedcustodycalendar

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.project.sharedcustodycalendar.model.User
import com.project.sharedcustodycalendar.objects.Child

import com.project.sharedcustodycalendar.objects.FamilyDataHolder
import com.project.sharedcustodycalendar.utils.CalendarStorageUtils
import com.project.sharedcustodycalendar.utils.FirebaseUtils
import com.project.sharedcustodycalendar.utils.IDEncoder

class ChildIdActivity :  AppCompatActivity() {

    private lateinit var childIdField: EditText
    private lateinit var childIdButton: Button
    private lateinit var childNameField: EditText
    private lateinit var createNewCalendarButton : Button
    private var permission :Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_child_id)

        childIdField = findViewById(R.id.child_id_input)
        childIdButton = findViewById(R.id.child_id_button)
        childNameField = findViewById(R.id.new_calendar_input)
        createNewCalendarButton = findViewById(R.id.new_calendar_button)

        childIdButton .setOnClickListener {
            val idInput =  childIdField.text.toString().trim()
            if (idInput.isNotEmpty()) {
                var childID = idInput
                var viewerID = idInput

                if(idInput.startsWith(IDEncoder.VIEWER_PREFIX)){
                    childID = IDEncoder.decodeID(idInput).toString()
                    permission = 2
                }
                else {
                    viewerID = IDEncoder.encodeViewerID(idInput)
                    permission = -1
                }

                FirebaseUtils.loadChild(childID) { child ->
                    if (child != null) {
                        Log.d("Firebase", "Loaded child: ${child.childName}")

                        // Add child to the family list if not already there
                        val existing = FamilyDataHolder.familyData.children.find { it.childID == child.childID }
                        if (existing == null) {
                            FamilyDataHolder.familyData.children.add(child)
                        }

                        // Set active child
                        FamilyDataHolder.familyData.setActiveChild(child.childID)

                        if (permission == -1) {
                            whichParentWindow(child) { selectedPermission ->
                                permission = selectedPermission

                                User.addChildPermission(childID, permission)
                                CalendarStorageUtils.saveLocally(this)
                                FirebaseUtils.saveUserPermission()

                                startActivity(Intent(this, DashboardActivity::class.java))
                            }
                        } else {
                            // permission is already set (e.g., 2), proceed directly
                            User.addChildPermission(childID, permission)
                            CalendarStorageUtils.saveLocally(this)
                            FirebaseUtils.saveUserPermission()

                            startActivity(Intent(this, DashboardActivity::class.java))
                        }
                    } else {
                        Log.e("Firebase", "Child not found.")
                    }
                }
            } else {
                Toast.makeText(this, "Please enter a Child ID.", Toast.LENGTH_SHORT).show()
            }
        }

        createNewCalendarButton.setOnClickListener {
            val childName = childNameField.text.toString().trim()
            FamilyDataHolder.familyData.createNewChild(childName)

            if (childName.isNotEmpty()) {
                Toast.makeText(this, "New calendar being created for $childName...", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, SetParentsActivity::class.java))

            } else {
                Toast.makeText(this, "Please enter a child Name.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun whichParentWindow(child: Child, onParentChosen: (Int) -> Unit) {
        val parent0Name = child.parents.getOrNull(0)?.name ?: "Parent 1"
        val parent1Name = child.parents.getOrNull(1)?.name ?: "Parent 2"

        AlertDialog.Builder(this)
            .setTitle("Who are you?")
            .setMessage("Select your name:")
            .setPositiveButton(parent0Name) { _, _ -> onParentChosen(0) }
            .setNegativeButton(parent1Name) { _, _ ->
                onParentChosen(1)
                FamilyDataHolder.familyData.activeChild?.parentHasConfirmed()
            }
            .setCancelable(false)
            .show()
    }

}
