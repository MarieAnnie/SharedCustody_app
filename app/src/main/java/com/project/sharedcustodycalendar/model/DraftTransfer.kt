package com.project.sharedcustodycalendar.model

import java.time.LocalTime

data class DraftTransfer(
    val time: LocalTime,
    val toParentID: Int,
    val note: String? = null
)