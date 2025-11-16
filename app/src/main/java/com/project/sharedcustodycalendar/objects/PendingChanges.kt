package com.project.sharedcustodycalendar.objects

import com.project.sharedcustodycalendar.model.User
import org.json.JSONObject

enum class ChangeStatus {
    PENDING, APPROVED, REJECTED, TOBEDELETED
}

data class PendingChanges (
    var year: Int = -1,
    var monthId: Int = -1,
    var night: Int = -1,
    var proposedByParent: Int? = -1,
    var newParent: Int = -1,
    val timeStamp: Long = System.currentTimeMillis(),
    var status: ChangeStatus = ChangeStatus.PENDING
){
    fun toJson(): JSONObject {
        val json = JSONObject()
        json.put("night", night)
        json.put("monthId", monthId)
        json.put("year", year)
        json.put("newParent", newParent)
        json.put("proposedByParent", proposedByParent)
        json.put("status", status.name)
        json.put("timeStamp", timeStamp)
        return json
    }

    companion object {
        fun fromJson(json: JSONObject): PendingChanges {
            return PendingChanges(
                night = json.getInt("night"),
                monthId = json.getInt("monthId"),
                year = json.getInt("year"),
                newParent = json.getInt("newParent"),
                proposedByParent = json.getInt("proposedByParent"),
                status = ChangeStatus.valueOf(json.getString("status")),
                timeStamp = json.getLong("timeStamp")
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

    fun forCurrentParent(): Boolean{
        val childID = FamilyDataHolder.familyData.activeChild?.childID ?: return false
        val currentParent = User.userData.childPermissions[childID] ?: return false

        return when {
            // 1) Pending: always show (mine or the other parent's)
            isPending() -> true

            // 2) Approved/Rejected: show ONLY if proposed by me
            (isApproved() || isRejected()) && proposedByParent == currentParent -> true

            // Otherwise: don't show
            else -> false
        }
    }

    fun showOnCalendarToModify(): Boolean {
        val childID = FamilyDataHolder.familyData.activeChild?.childID ?: return false
        val currentParent = User.userData.childPermissions[childID] ?: return false
        return proposedByParent == currentParent && isPending()
    }
}

