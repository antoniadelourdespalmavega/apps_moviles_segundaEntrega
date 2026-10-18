package cl.duoc.huertoescolar.ui.problemas

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
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material3.Card
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
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
import cl.duoc.huertoescolar.data.local.entity.Usuario
import cl.duoc.huertoescolar.domain.EstadoReporte
import cl.duoc.huertoescolar.ui.componentes.BarraSuperior
import cl.duoc.huertoescolar.ui.componentes.CampoTexto
import cl.duoc.huertoescolar.ui.componentes.CargandoPantalla
import cl.duoc.huertoescolar.ui.componentes.ChipDeEstado
import cl.duoc.huertoescolar.ui.componentes.EstadoVacio
import cl.duoc.huertoescolar.ui.componentes.FilaDeDato
import cl.duoc.huertoescolar.ui.componentes.SelectorDesplegable
import cl.duoc.huertoescolar.ui.componentes.VistaPreviaFoto
import cl.duoc.huertoescolar.ui.componentes.clickableSinRipple
import cl.duoc.huertoescolar.ui.theme.colorDeEstadoReporte

/**
 * Pantalla 11: listado de problemas reportados.
 * Solo el administrador puede cambiar el estado de atencion de un reporte.
 */
@Composable
fun ProblemasScreen(
    usuario: Usuario,
    alVolver: () -> Unit,
    alReportar: () -> Unit,
    viewModel: ProblemasViewModel = viewModel(factory = ProblemasViewModel.Factory)
) {
    val estado by viewModel.ui.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var expandido by remember { mutableStateOf(0L) }
    var comentario by remember { mutableStateOf("") }

    LaunchedEffect(usuario.id) {
        viewModel.establecerUsuario(usuario)
    }

    LaunchedEffect(estado.mensaje) {
        val aviso = estado.mensaje
        if (aviso != null) {
            snackbar.showSnackbar(aviso)
            viewModel.limpiarMensaje()
        }
    }

    Scaffold(
        topBar = { BarraSuperior(titulo = "Problemas del huerto", alVolver = alVolver) },
        snackbarHost = { SnackbarHost(snackbar) },
        floatingActionButton = {
            // Solo el estudiante reporta; el docente o tallerista los soluciona.
            if (!estado.esAdministrador) {
                ExtendedFloatingActionButton(
                    onClick = alReportar,
                    icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                    text = { Text("Reportar problema") }
                )
            }
        }
    ) { innerPadding ->
        when {
            estado.cargando -> CargandoPantalla(modifier = Modifier.padding(innerPadding))
            estado.reportes.isEmpty() -> EstadoVacio(
                titulo = "Sin problemas reportados",
                detalle = if (estado.esAdministrador) {
                    "Cuando un estudiante reporte un problema, aparecerá aquí para que lo soluciones."
                } else {
                    "Usa el botón Reportar problema cuando detectes algo en el huerto."
                },
                icono = Icons.Filled.ReportProblem,
                modifier = Modifier.padding(innerPadding)
            )
            else -> LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(estado.reportes, key = { it.reporte.id }) { detalle ->
                    val abierto = expandido == detalle.reporte.id
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .animateContentSize()
                            .clickableSinRipple(true) {
                                expandido = if (abierto) 0L else detalle.reporte.id
                                comentario = detalle.reporte.comentarioDocente
                            }
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = detalle.reporte.tipo.etiqueta,
                                    style = MaterialTheme.typography.titleMedium,
                                    modifier = Modifier.weight(1f)
                                )
                                ChipDeEstado(
                                    texto = detalle.reporte.estado.etiqueta,
                                    color = colorDeEstadoReporte(detalle.reporte.estado)
                                )
                            }
                            Text(
                                text = detalle.reporte.descripcion,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = detalle.sectorNombre +
                                        (if (detalle.cultivoNombre != null) {
                                            " - " + detalle.cultivoNombre
                                        } else {
                                            ""
                                        }),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            if (abierto) {
                                Column(modifier = Modifier.padding(top = 8.dp)) {
                                    if (detalle.reporte.rutaFoto != null) {
                                        VistaPreviaFoto(ruta = detalle.reporte.rutaFoto, altura = 150)
                                    }
                                    FilaDeDato("Reportado por", detalle.reportanteNombre)
                                    FilaDeDato(
                                        "Fecha del reporte",
                                        Fechas.formatearConHora(detalle.reporte.fechaReporte)
                                    )
                                    FilaDeDato(
                                        "Fecha de atención",
                                        Fechas.formatearOpcional(detalle.reporte.fechaCierre)
                                    )
                                    if (detalle.reporte.comentarioDocente.isNotBlank()) {
                                        FilaDeDato(
                                            "Comentario del docente",
                                            detalle.reporte.comentarioDocente
                                        )
                                    }

                                    if (estado.esAdministrador) {
                                        val pendiente = detalle.reporte.estado == EstadoReporte.INFORMADO ||
                                                detalle.reporte.estado == EstadoReporte.EN_REVISION
                                        if (pendiente) {
                                            Button(
                                                onClick = {
                                                    viewModel.cambiarEstado(
                                                        detalle.reporte.id,
                                                        EstadoReporte.ATENDIDO,
                                                        comentario
                                                    )
                                                },
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(top = 8.dp)
                                            ) {
                                                Icon(Icons.Filled.CheckCircle, contentDescription = null)
                                                Text(
                                                    "Marcar como solucionado",
                                                    modifier = Modifier.padding(start = 6.dp)
                                                )
                                            }
                                        }
                                        CampoTexto(
                                            valor = comentario,
                                            alCambiar = { comentario = it },
                                            etiqueta = "Comentario de seguimiento",
                                            lineaUnica = false,
                                            minimoDeLineas = 2,
                                            modifier = Modifier.padding(top = 8.dp)
                                        )
                                        SelectorDesplegable(
                                            etiqueta = "Cambiar estado de atención",
                                            opciones = EstadoReporte.entries.toList(),
                                            seleccionada = detalle.reporte.estado,
                                            textoDe = { it.etiqueta },
                                            alSeleccionar = {
                                                viewModel.cambiarEstado(
                                                    detalle.reporte.id,
                                                    it,
                                                    comentario
                                                )
                                            },
                                            modifier = Modifier.padding(top = 8.dp)
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
