package cl.duoc.huertoescolar.ui.cultivos

import androidx.compose.animation.animateContentSize
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material.icons.filled.Grass
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import cl.duoc.huertoescolar.ui.componentes.BarraSuperior
import cl.duoc.huertoescolar.ui.componentes.CampoTexto
import cl.duoc.huertoescolar.ui.componentes.CargandoPantalla
import cl.duoc.huertoescolar.ui.componentes.ChipDeEstado
import cl.duoc.huertoescolar.ui.componentes.EstadoVacio
import cl.duoc.huertoescolar.ui.componentes.FilaDeDato
import cl.duoc.huertoescolar.ui.theme.colorDeEstadoCultivo
import cl.duoc.huertoescolar.ui.theme.colorDeEstadoSector

/**
 * Informacion de cultivos: cada sector con los cultivos que van en el, sus
 * fechas, frecuencia de riego y estado. La ven todos los perfiles; solo el
 * administrador puede abrir la edicion de un cultivo.
 */
@Composable
fun CultivosInfoScreen(
    esAdministrador: Boolean,
    alVolver: () -> Unit,
    alEditarCultivo: (sectorId: Long, cultivoId: Long) -> Unit,
    alCrearCultivo: () -> Unit,
    viewModel: CultivosInfoViewModel = viewModel(factory = CultivosInfoViewModel.Factory)
) {
    val estado by viewModel.ui.collectAsStateWithLifecycle()

    Scaffold(
        topBar = { BarraSuperior(titulo = "Información de cultivos", alVolver = alVolver) },
        floatingActionButton = {
            if (esAdministrador) {
                ExtendedFloatingActionButton(
                    onClick = alCrearCultivo,
                    icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                    text = { Text("Nuevo cultivo") }
                )
            }
        }
    ) { innerPadding ->
        if (estado.cargando) {
            CargandoPantalla(modifier = Modifier.padding(innerPadding))
            return@Scaffold
        }
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Text(
                    text = estado.totalCultivos.toString() + " cultivos en " + estado.grupos.size +
                            " sectores. Aquí ves en que sector va cada cultivo.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            item {
                CampoTexto(
                    valor = estado.busqueda,
                    alCambiar = { viewModel.buscar(it) },
                    etiqueta = "Buscar cultivo",
                    ayuda = "Ejemplo: tomate"
                )
            }

            if (estado.grupos.isEmpty()) {
                item {
                    EstadoVacio(
                        titulo = "Sin resultados",
                        detalle = "No hay cultivos que coincidan con la búsqueda.",
                        icono = Icons.Filled.Grass
                    )
                }
            }

            estado.grupos.forEach { grupo ->
                item(key = "sector-" + grupo.sector.id) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = grupo.sector.codigo + " - " + grupo.sector.nombre,
                                style = MaterialTheme.typography.titleLarge
                            )
                            Text(
                                text = grupo.sector.ubicacion,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        ChipDeEstado(
                            texto = grupo.sector.estado.etiqueta,
                            color = colorDeEstadoSector(grupo.sector.estado)
                        )
                    }
                }
                if (grupo.cultivos.isEmpty()) {
                    item(key = "vacio-" + grupo.sector.id) {
                        Text(
                            text = "Sin cultivos asignados a este sector.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                items(grupo.cultivos, key = { "cultivo-" + it.id }) { cultivo ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .animateContentSize(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = cultivo.nombre,
                                    style = MaterialTheme.typography.titleMedium,
                                    modifier = Modifier.weight(1f)
                                )
                                ChipDeEstado(
                                    texto = cultivo.estado.etiqueta,
                                    color = colorDeEstadoCultivo(cultivo.estado)
                                )
                                if (esAdministrador) {
                                    IconButton(onClick = { alEditarCultivo(cultivo.sectorId, cultivo.id) }) {
                                        Icon(Icons.Filled.Edit, contentDescription = "Editar " + cultivo.nombre)
                                    }
                                }
                            }
                            Text(cultivo.descripcion, style = MaterialTheme.typography.bodyMedium)
                            FilaDeDato("Siembra", Fechas.formatear(cultivo.fechaSiembra))
                            cultivo.fechaTrasplante?.let { FilaDeDato("Trasplante", Fechas.formatear(it)) }
                            FilaDeDato("Cosecha estimada", Fechas.formatear(cultivo.fechaCosechaEstimada))
                            FilaDeDato("Riego", "Cada " + cultivo.frecuenciaRiegoDias + " días")
                            if (cultivo.observaciones.isNotBlank()) {
                                FilaDeDato("Observaciones", cultivo.observaciones)
                            }
                        }
                    }
                }
            }
        }
    }
}
