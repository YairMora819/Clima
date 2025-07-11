package com.example.revisiondeclima

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class WeatherUiState {
    object Loading : WeatherUiState()
    data class Success(val weather: WeatherResponse) : WeatherUiState()
    data class Error(val message: String) : WeatherUiState()
}

sealed class ForecastUiState {
    object Loading : ForecastUiState()
    data class Success(val forecast: ForecastResponse) : ForecastUiState()
    data class Error(val message: String) : ForecastUiState()
}

class MainViewModel : ViewModel() {

    private val weatherService = WeatherService.create()

    // Estado para el clima actual
    private val _uiState = MutableStateFlow<WeatherUiState>(WeatherUiState.Loading)
    val uiState: StateFlow<WeatherUiState> = _uiState

    // Estado para el pronóstico
    private val _forecastUiState = MutableStateFlow<ForecastUiState>(ForecastUiState.Loading)
    val forecastUiState: StateFlow<ForecastUiState> = _forecastUiState

    fun fetchWeatherByCity(city: String) {
        viewModelScope.launch {
            _uiState.value = WeatherUiState.Loading
            try {
                val response = weatherService.getCurrentWeatherByCity(city)
                _uiState.value = WeatherUiState.Success(response)
            } catch (e: Exception) {
                _uiState.value = WeatherUiState.Error("Error: ${e.message ?: "desconocido"}")
            }
        }
    }

    fun fetchWeatherByCoords(lat: Double, lon: Double) {
        viewModelScope.launch {
            _uiState.value = WeatherUiState.Loading
            try {
                val response = weatherService.getCurrentWeatherByCoords(lat, lon)
                _uiState.value = WeatherUiState.Success(response)
            } catch (e: Exception) {
                _uiState.value = WeatherUiState.Error("Error: ${e.message ?: "desconocido"}")
            }
        }
    }

    fun fetchForecastByCoords(lat: Double, lon: Double) {
        viewModelScope.launch {
            _forecastUiState.value = ForecastUiState.Loading
            try {
                val response = weatherService.getForecastByCoords(lat, lon)
                _forecastUiState.value = ForecastUiState.Success(response)
            } catch (e: Exception) {
                _forecastUiState.value = ForecastUiState.Error("Error: ${e.message ?: "desconocido"}")
            }
        }
    }
}

