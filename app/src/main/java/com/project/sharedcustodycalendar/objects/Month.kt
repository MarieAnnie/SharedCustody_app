package com.project.sharedcustodycalendar.objects

import com.project.sharedcustodycalendar.model.CalendarDayData
import com.project.sharedcustodycalendar.model.DraftTransfer
import java.time.LocalDate

class Month(
    val monthId: Int,
    val year: Int
) {
    val nbDaysInMonth : Int = LocalDate.of(year,monthId,1).lengthOfMonth()
    val days: MutableMap<Int, Day> = mutableMapOf()

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

    fun addChange(groupID: String, drafts_map : Map <Int, List<DraftTransfer>>) {
        for ((date, drafts) in drafts_map) {
            val day = requireDay(date)
            day.setPendingChangedFromDraft(groupID, drafts)
        }

    }

    fun getModifiedCalendar() : List<CalendarDayData> {
        val calendar = mutableListOf<CalendarDayData>()
        for (i in 1..nbDaysInMonth) {
            val day = requireDay(i)
            calendar.add(day.getModifiedCalendarData())
        }
        return calendar
    }

    fun getOfficialCalendar(): List<CalendarDayData> {
        val calendar = mutableListOf<CalendarDayData>()
        for (i in 1..nbDaysInMonth){
            val day = requireDay(i)
            calendar.add(day.getOfficialCalendarData())
        }
        return calendar
    }

    fun canBeModified () : Boolean {
        return days.values.all { it.canBeModified() }
    }

    fun applyChanges(groupID: String, dayIDs:List<Int>) {
        for (dayInt in dayIDs) {
            val day = requireDay(dayInt)
            day.setTransfersFromApprovedChanges(groupID)
        }
    }

    fun removeChangesFromDayInclusive(dayOfTheMonth: Int) {
        for ((dayInt, day) in days) {
            if (dayInt >= dayOfTheMonth) {
                day.removePendingChanges()
            }
        }
    }

    fun requireDay(index : Int) : Day{
        val day = days[index]
        require(day != null) {
            "Day $index is missing in Month($monthId $year)"
        }
        return day
    }
}