package com.example.revisiondeclima

data class OpenUVResponse(
    val result: OpenUVResult
)

data class OpenUVResult(
    val uv: Double,
    val uv_time: String,
    val uv_max: Double,
    val uv_max_time: String,
    val ozone: Double? = null,
    val ozone_time: String? = null,
    val safe_exposure_time: SafeExposureTime? = null
)

data class SafeExposureTime(
    val st1: Int? = null, // Tipo de piel 1
    val st2: Int? = null, // Tipo de piel 2
    val st3: Int? = null, // Tipo de piel 3
    val st4: Int? = null, // Tipo de piel 4
    val st5: Int? = null, // Tipo de piel 5
    val st6: Int? = null  // Tipo de piel 6
)