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

sealed class AirQualityUiState {
    object Loading : AirQualityUiState()
    data class Success(val airQuality: AirQualityResponse) : AirQualityUiState()
    data class Error(val message: String) : AirQualityUiState()
}

class MainViewModel : ViewModel() {

    private val weatherService = WeatherService.create()

    // Estado para el clima actual
    private val _uiState = MutableStateFlow<WeatherUiState>(WeatherUiState.Loading)
    val uiState: StateFlow<WeatherUiState> = _uiState

    // Estado para el pronóstico
    private val _forecastUiState = MutableStateFlow<ForecastUiState>(ForecastUiState.Loading)
    val forecastUiState: StateFlow<ForecastUiState> = _forecastUiState

    // Estado para la calidad del aire
    private val _airQualityUiState = MutableStateFlow<AirQualityUiState>(AirQualityUiState.Loading)
    val airQualityUiState: StateFlow<AirQualityUiState> = _airQualityUiState

    // ✅ CORREGIDO: Estado para el índice UV real
    private val _uvIndex = MutableStateFlow(0)
    val uvIndex: StateFlow<Int> = _uvIndex

    // ✅ NUEVA FUNCIÓN: Obtener UV Index real desde la API
    private fun fetchUVIndex(lat: Double, lon: Double) {
        viewModelScope.launch {
            try {
                val response = weatherService.getUVIndexByCoords(lat, lon)
                _uvIndex.value = response.value.toInt()
            } catch (e: Exception) {
                // Si falla, usar un valor por defecto
                _uvIndex.value = 5
            }
        }
    }

    fun fetchWeatherByCity(city: String) {
        viewModelScope.launch {
            _uiState.value = WeatherUiState.Loading
            try {
                val response = weatherService.getCurrentWeatherByCity(city)
                _uiState.value = WeatherUiState.Success(response)
                // ✅ CORREGIDO: Obtener UV Index real usando las coordenadas
                fetchUVIndex(response.coord.lat, response.coord.lon)
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
                // ✅ CORREGIDO: Obtener UV Index real
                fetchUVIndex(lat, lon)
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

    fun fetchForecastByCity(city: String) {
        viewModelScope.launch {
            _forecastUiState.value = ForecastUiState.Loading
            try {
                val response = weatherService.getForecastByCity(city)
                _forecastUiState.value = ForecastUiState.Success(response)
            } catch (e: Exception) {
                _forecastUiState.value = ForecastUiState.Error("Error: ${e.message ?: "desconocido"}")
            }
        }
    }

    fun fetchAirQualityByCoords(lat: Double, lon: Double) {
        viewModelScope.launch {
            _airQualityUiState.value = AirQualityUiState.Loading
            try {
                val response = weatherService.getAirQualityByCoords(lat, lon)
                _airQualityUiState.value = AirQualityUiState.Success(response)
            } catch (e: Exception) {
                _airQualityUiState.value = AirQualityUiState.Error("Error: ${e.message ?: "desconocido"}")
            }
        }
    }

    fun fetchAirQualityByCity(city: String) {
        viewModelScope.launch {
            _airQualityUiState.value = AirQualityUiState.Loading
            try {
                // Primero obtenemos las coordenadas de la ciudad
                val weatherResponse = weatherService.getCurrentWeatherByCity(city)
                // Luego obtenemos la calidad del aire usando las coordenadas
                val response = weatherService.getAirQualityByCoords(
                    weatherResponse.coord.lat,
                    weatherResponse.coord.lon
                )
                _airQualityUiState.value = AirQualityUiState.Success(response)
            } catch (e: Exception) {
                _airQualityUiState.value = AirQualityUiState.Error("Error: ${e.message ?: "desconocido"}")
            }
        }
    }
}