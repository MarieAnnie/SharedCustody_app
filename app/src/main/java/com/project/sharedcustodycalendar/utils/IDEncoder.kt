package com.project.sharedcustodycalendar.utils

object IDEncoder {
    const val VIEWER_PREFIX = "V"
    const val SECRET = "X1B9QZ"

    fun encodeViewerID(originalID: String): String {
        require(originalID.length == SECRET.length) { "ID length must match secret length." }

        val encoded = originalID.mapIndexed { i, c ->
            val xor = c.code xor SECRET[i].code
            xor.toString(36).uppercase().padStart(2, '0')
        }.joinToString("")

        return VIEWER_PREFIX + encoded
    }

    fun decodeID(encodedID: String): String? {
        if (!encodedID.startsWith(VIEWER_PREFIX)) return encodedID  // Owner ID

        val body = encodedID.removePrefix(VIEWER_PREFIX)
        if (body.length != SECRET.length * 2) return null

        return body.chunked(2).mapIndexed { i, chunk ->
            val xorVal = chunk.toInt(36)
            val originalCharCode = xorVal xor SECRET[i].code
            originalCharCode.toChar()
        }.joinToString("")
    }
}