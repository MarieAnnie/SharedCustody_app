package com.project.sharedcustodycalendar.objects

import com.project.sharedcustodycalendar.model.CalendarDayData
import com.project.sharedcustodycalendar.model.DraftTransfer
import com.project.sharedcustodycalendar.model.SessionContext
import com.project.sharedcustodycalendar.model.User
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

enum class CalendarTypes {
    OFFICIAL, MODIFIED
}

class Day(
    var startingParentID: Int,
    val date: LocalDate,
) {
    val transfers: MutableList<TransferEvent> = mutableListOf()
    val pendingChanges: MutableList<PendingChange> = mutableListOf()
    val endingParentID: Int
        get() = transfers.lastOrNull()?.toParentID ?: startingParentID

    fun toJson(): JSONObject {
        val json = JSONObject()
        json.put("startingParent", startingParentID)
        json.put("date", date.toString())

        val transfersArray = JSONArray()
        for (transfer in transfers) {
            transfersArray.put(transfer.toJson())  // Assuming PendingChanges has toJson()
        }
        json.put("transfers", transfersArray)

        val changesArray = JSONArray()
        for (change in pendingChanges) {
            changesArray.put(change.toJson())  // Assuming PendingChanges has toJson()
        }
        json.put("pendingChanges", changesArray)

        return json
    }

    companion object {
        fun fromJson(json: JSONObject): Day {
            val day = Day(
                startingParentID = json.getInt("startingParent"),
                date = LocalDate.parse(json.getString("date"))
            )

            val changesArray = json.optJSONArray("pendingChanges")
            if (changesArray != null) {
                for (i in 0 until changesArray.length()) {
                    day.pendingChanges.add(PendingChange.fromJson(changesArray.getJSONObject(i)))
                }
            }

            val transfersArray = json.optJSONArray("transfers")
            if (transfersArray != null) {
                for (i in 0 until transfersArray.length()) {
                    day.transfers.add(TransferEvent.fromJson(transfersArray.getJSONObject(i)))
                }
            }

            return day
        }
    }

    fun setTransfersFromApprovedChanges(groupID: String) {
        val changes = getApprovedBlockToApply(groupID)

        if (changes.isEmpty()) return

        transfers.clear()

        // CASE 1: full day override
        if (changes.size == 1 && changes.first().isFullDayOverride) {
            startingParentID = changes.first().toParentID
            changes.first().applied = true
            return
        }

        // CASE 2: normal transfers
        val sorted = changes.sortedBy { it.time }

        var current = sorted.first().fromParentID
        startingParentID = current

        for (change in sorted) {
            val transfer = TransferEvent(
                time = change.time!!,
                fromParentID = current,
                toParentID = change.toParentID
            )
            transfers.add(transfer)
            current = change.toParentID
        }
        changes.forEach { it.applied = true }
    }

    fun setPendingChangedFromDraft(groupID: String, drafts: List<DraftTransfer>) {
        val sorted = drafts.sortedBy { it.time }

        val newChanges = mutableListOf<PendingChange>()
        val user = SessionContext.requireCurrentParentID()

        var current = startingParentID

        for (draft in sorted) {
            val change = PendingChange(
                date = date,
                time = draft.time,
                toParentID = draft.toParentID,
                fromParentID = current,
                proposedByParentID = user,
                groupID = groupID
            )

            newChanges.add(change)
            current = draft.toParentID
        }

        removePendingChangesOfUser(user)
        pendingChanges.addAll(newChanges)
    }

    fun removePendingChangesOfUser(user: Int) {
        pendingChanges.removeAll {
            it.isProposedParent(user) && it.isPending()
        }
    }

    fun canBeModified(parentID: Int): Boolean {
        var canBeModified = true
        for (change in pendingChanges) {
            if (!change.isProposedParent(parentID) && change.isPending()) {
                canBeModified = false
                break
            } else if (change.isProposedParent(parentID) && (change.isApproved() || change.isRejected())) {
                canBeModified = false
                break
            }
        }
        return canBeModified
    }

    fun getCalendarData(calendarType: CalendarTypes): CalendarDayData {
        when (calendarType) {
            CalendarTypes.OFFICIAL -> return getOfficialCalendarData()
            CalendarTypes.MODIFIED -> return getModifiedCalendarData()
        }
    }

    fun getModifiedCalendarData(): CalendarDayData {
        val data = getCurrentPendingBlock()
        val groupID = data.first
        val changes = data.second

        if (changes.isEmpty()) {
            return getOfficialCalendarData()
        }

        val isFullDayOverride = changes.size == 1 && changes.first().isFullDayOverride
        val transferBool = !isFullDayOverride

        val first = changes.first()
        val last = changes.last()

        return CalendarDayData(
            index = date.dayOfMonth - 1,
            morningParentID = first.fromParentID,
            eveningParentID = last.toParentID,
            transfers = transferBool,
            changesID = groupID
        )
    }

    fun getOfficialCalendarData(): CalendarDayData {
        val data = CalendarDayData(
            index = date.dayOfMonth - 1,
            morningParentID = startingParentID,
            eveningParentID = endingParentID,
            transfers = transfers.isNotEmpty()
        )
        return data
    }

    fun getApprovedBlockToApply(groupID: String) =
        pendingBlockOfGroupID(groupID).filter { it.isApproved() && !it.applied }

    fun getRejectedBlock(groupID: String) =
        pendingBlockOfGroupID(groupID).filter { it.isRejected() }

    fun getApprovedBlockWasApplied(groupID: String) =
        pendingBlockOfGroupID(groupID).filter { it.isApproved() && it.applied }

    fun pendingBlockOfGroupID(groupID: String): List<PendingChange> {
        return pendingChanges.filter { it.groupID == groupID }
    }

    fun getCurrentPendingBlock(): Pair<String, List<PendingChange>> {
        val currentParent = SessionContext.requireCurrentParentID()

        val changes = pendingChanges
            .filter { it.isPending() && it.isProposedParent(currentParent) }
            .sortedBy { it.time }

        if (changes.isEmpty()) return "" to emptyList()

        val id = changes.first().groupID

        require(changes.all { it.groupID == id }) {
            "Pending changes are not all from the same group"
        }
        return id to changes
    }

    fun cleanPendingChanges() {
        pendingChanges.removeAll { it.isAcknowledged() }
    }
}