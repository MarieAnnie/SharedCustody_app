package com.project.sharedcustodycalendar.model

import com.project.sharedcustodycalendar.objects.Child
import com.project.sharedcustodycalendar.objects.FamilyDataHolder

object SessionContext {

    fun requireActiveChild(): Child {
        return requireNotNull(FamilyDataHolder.familyData.activeChild) {
            "Active child must be set before using SessionContext"
        }
    }

    fun requireCurrentParentID(): Int {
        val child = requireActiveChild()

        return requireNotNull(
            User.userData.childPermissions[child.childID]
        ) {
            "No parent mapping for child ${child.childID}"
        }
    }
}