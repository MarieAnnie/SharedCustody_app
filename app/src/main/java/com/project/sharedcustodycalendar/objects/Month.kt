package com.project.sharedcustodycalendar.objects

data class Month(
    val monthId: Int = 0,
    val starting_parent: Int = 0,
    var parent1_nights: List<Int> = emptyList()
) {
}