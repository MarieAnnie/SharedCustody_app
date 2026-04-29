package com.project.sharedcustodycalendar.objects

import com.project.sharedcustodycalendar.model.User
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

class Day(
    val startingParentID: Int,
    val date: LocalDate,
) {
    val transfers: MutableList<TransferEvent> = mutableListOf()
    val pendingChanges: MutableList<PendingChanges> = mutableListOf()
    val endingParentID: Int
        get() = transfers.lastOrNull()?.toParentID ?: startingParentID

    fun toJson(): JSONObject{
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
        fun fromJson(json: JSONObject): Day{
            val day = Day(
                startingParentID = json.getInt("startingParent"),
                date = LocalDate.parse(json.getString("date"))
            )

            val changesArray = json.optJSONArray("pendingChanges")
            if (changesArray != null) {
                for (i in 0 until changesArray.length()) {
                    day.pendingChanges.add(PendingChanges.fromJson(changesArray.getJSONObject(i)))
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

    fun setTransfersFromApprovedChanges(newTransfers: List<TransferEvent>) {

        for (change in pendingChanges) {
            if (change.isApproved() && !change.isApplied()) {

            }
        }

        val sorted = newTransfers.sortedBy { it.time }

        // optional: validate here

        transfers.clear()
        transfers.addAll(sorted)
    }

    fun setPendingChangedFromDraft(drafts: List<DraftTransfer>) {
        val sorted = drafts.sortedBy { it.time }

        val newChanges = mutableListOf<PendingChanges>()
        val childID = FamilyDataHolder.familyData.activeChild?.childID ?: -1
        val user = User.userData.childPermissions[childID] ?: -1

        var current = startingParentID

        for (draft in sorted) {
            if (draft.toParentID == user) continue // skip invalid

            val change = PendingChanges(
                date = date,
                time = draft.time,
                toParentID = draft.toParentID,
                fromParentID = current,
                proposedByParentID = user
            )

            newChanges.add(change)
            current = draft.toParentID
        }

        removePendingChangesOfUser(user)
        pendingChanges.addAll(newChanges)
    }

    fun removePendingChangesOfUser(user: Int){
        for (change in pendingChanges){
            if (change.isProposedParent(user) && change.isPending()){
                pendingChanges.remove(change)
            }

        }
    }

    fun canBeModified (parentID: Int) : Boolean {
        var canBeModified = true
        for (change in pendingChanges) {
            if (!change.isProposedParent(parentID) && change.isPending()) {
                canBeModified = false
                break
            } else if (change.isToBeDeleted()) {
                pendingChanges.remove(change)
            } else if (change.isProposedParent(parentID) && (change.isApproved() || change.isRejected()) ) {
                canBeModified = false
                break
                }
        }
        return canBeModified
    }
}