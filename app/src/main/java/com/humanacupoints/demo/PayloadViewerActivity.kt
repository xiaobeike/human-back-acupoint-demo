package com.humanacupoints.demo

import android.content.ClipData
import android.content.ClipboardManager
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.humanacupoints.demo.databinding.ActivityPayloadViewerBinding

class PayloadViewerActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_PAYLOAD = "extra_payload"
        const val EXTRA_SUMMARY = "extra_summary"
        const val EXTRA_TITLE = "extra_title"
    }

    private lateinit var binding: ActivityPayloadViewerBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPayloadViewerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val payload = intent.getStringExtra(EXTRA_PAYLOAD).orEmpty()
        val summary = intent.getStringExtra(EXTRA_SUMMARY).orEmpty()
        val title = intent.getStringExtra(EXTRA_TITLE).orEmpty()

        binding.backButton.setOnClickListener { finish() }
        binding.copyJsonButton.setOnClickListener { copyPayloadToClipboard(payload) }

        binding.titleText.text = title.ifBlank {
            getString(R.string.payload_page_title)
        }
        binding.payloadSummaryText.text = summary.ifBlank {
            getString(R.string.payload_summary_placeholder)
        }
        binding.payloadJsonText.text = payload.ifBlank {
            getString(R.string.payload_placeholder)
        }
    }

    private fun copyPayloadToClipboard(payload: String) {
        if (payload.isBlank()) {
            Toast.makeText(this, R.string.copy_json_empty, Toast.LENGTH_SHORT).show()
            return
        }
        val clipboardManager = getSystemService(ClipboardManager::class.java)
        clipboardManager?.setPrimaryClip(ClipData.newPlainText("acupoint-payload", payload))
        Toast.makeText(this, R.string.copy_json_success, Toast.LENGTH_SHORT).show()
    }
}
