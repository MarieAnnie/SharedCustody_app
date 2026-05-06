package com.project.sharedcustodycalendar.model

import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

data class DraftTransferGroup(
    val note: String? = null
){
    val groupID: String =  UUID.randomUUID().toString()
    private val listOfDraftTransfer : MutableList<DraftTransfer> = mutableListOf()
    data class DraftTransfer(
        val date: LocalDate,
        val time: LocalTime,
        val toParentID: Int,
        val note: String? = null
    )
    fun getAllDraftsByDay() : Map<LocalDate, List<DraftTransfer>> {
        val map = mutableMapOf<LocalDate, MutableList<DraftTransfer>>()
        for (draft in listOfDraftTransfer){
            map.getOrPut(draft.date) { mutableListOf() }.add(draft)
        }
        return map

    }

    fun getDraftsForOneDay(date : LocalDate) : MutableList<DraftTransfer>{
        val list = listOfDraftTransfer.filter { it.date == date }.toMutableList()
        return list
    }

    fun resolveConflicts(date : LocalDate, draft: DraftTransfer) {
        val sameDayDrafts = getDraftsForOneDay(date)
        sameDayDrafts.add(draft)
    }

    fun addDraft(date : LocalDate, draft: DraftTransfer , isFullDayOverride : Boolean) {
        val sameDayDrafts = getDraftsForOneDay(date).sortedBy { it.time }
        if (sameDayDrafts.isEmpty()){
            listOfDraftTransfer.add(draft)
        } else if (isFullDayOverride){
            val toRemove = listOfDraftTransfer.filter { it.date == date }
            listOfDraftTransfer.removeAll(toRemove)
            listOfDraftTransfer.add(draft)
        } else {




        }
    }
}