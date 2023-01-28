package com.nuarz.scancalc.data

import android.util.Log
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

class Encryptor {
	
	companion object {
		const val TAG = "Encryptor"
		private const val SEC_KEY = "a1b2c3d4e5f6g7h8"
		private const val IVX = "1a2b3c4d5e6f7g8h"
		private const val SALT = "1234567890123456"
		private const val SEC_KEY_FACTORY_ALG = "PBKDF2WithHmacSHA1And8bit"
	}
	
	private val secKey: ByteArray
	
	init {
		val keySpec = PBEKeySpec(SEC_KEY.toCharArray(), SALT.toByteArray(), 1000, 128)
		secKey = SecretKeyFactory.getInstance(SEC_KEY_FACTORY_ALG).generateSecret(keySpec).encoded
	}
	
	fun encrypt(content: ByteArray): ByteArray? {
		return encrypt(content, Cipher.ENCRYPT_MODE)
	}
	
	fun decrypt(content: ByteArray): ByteArray? {
		return encrypt(content, Cipher.DECRYPT_MODE)
	}
	
	private fun encrypt(
		content: ByteArray,
		@androidx.annotation.IntRange(from = 1, to = 2) mode: Int
	): ByteArray? {
		return try {
			val secret = SecretKeySpec(secKey, "AES")
			val iv = IvParameterSpec(IVX.toByteArray())
			val transformation = "AES/CBC/PKCS5Padding"
			val cipher = Cipher.getInstance(transformation)
			cipher.init(mode, secret, iv)
			cipher.doFinal(content)
		} catch (e: Throwable) {
			Log.d(TAG, "ERROR: ${e.message}")
			null
		}
	}
}