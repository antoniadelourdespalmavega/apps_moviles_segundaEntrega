package cl.duoc.huertoescolar.ui.clima

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import cl.duoc.huertoescolar.core.Fechas
import cl.duoc.huertoescolar.data.repository.ClimaRepository
import cl.duoc.huertoescolar.domain.Alerta
import cl.duoc.huertoescolar.domain.clima.Clima
import cl.duoc.huertoescolar.domain.clima.PronosticoDia
import cl.duoc.huertoescolar.domain.clima.Recomendacion
import cl.duoc.huertoescolar.ui.animaciones.aparecerEnLista
import cl.duoc.huertoescolar.ui.componentes.BarraSuperior
import cl.duoc.huertoescolar.ui.componentes.CargandoPantalla
import cl.duoc.huertoescolar.ui.componentes.EncabezadoHoja
import cl.duoc.huertoescolar.ui.componentes.EstadoVacio
import cl.duoc.huertoescolar.ui.componentes.TarjetaAlerta
import kotlin.math.roundToInt

/**
 * Pantalla de la API externa: clima actual y pronostico de 7 dias de
 * San Bernardo (Open-Meteo), con la recomendacion de riego del dia.
 */
@Composable
fun ClimaScreen(
    alVolver: () -> Unit,
    viewModel: ClimaViewModel = viewModel(factory = ClimaViewModel.Factory)
) {
    val estado by viewModel.ui.collectAsStateWithLifecycle()

    Scaffold(
        topBar = { BarraSuperior(titulo = "Clima del huerto", alVolver = alVolver) }
    ) { innerPadding ->
        when (val actual = estado) {
            is ClimaUiState.Cargando -> CargandoPantalla(modifier = Modifier.padding(innerPadding))

            is ClimaUiState.Error -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                EstadoVacio(
                    titulo = "No se pudo obtener el clima",
                    detalle = actual.mensaje,
                    icono = Icons.Filled.CloudOff
                )
                Button(onClick = { viewModel.cargar() }) { Text("Reintentar") }
            }

            is ClimaUiState.Exito -> ContenidoClima(
                clima = actual.clima,
                recomendacion = actual.recomendacion,
                modifier = Modifier.padding(innerPadding)
            )
        }
    }
}

@Composable
private fun ContenidoClima(
    clima: Clima,
    recomendacion: Recomendacion,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            EncabezadoHoja(forma = MaterialTheme.shapes.large) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                        imageVector = iconoDeCondicion(clima.actual.condicion),
                        contentDescription = clima.actual.condicion.descripcion,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(56.dp)
                    )
                    Column {
                        Text(
                            text = "${clima.actual.temperatura.roundToInt()} °C",
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Text(
                            text = clima.actual.condicion.descripcion + " en " + ClimaRepository.LUGAR,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Text(
                            text = "Humedad ${clima.actual.humedad} % · viento ${clima.actual.viento.roundToInt()} km/h",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f)
                        )
                    }
                }
            }
        }

        item {
            TarjetaAlerta(Alerta(recomendacion.titulo, recomendacion.detalle, recomendacion.nivel))
        }

        item {
            Text("Pronóstico de la semana", style = MaterialTheme.typography.titleMedium)
        }

        itemsIndexed(clima.dias, key = { _, dia -> dia.fecha }) { indice, dia ->
            FilaPronostico(dia = dia, modifier = Modifier.animateItem().aparecerEnLista(indice))
        }

        item {
            Text(
                text = "Datos del clima: Open-Meteo.com (API externa gratuita, sin clave).",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun FilaPronostico(dia: PronosticoDia, modifier: Modifier = Modifier) {
    Card(modifier = modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Icon(
                imageVector = iconoDeCondicion(dia.condicion),
                contentDescription = dia.condicion.descripcion,
                modifier = Modifier.size(28.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(Fechas.nombreDia(dia.fecha), style = MaterialTheme.typography.titleMedium)
                Text(
                    text = dia.condicion.descripcion + " · lluvia ${dia.probabilidadLluvia} %",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = "${dia.maxima.roundToInt()}° / ${dia.minima.roundToInt()}°",
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}
