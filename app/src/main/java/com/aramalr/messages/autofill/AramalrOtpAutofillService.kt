package com.aramalr.messages.autofill

import android.app.assist.AssistStructure
import android.os.CancellationSignal
import android.service.autofill.AutofillService
import android.service.autofill.Dataset
import android.service.autofill.FillCallback
import android.service.autofill.FillRequest
import android.service.autofill.FillResponse
import android.service.autofill.SaveCallback
import android.service.autofill.SaveRequest
import android.view.autofill.AutofillId
import android.view.autofill.AutofillValue
import android.widget.RemoteViews
import com.aramalr.messages.evidence.OtpEvidenceLog
import com.aramalr.messages.otp.LatestOtpStore
import java.io.File

class AramalrOtpAutofillService : AutofillService() {
    override fun onFillRequest(
        request: FillRequest,
        cancellationSignal: CancellationSignal,
        callback: FillCallback,
    ) {
        val latest = LatestOtpStore.latest()
        if (latest == null) {
            callback.onSuccess(null)
            return
        }
        val (match, _) = latest
        val structure = request.fillContexts.lastOrNull()?.structure
        if (structure == null) {
            evidenceLog().updateLatestPending(match.code, "none")
            callback.onSuccess(null)
            return
        }

        val ids = mutableListOf<AutofillId>()
        for (i in 0 until structure.windowNodeCount) {
            walkNode(structure.getWindowNodeAt(i).rootViewNode, ids)
        }

        if (ids.isEmpty()) {
            evidenceLog().updateLatestPending(match.code, "none")
            callback.onSuccess(null)
            return
        }

        val presentation =
            RemoteViews(packageName, android.R.layout.simple_list_item_1).apply {
                setTextViewText(android.R.id.text1, "aramalr kod: ${match.code}")
            }
        val datasetBuilder = Dataset.Builder()
        ids.forEach { id ->
            @Suppress("DEPRECATION")
            datasetBuilder.setValue(id, AutofillValue.forText(match.code), presentation)
        }
        val response =
            FillResponse.Builder()
                .addDataset(datasetBuilder.build())
                .build()
        // Honest trail: dataset offered to the user (selection not separately observable here).
        evidenceLog().updateLatestPending(match.code, "offered")
        callback.onSuccess(response)
    }

    override fun onSaveRequest(
        request: SaveRequest,
        callback: SaveCallback,
    ) {
        callback.onSuccess()
    }

    private fun evidenceLog(): OtpEvidenceLog = OtpEvidenceLog(File(filesDir, "evidence"))

    private fun walkNode(
        node: AssistStructure.ViewNode,
        out: MutableList<AutofillId>,
    ) {
        val id = node.autofillId
        if (id != null && isOtpField(node)) {
            out += id
        }
        for (i in 0 until node.childCount) {
            walkNode(node.getChildAt(i), out)
        }
    }

    private fun isOtpField(node: AssistStructure.ViewNode): Boolean {
        val hints = node.autofillHints?.map { it.lowercase() }.orEmpty()
        if (hints.any { it.contains("otp") || it.contains("sms") || it.contains("one-time") }) {
            return true
        }
        val hint = (node.hint ?: "").lowercase()
        val text = (node.text?.toString() ?: "").lowercase()
        val idEntry = (node.idEntry ?: "").lowercase()
        val blob = "$hint $text $idEntry"
        return listOf("otp", "kod", "code", "doğrulama", "dogrulama", "sms", "pin").any { blob.contains(it) }
    }
}
