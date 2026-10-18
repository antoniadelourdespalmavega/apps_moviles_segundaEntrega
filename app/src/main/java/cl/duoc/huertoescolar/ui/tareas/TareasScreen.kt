package cl.duoc.huertoescolar.ui.tareas

import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material3.Card
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import cl.duoc.huertoescolar.core.Fechas
import cl.duoc.huertoescolar.data.local.entity.Usuario
import cl.duoc.huertoescolar.domain.Alertas
import cl.duoc.huertoescolar.domain.CategoriaTarea
import cl.duoc.huertoescolar.ui.componentes.BarraSuperior
import cl.duoc.huertoescolar.ui.componentes.CargandoPantalla
import cl.duoc.huertoescolar.ui.componentes.ChipDeEstado
import cl.duoc.huertoescolar.ui.componentes.EstadoVacio
import cl.duoc.huertoescolar.ui.componentes.SelectorDesplegable
import cl.duoc.huertoescolar.ui.componentes.clickableSinRipple
import cl.duoc.huertoescolar.ui.theme.colorDeEstadoTarea

/**
 * Pantalla 7: listado de tareas con filtros por estado, fecha y responsable.
 */
@Composable
fun TareasScreen(
    usuario: Usuario,
    alVolver: () -> Unit,
    alAbrirTarea: (Long) -> Unit,
    alCrearTarea: () -> Unit,
    categoriaInicial: CategoriaTarea? = null,
    viewModel: TareasViewModel = viewModel(factory = TareasViewModel.Factory)
) {
    val estado by viewModel.ui.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(usuario.id) {
        viewModel.establecerUsuario(usuario)
        viewModel.aplicarCategoriaInicial(categoriaInicial)
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
                titulo = if (estado.esAdministrador) "Tareas del huerto" else "Mis tareas",
                alVolver = alVolver
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
        floatingActionButton = {
            if (estado.esAdministrador) {
                ExtendedFloatingActionButton(
                    onClick = alCrearTarea,
                    icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                    text = { Text("Nueva tarea") }
                )
            }
        }
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
                    selected = estado.filtros.categoria == null &&
                        estado.filtros.fecha == FiltroFecha.TODAS &&
                        estado.filtros.responsableId == null,
                    onClick = { viewModel.limpiarFiltros() },
                    label = { Text("Todas (" + estado.total + ")") }
                )
                // Las mismas categorias y numeros que el panel principal.
                CategoriaTarea.entries.forEach { valor ->
                    FilterChip(
                        selected = estado.filtros.categoria == valor,
                        onClick = {
                            viewModel.filtrarPorCategoria(
                                if (estado.filtros.categoria == valor) null else valor
                            )
                        },
                        label = { Text(valor.etiqueta + " (" + (estado.conteo[valor] ?: 0) + ")") }
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
                    opciones = FiltroFecha.entries.toList(),
                    seleccionada = estado.filtros.fecha,
                    textoDe = { it.etiqueta },
                    alSeleccionar = { viewModel.filtrarPorFecha(it) },
                    modifier = Modifier.weight(1f)
                )
                if (estado.esAdministrador) {
                    SelectorDesplegable<Usuario>(
                        etiqueta = "Responsable",
                        opciones = estado.estudiantes,
                        seleccionada = estado.estudiantes.firstOrNull {
                            it.id == estado.filtros.responsableId
                        },
                        textoDe = { it.nombre },
                        alSeleccionar = { viewModel.filtrarPorResponsable(it.id) },
                        modifier = Modifier.weight(1f),
                        textoVacio = "Sin estudiantes"
                    )
                }
            }

            // ------------------------- Listado -------------------------
            // Crossfade: el listado cambia suavemente al aplicar un filtro.
            Crossfade(
                targetState = estado.tareas,
                label = "listadoTareas",
                modifier = Modifier.fillMaxSize()
            ) { tareas ->
                when {
                    estado.cargando -> CargandoPantalla()
                    tareas.isEmpty() -> EstadoVacio(
                        titulo = if (estado.total == 0) "Aun no hay tareas" else "No hay tareas para este filtro",
                        detalle = when {
                            estado.total > 0 -> "Cambia los filtros para ver otras tareas."
                            estado.esAdministrador -> "Usa el boton Nueva tarea para crear y asignar la primera."
                            else -> "Cuando el docente o tallerista te asigne una tarea, aparecera aqui."
                        },
                        icono = Icons.Filled.Assignment
                    )
                    else -> LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(tareas, key = { it.tarea.id }) { detalle ->
                            val atrasada = Alertas.estaAtrasada(detalle)
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .animateContentSize()
                                    .clickableSinRipple(true) { alAbrirTarea(detalle.tarea.id) }
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = detalle.tarea.descripcion,
                                            style = MaterialTheme.typography.titleMedium,
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
                                    Text(
                                        text = detalle.tarea.tipo.etiqueta + " - " + detalle.sectorNombre,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "Programada: " +
                                            Fechas.formatear(detalle.tarea.fechaProgramada),
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Text(
                                        text = "Responsable: " + detalle.responsableNombre,
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
