package com.project.sharedcustodycalendar.objects

import com.project.sharedcustodycalendar.model.User

enum class ChangeStatus {
    PENDING, APPROVED, REJECTED, TOBEDELETED
}

data class PendingChanges (
    var year: Int = -1,
    var monthId: Int = -1,
    var night: Int = -1,
    var proposedByParent: Int? = -1,
    var newParent: Int = -1,
    val timsStamp: Long = System.currentTimeMillis(),
    var status: ChangeStatus = ChangeStatus.PENDING
){
    fun markAsPending(){
        status = ChangeStatus.PENDING
    }

    fun isPending(): Boolean{
        return status == ChangeStatus.PENDING
    }

    fun approveChange(){
        status = ChangeStatus.APPROVED
        FamilyDataHolder.familyData.activeChild?.changeParentNight(year.toString(), monthId, night, newParent)
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
        val childID = FamilyDataHolder.familyData.activeChild?.childID
        val currentParent = User.userData.childPermissions[childID]
        if (isPending() ){
            return true
        }
        if ((isApproved() || isRejected()) && proposedByParent == currentParent ){
            return true
        }
        return false
    }

    fun showOnCalendarToModify() : Boolean{
        val childID = FamilyDataHolder.familyData.activeChild?.childID
        val currentParent = User.userData.childPermissions[childID]
        if (proposedByParent == currentParent && isPending()) {
            return true
        }
        if (isApproved() || isRejected()){ // modification should be already applied
            return true
        }
        return false
    }
}

