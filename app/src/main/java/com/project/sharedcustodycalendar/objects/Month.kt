package com.project.sharedcustodycalendar.objects

import java.time.LocalDate

class Month(
    val monthId: Int,
    val year: Int
) {
    val nbDaysInMonth : Int = LocalDate.of(year,monthId,1).lengthOfMonth()
    val days: MutableMap<Int, Day> = mutableMapOf()

    val startParentID: Int
        get() = days[1]?.startingParentID?: UNKNOWN_PARENT

    companion object {
        const val UNKNOWN_PARENT = -1
    }

    fun addDayFromFirebase(day: Day) {
        val dayNumber = day.date.dayOfMonth

        if (dayNumber !in 1..nbDaysInMonth) return

        // Optional: detect overwrite
        //if (days[index].startingParentID != UNKNOWN_PARENT) {
            // already filled → possible duplicate from Firebase
        //}

        days[dayNumber] = day
    }

    fun isComplete(): Boolean {
        if (days.size != nbDaysInMonth) return false

        return (1..nbDaysInMonth).all { it in days }
    }


    fun addChange(change: PendingChange) {
        val dayOfTheMonth = change.date.dayOfMonth
        val day = days[dayOfTheMonth]

        if (day){
            if(checkIfChangeExists(change, day)){
                changes.add()
            }
        }

        if (!changes.any { it.night == change.night && it.proposedByParent == change.proposedByParent && it.status == ChangeStatus.PENDING }) {
            changes.add(change)
        }
    }

    fun checkIfChangeExists(change: PendingChange, day : Day) : Boolean {
        return (day.pendingChanges.any { it.time == change.time})
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
        val result = mutableListOf<PendingChange>()

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

    fun applyChange(change: PendingChange){
        var night = change.night
        var newParent = change.newParent
        if (newParent == 0){
            parent0_nights.add(night)
            parent0_nights.sort()
        } else{
            parent0_nights.remove(night)

        }
        change.applied = true
    }

    fun removeChangesFromDayInclusive(day: Int) {
        changes = changes.filter { it.night < day }.toMutableList()
    }
}