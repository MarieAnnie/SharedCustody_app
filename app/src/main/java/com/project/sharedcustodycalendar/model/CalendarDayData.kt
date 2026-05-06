package com.project.sharedcustodycalendar.model

data class CalendarDayData (
    val index : Int,
    var eveningParentID: Int,
    var transfers : Boolean,
    var changesID : String = "",
)