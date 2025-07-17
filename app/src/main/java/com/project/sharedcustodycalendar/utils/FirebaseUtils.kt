package com.project.sharedcustodycalendar.utils

import android.content.Context
import android.content.Intent
import com.project.sharedcustodycalendar.objects.FamilyDataHolder
import com.project.sharedcustodycalendar.objects.Child

// Firebase core
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener


import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.GenericTypeIndicator
import com.project.sharedcustodycalendar.LoginActivity
import com.project.sharedcustodycalendar.model.User
import kotlin.collections.MutableMap


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

    fun saveUserPermission() {
        if (User.userData.userID.isNotEmpty()) {
            db.child("userPermissions").child(User.userData.userID).setValue(User.userData.childPermissions)
                .addOnSuccessListener {
                    Log.d("Firebase", "User permissions saved successfully")
                }
                .addOnFailureListener {
                    Log.e("Firebase", "Failed to save user permissions", it)
                }
        }
    }

    fun getCurrentUserIDOrRedirect(context: Context): String {
        val user = FirebaseAuth.getInstance().currentUser
        if (user != null) {
            return user.uid
        } else {
            val intent = Intent(context, LoginActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            context.startActivity(intent)
            throw IllegalStateException("User not logged in – redirected to login.")
        }
    }

    fun loadUserPermissions(context: Context, onComplete: (MutableMap<String, Int>?) -> Unit) {
        val userID = try {
            getCurrentUserIDOrRedirect(context)
        } catch (e: IllegalStateException) {
            // User not logged in, redirect already handled inside getCurrentUserIDOrRedirect
            onComplete(null)
            return
        }
        User.userData.userID = userID

        db.child("userPermissions").child(User.userData.userID).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val type = object : GenericTypeIndicator<MutableMap<String, Int>>() {}
                val permissions = dataSnapshot.getValue(type)
                onComplete(permissions)
            }
            override fun onCancelled(databaseError: DatabaseError) {
                onComplete(null)
            }
        })
    }

}