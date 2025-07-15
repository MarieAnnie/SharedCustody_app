package com.project.sharedcustodycalendar.objects

data class Month(
    val monthId: Int = 0,
    var starting_parent: Int = 0,
    var parent0_nights: List<Int> = emptyList()
) {
}