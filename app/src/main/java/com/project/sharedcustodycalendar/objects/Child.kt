package com.project.sharedcustodycalendar.objects

import android.util.Log
import com.project.sharedcustodycalendar.model.User
import com.project.sharedcustodycalendar.utils.FirebaseUtils
import com.project.sharedcustodycalendar.utils.GenerateCalendar
import org.json.JSONArray
import org.json.JSONObject

import com.project.sharedcustodycalendar.utils.CalendarStorageUtils.toIntList
import com.project.sharedcustodycalendar.utils.CalendarStorageUtils.mapToJson
import com.project.sharedcustodycalendar.utils.CalendarStorageUtils.map
import com.project.sharedcustodycalendar.utils.CalendarStorageUtils.toMonthMap
import com.project.sharedcustodycalendar.utils.IDEncoder
import java.util.Calendar

data class Child(
    var childName: String = "",
    var childID: String = "",
    var parents: List<Parent> = emptyList(),
    var schedulePattern: List<Int> = emptyList(),
    var hour_parent_switch: String = "08:00",
    var officialCalendar : MutableMap<String, MutableList<Month>> = mutableMapOf(),
    var modifiedCalendar : MutableMap<String, MutableList<Month>> = mutableMapOf(),
    var parentConfirmed: Boolean = false
) {

    fun toJson(): JSONObject {
        val json = JSONObject()
        json.put("childName", childName)
        json.put("childID", childID)
        json.put("parents", JSONArray(parents.map { it.toJson() }))
        json.put("schedulePattern", JSONArray(schedulePattern))
        json.put("hour_parent_switch", hour_parent_switch)
        json.put("officialCalendar", mapToJson(officialCalendar))
        return json
    }

    fun fromJson(json: JSONObject) {
        childName = json.getString("childName")
        childID = json.getString("childID")
        parents = json.getJSONArray("parents").map { Parent.fromJson(it as JSONObject) }
        schedulePattern = json.getJSONArray("schedulePattern").toIntList()
        hour_parent_switch = json.getString("hour_parent_switch")
        officialCalendar = json.getJSONObject("officialCalendar").toMonthMap().toMutableMap()
    }

    fun setOrUpdateMonth(year: String, newMonth: Month) {
        val yearMonths = officialCalendar.getOrPut(year) { mutableListOf() }

        val existingIndex = yearMonths.indexOfFirst { it.monthId == newMonth.monthId }
        if (existingIndex != -1) {
            // Overwrite existing month
            yearMonths[existingIndex] = newMonth
        } else {
            // Add new month
            yearMonths.add(newMonth)
        }

        officialCalendar[year] = yearMonths
    }

    fun getMonthIdsForYear(year: String): List<Int> {
        return officialCalendar[year]?.map { it.monthId } ?: emptyList()
    }

    fun changeParentNight(year: String, monthIdx: Int, day: Int, newParent: Int = -1) {
        val month = modifiedCalendar[year]?.find { it.monthId == monthIdx }
        var updatedParent = month?.updateParent0Nights(day, newParent)
        if (updatedParent == null) {
            updatedParent = 0
        }

        val calendar = Calendar.getInstance()
        calendar.set(year.toInt(), monthIdx - 1, 1) // month is 0-based
        val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)

        if (day == daysInMonth) {
            if (monthIdx == 12) {
                val nextYear = (year.toInt() + 1).toString()
                val nextMonth = modifiedCalendar[nextYear]?.find { it.monthId == 1 }
                if (nextMonth != null) {
                    nextMonth.updateParent0Nights(day, newParent)
                } else {
                    initializeCalendar(nextYear.toInt(), 1, updatedParent)
                }
            }else {
                val nextMonth = modifiedCalendar[year]?.find { it.monthId == monthIdx +1 }
                nextMonth?.updateStartingParent(newParent)
            }
        }
    }

    fun getStartingParent(calendar : MutableMap<String, MutableList<Month>>, year: String, monthIdx: Int ): Int {
        val month = calendar[year]?.find { it.monthId == monthIdx }
        return month?.starting_parent ?: 0
    }


    fun initializeCalendar(year: Int, monthIdx: Int, startingParent: Int) : GenerateCalendar {
        val calendar = GenerateCalendar()
        val monthsRemaining = (monthIdx..12).toList()

        val monthIndexes = if (officialCalendar.containsKey(year.toString())) {
            getMonthIdsForYear(year.toString())
        } else {
            emptyList()
        }

        val filteredMonths = monthsRemaining.filterNot { it in monthIndexes }

        if (filteredMonths.isNotEmpty()) {
            calendar.generating_calendar(filteredMonths.first(), startingParent, year)
        }

        FirebaseUtils.saveActiveChild()

        return calendar
    }

    fun regenerateCalendarFromDate(year: Int, month: Int, day: Int) {
        val calendar = initializeCalendar(year, month, 0)

        // Step 1: Remove future data from today onward
        removeCalendarFrom(day, month, year)

        // Step 2: Regenerate from today
        calendar.regenerateFromDate(year, month, day)

        // Persist changes
        FirebaseUtils.saveActiveChild()

    }

    fun removeCalendarFrom(startDay: Int, startMonth: Int, startYear: Int) {
        val yearStr = startYear.toString()
        val months = officialCalendar[yearStr] ?: return

        val iterator = months.iterator()
        while (iterator.hasNext()) {
            val month = iterator.next()
            when {
                month.monthId < startMonth -> {
                    // Past months → keep unchanged
                    continue
                }
                month.monthId == startMonth -> {
                    // Current month → remove future days from startDay onward
                    month.parent0_nights = month.parent0_nights.filter { it < startDay }.toMutableList()
                }
                month.monthId > startMonth -> {
                    // Future months → remove completely
                    iterator.remove()
                }
            }
        }

        // Optional: remove year if no months left
        if (months.isEmpty()) {
            officialCalendar.remove(yearStr)
        }
    }

    fun getViewerToken(): String {
        return IDEncoder.encodeViewerID(childID)
    }

    fun isContinuous(sequence: List<Int>): Boolean {
        if (sequence.isEmpty() || sequence.size == 1) return true
        val firstElement = sequence.first()

        for (i in firstElement until sequence.size + firstElement) {
            if (sequence[i] != sequence[i - 1] + 1) {
                return false
            }
        }
        return true
    }

    fun parentHasConfirmed(){
        parentConfirmed = true
    }


    fun getCalendarChanges() {
        for ((year, months) in modifiedCalendar) {
            val officialMonths = officialCalendar[year] ?: continue

            for ((index, editedMonth) in months.withIndex()) {
                val officialMonth = officialMonths.getOrNull(index) ?: continue

                val allDays = (1..31)
                for (day in allDays) {
                    val wasParent0 = officialMonth.parent0_nights.contains(day)
                    val isParent0 = editedMonth.parent0_nights.contains(day)

                    if (wasParent0 != isParent0) {
                        val proposedNewParent = if (isParent0) 0 else 1

                        // Check for existing pending change
                        val alreadyExists = officialMonth.changes.any {
                            it.night == day &&
                                    it.monthId == officialMonth.monthId &&
                                    it.year == year.toInt() &&
                                    it.proposedByParent == User.userData.childPermissions[childID] &&
                                    it.isPending()
                        }

                        if (!alreadyExists) {
                            val change = PendingChanges(
                                year = year.toInt(),
                                monthId = officialMonth.monthId,
                                night = day,
                                proposedByParent = User.userData.childPermissions[childID],
                                newParent = proposedNewParent
                            )
                            officialMonth.addChange(change)
                        }
                    }
                }
                for (change in officialMonth.changes) {
                    Log.d("CalendarActivity", "Change: $change")
                }
            }
        }
    }



    fun resolvePendingChanges() {
        for ((_, months) in modifiedCalendar) {
            for (month in months) {
                month.resolvePendingChanges()
            }
        }
    }


    fun createModifiedCalendar() {
        val copy = mutableMapOf<String, MutableList<Month>>()
        for ((year, months) in officialCalendar) {
            copy[year] = mutableListOf()
            for (month in months) {
                val monthCopy = month.deepCopy()
                // Apply only pending changes for this month
                for (change in month.changes) {
                    if (change.showOnCalendarToModify()) {
                        monthCopy.updateParent0Nights(change.night, change.newParent)
                    }
                }
                copy[year]?.add(monthCopy)
            }
        }
        modifiedCalendar = copy
    }

    fun deleteModifiedCalendar(){
        modifiedCalendar.clear()
    }
}