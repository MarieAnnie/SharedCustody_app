package com.project.sharedcustodycalendar.objects

import com.project.sharedcustodycalendar.utils.GenerateCalendar

data class Child(
    val childName: String = "",
    val childID: String = "",
    val parents: List<Parent> = emptyList(),
    val schedulePattern: List<Int> = emptyList(),
    var hour_parent_switch: String = "08:00",
    var years: MutableMap<String, MutableList<Month>> = mutableMapOf()
) {
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

    fun getStartingParent(year: String, monthIdx: Int ): Int {
        val month = years[year]?.find { it.monthId == monthIdx }
        return month?.starting_parent ?: 0
    }

    fun initializeCalendar(year: Int, monthIdx: Int, starting_parent: Int) {
        var calendar = GenerateCalendar()
        val monthsRemaining = (monthIdx..12).toList()

        if (years.containsKey(year.toString())) {
            val monthIndexes = getMonthIdsForYear(year.toString())
            val filteredMonths = monthsRemaining.filterNot { it in monthIndexes }
            calendar.generating_calendar(filteredMonths.first(), starting_parent, year)
            if (!isContinuous(filteredMonths)) {
                // TODO message about overwriting
            }
        }
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