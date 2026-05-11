package com.humanacupoints.demo

import android.Manifest
import android.graphics.Bitmap
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import com.humanacupoints.demo.databinding.ActivityMainBinding
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var cameraExecutor: ExecutorService
    private var isFrozen = false
    private var frozenBitmap: Bitmap? = null

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
            binding.backOverlayView.resetAnchors()
        }

        binding.freezeButton.setOnClickListener {
            isFrozen = !isFrozen
            applyFrozenState()
        }

        binding.resetButton.setOnClickListener {
            binding.backOverlayView.resetAnchors()
        }

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

    private fun applyFrozenState() {
        if (isFrozen) {
            captureFreezeFrame()
        } else {
            clearFreezeFrame()
        }
        binding.backOverlayView.setFrozen(isFrozen)
        binding.statusText.setText(if (isFrozen) R.string.status_frozen else R.string.status_live)
        binding.hintText.setText(if (isFrozen) R.string.hint_frozen else R.string.hint_live)
        binding.freezeButton.setText(if (isFrozen) R.string.action_resume else R.string.action_freeze)
        binding.previewView.alpha = if (isFrozen) 0f else 1f
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

    override fun onDestroy() {
        clearFreezeFrame()
        super.onDestroy()
        cameraExecutor.shutdown()
    }
}
