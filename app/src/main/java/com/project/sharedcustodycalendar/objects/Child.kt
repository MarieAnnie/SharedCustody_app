package com.project.sharedcustodycalendar.objects

data class Child(
    val childName: String = "",
    val childID: String = "",
    val parents: List<Parent> = emptyList(),
    val schedulePattern: List<Int> = emptyList(),
    var hour_parent_switch: String = "08:00",
    var years: Map<String, List<Month>> = emptyMap()
)