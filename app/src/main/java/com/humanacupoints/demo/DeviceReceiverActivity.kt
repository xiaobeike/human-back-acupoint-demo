package com.humanacupoints.demo

import android.content.ClipData
import android.content.ClipboardManager
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.humanacupoints.demo.databinding.ActivityDeviceReceiverBinding

class DeviceReceiverActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_POINT_LABEL = "extra_point_label"
        const val EXTRA_POINT_MERIDIAN = "extra_point_meridian"
        const val EXTRA_POINT_SUMMARY = "extra_point_summary"
        const val EXTRA_RECEIVED_AT = "extra_received_at"
        const val EXTRA_PAYLOAD = "extra_payload"
    }

    private lateinit var binding: ActivityDeviceReceiverBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDeviceReceiverBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val pointLabel = intent.getStringExtra(EXTRA_POINT_LABEL).orEmpty()
        val meridian = intent.getStringExtra(EXTRA_POINT_MERIDIAN).orEmpty()
        val summary = intent.getStringExtra(EXTRA_POINT_SUMMARY).orEmpty()
        val receivedAt = intent.getStringExtra(EXTRA_RECEIVED_AT).orEmpty()
        val payload = intent.getStringExtra(EXTRA_PAYLOAD).orEmpty()

        binding.backButton.setOnClickListener { finish() }
        binding.copyJsonButton.setOnClickListener { copyPayloadToClipboard(payload) }

        binding.receivedPointNameText.text = pointLabel.ifBlank {
            getString(R.string.default_point_name)
        }
        binding.receivedPointMeridianText.text = meridian.ifBlank {
            getString(R.string.default_point_meridian)
        }
        binding.receivedPointSummaryText.text = summary.ifBlank {
            getString(R.string.selected_payload_summary_placeholder)
        }
        binding.receivedAtText.text = receivedAt.ifBlank {
            getString(R.string.device_received_at_placeholder)
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
