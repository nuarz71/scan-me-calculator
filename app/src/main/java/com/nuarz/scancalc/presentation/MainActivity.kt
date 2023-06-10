package com.nuarz.scancalc.presentation

import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.mlkit.vision.common.InputImage
import com.nuarz.scancalc.R
import com.nuarz.scancalc.data.LocalDataSource
import com.nuarz.scancalc.databinding.ActivityMainBinding
import com.nuarz.scancalc.ext.isBuildApiCapture
import com.nuarz.scancalc.ext.isBuildApiPick
import com.nuarz.scancalc.presentation.adapter.CalculationAdapter
import com.nuarz.scancalc.presentation.viewmodel.MainViewModel
import dagger.hilt.android.AndroidEntryPoint
import java.io.File
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityMainBinding
    
    private var captureUri: Uri? = null
    
    private lateinit var launcherTakeImage: ActivityResultLauncher<Uri>
    private lateinit var launcherGetContent: ActivityResultLauncher<Array<String>>
    
    private val calculationAdapter by lazy { CalculationAdapter() }
    
    private val decimalFormat by lazy {
        DecimalFormat("#,###.###", DecimalFormatSymbols(Locale.getDefault())).apply {
            maximumIntegerDigits = 15
            maximumFractionDigits = 18
        }
    }
    
    private val viewModel: MainViewModel by viewModels()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        checkPlayServicesAvailable()
        launcherGetContent = registerForActivityResult(ActivityResultContracts.OpenDocument()) {
            it?.let(this::updateImage)
        }
        launcherTakeImage = registerForActivityResult(ActivityResultContracts.TakePicture()) {
            if (it && captureUri != null) {
                updateImage(captureUri!!)
                captureUri = null
            }
        }
        
        configureUi()
        observers()
    }
    
    private fun configureUi() {
        binding.btnClose.setOnClickListener {
            hideImagePreview()
        }
        binding.buttonInputImage.apply {
            setText(
                if (isBuildApiCapture()) R.string.label_btn_input_capture_image
                else R.string.label_btn_input_open_image
            )
            setOnClickListener {
                binding.ivImgPreview.apply {
                    setImageDrawable(null)
                    isVisible = false
                }
                if (isBuildApiPick()) {
                    launcherGetContent.launch(arrayOf("image/*"))
                } else {
                    takePicture()
                }
            }
        }
        
        binding.rvContent.adapter = calculationAdapter
        
        binding.radioGroupStorage.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.radio_file -> viewModel.switchStorage(LocalDataSource.MODE_FILE)
                R.id.radio_db -> viewModel.switchStorage(LocalDataSource.MODE_DB)
            }
        }
    }
    
    private fun updateCurrentCalculationText(message: String, isError: Boolean = false) {
        binding.tvCurrentCalculation.apply {
            text = message
            val size = if (isError) {
                14f
            } else {
                24f
            }
            textSize = size
        }
    }
    
    private fun observers() {
        viewModel.calculationResult.observe(this) { model ->
            model?.run {
                val formattedResult = decimalFormat.format(model.result)
                
                
                updateCurrentCalculationText(
                    "Input: ${model.input}\n" +
                        "Result: $formattedResult"
                )
            }
            
        }
        viewModel.histories.observe(this) {
            calculationAdapter.submitList(it) {
                Log.d("RVContent", "Called")
                if (it.isNotEmpty()) {
                    with(binding.rvContent) {
                        postDelayed({
                            if (canScrollVertically(-1) && scrollState == RecyclerView.SCROLL_STATE_IDLE) {
                                scrollToPosition(0)
                            }
                        }, 300L)
                    }
                }
            }
        }
        viewModel.currentStorageMode.observe(this) {
            when (it) {
                LocalDataSource.MODE_FILE -> {
                    binding.radioGroupStorage.check(R.id.radio_file)
                }
                
                LocalDataSource.MODE_DB -> {
                    binding.radioGroupStorage.check(R.id.radio_db)
                }
                
                else -> {
                    binding.radioGroupStorage.clearCheck()
                }
            }
            updateCurrentCalculationText("")
        }
        viewModel.isProcessingImage.observe(this) {
            binding.buttonInputImage.isEnabled = it.not()
            showProgress(it)
        }
        viewModel.errorProcessingImage.observe(this) {
            it?.let { message ->
                updateCurrentCalculationText(message, true)
            }
        }
    }
    
    private fun hideImagePreview() {
        binding.ivImgPreview.apply {
            setImageDrawable(null)
            isVisible = false
        }
        binding.btnClose.isVisible = false
    }
    
    private fun updateImage(uri: Uri) {
        binding.ivImgPreview.apply {
            setImageURI(uri)
            isVisible = true
        }
        binding.btnClose.isVisible = true
        viewModel.processImage(InputImage.fromFilePath(this, uri))
    }
    
    private fun takePicture() {
        val hasCamera = packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)
        if (hasCamera.not()) {
            updateCurrentCalculationText("Camera devices need to take a picture", true)
            return
        }
        val dirname = "capture"
        val baseDir = File(cacheDir, dirname)
        if (baseDir.exists().not()) {
            baseDir.mkdir()
        }
        val dateFormat = SimpleDateFormat("ddMMyyyy'_'HHmmss", Locale.getDefault())
        val filename = "IMG_${dateFormat.format(Date(System.currentTimeMillis()))}.jpg"
        val target = File(baseDir, filename)
        val uri = FileProvider.getUriForFile(this, packageName.plus(".fileprovider"), target)
        Log.d("CAPTURE_URI", "$uri")
        captureUri = uri
        captureUri?.let {
            launcherTakeImage.launch(it)
        }
    }
    
    private fun showProgress(show: Boolean) {
        binding.tvProgress.setText(R.string.label_progressing_image)
        binding.groupProgress.isVisible = show
    }
    
    private fun checkPlayServicesAvailable() {
        val googleApi = GoogleApiAvailability.getInstance()
        val resultCode = googleApi.isGooglePlayServicesAvailable(this)
        if (resultCode != ConnectionResult.SUCCESS) {
            if (googleApi.isUserResolvableError(resultCode)) {
                val dialog = googleApi.getErrorDialog(this, resultCode, 101)
                    ?: return
                dialog.show()
            }
        }
    }
}