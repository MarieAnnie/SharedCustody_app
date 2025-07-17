package com.project.sharedcustodycalendar.utils

import android.content.Context
import android.util.Log

import com.project.sharedcustodycalendar.model.User
import com.project.sharedcustodycalendar.objects.Child
import com.project.sharedcustodycalendar.objects.FamilyDataHolder
import com.project.sharedcustodycalendar.objects.Month
import org.json.JSONArray
import org.json.JSONObject
import java.io.FileNotFoundException

object CalendarStorageUtils {
    private var USER_FILE_NAME = "user.json"
    private var CALENDAR_FILE_NAME = "calendar.json"

    fun saveLocally(context: Context) {
        saveUserDataLocally(context)
        saveChildrenLocally(context)
    }

    fun saveChildrenLocally(context: Context) {
        try {
            val json = FamilyDataHolder.familyData.toJson()
            context.openFileOutput(CALENDAR_FILE_NAME, Context.MODE_PRIVATE).use {
                it.write(json.toString().toByteArray())
                Log.w("SavedLocal", "saving data locally.")
            }
        } catch (e: Exception) {
            Log.e("SaveLocal", "Failed to save calendar data", e)
        }
    }

    fun saveUserDataLocally(context: Context) {
        try {
            val jsonString = User.toJson()
            context.openFileOutput(USER_FILE_NAME, Context.MODE_PRIVATE).use {
                it.write(jsonString.toByteArray())
                Log.d("SaveLocal", "User data saved successfully.")
            }
        } catch (e: Exception) {
            Log.e("SaveLocal", "Failed to save user data", e)
        }
    }

    fun loadLocally(context: Context) {
        loadUserPermissions(context)
        loadFamilyData(context)
        syncChildPermissionsWithFamilyData()
        saveLocally(context)
    }

    fun loadFromFirebaseAndCacheLocally(context: Context, onComplete: () -> Unit) {
        FirebaseUtils.loadUserPermissions(context) { permissions ->
            if (permissions == null) {
                Log.e("Firebase", "Failed to load user permissions.")
                onComplete()
                return@loadUserPermissions
            }

            User.userData.childPermissions = permissions
            val permissionIDs = permissions.keys.toSet()
            val loadedChildren = mutableListOf<Child>()
            val total = permissionIDs.size

            if (total == 0) {
                Log.w("Firebase", "No child permissions.")
                FamilyDataHolder.familyData.children.clear()
                saveLocally(context)
                onComplete()
                return@loadUserPermissions
            }

            var count = 0
            permissionIDs.forEach { childID ->
                FirebaseUtils.loadChild(childID) { child ->
                    count++
                    if (child != null) {
                        loadedChildren.add(child)
                        Log.d("Firebase", "Loaded child: ${child.childName}")
                    } else {
                        Log.w("Firebase", "Missing child: $childID")
                    }

                    if (count == total) {
                        FamilyDataHolder.familyData.children = loadedChildren
                        FamilyDataHolder.familyData.setActiveChild(loadedChildren.firstOrNull()?.childID ?: "")
                        saveLocally(context)
                        onComplete()
                    }
                }
            }
        }
    }


