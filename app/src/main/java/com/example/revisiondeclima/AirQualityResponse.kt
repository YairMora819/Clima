package com.example.revisiondeclima

import com.google.gson.annotations.SerializedName

data class AirQualityResponse(
    val list: List<AirQualityItem>
)

data class AirQualityItem(
    val main: AirQualityMain,
    val components: AirQualityComponents
)

data class AirQualityMain(
    val aqi: Int // 1 = Buena, 2 = Aceptable, 3 = Moderada, 4 = Mala, 5 = Muy mala
)

data class AirQualityComponents(
    val co: Double,     // Monóxido de carbono
    val no: Double,     // Óxido nítrico
    val no2: Double,    // Dióxido de nitrógeno
    val o3: Double,     // Ozono
    val so2: Double,    // Dióxido de azufre
    val pm2_5: Double,  // Partículas finas
    val pm10: Double,   // Partículas gruesas
    val nh3: Double     // Amoníaco
)