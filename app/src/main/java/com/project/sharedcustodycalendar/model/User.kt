package com.project.sharedcustodycalendar.model

import com.google.firebase.auth.FirebaseAuth
import android.util.Log
import com.google.gson.Gson

object User {

    data class UserData(
        var userID: String = "",
        var childPermissions: MutableMap<String, Int> = mutableMapOf()
    )

    var userData = UserData()

    fun addChildPermission(childID: String, permission: Int) {
        userData.childPermissions[childID] = permission
    }

    fun getChildPermission(childID: String): Int {
        return userData.childPermissions[childID] ?: -1
    }

    fun setUserID() {
        val user = FirebaseAuth.getInstance().currentUser
        if (user != null) {
            userData.userID = user.uid
            Log.d("Auth", "Current user ID: ${userData.userID}")
        } else {
            Log.d("Auth", "No user is signed in")
        }
    }

    fun toJson(): String {
        return Gson().toJson(userData)
    }

    fun fromJson(json: String) : UserData{
        val parsed = Gson().fromJson(json, UserData::class.java)
        val temp_userData = parsed ?: UserData()
        return temp_userData
    }

}
