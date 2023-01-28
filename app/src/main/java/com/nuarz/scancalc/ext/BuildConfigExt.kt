package com.nuarz.scancalc.ext

import com.nuarz.scancalc.BuildConfig

fun isBuildApiPick() = BuildConfig.FLAVOR_API == "pick"

fun isBuildApiCapture() = BuildConfig.FLAVOR_API == "capture"