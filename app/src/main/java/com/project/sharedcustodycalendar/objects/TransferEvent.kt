package com.project.sharedcustodycalendar.objects

import org.json.JSONObject
import java.time.LocalTime
import java.util.UUID


data class TransferEvent(
    val time: LocalTime,
    val fromParentID: Int,
    val toParentID: Int,
    val note: String? = null,
    var status: TransferStatus = TransferStatus.PENDING,
    val id: String = UUID.randomUUID().toString(),
) {
    fun toJson(): JSONObject{
        val json = JSONObject()
        json.put("time", time.toString())
        json.put("fromParent", fromParentID)
        json.put("toParent", toParentID)
        json.putOpt("note", note)
        json.put("status", status.name)
        json.put("id",id)

        return json
    }
    companion object {
        fun fromJson(json: JSONObject): TransferEvent {

            return TransferEvent(
                time = LocalTime.parse(json.getString("time")),
                fromParentID = json.getInt("fromParent"),
                toParentID = json.getInt("toParent"),
                status = TransferStatus.valueOf(json.getString("status")),
                id = json.getString("id"),
                note = if (json.has("note") && !json.isNull("note")) {
                    json.getString("note")
                } else {
                    null
                }
            )
        }
    }
}

enum class TransferStatus {
    PENDING,
    CONFIRMED,
    DECLINED
}