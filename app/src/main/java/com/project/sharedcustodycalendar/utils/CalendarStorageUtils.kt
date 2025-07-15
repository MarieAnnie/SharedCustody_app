package com.project.sharedcustodycalendar.utils

import android.content.Context
import android.util.Log
import com.project.sharedcustodycalendar.objects.FamilyDataHolder
import com.project.sharedcustodycalendar.objects.Month
import org.json.JSONArray
import org.json.JSONObject
import java.io.FileNotFoundException

object CalendarStorageUtils {
    fun saveLocally(context: Context) {
        val json = FamilyDataHolder.familyData.toJson()
        context.openFileOutput("calendar.json", Context.MODE_PRIVATE).use {
            it.write(json.toString().toByteArray())
            Log.w("SavedLocal", "saving data locally.")
        }
    }

    fun loadLocally(context: Context) {
        try {
            val jsonString = context.openFileInput("calendar.json")
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