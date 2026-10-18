package cl.duoc.huertoescolar.ui.historial

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.filled.FilterAltOff
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.TextButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import cl.duoc.huertoescolar.core.Fechas
import cl.duoc.huertoescolar.data.local.entity.Usuario
import cl.duoc.huertoescolar.ui.componentes.BarraSuperior
import cl.duoc.huertoescolar.ui.componentes.CargandoPantalla
import cl.duoc.huertoescolar.ui.componentes.ChipDeEstado
import cl.duoc.huertoescolar.ui.componentes.EstadoVacio
import cl.duoc.huertoescolar.ui.componentes.SelectorDesplegable
import cl.duoc.huertoescolar.ui.theme.ColoresHuerto

/**
 * Pantalla 13: historial cronologico de actividades, revisiones y problemas.
 */
@Composable
fun HistorialScreen(
    usuario: Usuario,
    alVolver: () -> Unit,
    viewModel: HistorialViewModel = viewModel(factory = HistorialViewModel.Factory)
) {
    val estado by viewModel.ui.collectAsStateWithLifecycle()

    LaunchedEffect(usuario.id) {
        viewModel.establecerUsuario(usuario)
    }

    Scaffold(
        topBar = { BarraSuperior(titulo = "Historial de actividades", alVolver = alVolver) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // ------------------------- Filtros -------------------------
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = estado.filtros.tipo == null,
                    onClick = { viewModel.filtrarPorTipo(null) },
                    label = { Text("Todo") }
                )
                TipoRegistro.entries.forEach { tipo ->
                    FilterChip(
                        selected = estado.filtros.tipo == tipo,
                        onClick = {
                            viewModel.filtrarPorTipo(if (estado.filtros.tipo == tipo) null else tipo)
                        },
                        label = { Text(tipo.etiqueta) }
                    )
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SelectorDesplegable(
                    etiqueta = "Fecha",
                    opciones = PeriodoHistorial.entries.toList(),
                    seleccionada = estado.filtros.periodo,
                    textoDe = { it.etiqueta },
                    alSeleccionar = { viewModel.filtrarPorPeriodo(it) },
                    modifier = Modifier.weight(1f)
                )
                if (estado.esAdministrador) {
                    SelectorDesplegable(
                        etiqueta = "Persona",
                        opciones = estado.autores,
                        seleccionada = estado.filtros.autor,
                        textoDe = { it },
                        alSeleccionar = { viewModel.filtrarPorAutor(it) },
                        modifier = Modifier.weight(1f),
                        textoVacio = "Sin registros"
                    )
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Mostrando " + estado.registros.size + " de " + estado.total,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                TextButton(
                    onClick = { viewModel.limpiarFiltros() },
                    enabled = estado.filtros.activos
                ) {
                    Icon(Icons.Filled.FilterAltOff, contentDescription = null)
                    Text("Limpiar filtros", modifier = Modifier.padding(start = 6.dp))
                }
            }

            when {
                estado.cargando -> CargandoPantalla()
                estado.registros.isEmpty() -> EstadoVacio(
                    titulo = if (estado.total == 0) "El historial está vacío" else "Nada para estos filtros",
                    detalle = if (estado.total == 0) {
                        "Las tareas asignadas, entregas, revisiones y reportes aparecerán aquí en orden cronológico."
                    } else {
                        "Cambia los filtros o usa Limpiar filtros para ver todo."
                    },
                    icono = Icons.Filled.History
                )
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(estado.registros, key = { it.id }) { registro ->
                        val color = when (registro.tipo) {
                            TipoRegistro.TAREA -> ColoresHuerto.actual.neutro
                            TipoRegistro.EVIDENCIA -> ColoresHuerto.actual.informacion
                            TipoRegistro.VALIDACION -> ColoresHuerto.actual.exito
                            TipoRegistro.REPORTE -> ColoresHuerto.actual.error
                        }
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = registro.titulo,
                                        style = MaterialTheme.typography.titleMedium,
                                        modifier = Modifier.weight(1f)
                                    )
                                    ChipDeEstado(texto = registro.tipo.etiqueta, color = color)
                                }
                                Text(
                                    text = registro.detalle,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = registro.autor + " - " + Fechas.formatearConHora(registro.fecha),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
