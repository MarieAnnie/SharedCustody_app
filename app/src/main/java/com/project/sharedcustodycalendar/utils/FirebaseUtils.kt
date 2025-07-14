package com.project.sharedcustodycalendar.utils

import com.project.sharedcustodycalendar.objects.FamilyDataHolder
import com.project.sharedcustodycalendar.objects.Child

// Firebase core
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.android.gms.tasks.OnFailureListener

import android.util.Log


object FirebaseUtils {

    val db = FirebaseDatabase.getInstance().reference

    fun saveChildren(){
        val children = FamilyDataHolder.familyData.children
        for (child in children) {
            db.child("children").child(child.childID).setValue(child)
                .addOnSuccessListener {
                    Log.d("Firebase", "Child saved successfully")
                }
                .addOnFailureListener {
                    Log.e("Firebase", "Failed to save child", it)
                }
        }
    }

    fun saveActiveChild() {
        val activeChild = FamilyDataHolder.familyData.activeChild
        if (activeChild != null) {
            db.child("children").child(activeChild.childID).setValue(activeChild)
                .addOnSuccessListener {
                    Log.d("Firebase", "Child saved successfully")
                }
                .addOnFailureListener {
                    Log.e("Firebase", "Failed to save child", it)
                }
        }
    }

    fun loadChild(childID: String, onComplete: (Child?) -> Unit) {
        db.child("children").child(childID).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val child = dataSnapshot.getValue(Child::class.java)
                onComplete(child)
            }
            override fun onCancelled(databaseError: DatabaseError) {
                onComplete(null)
            }
        })
    }

}