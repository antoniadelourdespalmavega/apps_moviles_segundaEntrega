package cl.duoc.huertoescolar.ui.tareas

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import cl.duoc.huertoescolar.domain.Alertas
import cl.duoc.huertoescolar.domain.EstadoTarea
import cl.duoc.huertoescolar.domain.Rol
import cl.duoc.huertoescolar.domain.Validaciones
import cl.duoc.huertoescolar.ui.componentes.BarraSuperior
import cl.duoc.huertoescolar.ui.componentes.CampoTexto
import cl.duoc.huertoescolar.ui.componentes.CargandoPantalla
import cl.duoc.huertoescolar.ui.componentes.ChipDeEstado
import cl.duoc.huertoescolar.ui.componentes.EstadoVacio
import cl.duoc.huertoescolar.ui.componentes.FilaDeDato
import cl.duoc.huertoescolar.ui.componentes.SelectorDeFoto
import cl.duoc.huertoescolar.ui.componentes.VistaPreviaFoto
import cl.duoc.huertoescolar.ui.theme.ColoresHuerto
import cl.duoc.huertoescolar.ui.theme.colorDeEstadoTarea

/**
 * Pantalla 9: detalle de la tarea.
 * El estudiante responsable inicia y entrega la actividad con evidencia; el
 * administrador puede validarla o rechazarla indicando un motivo.
 */
