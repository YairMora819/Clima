package com.example.revisiondeclima

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
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
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                location?.let {
                    viewModel.fetchWeatherByCoords(it.latitude, it.longitude)
                    viewModel.fetchForecastByCoords(it.latitude, it.longitude) // Trae el forecast también
                }
            }
        }

        setContent {
            RevisionDeClimaTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    WeatherScreen(viewModel)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeatherScreen(viewModel: MainViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val forecastUiState by viewModel.forecastUiState.collectAsState()
    var cityInput by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current

    val gradientColors = when ((uiState as? WeatherUiState.Success)?.weather?.weather?.firstOrNull()?.main) {
        "Clear" -> listOf(Color(0xFF2193b0), Color(0xFF6dd5ed))
        "Clouds" -> listOf(Color(0xFFbdc3c7), Color(0xFF2c3e50))
        "Rain" -> listOf(Color(0xFF373B44), Color(0xFF4286f4))
        else -> listOf(Color(0xFF83a4d4), Color(0xFFb6fbff))
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(brush = Brush.verticalGradient(colors = gradientColors))
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            OutlinedTextField(
                value = cityInput,
                onValueChange = { cityInput = it },
                label = { Text("Buscar ciudad") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {
                    focusManager.clearFocus()
                    if (cityInput.isNotBlank()) {
                        viewModel.fetchWeatherByCity(cityInput.trim())
                        viewModel.fetchForecastByCoords(0.0, 0.0) // Opcional: actualizar forecast con la ciudad
                    }
                }),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = getCurrentDateTime(),
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            when (uiState) {
                is WeatherUiState.Loading -> CircularProgressIndicator()
                is WeatherUiState.Success -> {
                    val weather = (uiState as WeatherUiState.Success).weather

                    Text(
                        "Ciudad: ${weather.name}",
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color.White,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        "Temperatura: ${weather.main.temp} °C",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                    Text(
                        "Humedad: ${weather.main.humidity} %",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        weather.weather.firstOrNull()?.description?.replaceFirstChar { it.uppercase() }
                            ?: "",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                }
                is WeatherUiState.Error -> {
                    Text(
                        text = (uiState as WeatherUiState.Error).message,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (uiState is WeatherUiState.Success) {
                val iconCode = (uiState as WeatherUiState.Success).weather.weather.firstOrNull()?.icon
                if (iconCode != null) {
                    AsyncImage(
                        model = "https://openweathermap.org/img/wn/${iconCode}@4x.png",
                        contentDescription = "Icono del clima",
                        modifier = Modifier.size(160.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Mostrar pronóstico básico
            when (forecastUiState) {
                is ForecastUiState.Loading -> {
                    Text(
                        "Cargando pronóstico...",
                        color = Color.White,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                }
                is ForecastUiState.Success -> {
                    val forecast = (forecastUiState as ForecastUiState.Success).forecast
                    Text(
                        "Pronóstico para ${forecast.city.name}, ${forecast.city.country}",
                        color = Color.White,
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    // Por ejemplo, mostrar los primeros 5 items de forecast
                    forecast.list.take(5).forEach { item ->
                        ForecastItemRow(item)
                    }
                }
                is ForecastUiState.Error -> {
                    Text(
                        "Error al cargar pronóstico: ${(forecastUiState as ForecastUiState.Error).message}",
                        color = Color.Red,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                }
            }
        }
    }
}

@Composable
fun ForecastItemRow(item: ForecastItem) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = item.dtTxt,
            modifier = Modifier.weight(1f),
            color = Color.White,
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            text = "${item.main.temp} °C",
            modifier = Modifier.weight(1f),
            color = Color.White,
            style = MaterialTheme.typography.bodyMedium
        )
        AsyncImage(
            model = "https://openweathermap.org/img/wn/${item.weather.firstOrNull()?.icon}@2x.png",
            contentDescription = "Icono pronóstico",
            modifier = Modifier.size(48.dp)
        )
    }
}

fun getCurrentDateTime(): String {
    val sdf = SimpleDateFormat("EEEE, d 'de' MMMM yyyy - HH:mm", Locale("es", "MX"))
    return sdf.format(Date())
}

