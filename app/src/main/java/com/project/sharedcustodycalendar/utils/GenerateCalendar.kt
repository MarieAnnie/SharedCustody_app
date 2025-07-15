package com.project.sharedcustodycalendar.utils

import com.project.sharedcustodycalendar.objects.FamilyDataHolder
import com.project.sharedcustodycalendar.objects.Month
import java.util.Calendar

class GenerateCalendar {
    data class MonthMatrixResult(
        val matrix: List<List<Int>>,
        val firstDay: Int,
        val numberOfDays: Int
    )

    private var current_week_pattern: Int? = null

    fun generating_calendar(starting_month: Int, starting_parent: Int, year: Int) {
        var new_parent = starting_parent
        for (i in starting_month..12) {
            new_parent = add_new_month(i, new_parent, year)
        }
    }

    fun add_new_month(monthIdx: Int, starting_parent: Int, year: Int) : Int {
        val activeChild = FamilyDataHolder.familyData?.activeChild ?: return -1
        val schedulePattern = activeChild.schedulePattern

        var monthMatrix = getMonthMatrix(year, monthIdx)
        var firstday = monthMatrix.firstDay

        var parent0_nights = mutableListOf<Int>()
        var day = firstday

        set_pattern(firstday, starting_parent, schedulePattern)
        var night_parent = -1

        for (i in 0 until monthMatrix.numberOfDays) {
            night_parent = getNightParentIdx(day, schedulePattern)
            if (night_parent == 0){
                parent0_nights.add(i+1)
            }
            day = (day + 1) % 7
        }
        var new_month = Month(monthIdx, starting_parent, parent0_nights)
        activeChild.setOrUpdateMonth(year.toString(), new_month)
        return night_parent
    }

        fun getMonthMatrix(year: Int, month: Int): MonthMatrixResult {
            val calendar = Calendar.getInstance()
            calendar.set(year, month - 1, 1) // month is 0-based

            val firstDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK) // 1 (Sunday) to 7 (Saturday)
            val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)

            val matrix = mutableListOf<MutableList<Int>>()
            var currentWeek = MutableList(7) { 0 }

            var dayCounter = 1
            var dayIndex = firstDayOfWeek - 1 // make 0-based (Sunday = 0)

            // Fill first week
            while (dayIndex < 7 && dayCounter <= daysInMonth) {
                currentWeek[dayIndex++] = dayCounter++
            }
            matrix.add(currentWeek)

            // Fill remaining weeks
            while (dayCounter <= daysInMonth) {
                currentWeek = MutableList(7) { 0 }
                for (i in 0 until 7) {
                    if (dayCounter <= daysInMonth) {
                        currentWeek[i] = dayCounter++
                    }
                }
                matrix.add(currentWeek)
            }

            return MonthMatrixResult(matrix, dayIndex, daysInMonth)
        }

        fun set_pattern(day: Int, starting_parent: Int, schedulePattern : List<Int>) {
            val previous_day = (day + 6) % 7

            if (current_week_pattern == null) {
                var x = 0
                while (x < schedulePattern.size / 7 && schedulePattern[7 * x + previous_day] != starting_parent) {
                    x += 1
                }

                if (x < schedulePattern.size / 7) {
                    current_week_pattern = x
                } else {
                    // TODO : error
                    current_week_pattern = 0
                }
            }// # TODO if schedule not found go to input schedule
        }

    fun getNightParentIdx(day:Int, schedulePattern : List<Int>) : Int {
        val weekPattern = current_week_pattern ?: return -1  // Handle null safely

        val patternSize = schedulePattern.size
        val index = weekPattern * 7 + day

        // Make sure the index is within bounds
        if (index >= patternSize) return -1

        val nightParent = schedulePattern[index]

        // Update the pattern index for the next day
        if (day == 6) {
            val totalWeeks = patternSize / 7
            current_week_pattern = (weekPattern + 1) % totalWeeks
        }

        return nightParent
    }
    }
