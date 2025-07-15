package com.project.sharedcustodycalendar.utils

import android.content.Context
import android.graphics.Color
import android.widget.LinearLayout
import android.widget.TextView
import com.project.sharedcustodycalendar.objects.FamilyDataHolder

object CalendarUIUtils {
    fun drawLegend(context: Context, legendLayout: LinearLayout) {
        legendLayout.removeAllViews()
        val parents = FamilyDataHolder.familyData.activeChild?.parents ?: return

        parents.forEach { parent ->
            val item = TextView(context).apply {
                text = parent.name
                setPadding(16, 0, 16, 0)
                setBackgroundColor(Color.parseColor(parent.color))
                setTextColor(Color.WHITE)
            }
            legendLayout.addView(item)
        }
    }
}
