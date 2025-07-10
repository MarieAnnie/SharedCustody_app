package com.project.sharedcustodycalendar.objects

data class Parent(val name: String, val color: String) {

    companion object {
        fun fromString(str: String): Parent? {
            // Example input format: "name:Alice,color:Blue"
            val parts = str.split(",")
            var name: String? = null
            var color: String? = null

            for (part in parts) {
                val keyValue = part.split(":")
                if (keyValue.size == 2) {
                    when (keyValue[0].trim().lowercase()) {
                        "name" -> name = keyValue[1].trim()
                        "color" -> color = keyValue[1].trim()
                    }
                }
            }

            return if (name != null && color != null) Parent(name, color) else null
        }
    }
}