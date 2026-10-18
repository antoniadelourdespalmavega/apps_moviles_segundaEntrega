package cl.duoc.huertoescolar.ui.sectores

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
import androidx.compose.material.icons.filled.Grass
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.material.icons.filled.Delete
import androidx.compose.runtime.getValue
import cl.duoc.huertoescolar.ui.componentes.DialogoConfirmacion
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import cl.duoc.huertoescolar.core.Fechas
import cl.duoc.huertoescolar.domain.EstadoCultivo
import cl.duoc.huertoescolar.ui.componentes.BarraSuperior
import cl.duoc.huertoescolar.ui.componentes.CargandoPantalla
import cl.duoc.huertoescolar.ui.componentes.ChipDeEstado
import cl.duoc.huertoescolar.ui.componentes.EstadoVacio
import cl.duoc.huertoescolar.ui.componentes.FilaDeDato
import cl.duoc.huertoescolar.ui.componentes.SelectorDesplegable
import cl.duoc.huertoescolar.ui.componentes.VistaPreviaFoto
import cl.duoc.huertoescolar.ui.componentes.clickableSinRipple
import cl.duoc.huertoescolar.ui.theme.colorDeEstadoCultivo
import cl.duoc.huertoescolar.ui.theme.colorDeEstadoSector

/**
 * Pantalla 5: detalle del sector con sus cultivos.
 * El administrador puede editar el sector, agregar cultivos y cambiar su estado
 * de desarrollo; el estudiante solo consulta la informacion.
 */
@Composable
fun SectorDetalleScreen(
    sectorId: Long,
    esAdministrador: Boolean,
    alVolver: () -> Unit,
    alEditarSector: (Long) -> Unit,
    alCrearCultivo: (Long) -> Unit,
    alEditarCultivo: (Long, Long) -> Unit,
    alEliminado: (mensaje: String) -> Unit,
    viewModel: SectorDetalleViewModel = viewModel(factory = SectorDetalleViewModel.Factory)
) {
    val estado by viewModel.ui.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var cultivoExpandido by remember { mutableStateOf(0L) }
    var confirmarEliminar by remember { mutableStateOf(false) }

    if (confirmarEliminar) {
        DialogoConfirmacion(
            titulo = "Eliminar sector",
            mensaje = "Se eliminará el sector con sus cultivos. Un sector con tareas registradas no se puede eliminar.",
            textoConfirmar = "Eliminar",
            alConfirmar = {
                confirmarEliminar = false
                viewModel.eliminarSector(alEliminado)
            },
            alCancelar = { confirmarEliminar = false }
        )
    }

    LaunchedEffect(sectorId) {
        viewModel.establecerSector(sectorId)
    }

    LaunchedEffect(estado.mensaje) {
        val aviso = estado.mensaje
        if (aviso != null) {
            snackbar.showSnackbar(aviso)
            viewModel.limpiarMensaje()
        }
    }

    Scaffold(
        topBar = {
            BarraSuperior(
                titulo = estado.sector?.nombre ?: "Detalle del sector",
                alVolver = alVolver,
                acciones = {
                    if (esAdministrador && estado.sector != null) {
                        IconButton(onClick = { alEditarSector(sectorId) }) {
                            Icon(Icons.Filled.Edit, contentDescription = "Editar sector")
                        }
                        IconButton(onClick = { confirmarEliminar = true }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Eliminar sector")
                        }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
        floatingActionButton = {
            if (esAdministrador && estado.sector != null) {
                ExtendedFloatingActionButton(
                    onClick = { alCrearCultivo(sectorId) },
                    icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                    text = { Text("Nuevo cultivo") }
                )
            }
        }
    ) { innerPadding ->
        val sector = estado.sector
        when {
            estado.cargando -> CargandoPantalla(modifier = Modifier.padding(innerPadding))
            sector == null -> EstadoVacio(
                titulo = "El sector no existe",
                detalle = "Es posible que haya sido eliminado por el administrador.",
                modifier = Modifier.padding(innerPadding)
            )
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
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = sector.codigo,
                                    style = MaterialTheme.typography.titleMedium
                                )
                                ChipDeEstado(
                                    texto = sector.estado.etiqueta,
                                    color = colorDeEstadoSector(sector.estado)
                                )
                            }
                            Text(
                                text = sector.descripcion,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            FilaDeDato("Ubicación", sector.ubicacion)
                            FilaDeDato("Creado", Fechas.formatear(sector.fechaCreacion))
                            FilaDeDato("Actualizado", Fechas.formatear(sector.fechaActualizacion))
                        }
                    }
                }

                item {
                    Text(
                        text = "Cultivos del sector (" + estado.cultivos.size + ")",
                        style = MaterialTheme.typography.titleMedium
                    )
                }

                if (estado.cultivos.isEmpty()) {
                    item {
                        EstadoVacio(
                            titulo = "Sin cultivos registrados",
                            detalle = if (esAdministrador) {
                                "Agrega el primer cultivo de este sector."
                            } else {
                                "Todavía no hay cultivos asociados a este sector."
                            },
                            icono = Icons.Filled.Grass
                        )
                    }
                } else {
                    items(estado.cultivos, key = { it.id }) { cultivo ->
                        val expandido = cultivoExpandido == cultivo.id
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                // La tarjeta crece suavemente al desplegarse.
                                .animateContentSize()
                                .clickableSinRipple(true) {
                                    cultivoExpandido = if (expandido) 0L else cultivo.id
                                }
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = cultivo.nombre,
                                        style = MaterialTheme.typography.titleMedium,
                                        modifier = Modifier.weight(1f)
                                    )
                                    ChipDeEstado(
                                        texto = cultivo.estado.etiqueta,
                                        color = colorDeEstadoCultivo(cultivo.estado)
                                    )
                                }
                                Text(
                                    text = "Cosecha estimada: " +
                                            Fechas.formatear(cultivo.fechaCosechaEstimada),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                if (expandido) {
                                    Column(modifier = Modifier.padding(top = 8.dp)) {
                                        if (cultivo.rutaFoto != null) {
                                            VistaPreviaFoto(ruta = cultivo.rutaFoto, altura = 150)
                                        }
                                        Text(
                                            text = cultivo.descripcion,
                                            style = MaterialTheme.typography.bodyMedium,
                                            modifier = Modifier.padding(top = 8.dp)
                                        )
                                        FilaDeDato("Siembra", Fechas.formatear(cultivo.fechaSiembra))
                                        FilaDeDato(
                                            "Trasplante",
                                            Fechas.formatearOpcional(cultivo.fechaTrasplante)
                                        )
                                        FilaDeDato(
                                            "Frecuencia de riego",
                                            "Cada " + cultivo.frecuenciaRiegoDias + " días"
                                        )
                                        FilaDeDato("Observaciones", cultivo.observaciones)

                                        if (esAdministrador) {
                                            SelectorDesplegable(
                                                etiqueta = "Cambiar estado de desarrollo",
                                                opciones = EstadoCultivo.entries.toList(),
                                                seleccionada = cultivo.estado,
                                                textoDe = { it.etiqueta },
                                                alSeleccionar = {
                                                    viewModel.cambiarEstadoCultivo(cultivo.id, it)
                                                },
                                                modifier = Modifier.padding(top = 8.dp)
                                            )
                                            Text(
                                                text = "Tocar aquí para editar el cultivo completo",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier
                                                    .padding(top = 8.dp)
                                                    .clickableSinRipple(true) {
                                                        alEditarCultivo(sectorId, cultivo.id)
                                                    }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
