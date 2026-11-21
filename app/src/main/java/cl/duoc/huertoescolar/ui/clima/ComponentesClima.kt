package cl.duoc.huertoescolar.ui.clima

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Umbrella
import androidx.compose.material.icons.filled.WbCloudy
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import cl.duoc.huertoescolar.data.repository.ClimaRepository
import cl.duoc.huertoescolar.domain.clima.CondicionClima
import cl.duoc.huertoescolar.ui.animaciones.tocableConRebote
import cl.duoc.huertoescolar.ui.theme.colorDeNivelAlerta
import kotlin.math.roundToInt

/** Icono de Material que representa cada condicion del cielo. */
fun iconoDeCondicion(condicion: CondicionClima): ImageVector = when (condicion) {
    CondicionClima.DESPEJADO -> Icons.Filled.WbSunny
    CondicionClima.PARCIAL -> Icons.Filled.WbCloudy
    CondicionClima.LLOVIZNA, CondicionClima.LLUVIA -> Icons.Filled.Umbrella
    CondicionClima.NIEVE -> Icons.Filled.AcUnit
    CondicionClima.TORMENTA -> Icons.Filled.FlashOn
    CondicionClima.NUBLADO, CondicionClima.NIEBLA, CondicionClima.DESCONOCIDO -> Icons.Filled.Cloud
}

/**
 * Tarjeta del panel principal con el clima actual y el consejo de riego.
 * Al tocarla se abre la pantalla con el pronostico de 7 dias.
 */
@Composable
fun TarjetaClima(
    estado: ClimaUiState,
    alTocar: () -> Unit,
    alReintentar: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .tocableConRebote(estado is ClimaUiState.Exito, alTocar),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "Clima en " + ClimaRepository.LUGAR + " (API externa)",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            when (estado) {
                is ClimaUiState.Cargando -> Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    Text("Consultando el clima...", style = MaterialTheme.typography.bodyMedium)
                }

                is ClimaUiState.Error -> Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = estado.mensaje,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = alReintentar) { Text("Reintentar") }
                }

                is ClimaUiState.Exito -> {
                    val actual = estado.clima.actual
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = iconoDeCondicion(actual.condicion),
                            contentDescription = actual.condicion.descripcion,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(40.dp)
                        )
                        Column {
                            Text(
                                text = "${actual.temperatura.roundToInt()} °C",
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Text(
                                text = actual.condicion.descripcion + " · humedad ${actual.humedad} %",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                    Text(
                        text = estado.recomendacion.titulo,
                        style = MaterialTheme.typography.titleMedium,
                        color = colorDeNivelAlerta(estado.recomendacion.nivel)
                    )
                    Text(
                        text = "Toca para ver el pronóstico de la semana",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }
    }
}
