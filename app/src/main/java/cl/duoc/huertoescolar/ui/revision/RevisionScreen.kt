package cl.duoc.huertoescolar.ui.revision

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
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
import androidx.compose.material.icons.filled.FactCheck
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import cl.duoc.huertoescolar.core.Fechas
import cl.duoc.huertoescolar.data.local.entity.Usuario
import cl.duoc.huertoescolar.ui.componentes.BarraSuperior
import cl.duoc.huertoescolar.ui.componentes.CampoTexto
import cl.duoc.huertoescolar.ui.componentes.CargandoPantalla
import cl.duoc.huertoescolar.ui.componentes.EstadoVacio
import cl.duoc.huertoescolar.ui.componentes.VistaPreviaFoto

/**
 * Pantalla de revision (solo administrador): entregas pendientes de validacion
 * con su evidencia, para validarlas o rechazarlas con motivo.
 * Se abre desde la tarjeta "Por validar" del panel principal.
 */
@Composable
fun RevisionScreen(
    usuario: Usuario,
    alVolver: () -> Unit,
    alAbrirTarea: (Long) -> Unit,
    viewModel: RevisionViewModel = viewModel(factory = RevisionViewModel.Factory)
) {
    val estado by viewModel.ui.collectAsStateWithLifecycle()
    val form = estado.formulario
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(form.mensaje) {
        val aviso = form.mensaje
        if (aviso != null) {
            snackbar.showSnackbar(aviso)
            viewModel.limpiarMensaje()
        }
    }

    Scaffold(
        topBar = { BarraSuperior(titulo = "Revisión de actividades", alVolver = alVolver) },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { innerPadding ->
        when {
            estado.cargando -> CargandoPantalla(modifier = Modifier.padding(innerPadding))
            estado.entregas.isEmpty() -> EstadoVacio(
                titulo = "No hay entregas por revisar",
                detalle = "Cuando un estudiante entregue una tarea, aparecerá aquí para validarla o rechazarla.",
                icono = Icons.Filled.FactCheck,
                modifier = Modifier.padding(innerPadding)
            )
            else -> LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(estado.entregas, key = { it.detalle.tarea.id }) { entrega ->
                    val tarea = entrega.detalle.tarea
                    val rechazando = form.rechazandoId == tarea.id
                    Card(modifier = Modifier.fillMaxWidth().animateContentSize()) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(tarea.descripcion, style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = entrega.detalle.responsableNombre + " - " + entrega.detalle.sectorNombre,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            entrega.evidencia?.let { evidencia ->
                                VistaPreviaFoto(ruta = evidencia.evidencia.rutaFoto)
                                Text(
                                    text = "\"" + evidencia.evidencia.comentario + "\"",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = "Entregada el " + Fechas.formatearConHora(evidencia.evidencia.fechaRegistro),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Button(
                                    onClick = { viewModel.validar(tarea.id, usuario.id) },
                                    enabled = !form.procesando,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Validar")
                                }
                                OutlinedButton(
                                    onClick = { viewModel.alternarRechazo(tarea.id) },
                                    enabled = !form.procesando,
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = MaterialTheme.colorScheme.error
                                    ),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(if (rechazando) "Cancelar" else "Rechazar")
                                }
                            }

                            // El motivo de rechazo aparece con animacion y es obligatorio.
                            AnimatedVisibility(
                                visible = rechazando,
                                enter = fadeIn() + expandVertically(),
                                exit = fadeOut() + shrinkVertically()
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    CampoTexto(
                                        valor = form.motivo,
                                        alCambiar = { viewModel.cambiarMotivo(it) },
                                        etiqueta = "Motivo del rechazo (obligatorio)",
                                        error = form.error,
                                        lineaUnica = false,
                                        minimoDeLineas = 2
                                    )
                                    Button(
                                        onClick = { viewModel.rechazar(tarea.id, usuario.id) },
                                        enabled = !form.procesando,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("Confirmar rechazo")
                                    }
                                }
                            }

                            TextButton(onClick = { alAbrirTarea(tarea.id) }) {
                                Text("Ver detalle completo")
                            }
                        }
                    }
                }
            }
        }
    }
}
