package com.project.sharedcustodycalendar.objects

import org.json.JSONObject

data class Parent(
    var name: String = "",
    var color: String = "",
    var id: Int = 0
) {
    fun toJson(): JSONObject {
        val json = JSONObject()
        json.put("name", name)
        json.put("color", color)
        json.put("id", id)
        return json
    }

    companion object {
        fun fromJson(json: JSONObject): Parent {
            return Parent(
                name = json.getString("name"),
                color = json.getString("color"),
                id = json.getInt("id")
            )
        }
    }

}