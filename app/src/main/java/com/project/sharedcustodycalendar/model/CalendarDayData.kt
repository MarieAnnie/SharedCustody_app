package com.project.sharedcustodycalendar.model

data class CalendarDayData (
    val index : Int,
    val morningParentID : Int,
    val eveningParentID: Int,
    val transfers : Boolean,
    val changesID : String = "",
)