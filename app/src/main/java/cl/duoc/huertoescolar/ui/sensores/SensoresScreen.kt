package cl.duoc.huertoescolar.ui.sensores

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
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import cl.duoc.huertoescolar.core.Fechas
import cl.duoc.huertoescolar.data.local.entity.Sector
import cl.duoc.huertoescolar.ui.componentes.BarraSuperior
import cl.duoc.huertoescolar.ui.componentes.CargandoPantalla
import cl.duoc.huertoescolar.ui.componentes.ChipDeEstado
import cl.duoc.huertoescolar.ui.componentes.EstadoVacio
import cl.duoc.huertoescolar.ui.componentes.FilaDeDato
import cl.duoc.huertoescolar.ui.componentes.SelectorDesplegable
import cl.duoc.huertoescolar.ui.theme.ColoresHuerto
import cl.duoc.huertoescolar.ui.theme.colorDeEstadoLectura

/** Opcion del filtro de sectores: id null representa "Todos los sectores". */
private data class OpcionSector(val id: Long?, val texto: String)

/**
 * Pantalla 14: datos simulados de sensores.
 * Toda la pantalla deja en claro que son datos de prueba y no mediciones reales.
 */
@Composable
fun SensoresScreen(
    alVolver: () -> Unit,
    viewModel: SensoresViewModel = viewModel(factory = SensoresViewModel.Factory)
) {
    val estado by viewModel.ui.collectAsStateWithLifecycle()
    val opcionesFiltro = listOf(OpcionSector(null, "Todos los sectores")) +
        estado.sectores.map { OpcionSector(it.id, it.codigo + " - " + it.nombre) }

    Scaffold(
        topBar = { BarraSuperior(titulo = "Sensores simulados", alVolver = alVolver) }
    ) { innerPadding ->
        when {
            estado.cargando -> CargandoPantalla(modifier = Modifier.padding(innerPadding))
            else -> LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = ColoresHuerto.actual.informacion.copy(alpha = 0.14f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = "Simulacion academica",
                                style = MaterialTheme.typography.titleMedium,
                                color = ColoresHuerto.actual.informacion
                            )
                            Text(
                                text = "Estos valores son ficticios y se generan dentro de la " +
                                    "aplicacion. No provienen de sensores fisicos ni activan " +
                                    "riego, bombas o alarmas.",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }

                // Filtro del listado: disponible para todos los perfiles.
                item {
                    SelectorDesplegable(
                        etiqueta = "Filtrar por sector",
                        opciones = opcionesFiltro,
                        seleccionada = opcionesFiltro.firstOrNull { it.id == estado.filtroSectorId },
                        textoDe = { it.texto },
                        alSeleccionar = { viewModel.filtrarPorSector(it.id) }
                    )
                }

                if (estado.lecturas.isEmpty()) {
                    item {
                        EstadoVacio(
                            titulo = if (estado.filtroSectorId == null) {
                                "Sin lecturas simuladas"
                            } else {
                                "Este sector no tiene lecturas"
                            },
                            detalle = "Elige otro sector en el filtro.",
                            icono = Icons.Filled.Sensors
                        )
                    }
                } else {
                    items(estado.lecturas, key = { it.lectura.id }) { item ->
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = item.sectorNombre,
                                        style = MaterialTheme.typography.titleMedium,
                                        modifier = Modifier.weight(1f)
                                    )
                                    ChipDeEstado(
                                        texto = item.lectura.estado.etiqueta,
                                        color = colorDeEstadoLectura(item.lectura.estado)
                                    )
                                }
                                // Cultivos del sector: un cultivo nuevo aparece aqui al instante.
                                FilaDeDato(
                                    "Cultivos del sector",
                                    if (item.cultivos.isEmpty()) "Sin cultivos" else item.cultivos.joinToString(", ")
                                )
                                FilaDeDato(
                                    "Humedad del suelo",
                                    item.lectura.humedadSuelo.toString() + " %"
                                )
                                FilaDeDato(
                                    "Temperatura ambiental",
                                    item.lectura.temperatura.toString() + " C"
                                )
                                FilaDeDato(
                                    "Humedad ambiental",
                                    item.lectura.humedadAmbiente.toString() + " %"
                                )
                                FilaDeDato(
                                    "Fecha de la lectura",
                                    Fechas.formatearConHora(item.lectura.fechaLectura)
                                )
                                FilaDeDato("Origen del dato", item.lectura.origen)
                            }
                        }
                    }
                }
            }
        }
    }
}