@Composable
fun TareaDetalleScreen(
    tareaId: Long,
    usuario: Usuario,
    alVolver: () -> Unit,
    alEditar: (Long) -> Unit,
    viewModel: TareaDetalleViewModel = viewModel(factory = TareaDetalleViewModel.Factory)
) {
    val estado by viewModel.ui.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var mostrarRechazo by remember { mutableStateOf(false) }

    val esAdmin = usuario.rol == Rol.ADMINISTRADOR

    LaunchedEffect(tareaId) {
        viewModel.establecerTarea(tareaId)
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
                titulo = "Detalle de la tarea",
                alVolver = alVolver,
                acciones = {
                    val detalle = estado.detalle
                    if (esAdmin && detalle != null && detalle.tarea.estado != EstadoTarea.VALIDADA) {
                        IconButton(onClick = { alEditar(tareaId) }) {
                            Icon(Icons.Filled.Edit, contentDescription = "Editar tarea")
                        }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { innerPadding ->
        val detalle = estado.detalle
        when {
            estado.cargando -> CargandoPantalla(modifier = Modifier.padding(innerPadding))
            detalle == null -> EstadoVacio(
                titulo = "La tarea no esta disponible",
                detalle = "Puede haber sido eliminada por el administrador.",
                modifier = Modifier.padding(innerPadding)
            )
            else -> {
                val esResponsable = detalle.tarea.responsableId == usuario.id
                val atrasada = Alertas.estaAtrasada(detalle)

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
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
                                        text = detalle.tarea.descripcion,
                                        style = MaterialTheme.typography.titleLarge,
                                        modifier = Modifier.weight(1f)
                                    )
                                    ChipDeEstado(
                                        texto = if (atrasada) {
                                            "Atrasada"
                                        } else {
                                            detalle.tarea.estado.etiqueta
                                        },
                                        color = colorDeEstadoTarea(detalle.tarea.estado, atrasada)
                                    )
                                }
                                FilaDeDato("Tipo", detalle.tarea.tipo.etiqueta)
                                FilaDeDato("Sector", detalle.sectorNombre)
                                FilaDeDato("Cultivo", detalle.cultivoNombre ?: "Sin cultivo asociado")
                                FilaDeDato("Responsable", detalle.responsableNombre)
                                FilaDeDato("Creada por", detalle.creadorNombre)
                                FilaDeDato(
                                    "Fecha programada",
                                    Fechas.formatear(detalle.tarea.fechaProgramada)
                                )
                                FilaDeDato(
                                    "Fecha de realizacion",
                                    Fechas.formatearOpcional(detalle.tarea.fechaRealizacion)
                                )
                            }
                        }
                    }

                    item {
                        Column {
                            Text("Instrucciones", style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = detalle.tarea.instrucciones,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }

                    // ---------------- Acciones del estudiante ----------------
                    if (!esAdmin && esResponsable) {
                        if (detalle.tarea.estado == EstadoTarea.PENDIENTE) {
                            item {
                                Button(
                                    onClick = { viewModel.iniciar(usuario.id) },
                                    enabled = !estado.procesando,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Iniciar tarea")
                                }
                            }
                        }

                        if (detalle.tarea.estado == EstadoTarea.EN_DESARROLLO ||
                            detalle.tarea.estado == EstadoTarea.RECHAZADA
                        ) {
                            item {
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Text(
                                        text = if (detalle.tarea.estado == EstadoTarea.RECHAZADA) {
                                            "Corrige la actividad y vuelve a entregarla"
                                        } else {
                                            "Entregar actividad"
                                        },
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    CampoTexto(
                                        valor = estado.comentario,
                                        alCambiar = { viewModel.cambiarComentario(it) },
                                        etiqueta = "Comentario del trabajo realizado",
                                        error = estado.errores[Validaciones.CAMPO_COMENTARIO],
                                        lineaUnica = false,
                                        minimoDeLineas = 2
                                    )
                                    SelectorDeFoto(
                                        rutaActual = estado.rutaEvidencia,
                                        alSeleccionar = { viewModel.cambiarEvidencia(it) },
                                        error = estado.errores[Validaciones.CAMPO_EVIDENCIA],
                                        alAvisar = { viewModel.avisar(it) },
                                        alQuitar = { viewModel.cambiarEvidencia(null) }
                                    )
                                    Button(
                                        onClick = { viewModel.entregar(usuario.id) },
                                        enabled = !estado.procesando,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("Entregar para validacion")
                                    }
                                }
                            }
                        }

                        if (detalle.tarea.estado == EstadoTarea.PENDIENTE_VALIDACION) {
                            item {
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = ColoresHuerto.actual.advertencia
                                            .copy(alpha = 0.14f)
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "Tu entrega esta esperando la revision de un " +
                                            "docente o tallerista.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.padding(14.dp)
                                    )
                                }
                            }
                        }
                    }

                    // --------------- Acciones del administrador ---------------
                    if (esAdmin && detalle.tarea.estado == EstadoTarea.PENDIENTE_VALIDACION) {
                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text("Revision de la actividad", style = MaterialTheme.typography.titleMedium)
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Button(
                                        onClick = { viewModel.validar(usuario.id) },
                                        enabled = !estado.procesando,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("Validar")
                                    }
                                    OutlinedButton(
                                        onClick = { mostrarRechazo = !mostrarRechazo },
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            contentColor = MaterialTheme.colorScheme.error
                                        ),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("Rechazar")
                                    }
                                }
                                // El campo de motivo aparece con animacion al rechazar.
                                AnimatedVisibility(
                                    visible = mostrarRechazo,
                                    enter = fadeIn() + expandVertically(),
                                    exit = fadeOut() + shrinkVertically()
                                ) {
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        CampoTexto(
                                            valor = estado.motivoRechazo,
                                            alCambiar = { viewModel.cambiarMotivo(it) },
                                            etiqueta = "Motivo del rechazo (obligatorio)",
                                            error = estado.errores[Validaciones.CAMPO_MOTIVO],
                                            lineaUnica = false,
                                            minimoDeLineas = 2
                                        )
                                        Button(
                                            onClick = { viewModel.rechazar(usuario.id) },
                                            enabled = !estado.procesando,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text("Confirmar rechazo")
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // ------------------------ Evidencias ----------------------
                    item {
                        Text(
                            text = "Evidencias (" + estado.evidencias.size + ")",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }

                    if (estado.evidencias.isEmpty()) {
                        item {
                            Text(
                                text = "Todavia no se adjuntan fotografias a esta tarea.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        items(estado.evidencias, key = { "evidencia-" + it.evidencia.id }) { evidencia ->
                            Card(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    VistaPreviaFoto(ruta = evidencia.evidencia.rutaFoto)
                                    Text(
                                        text = evidencia.evidencia.comentario,
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.padding(top = 8.dp)
                                    )
                                    Text(
                                        text = evidencia.usuarioNombre + " - " +
                                            Fechas.formatearConHora(evidencia.evidencia.fechaRegistro),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    // ----------------------- Validaciones ---------------------
                    if (estado.validaciones.isNotEmpty()) {
                        item {
                            Text("Historial de revisiones", style = MaterialTheme.typography.titleMedium)
                        }
                        items(estado.validaciones, key = { "validacion-" + it.validacion.id }) { validacion ->
                            Card(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = validacion.validacion.resultado.etiqueta +
                                            " por " + validacion.validadorNombre,
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Text(
                                        text = validacion.validacion.motivo,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Text(
                                        text = Fechas.formatearConHora(
                                            validacion.validacion.fechaValidacion
                                        ),
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
}
