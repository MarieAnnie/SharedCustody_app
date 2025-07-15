package com.project.sharedcustodycalendar.objects

import com.project.sharedcustodycalendar.utils.FirebaseUtils
import com.project.sharedcustodycalendar.utils.GenerateCalendar
import org.json.JSONArray
import org.json.JSONObject

import com.project.sharedcustodycalendar.utils.CalendarStorageUtils.toIntList
import com.project.sharedcustodycalendar.utils.CalendarStorageUtils.mapToJson
import com.project.sharedcustodycalendar.utils.CalendarStorageUtils.map
import com.project.sharedcustodycalendar.utils.CalendarStorageUtils.toMonthMap

data class Child(
    var childName: String = "",
    var childID: String = "",
    var parents: List<Parent> = emptyList(),
    var schedulePattern: List<Int> = emptyList(),
    var hour_parent_switch: String = "08:00",
    var years: MutableMap<String, MutableList<Month>> = mutableMapOf()
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

    fun getParent0EveningSchedule(year: String, monthIdx: Int ): List<Int> {
        val month = years[year]?.find { it.monthId == monthIdx }
        return month?.parent0_nights ?: emptyList()
    }

    fun changeParentNight(year: String, monthIdx: Int, day: Int, newParent: Int = -1) {
        val month = years[year]?.find { it.monthId == monthIdx }
        month?.updateParent0Nights(day, newParent)
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
        calendar.generating_calendar(filteredMonths.first(), starting_parent, year)
        //if (!isContinuous(filteredMonths)) {
            // TODO message about overwriting
        //}
        FirebaseUtils.saveActiveChild()

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

}