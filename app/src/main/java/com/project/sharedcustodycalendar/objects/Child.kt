package com.project.sharedcustodycalendar.objects

import com.project.sharedcustodycalendar.CalendarActivity
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
    var years: MutableMap<String, MutableList<Month>> = mutableMapOf(),
    var originalCalendar : MutableMap<String, MutableList<Month>> = mutableMapOf(),
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
        json.put("years", mapToJson(years))
        return json
    }

    fun fromJson(json: JSONObject) {
        childName = json.getString("childName")
        childID = json.getString("childID")
        parents = json.getJSONArray("parents").map { Parent.fromJson(it as JSONObject) }
        schedulePattern = json.getJSONArray("schedulePattern").toIntList()
        hour_parent_switch = json.getString("hour_parent_switch")
        years = json.getJSONObject("years").toMonthMap().toMutableMap()
    }

    fun setOrUpdateMonth(year: String, newMonth: Month) {
        val yearMonths = years.getOrPut(year) { mutableListOf() }

        val existingIndex = yearMonths.indexOfFirst { it.monthId == newMonth.monthId }
        if (existingIndex != -1) {
            // Overwrite existing month
            yearMonths[existingIndex] = newMonth
        } else {
            // Add new month
            yearMonths.add(newMonth)
        }

        years[year] = yearMonths
    }

    fun getMonthIdsForYear(year: String): List<Int> {
        return years[year]?.map { it.monthId } ?: emptyList()
    }

    fun getParent0EveningSchedule(year: String, monthIdx: Int, isCalendarActivity: Boolean = false ): List<Int> {
        val month = if (isCalendarActivity) {
            modifiedCalendar[year]?.find { it.monthId == monthIdx }
        } else {
            years[year]?.find { it.monthId == monthIdx }
        }
        return month?.parent0_nights ?: emptyList()
    }

    fun changeParentNight(year: String, monthIdx: Int, day: Int, newParent: Int = -1) {
        val month = years[year]?.find { it.monthId == monthIdx }
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
                val nextMonth = years[nextYear]?.find { it.monthId == 1 }
                if (nextMonth != null) {
                    nextMonth.updateParent0Nights(day, newParent)
                } else {
                    initializeCalendar(nextYear.toInt(), 1, updatedParent)
                }
            }else {
                val nextMonth = years[year]?.find { it.monthId == monthIdx +1 }
                nextMonth?.updateStartingParent(newParent)
            }
        }
    }

    fun getStartingParent(year: String, monthIdx: Int ): Int {
        val month = years[year]?.find { it.monthId == monthIdx }
        return month?.starting_parent ?: 0
    }

    fun initializeCalendar(year: Int, monthIdx: Int, starting_parent: Int) {
        var calendar = GenerateCalendar()
        val monthsRemaining = (monthIdx..12).toList()

        var monthIndexes = emptyList<Int>()

        if (years.containsKey(year.toString())) {
            monthIndexes = getMonthIdsForYear(year.toString())
        }

        val filteredMonths = monthsRemaining.filterNot { it in monthIndexes }
        if (filteredMonths.isNotEmpty()) {
            calendar.generating_calendar(filteredMonths.first(), starting_parent, year)
        }
        //if (!isContinuous(filteredMonths)) {
            // TODO message about overwriting
        //}
        FirebaseUtils.saveActiveChild()

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

    fun copyOriginalCalendar(){
        val copy = mutableMapOf<String, MutableList<Month>>()

        for ((year, months) in years) {
            val copiedMonths = months.map { it.deepCopy() }.toMutableList()
            copy[year] = copiedMonths
        }

        originalCalendar = copy
    }

    fun getCalendarChanges ( ) {
        for ((year, months) in years) {
            val originalMonths = originalCalendar[year] ?: continue

            for ((index, editedMonth) in months.withIndex()) {
                val originalMonth = originalMonths.getOrNull(index) ?: continue

                val allDays = (1..31)
                for (day in allDays) {
                    val wasParent0 = originalMonth.parent0_nights.contains(day)
                    val isParent0 = editedMonth.parent0_nights.contains(day)

                    if (wasParent0 != isParent0) {
                        val proposedNewParent = if (isParent0) 0 else 1
                        val change = pendingChanges(
                            year = year.toInt(),
                            monthInt = originalMonth.monthId,
                            night = day,
                            proposedByParent = User.userData.childPermissions[childID],
                            newParent = proposedNewParent
                        )
                        originalMonth.addChange(change)
                    }
                }
            }
        }
        resolvePendingChanges()
    }

    fun resolvePendingChanges() {
        for ((_, months) in years) {
            for (month in months) {
                month.resolvePendingChanges()
            }
        }
    }


    fun createModifiedCalendar(){
        val copy = mutableMapOf<String, MutableList<Month>>()
        for ((year, months) in years) {
            copy[year] = mutableListOf<Month>()
            for (month in months) {
                var monthCopy = month.deepCopy()
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
}