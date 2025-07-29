package com.project.sharedcustodycalendar.objects

import org.json.JSONObject

import com.project.sharedcustodycalendar.utils.CalendarStorageUtils.toIntList
import org.json.JSONArray

data class Month(
    var monthId: Int = 0,
    var starting_parent: Int = 0,
    var parent0_nights: MutableList<Int>,
    var changes : MutableList<pendingChanges> = mutableListOf<pendingChanges>()
) {
    // for Firebase
    constructor() : this(0, 0, mutableListOf(), mutableListOf())

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

    fun addChange(change: pendingChanges) {
        changes.add(change)
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

    fun deepCopy(): Month {
        return Month(
            monthId = this.monthId,
            starting_parent = this.starting_parent,
            parent0_nights = this.parent0_nights.toMutableList(),
            changes = this.changes.map { it.copy() }.toMutableList()
        )
    }

    fun resolvePendingChanges()  {
        val result = mutableListOf<pendingChanges>()

        // Group all changes by night (inside a single month)
        val grouped = changes.groupBy { it.night }

        for ((_, sameNightChanges) in grouped) {
            // If any approved, skip this night entirely (already reflected in calendar)
            if (sameNightChanges.any { it.status == "approved" }) {
                continue
            }

            val pending = sameNightChanges.filter { it.status == "pending" }
            val rejected = sameNightChanges.filter { it.status == "rejected" }

            when {
                pending.size >= 2 -> {
                    // Both parents proposed same night → keep the latest
                    val latest = pending.maxByOrNull { it.timsStamp }!!
                    result.add(latest.copy(status = "pending"))
                }
                pending.size == 1 && rejected.isNotEmpty() -> {
                    // One parent proposed, other rejected → keep the pending
                    result.add(pending[0])
                }
                pending.size == 1 -> {
                    result.add(pending[0])
                }
                rejected.size > 0  -> {
                    val latest = rejected.maxByOrNull { it.timsStamp }!!
                    result.add(latest)
                }
            }
        }
        changes = result
    }


}