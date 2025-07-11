package com.example.revisiondeclima

data class WeatherResponse(
    val main: Main,
    val weather: List<Weather>,
    val name: String
)

data class Main(
    val temp: Float,
    val humidity: Int
)

data class Weather(
    val main: String,
    val description: String,
    val icon: String
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
