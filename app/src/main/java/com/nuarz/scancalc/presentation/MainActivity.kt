package com.nuarz.scancalc.presentation

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
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
import com.nuarz.scancalc.ext.isBuildApiPick
import com.nuarz.scancalc.presentation.adapter.CalculationAdapter
import com.nuarz.scancalc.presentation.viewmodel.MainViewModel
import dagger.hilt.android.AndroidEntryPoint
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
	
	private lateinit var binding: ActivityMainBinding
	
	private var captureUri: Uri? = null
	
	private lateinit var launcherTakeImage: ActivityResultLauncher<Uri>
	private lateinit var launcherGetContent: ActivityResultLauncher<String>
	
	private val calculationAdapter by lazy { CalculationAdapter() }
	
	private val viewModel: MainViewModel by viewModels()
	
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		binding = ActivityMainBinding.inflate(layoutInflater)
		setContentView(binding.root)
		checkPlayServicesAvailable()
		launcherGetContent = registerForActivityResult(ActivityResultContracts.GetContent()) {
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
		binding.buttonInputImage.setOnClickListener {
			binding.ivImgPreview.apply {
				setImageDrawable(null)
				isVisible = false
			}
			if (isBuildApiPick()) {
				launcherGetContent.launch("image/*")
			} else {
				takePicture()
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
	
	private fun observers() {
		viewModel.recentCalculations.observe(this) {
			calculationAdapter.submitList(it) {
				Log.d("RVContent", "Called")
				if (it.isNotEmpty()) {
					with(binding.rvContent) {
						postDelayed({
							if (canScrollVertically(-1) && scrollState == RecyclerView.SCROLL_STATE_IDLE) {
								smoothScrollToPosition(0)
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
		}
		viewModel.isProcessingImage.observe(this) {
			binding.buttonInputImage.isEnabled = it.not()
			showProgress(it)
		}
		viewModel.errorProcessingImage.observe(this) {
			it?.let { message ->
				Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
			}
		}
	}
	
	private fun updateImage(uri: Uri) {
		binding.ivImgPreview.apply {
			setImageURI(uri)
			isVisible = true
		}
		viewModel.processImage(InputImage.fromFilePath(this, uri))
	}
	
	private fun takePicture() {
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