    fun loadUserPermissions(context: Context) {
        var loadFromFirebase = false
        try {
            val jsonString = context.openFileInput(USER_FILE_NAME)
                .bufferedReader()
                .use { it.readText() }
            val dataholder = User.fromJson(jsonString)
            val currentID = FirebaseUtils.getCurrentUserIDOrRedirect(context)
            if (dataholder.userID.isNotEmpty()) {
                if (dataholder.userID == currentID) {
                    User.userData = dataholder
                } else {
                    loadFromFirebase = true
                }
            }
        } catch (e: FileNotFoundException) {
            // File doesn't exist yet — maybe first launch
            Log.w("LoadLocal", "No local user file found. Loading from Firebase.")
            loadFromFirebase = true
            // Optional: initialize with default values if needed
        } catch (e: IllegalStateException) {
            Log.w("Auth", "User not logged in, redirected to login.", e)
            // If redirect happened, maybe stop further processing
            return
        } catch (e: Exception) {
            // Handle other issues like malformed JSON
            Log.e("LoadLocal", "Error loading user saved data", e)
        }
        FirebaseUtils.loadUserPermissions(context) { permissions ->
            if (permissions != null) {
                val localPermissions = User.userData.childPermissions

                // Compare Firebase permissions with local permissions
                if (loadFromFirebase) {
                    User.userData.childPermissions = permissions
                } else if (permissions != localPermissions) {
                    // They differ → update local cache and in-memory data
                    User.userData.childPermissions = permissions

                    Log.i("LoadPermissions", "Permissions updated from Firebase")

                    // Optionally, notify UI or trigger refresh here
                } else {
                    Log.i("LoadPermissions", "Local permissions are up to date")
                }
            } else {
                Log.e("Firebase", "Permissions not found.")
            }
        }
    }

    fun loadFamilyData(context: Context) {
        try {
            val jsonString = context.openFileInput(CALENDAR_FILE_NAME)
                .bufferedReader()
                .use { it.readText() }
            val json = JSONObject(jsonString)
            FamilyDataHolder.familyData.fromJson(json)
        } catch (e: FileNotFoundException) {
            // File doesn't exist yet — maybe first launch
            Log.w("LoadLocal", "No local data file found. Starting fresh.")
            // Optional: initialize with default values if needed
        } catch (e: Exception) {
            // Handle other issues like malformed JSON
            Log.e("LoadLocal", "Error loading saved data", e)
        }

    }

    fun syncChildPermissionsWithFamilyData() {
        val permissionIDs = User.userData.childPermissions.keys.toSet()
        val familyChildIDs = FamilyDataHolder.familyData.children.map { it.childID }.toSet()

        // Find children to remove (no permission)
        FamilyDataHolder.familyData.children.removeAll { it.childID !in permissionIDs }

        // 2. Download missing children from Firebase
        val toDownload = permissionIDs.subtract(familyChildIDs)
        toDownload.forEach { id ->
            FirebaseUtils.loadChild(id) { child ->
                if (child != null) {
                    Log.d("Firebase", "Loaded child: ${child.childName}")
                    FamilyDataHolder.familyData.children.add(child) // <-- Add to list
                    FamilyDataHolder.familyData.setActiveChild(child.childID)
                } else {
                    Log.e("Firebase", "Child not found.")
                }
            }
        }
    }

    fun mapToJson(years: Map<String, List<Month>>): JSONObject {
        val json = JSONObject()

        for ((yearKey, months) in years) {
            val monthArray = JSONArray()
            for (month in months) {
                monthArray.put(month.toJson()) // Assuming Month.toJson() returns JSONObject
            }
            json.put(yearKey, monthArray)
        }
        return json
    }

    fun JSONArray.toIntList(): List<Int> {
        val list = mutableListOf<Int>()
        for (i in 0 until this.length()) {
            list.add(this.getInt(i))
        }
        return list
    }

    inline fun <reified T> JSONArray.map(transform: (Any) -> T): List<T> {
        val list = mutableListOf<T>()
        for (i in 0 until this.length()) {
            list.add(transform(this.get(i)))
        }
        return list
    }

    fun JSONObject.toMonthMap(): Map<String, MutableList<Month>> {
        val map = mutableMapOf<String, MutableList<Month>>()
        val keys = keys()
        while (keys.hasNext()) {
            val key = keys.next()
            val monthArray = getJSONArray(key)
            val monthList = mutableListOf<Month>()
            for (i in 0 until monthArray.length()) {
                val monthJson = monthArray.getJSONObject(i)
                monthList.add(Month.fromJson(monthJson))
            }
            map[key] = monthList
        }
        return map
    }
}