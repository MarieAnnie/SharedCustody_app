package com.project.sharedcustodycalendar.objects

import com.project.sharedcustodycalendar.model.User

data class pendingChanges (
    var year: Int = -1,
    var monthInt: Int = -1,
    var night: Int = -1,
    var proposedByParent: Int? = -1,
    var newParent: Int = -1,
    val timsStamp: Long = System.currentTimeMillis(),
    var status: String = "pending"
){
    fun approveChange(){
        status = "approved"
        FamilyDataHolder.familyData.activeChild?.changeParentNight(year.toString(), monthInt, night, newParent)
    }

    fun rejectChange(){
        status = "rejected"
    }

    fun forCurrentParent(): Boolean{
        val childID = FamilyDataHolder.familyData.activeChild?.childID
        val currentParent = User.userData.childPermissions[childID]
        if (status == "pending" && proposedByParent != currentParent ){
            return true
        }
        if ((status=="approved" || status=="rejected") && proposedByParent == currentParent ){
            return true
        }
        return false
    }

    fun showOnCalendarToModify() : Boolean{
        val childID = FamilyDataHolder.familyData.activeChild?.childID
        val currentParent = User.userData.childPermissions[childID]
        if (proposedByParent == currentParent && status=="pending") {
            return true
        }
        if (status=="approved"){
            return true
        }
        return false
    }
}

