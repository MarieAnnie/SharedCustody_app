package com.project.sharedcustodycalendar.objects

import android.content.Context
import android.widget.GridLayout
import android.widget.TextView
import com.project.sharedcustodycalendar.views.TriangleToggleCell

data class CalendarParameters (
    var year: Int,
    var month: Int,

    var monthLabelView: TextView,
    val cellViews: MutableList<TriangleToggleCell> = mutableListOf<TriangleToggleCell>(),
    var calendarGrid: GridLayout,

    var context: Context,
    var activeChild: Child? = null,

    var isViewer: Boolean = false,
    var isCalendarActivity: Boolean = false
){
}