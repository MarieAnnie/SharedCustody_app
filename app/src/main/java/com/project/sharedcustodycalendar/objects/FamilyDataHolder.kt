package com.project.sharedcustodycalendar.objects

import java.util.UUID

object FamilyDataHolder {

    // TODO : add a time for the parent switch

    data class Child(
        val childName: String = "",
        val childID: String = "",
        val parents: List<Parent> = emptyList(),
        val schedulePattern: List<Int> = emptyList()
    )

    data class FamilyData(
        val children: MutableList<Child> = mutableListOf()
    ) {
        var activeChild: Child? = null

        fun setActiveChild(childID: String) {
            activeChild = children.find { it.childID == childID }
        }

        fun createNewChild(childName: String) {
            val childToken = UUID.randomUUID().toString().take(6).uppercase()
            val newChild = Child(childID = childToken, childName = childName)
            children.add(newChild)
            activeChild = newChild
        }

        fun setParentsForActiveChild(newParents: List<Parent>) {
            val active = activeChild
            if (active != null) {
                val updatedChild = active.copy(parents = newParents)
                val index = children.indexOfFirst { it.childID == active.childID }
                if (index != -1) {
                    children[index] = updatedChild
                    activeChild = updatedChild
                }
            }
        }

        fun setSchedulePatternForActiveChild(pattern: List<Int>) {
            val active = activeChild
            if (active != null) {
                val updatedChild = active.copy(schedulePattern = pattern)
                val index = children.indexOfFirst { it.childID == active.childID }
                if (index != -1) {
                    children[index] = updatedChild
                    activeChild = updatedChild
                }
            }
        }
    }

    var familyData: FamilyData = FamilyData()
}
