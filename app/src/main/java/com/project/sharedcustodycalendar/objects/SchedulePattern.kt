package com.project.sharedcustodycalendar.objects

import com.project.sharedcustodycalendar.model.CalendarDayData
import java.time.LocalTime

class SchedulePattern (
    val nbOfWeeks : Int,
    val nbParents : Int
) {
    val schedule: MutableList<CalendarDayData> = mutableListOf()
    val transferTimes : MutableList<LocalTime> = mutableListOf()

    init {
        for(x in  0.. 7*nbOfWeeks){
            schedule.add(CalendarDayData(x,0,0,false))
            transferTimes.add(LocalTime.MIDNIGHT)
        }
    }

    fun setNewEvening(parentID: Int, eveningIdx : Int) {
        schedule[eveningIdx].eveningParentID = parentID
        if (eveningIdx + 1 == 7 * nbOfWeeks){
            schedule[0].morningParentID = parentID
        } else {
            schedule[eveningIdx + 1].morningParentID = parentID
        }
    }

    fun saveTimes (times: MutableList<LocalTime>) {
        checkRange(times.size)
        transferTimes.clear()
        transferTimes.addAll(times)
    }

    fun checkRange(length: Int){
        require(length == 7 * nbOfWeeks) {
            "Number of weeks is incorrect in the schedule pattern"
        }
    }
}