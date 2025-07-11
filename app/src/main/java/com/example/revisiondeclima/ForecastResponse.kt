package com.example.revisiondeclima
import com.google.gson.annotations.SerializedName

data class ForecastResponse(
    val list: List<ForecastItem>,
    val city: City
)

data class ForecastItem(
    val dt: Long,
    val main: MainForecast,
    val weather: List<WeatherForecast>,
    @SerializedName("dt_txt")
    val dtTxt: String
)

data class MainForecast(
    val temp: Float,
    val humidity: Int
)

data class WeatherForecast(
    val description: String,
    val icon: String,
    val main: String
)

data class City(
    val name: String,
    val country: String
)
