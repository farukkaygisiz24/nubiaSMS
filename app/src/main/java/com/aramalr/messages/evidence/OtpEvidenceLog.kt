package com.aramalr.messages.evidence

import org.json.JSONObject
import java.io.File

class OtpEvidenceLog(private val dir: File) {
    private val file: File
        get() = File(dir, "otp.jsonl")

    fun append(event: OtpEvidenceEvent) {
        dir.mkdirs()
        file.appendText(toJsonLine(event) + "\n")
    }

    fun readAll(): List<OtpEvidenceEvent> {
        if (!file.exists()) return emptyList()
        return file.readLines()
            .filter { it.isNotBlank() }
            .map { fromJsonLine(it) }
    }

    fun clear() {
        if (file.exists()) {
            file.writeText("")
        }
    }

    fun updateLatestPending(
        code: String?,
        assistPath: String,
    ): Boolean {
        val events = readAll().toMutableList()
        val index =
            events.indexOfLast {
                it.assistPath == "pending" && (code == null || it.detectedCode == code)
            }
        if (index < 0) return false
        events[index] = events[index].copy(assistPath = assistPath)
        dir.mkdirs()
        file.writeText(events.joinToString(separator = "\n") { toJsonLine(it) } + if (events.isEmpty()) "" else "\n")
        return true
    }

    private fun toJsonLine(event: OtpEvidenceEvent): String {
        val o = JSONObject()
        o.put("timestampMillis", event.timestampMillis)
        o.put("sender", event.sender)
        o.put("body", event.body)
        o.put("detectedCode", event.detectedCode ?: JSONObject.NULL)
        if (event.confidence == null) {
            o.put("confidence", JSONObject.NULL)
        } else {
            o.put("confidence", event.confidence.toDouble())
        }
        o.put("assistPath", event.assistPath)
        o.put("notes", event.notes)
        return o.toString()
    }

    private fun fromJsonLine(line: String): OtpEvidenceEvent {
        val o = JSONObject(line)
        return OtpEvidenceEvent(
            timestampMillis = o.getLong("timestampMillis"),
            sender = o.getString("sender"),
            body = o.getString("body"),
            detectedCode = if (o.isNull("detectedCode")) null else o.getString("detectedCode"),
            confidence = if (o.isNull("confidence")) null else o.getDouble("confidence").toFloat(),
            assistPath = o.getString("assistPath"),
            notes = o.optString("notes", ""),
        )
    }
}
