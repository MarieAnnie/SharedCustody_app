package com.project.sharedcustodycalendar.objects

import org.json.JSONObject

import com.project.sharedcustodycalendar.utils.CalendarStorageUtils.toIntList
import org.json.JSONArray

data class Month(
    var monthId: Int = 0,
    var starting_parent: Int = 0,
    var parent0_nights: MutableList<Int>
) {
    // for Firebase
    constructor() : this(0, 0, mutableListOf())

    fun toJson(): JSONObject{
        val json = JSONObject()
        json.put("monthId", monthId)
        json.put("starting_parent", starting_parent)
        json.put("parent0_nights", JSONArray(parent0_nights))
        return json
    }

    companion object {
        fun fromJson(json: JSONObject): Month {
            return Month(
                monthId = json.getInt("monthId"),
                starting_parent = json.getInt("starting_parent"),
                parent0_nights = json.getJSONArray("parent0_nights").toIntList() as MutableList<Int>
            )
        }
    }


    fun updateParent0Nights(day:Int, newParent: Int =-1) : Int {
        if (newParent != -1) {
            if (parent0_nights.contains(day)) {
                parent0_nights.remove(day)
            } else {
                parent0_nights.add(day)
            }
        }
        else {
            if (newParent == 0) {
                parent0_nights.add(day)
            } else {
                parent0_nights.remove(day)
            }
        }
        // Keep the list sorted
        parent0_nights.sort()

        return if (parent0_nights.contains(day)) 0 else 1

    }

    fun updateStartingParent(newStartingParent: Int=-1) {
        if (newStartingParent != -1) {
            starting_parent = newStartingParent
        } else {
            if (starting_parent == 0) {
                starting_parent = 1
            } else {
                starting_parent = 0
            }
        }
    }

}