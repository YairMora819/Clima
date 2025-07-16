package com.example.revisiondeclima

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import coil.compose.AsyncImage
import com.example.revisiondeclima.ui.theme.RevisionDeClimaTheme
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {
    private val viewModel by viewModels<MainViewModel>()
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                1001
            )
        } else {
            getCurrentLocation()
        }

        setContent {
            RevisionDeClimaTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    WeatherScreen(viewModel) { getCurrentLocation() }
                }
            }
        }
    }

    private fun getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                location?.let {
                    viewModel.fetchWeatherByCoords(it.latitude, it.longitude)
                    viewModel.fetchForecastByCoords(it.latitude, it.longitude)
                    viewModel.fetchAirQualityByCoords(it.latitude, it.longitude)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeatherScreen(viewModel: MainViewModel, onLocationRequest: () -> Unit) {
    val uiState by viewModel.uiState.collectAsState()
    val forecastUiState by viewModel.forecastUiState.collectAsState()
    val airQualityUiState by viewModel.airQualityUiState.collectAsState()
    val uvIndex by viewModel.uvIndex.collectAsState() // ✅ AGREGADO: Observar el UV Index
    var cityInput by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current
    val scrollState = rememberScrollState()


    val currentUiState = uiState
    val currentForecastUiState = forecastUiState
    val currentAirQualityUiState = airQualityUiState

    val weatherMain = if (currentUiState is WeatherUiState.Success) {
        currentUiState.weather.weather.firstOrNull()?.main
    } else {
        null
    }

    val gradientColors = when (weatherMain) {
        "Clear" -> listOf(Color(0xFF87CEEB), Color(0xFFFFD700))
        "Clouds" -> listOf(Color(0xFF708090), Color(0xFFD3D3D3))
        "Rain" -> listOf(Color(0xFF4682B4), Color(0xFF191970))
        "Snow" -> listOf(Color(0xFFE6E6FA), Color(0xFFB0C4DE))
        "Thunderstorm" -> listOf(Color(0xFF2F2F2F), Color(0xFF4B0082))
        "Drizzle" -> listOf(Color(0xFF4F94CD), Color(0xFF6495ED))
        "Mist", "Fog" -> listOf(Color(0xFFD3D3D3), Color(0xFFA9A9A9))
        else -> listOf(Color(0xFF87CEEB), Color(0xFFB0E0E6))
    }

    val animatedGradient by animateColorAsState(
        targetValue = gradientColors[0],
        animationSpec = tween(durationMillis = 1000)
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(brush = Brush.verticalGradient(colors = gradientColors))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(32.dp))

            // Barra de búsqueda mejorada
            SearchBar(
                cityInput = cityInput,
                onValueChange = { cityInput = it },
                onSearch = {
                    focusManager.clearFocus()
                    if (cityInput.isNotBlank()) {
                        viewModel.fetchWeatherByCity(cityInput.trim())
                        viewModel.fetchForecastByCity(cityInput.trim())
                        viewModel.fetchAirQualityByCity(cityInput.trim())
                    }
                },
                onLocationRequest = onLocationRequest
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Fecha y hora
            DateTimeCard()

            Spacer(modifier = Modifier.height(16.dp))

            // Información principal del clima
            when (currentUiState) {
                is WeatherUiState.Loading -> {
                    CircularProgressIndicator(color = Color.White)
                }
                is WeatherUiState.Success -> {
                    val weather = currentUiState.weather
                    MainWeatherCard(weather = weather)
                }
                is WeatherUiState.Error -> {
                    ErrorCard(message = currentUiState.message)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Tarjetas de información adicional
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Calidad del aire
                when (currentAirQualityUiState) {
                    is AirQualityUiState.Success -> {
                        AirQualityCard(
                            airQuality = currentAirQualityUiState.airQuality,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    is AirQualityUiState.Loading -> {
                        LoadingCard(
                            title = "Calidad del Aire",
                            modifier = Modifier.weight(1f)
                        )
                    }
                    is AirQualityUiState.Error -> {
                        ErrorCard(
                            message = "Error cargando calidad del aire",
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Índice UV - ✅ CORREGIDO: Usar el valor del ViewModel
                if (currentUiState is WeatherUiState.Success) {
                    UVIndexCard(
                        uvIndex = uvIndex, // ✅ CAMBIADO: Usar el valor del StateFlow
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Pronóstico extendido
            when (currentForecastUiState) {
                is ForecastUiState.Success -> {
                    ForecastCard(forecast = currentForecastUiState.forecast)
                }
                is ForecastUiState.Loading -> {
                    LoadingCard(title = "Pronóstico")
                }
                is ForecastUiState.Error -> {
                    ErrorCard(message = "Error cargando pronóstico")
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchBar(
    cityInput: String,
    onValueChange: (String) -> Unit,
    onSearch: () -> Unit,
    onLocationRequest: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedTextField(
            value = cityInput,
            onValueChange = onValueChange,
            label = { Text("Buscar ciudad", color = Color.White.copy(alpha = 0.8f)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { onSearch() }),
            modifier = Modifier.weight(1f),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color.White,
                unfocusedBorderColor = Color.White.copy(alpha = 0.6f),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            )
        )

        IconButton(
            onClick = onLocationRequest,
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White.copy(alpha = 0.2f))
        ) {
            Icon(
                imageVector = Icons.Default.LocationOn,
                contentDescription = "Obtener ubicación",
                tint = Color.White
            )
        }
    }
}

@Composable
fun DateTimeCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.15f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Text(
            text = getCurrentDateTime(),
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun MainWeatherCard(weather: WeatherResponse) {
    val scale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 500)
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.15f)
        ),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = weather.name,
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "${weather.main.temp.toInt()}°C",
                style = MaterialTheme.typography.displayLarge,
                color = Color.White,
                fontWeight = FontWeight.Light
            )

            Text(
                text = weather.weather.firstOrNull()?.description?.replaceFirstChar { it.uppercase() } ?: "",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.9f)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Icono del clima
            weather.weather.firstOrNull()?.icon?.let { iconCode ->
                AsyncImage(
                    model = "https://openweathermap.org/img/wn/${iconCode}@4x.png",
                    contentDescription = "Icono del clima",
                    modifier = Modifier.size(120.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Información adicional
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                WeatherDetailItem(
                    label = "Humedad",
                    value = "${weather.main.humidity}%"
                )
                WeatherDetailItem(
                    label = "Sensación",
                    value = "${weather.main.feelsLike?.toInt() ?: weather.main.temp.toInt()}°C"
                )
            }
        }
    }
}

@Composable
fun WeatherDetailItem(label: String, value: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.7f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun AirQualityCard(airQuality: AirQualityResponse, modifier: Modifier = Modifier) {
    val aqi = airQuality.list.firstOrNull()?.main?.aqi ?: 1
    val aqiColor = when (aqi) {
        1 -> Color(0xFF4CAF50) // Buena
        2 -> Color(0xFF8BC34A) // Aceptable
        3 -> Color(0xFFFF9800) // Moderada
        4 -> Color(0xFFFF5722) // Mala
        5 -> Color(0xFFD32F2F) // Muy mala
        else -> Color.Gray
    }

    val aqiText = when (aqi) {
        1 -> "Buena"
        2 -> "Aceptable"
        3 -> "Moderada"
        4 -> "Mala"
        5 -> "Muy mala"
        else -> "Desconocida"
    }

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.15f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Calidad del Aire",
                style = MaterialTheme.typography.titleSmall,
                color = Color.White,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(8.dp))

            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(RoundedCornerShape(25.dp))
                    .background(aqiColor),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = aqi.toString(),
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = aqiText,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
fun UVIndexCard(uvIndex: Int, modifier: Modifier = Modifier) {
    val uvColor = when (uvIndex) {
        in 0..2 -> Color(0xFF4CAF50) // Bajo
        in 3..5 -> Color(0xFFFF9800) // Moderado
        in 6..7 -> Color(0xFFFF5722) // Alto
        in 8..10 -> Color(0xFFD32F2F) // Muy alto
        else -> Color(0xFF9C27B0) // Extremo
    }

    val uvText = when (uvIndex) {
        in 0..2 -> "Bajo"
        in 3..5 -> "Moderado"
        in 6..7 -> "Alto"
        in 8..10 -> "Muy alto"
        else -> "Extremo"
    }

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.15f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Índice UV",
                style = MaterialTheme.typography.titleSmall,
                color = Color.White,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(8.dp))

            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(RoundedCornerShape(25.dp))
                    .background(uvColor),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = uvIndex.toString(),
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = uvText,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
fun ForecastCard(forecast: ForecastResponse) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.15f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Pronóstico de 5 días",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(12.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(forecast.list.take(5)) { item ->
                    ForecastItem(item)
                }
            }
        }
    }
}

@Composable
fun ForecastItem(item: ForecastItem) {
    val dayOfWeek = SimpleDateFormat("EEE", Locale("es", "MX"))
        .format(Date(item.dt * 1000))

    Card(
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.1f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .width(80.dp)
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = dayOfWeek,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.8f)
            )

            Spacer(modifier = Modifier.height(4.dp))

            AsyncImage(
                model = "https://openweathermap.org/img/wn/${item.weather.firstOrNull()?.icon}@2x.png",
                contentDescription = "Icono pronóstico",
                modifier = Modifier.size(40.dp)
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "${item.main.temp.toInt()}°",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun LoadingCard(title: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.15f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = Color.White,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(16.dp))
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = Color.White
            )
        }
    }
}

@Composable
fun ErrorCard(message: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = Color.Red.copy(alpha = 0.2f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Text(
            text = message,
            color = Color.White,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(16.dp),
            textAlign = TextAlign.Center
        )
    }
}

fun getCurrentDateTime(): String {
    val sdf = SimpleDateFormat("EEEE, d 'de' MMMM yyyy - HH:mm", Locale("es", "MX"))
    return sdf.format(Date())
}