package com.project.sharedcustodycalendar.objects

import com.project.sharedcustodycalendar.model.User
import org.json.JSONObject
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

enum class ChangeStatus {
    PENDING, APPROVED, REJECTED, TOBEDELETED
}

data class PendingChanges (
    val date: LocalDate,
    val time: LocalTime?,
    val fromParentID: Int,
    val toParentID: Int,
    val proposedByParentID: Int,
    var status: ChangeStatus = ChangeStatus.PENDING,
    val applied: Boolean = false,
    val timeStamp: Long = System.currentTimeMillis(),
    val id: String = UUID.randomUUID().toString(),
){
    fun toJson(): JSONObject {
        val json = JSONObject()
        json.put("date", date.toString())
        json.put("time", time.toString())
        json.put("toParent", toParentID)
        json.put("fromParent", fromParentID)
        json.put("proposedByParent", proposedByParentID)
        json.put("status", status.name)
        json.put("timeStamp", timeStamp)
        json.put("applied",applied)
        json.put("id",id)

        return json
    }

    companion object {
        fun fromJson(json: JSONObject): PendingChanges {

            return PendingChanges(
                date = LocalDate.parse(json.getString("date")),
                time = LocalTime.parse(json.getString("time")),
                toParentID = json.getInt("toParent"),
                fromParentID = json.getInt("fromParent"),
                proposedByParentID = json.getInt("proposedByParent"),
                status = ChangeStatus.valueOf(json.getString("status")),
                timeStamp = json.getLong("timeStamp"),
                applied = json.getBoolean("applied"),
                id = json.getString("id")
            )
        }
    }

    fun markAsPending(){
        status = ChangeStatus.PENDING
    }

    fun isPending(): Boolean{
        return status == ChangeStatus.PENDING
    }

    fun approveChange(){
        status = ChangeStatus.APPROVED
    }

    fun isApproved(): Boolean{
        return status == ChangeStatus.APPROVED
    }

    fun rejectChange(){
        status = ChangeStatus.REJECTED
    }

    fun isRejected(): Boolean{
        return status == ChangeStatus.REJECTED
    }

    fun toBeDeleted(){
        status = ChangeStatus.TOBEDELETED
    }

    fun isToBeDeleted(): Boolean{
        return status == ChangeStatus.TOBEDELETED
    }

    fun isProposedParent(parentID: Int) : Boolean {
        return proposedByParentID == parentID
    }

    fun isApplied() : Boolean{
        return applied
    }

    fun forCurrentParent(): Boolean{
        val childID = FamilyDataHolder.familyData.activeChild?.childID ?: return false
        val currentParent = User.userData.childPermissions[childID] ?: return false

        return when {
            // 1) Pending: always show (mine or the other parent's)
            isPending() -> true

            // 2) Approved/Rejected: show ONLY if proposed by me
            (isApproved() || isRejected()) && proposedByParentID == currentParent -> true

            // Otherwise: don't show
            else -> false
        }
    }

    fun showOnCalendarToModify(): Boolean {
        val childID = FamilyDataHolder.familyData.activeChild?.childID ?: return false
        val currentParent = User.userData.childPermissions[childID] ?: return false
        return proposedByParentID == currentParent && isPending()
    }
}

