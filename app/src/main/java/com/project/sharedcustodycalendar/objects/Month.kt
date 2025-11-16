package com.project.sharedcustodycalendar.objects

import org.json.JSONObject

import com.project.sharedcustodycalendar.utils.CalendarStorageUtils.toIntList
import org.json.JSONArray

data class Month(
    var monthId: Int = 0,
    var starting_parent: Int = 0,
    var parent0_nights: MutableList<Int>,
    var changes : MutableList<PendingChanges> = mutableListOf<PendingChanges>()
) {
    // for Firebase
    constructor() : this(0, 0, mutableListOf(), mutableListOf())

    fun toJson(): JSONObject{
        val json = JSONObject()
        json.put("monthId", monthId)
        json.put("starting_parent", starting_parent)
        json.put("parent0_nights", JSONArray(parent0_nights))

        val changesArray = JSONArray()
        for (change in changes) {
            changesArray.put(change.toJson())  // Assuming PendingChanges has toJson()
        }
        json.put("changes", changesArray)

        return json
    }

    companion object {
        fun fromJson(json: JSONObject): Month {
            val month = Month(
                monthId = json.getInt("monthId"),
                starting_parent = json.getInt("starting_parent"),
                parent0_nights = json.getJSONArray("parent0_nights").toIntList().toMutableList()
            )

            val changesArray = json.optJSONArray("changes")
            if (changesArray != null) {
                for (i in 0 until changesArray.length()) {
                    month.changes.add(PendingChanges.fromJson(changesArray.getJSONObject(i)))
                }
            }

            return month
        }
    }

    fun addChange(change: PendingChanges) {
        if (!changes.any { it.night == change.night && it.proposedByParent == change.proposedByParent && it.status == ChangeStatus.PENDING }) {
            changes.add(change)
        }
    }

    fun updateParent0Nights(day: Int, newParent: Int = -1): Int {
        if (newParent == -1) return -1

        if (newParent == 0) {
            if (!parent0_nights.contains(day)) parent0_nights.add(day)
        } else {
            parent0_nights.remove(day)
        }

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

    fun hasPendingChangeFor(day: Int, parent: Int): Boolean {
        return changes.any { it.night == day && it.proposedByParent == parent && it.isPending() }
    }

    fun resolvePendingChanges()  {
        val result = mutableListOf<PendingChanges>()

        // Group all changes by night (inside a single month)
        val grouped = changes.groupBy { it.night }

        for ((_, sameNightChanges) in grouped) {
            // If any approved, skip this night entirely (already reflected in calendar)
            if (sameNightChanges.any { it.isApproved() }) {
                continue
            }

            val pending = sameNightChanges.filter { it.isPending() }
            val rejected = sameNightChanges.filter { it.isRejected()}

            when {
                pending.size >= 2 -> {
                    // Both parents proposed same night → keep the latest
                    val latest = pending.maxByOrNull { it.timeStamp }!!
                    result.add(latest.copy(status = ChangeStatus.PENDING))
                }
                pending.size == 1 && rejected.isNotEmpty() -> {
                    // One parent proposed, other rejected → keep the pending
                    result.add(pending[0])
                }
                pending.size == 1 -> {
                    result.add(pending[0])
                }
                rejected.size > 0  -> {
                    val latest = rejected.maxByOrNull { it.timeStamp }!!
                    result.add(latest)
                }
            }
        }
        changes = result
    }

    fun applyChanges() {
        changes = changes.filterNot { it.isToBeDeleted() }.toMutableList()
    }
}