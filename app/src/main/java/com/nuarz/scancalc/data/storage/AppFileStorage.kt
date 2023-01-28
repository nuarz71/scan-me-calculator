package com.nuarz.scancalc.data.storage

import android.content.SharedPreferences
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.nuarz.scancalc.data.Encryptor
import com.nuarz.scancalc.data.params.CalculationParams
import com.nuarz.scancalc.data.storage.entity.CalculationEntity
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

internal class AppFileStorage(
	appFileDir: File,
	private val preferences: SharedPreferences
) : AppStorage {
	
	private var cachedItems = mutableListOf<CalculationEntity>()
	private val encryptor by lazy { Encryptor() }
	private val baseFileDir: File = File(appFileDir, DIR_NAME)
	private var lastId = preferences.getInt("file_ocr_last_id", 0)
	
	init {
		if (baseFileDir.exists().not()) {
			baseFileDir.mkdir()
		}
	}
	
	override suspend fun saveItem(item: CalculationParams) {
		val nextId = lastId + 1
		cachedItems.add(
			0,
			CalculationEntity(
				id = nextId,
				input = item.input,
				result = item.result
			)
		)
		saveItems(cachedItems, true)
		lastId = nextId
		preferences.edit().putInt("file_ocr_last_id", lastId).apply()
	}
	
	override suspend fun saveItems(items: List<CalculationEntity>) {
		saveItems(items, false)
	}
	
	private fun saveItems(items: List<CalculationEntity>, isCache: Boolean = true) {
		val temp = mutableListOf<CalculationEntity>()
		val file = File(baseFileDir, FILE_NAME)
		if (isCache.not()) {
			temp.addAll(
				cachedItems.filterNot { src -> items.firstOrNull { it.id == src.id } != null }
			)
		}
		temp.addAll(items)
		
		val json = Gson().toJson(temp)
		saveFile(file, json.toByteArray())
		if (isCache.not()) {
			temp.sortByDescending { it.id }
		}
		cachedItems = temp
	}
	
	override suspend fun getItems(): List<CalculationEntity> {
		if (cachedItems.isNotEmpty()) return cachedItems
		
		val file = File(baseFileDir, FILE_NAME)
		if (file.exists().not()) return emptyList()
		val json = readFile(file)?.let { String(it) }
		val type = object : TypeToken<List<CalculationEntity>>() {}.type
		val items = Gson().fromJson<List<CalculationEntity>>(json, type).toMutableList()
		items.sortByDescending { it.id }
		Log.d(TAG, "READ JSON: $json")
		cachedItems = items
		return cachedItems
	}
	
	private fun readFile(file: File): ByteArray? {
		val stream = FileInputStream(file)
		val chunks = mutableListOf<Pair<ByteArray, Int>>()
		var size = 0
		var actualSize = 0
		do {
			try {
				val chunkSize = 8192
				val buffer = ByteArray(chunkSize)
				size = stream.read(buffer, 0, chunkSize)
				if (size > 0) {
					actualSize += size
					chunks.add(buffer to size)
				}
			} catch (e: Throwable) {
				Log.d(TAG, "Error Chunks: ${e.message}")
				e.printStackTrace()
			}
		} while (size > 0)
		val bytes = ByteArray(actualSize)
		var offset = 0
		chunks.forEach {
			System.arraycopy(it.first, 0, bytes, offset, it.second)
			offset += it.second
		}
		try {
			stream.close()
		} catch (e: Throwable) {
			Log.d(TAG, "Error Stream Close: ${e.message}")
			e.printStackTrace()
		}
		return encryptor.decrypt(bytes)
	}
	
	private fun saveFile(file: File, content: ByteArray) {
		val fos = FileOutputStream(file)
		val bytes = encryptor.encrypt(content)
		fos.write(bytes)
		fos.flush()
		fos.close()
	}
	
	companion object {
		private const val TAG = "AppFileStorage"
		private const val FILE_NAME = "recent-calculated-ocr-data"
		private const val DIR_NAME = ".ocr_calculations"
	}
}