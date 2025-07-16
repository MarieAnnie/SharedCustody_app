package com.project.sharedcustodycalendar.objects

import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

object FamilyDataHolder {

    val familyData: FamilyData = FamilyData()

    data class FamilyData(
        val children: MutableList<Child> = mutableListOf()
    ) {
        var activeChild: Child? = null

        fun toJson(): JSONObject {
            val json = JSONObject()
            json.put("children", JSONArray(children.map { it.toJson() }))
            return json
        }

        fun fromJson(json: JSONObject) {
            children.clear()
            val childrenArray = json.getJSONArray("children")
            for (i in 0 until childrenArray.length()) {
                val childJson = childrenArray.getJSONObject(i)
                val child = Child()
                child.fromJson(childJson)
                children.add(child)
            }
        }

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
}
