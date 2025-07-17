package com.project.sharedcustodycalendar

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.project.sharedcustodycalendar.model.User

import com.project.sharedcustodycalendar.objects.FamilyDataHolder
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
                    permission = 1
                }
                else {
                    viewerID = IDEncoder.encodeViewerID(idInput)
                    permission = 0
                }

                FirebaseUtils.loadChild(childID) { child ->
                    if (child != null) {
                        Log.d("Firebase", "Loaded child: ${child.childName}")
                        FamilyDataHolder.familyData.setActiveChild(child.childID)
                        User.userData.childPermissions[childID] = permission
                        FirebaseUtils.saveUserPermission()
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
}
