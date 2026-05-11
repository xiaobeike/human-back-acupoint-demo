package com.humanacupoints.demo

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import com.humanacupoints.demo.databinding.ActivityMainBinding
import com.humanacupoints.demo.model.AcupointRender
import com.humanacupoints.demo.model.DemoBackModel
import com.humanacupoints.demo.model.PayloadFormatter
import com.humanacupoints.demo.overlay.BackOverlayView
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity(), BackOverlayView.Listener {

    private lateinit var binding: ActivityMainBinding
    private lateinit var cameraExecutor: ExecutorService
    private var isFrozen = false
    private var isLocked = false
    private var frozenBitmap: Bitmap? = null
    private var currentTemplate = DemoBackModel.BodyTemplate.STANDARD
    private var currentStep = BackOverlayView.CalibrationStep.SHOULDERS
    private var latestPayload: String = ""
    private var latestPayloadSummary: String = ""
    private var latestSelectedPayload: String = ""
    private var latestSelectedPayloadSummary: String = ""
    private var selectedAcupoint: AcupointRender? = null
    private var latestDispatchTime: String = ""

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            startCamera()
        } else {
            Toast.makeText(this, R.string.camera_permission_denied, Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        cameraExecutor = Executors.newSingleThreadExecutor()
        binding.backOverlayView.post {
            binding.backOverlayView.applyTemplate(currentTemplate)
        }
        binding.backOverlayView.listener = this

        binding.freezeButton.setOnClickListener {
            handlePrimaryAction()
        }

        binding.resetButton.setOnClickListener {
            resetCalibration()
        }

        binding.exportButton.setOnClickListener {
            exportCurrentFrame()
        }

        binding.viewPayloadButton.setOnClickListener {
            openPayloadViewer(
                title = getString(R.string.payload_page_title),
                payload = latestPayload,
                summary = latestPayloadSummary,
            )
        }

        binding.viewSelectedPayloadButton.setOnClickListener {
            openPayloadViewer(
                title = getString(R.string.selected_payload_page_title),
                payload = latestSelectedPayload,
                summary = latestSelectedPayloadSummary,
            )
        }

        binding.shareSelectedPayloadButton.setOnClickListener {
            openSimulatedDeviceReceiver()
        }

        binding.lockButton.setOnClickListener {
            toggleLockState()
        }

        binding.templateSlimButton.setOnClickListener {
            applyTemplate(DemoBackModel.BodyTemplate.SLIM)
        }
        binding.templateStandardButton.setOnClickListener {
            applyTemplate(DemoBackModel.BodyTemplate.STANDARD)
        }
        binding.templateBroadButton.setOnClickListener {
            applyTemplate(DemoBackModel.BodyTemplate.BROAD)
        }

        seedDefaultPointCard()
        renderUiState()

        ensureCameraPermission()
    }

    private fun ensureCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            startCamera()
        } else {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also {
                it.surfaceProvider = binding.previewView.surfaceProvider
            }

            val cameraSelector = CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build()

            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(this, cameraSelector, preview)
        }, ContextCompat.getMainExecutor(this))
    }

    private fun handlePrimaryAction() {
        if (isLocked) {
            isLocked = false
            currentStep = BackOverlayView.CalibrationStep.SHOULDERS
            renderUiState()
            return
        }
        if (!isFrozen) {
            isFrozen = true
            currentStep = BackOverlayView.CalibrationStep.SHOULDERS
            captureFreezeFrame()
        } else {
            currentStep = when (currentStep) {
                BackOverlayView.CalibrationStep.SHOULDERS -> BackOverlayView.CalibrationStep.SPINE
                BackOverlayView.CalibrationStep.SPINE -> BackOverlayView.CalibrationStep.BODY_WIDTH
                BackOverlayView.CalibrationStep.BODY_WIDTH -> {
                    isLocked = true
                    BackOverlayView.CalibrationStep.LOCKED
                }
                BackOverlayView.CalibrationStep.LOCKED -> BackOverlayView.CalibrationStep.LOCKED
            }
        }
        renderUiState()
    }

    private fun resetCalibration() {
        isLocked = false
        currentStep = BackOverlayView.CalibrationStep.SHOULDERS
        binding.backOverlayView.applyTemplate(currentTemplate)
        if (!isFrozen) {
            clearFreezeFrame()
        }
        renderUiState()
    }

    private fun toggleLockState() {
        if (!isFrozen) {
            isFrozen = true
            captureFreezeFrame()
        }
        isLocked = !isLocked
        if (isLocked) {
            currentStep = BackOverlayView.CalibrationStep.LOCKED
        } else if (currentStep == BackOverlayView.CalibrationStep.LOCKED) {
            currentStep = BackOverlayView.CalibrationStep.SHOULDERS
        }
        renderUiState()
    }

    private fun applyTemplate(template: DemoBackModel.BodyTemplate) {
        currentTemplate = template
        binding.backOverlayView.applyTemplate(template)
        updateTemplateButtons()
        if (isFrozen) {
            captureFreezeFrame()
        }
    }

    private fun renderUiState() {
        val overlayStep = if (isLocked) BackOverlayView.CalibrationStep.LOCKED else currentStep
        binding.backOverlayView.setFrozen(isFrozen)
        binding.backOverlayView.setCalibrationStep(overlayStep)
        binding.previewView.alpha = if (isFrozen) 0f else 1f

        when {
            isLocked -> {
                binding.statusText.setText(R.string.status_locked)
                binding.hintText.setText(R.string.hint_locked)
                binding.anchorStatusText.setText(R.string.anchor_status_locked)
                binding.freezeButton.setText(R.string.action_recalibrate)
                binding.lockButton.setText(R.string.action_recalibrate)
                binding.backOverlayView.setShowAcupoints(true)
                ensureSelectedAcupoint()
                binding.viewPayloadButton.visibility = View.VISIBLE
                binding.viewSelectedPayloadButton.visibility = View.VISIBLE
                binding.shareSelectedPayloadButton.visibility = View.VISIBLE
                updatePayload()
            }

            isFrozen -> {
                binding.statusText.setText(R.string.status_frozen)
                binding.hintText.setText(R.string.hint_frozen)
                val buttonText = when (currentStep) {
                    BackOverlayView.CalibrationStep.SHOULDERS -> R.string.action_step_2
                    BackOverlayView.CalibrationStep.SPINE -> R.string.action_step_3
                    BackOverlayView.CalibrationStep.BODY_WIDTH -> R.string.action_finish_step
                    BackOverlayView.CalibrationStep.LOCKED -> R.string.action_finish_step
                }
                binding.freezeButton.setText(buttonText)
                binding.lockButton.setText(R.string.action_lock)
                updateAnchorStatusByStep()
                binding.backOverlayView.setShowAcupoints(false)
                binding.backOverlayView.clearSelectedAcupoint()
                binding.viewPayloadButton.visibility = View.GONE
                binding.viewSelectedPayloadButton.visibility = View.GONE
                binding.shareSelectedPayloadButton.visibility = View.GONE
            }

            else -> {
                binding.statusText.setText(R.string.status_live)
                binding.hintText.setText(R.string.hint_live)
                binding.anchorStatusText.setText(R.string.anchor_status_default)
                binding.freezeButton.setText(R.string.action_next_step)
                binding.lockButton.setText(R.string.action_lock)
                binding.backOverlayView.setShowAcupoints(false)
                binding.backOverlayView.clearSelectedAcupoint()
                binding.viewPayloadButton.visibility = View.GONE
                binding.viewSelectedPayloadButton.visibility = View.GONE
                binding.shareSelectedPayloadButton.visibility = View.GONE
            }
        }
        updateTemplateButtons()
    }

    private fun updateAnchorStatusByStep() {
        val resId = when (currentStep) {
            BackOverlayView.CalibrationStep.SHOULDERS -> R.string.anchor_status_shoulders
            BackOverlayView.CalibrationStep.SPINE -> R.string.anchor_status_spine
            BackOverlayView.CalibrationStep.BODY_WIDTH -> R.string.anchor_status_width
            BackOverlayView.CalibrationStep.LOCKED -> R.string.anchor_status_locked
        }
        binding.anchorStatusText.setText(resId)
    }

    private fun updateTemplateButtons() {
        binding.templateSlimButton.isEnabled = currentTemplate != DemoBackModel.BodyTemplate.SLIM
        binding.templateStandardButton.isEnabled = currentTemplate != DemoBackModel.BodyTemplate.STANDARD
        binding.templateBroadButton.isEnabled = currentTemplate != DemoBackModel.BodyTemplate.BROAD
    }

    private fun captureFreezeFrame() {
        binding.previewView.bitmap?.let { bitmap ->
            frozenBitmap?.recycle()
            frozenBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, false)
            binding.frozenImageView.setImageBitmap(frozenBitmap)
            binding.frozenImageView.visibility = View.VISIBLE
        }
    }

    private fun clearFreezeFrame() {
        binding.frozenImageView.setImageDrawable(null)
        binding.frozenImageView.visibility = View.GONE
        frozenBitmap?.recycle()
        frozenBitmap = null
    }

    private fun seedDefaultPointCard() {
        binding.pointNameText.setText(R.string.default_point_name)
        binding.pointMeridianText.setText(R.string.default_point_meridian)
        binding.pointSummaryText.setText(R.string.default_point_summary)
        latestPayloadSummary = getString(R.string.payload_summary_placeholder)
        latestSelectedPayloadSummary = getString(R.string.selected_payload_summary_placeholder)
    }

    override fun onAcupointSelected(point: AcupointRender) {
        selectedAcupoint = point
        binding.backOverlayView.selectAcupoint(point.id)
        binding.pointNameText.text = "当前点位：${point.label}"
        binding.pointMeridianText.text = point.meridian
        binding.pointSummaryText.text =
            "${point.summary} 相对坐标：vertical_t=${formatFloat(point.verticalT)}，lateral_t=${formatFloat(point.lateralSignedT)}"
        updateSelectedPayload()
    }

    override fun onAnchorChanged(anchorLabel: String) {
        binding.anchorStatusText.text = "锚点状态：正在调整 $anchorLabel"
    }

    private fun updatePayload() {
        val anchors = binding.backOverlayView.getCurrentAnchors() ?: return
        val acupoints = binding.backOverlayView.getCurrentAcupoints()
        val overlayWidth = binding.backOverlayView.width
        val overlayHeight = binding.backOverlayView.height
        if (overlayWidth == 0 || overlayHeight == 0 || acupoints.isEmpty()) return
        latestPayload = PayloadFormatter.buildPayload(
            currentTemplate,
            overlayWidth,
            overlayHeight,
            anchors,
            acupoints,
        )
        latestPayloadSummary =
            "已基于 6 个锚点生成 ${acupoints.size} 个穴位点；锁定轮廓后可直接发设备端或云端。"
        updateSelectedPayload()
    }

    private fun ensureSelectedAcupoint() {
        val firstPoint = binding.backOverlayView.getCurrentAcupoints().firstOrNull() ?: return
        onAcupointSelected(firstPoint)
    }

    private fun formatFloat(value: Float): String = String.format(Locale.US, "%.3f", value)

    private fun openPayloadViewer(
        title: String,
        payload: String,
        summary: String,
    ) {
        if (payload.isBlank()) {
            Toast.makeText(this, R.string.copy_json_empty, Toast.LENGTH_SHORT).show()
            return
        }
        startActivity(
            Intent(this, PayloadViewerActivity::class.java).apply {
                putExtra(PayloadViewerActivity.EXTRA_TITLE, title)
                putExtra(PayloadViewerActivity.EXTRA_PAYLOAD, payload)
                putExtra(PayloadViewerActivity.EXTRA_SUMMARY, summary)
            },
        )
    }

    private fun updateSelectedPayload() {
        val anchors = binding.backOverlayView.getCurrentAnchors() ?: return
        val point = selectedAcupoint ?: return
        val overlayWidth = binding.backOverlayView.width
        val overlayHeight = binding.backOverlayView.height
        if (overlayWidth == 0 || overlayHeight == 0) return
        latestSelectedPayload = PayloadFormatter.buildSingleAcupointPayload(
            currentTemplate,
            overlayWidth,
            overlayHeight,
            anchors,
            point,
        )
        latestSelectedPayloadSummary =
            "当前点位 ${point.label}，可按单点 JSON 直接发设备端或云端。"
    }

    private fun openSimulatedDeviceReceiver() {
        if (latestSelectedPayload.isBlank()) {
            Toast.makeText(this, R.string.copy_json_empty, Toast.LENGTH_SHORT).show()
            return
        }
        val point = selectedAcupoint ?: return
        latestDispatchTime = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
        startActivity(
            Intent(this, DeviceReceiverActivity::class.java).apply {
                putExtra(DeviceReceiverActivity.EXTRA_POINT_LABEL, point.label)
                putExtra(DeviceReceiverActivity.EXTRA_POINT_MERIDIAN, point.meridian)
                putExtra(
                    DeviceReceiverActivity.EXTRA_POINT_SUMMARY,
                    "${point.summary} 相对坐标：vertical_t=${formatFloat(point.verticalT)}，lateral_t=${formatFloat(point.lateralSignedT)}",
                )
                putExtra(
                    DeviceReceiverActivity.EXTRA_RECEIVED_AT,
                    getString(R.string.device_received_at_format, latestDispatchTime),
                )
                putExtra(DeviceReceiverActivity.EXTRA_PAYLOAD, latestSelectedPayload)
            },
        )
    }

    private fun exportCurrentFrame() {
        val bitmap = Bitmap.createBitmap(binding.root.width, binding.root.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        binding.root.draw(canvas)
        val uri = saveBitmapToGallery(bitmap)
        bitmap.recycle()
        if (uri != null) {
            Toast.makeText(this, R.string.export_success, Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, R.string.export_failed, Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveBitmapToGallery(bitmap: Bitmap): Uri? {
        val resolver = contentResolver
        val timestamp = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date())
        val contentValues = android.content.ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "back-acupoint-demo-$timestamp.png")
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/BackAcupointDemo")
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }

        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues) ?: return null
        return try {
            resolver.openOutputStream(uri).use { stream: OutputStream? ->
                if (stream == null || !bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)) {
                    throw IllegalStateException("Unable to write bitmap")
                }
            }
            contentValues.clear()
            contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
            resolver.update(uri, contentValues, null, null)
            uri
        } catch (_: Exception) {
            resolver.delete(uri, null, null)
            null
        }
    }

    override fun onDestroy() {
        clearFreezeFrame()
        super.onDestroy()
        cameraExecutor.shutdown()
    }
}
