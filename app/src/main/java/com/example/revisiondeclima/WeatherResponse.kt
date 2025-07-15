package com.example.revisiondeclima

data class WeatherResponse(
    val main: Main,
    val weather: List<Weather>,
    val name: String,
    val coord: Coord
)

data class Main(
    val temp: Float,
    val humidity: Int,
    val feelsLike: Float? = null
)

data class Weather(
    val main: String,
    val description: String,
    val icon: String
)

data class Coord(
    val lat: Double,
    val lon: Double
)

// Para pronóstico extendido (One Call API)
data class OneCallResponse(
    val daily: List<DailyWeather>
)

data class DailyWeather(
    val dt: Long,
    val temp: Temp,
    val weather: List<Weather>
)

data class Temp(
    val max: Float,
    val min: Float
)
