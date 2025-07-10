package com.project.sharedcustodycalendar.objects

object FamilyDataHolder {
    data class FamilyData(
        val childName: String = "",
        val parents: List<Parent> = emptyList(),
    )
    var familyData: FamilyData? = null
}
