package cl.duoc.huertoescolar.ui.tareas

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import cl.duoc.huertoescolar.data.local.entity.Sector
import cl.duoc.huertoescolar.data.local.entity.Usuario
import cl.duoc.huertoescolar.domain.TipoTarea
import cl.duoc.huertoescolar.domain.Validaciones
import cl.duoc.huertoescolar.ui.componentes.BarraSuperior
import cl.duoc.huertoescolar.ui.componentes.CampoFecha
import cl.duoc.huertoescolar.ui.componentes.CampoTexto
import cl.duoc.huertoescolar.ui.componentes.SelectorDesplegable

/** Opcion del selector de cultivo: id null = sin cultivo asociado. */
private data class OpcionCultivo(val id: Long?, val texto: String)

/**
 * Pantalla 8: creacion y edicion de tareas con asignacion de responsable.
 * Solo el administrador llega a esta pantalla.
 */
@Composable
fun TareaFormScreen(
    tareaId: Long,
    usuarioId: Long,
    alVolver: () -> Unit,
    alGuardado: (mensaje: String) -> Unit,
    viewModel: TareaFormViewModel = viewModel(factory = TareaFormViewModel.Factory)
) {
    val estado by viewModel.ui.collectAsStateWithLifecycle()
    val sectores by viewModel.sectores.collectAsStateWithLifecycle()
    val cultivos by viewModel.cultivos.collectAsStateWithLifecycle()
    val estudiantes by viewModel.estudiantes.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    val cultivosDelSector = cultivos.filter { it.sectorId == estado.sectorId }
    // "Sin cultivo" (id null) permite quitar un cultivo elegido por error.
    val opcionesCultivo = listOf(OpcionCultivo(null, "Sin cultivo asociado")) +
            cultivosDelSector.map { OpcionCultivo(it.id, it.nombre) }

    LaunchedEffect(tareaId) {
        viewModel.cargar(tareaId)
    }

    LaunchedEffect(estado.mensaje) {
        val aviso = estado.mensaje
        if (aviso != null) {
            snackbar.showSnackbar(aviso)
            viewModel.limpiarMensaje()
        }
    }

    // Al guardar bien se vuelve atras y la navegacion muestra la confirmacion.
    LaunchedEffect(estado.guardado) {
        if (estado.guardado) {
            alGuardado(estado.mensaje ?: "Guardado correctamente")
        }
    }

    Scaffold(
        topBar = {
            BarraSuperior(
                titulo = if (estado.esEdicion) "Editar tarea" else "Nueva tarea",
                alVolver = alVolver
            )
        },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SelectorDesplegable(
                etiqueta = "Tipo de tarea",
                opciones = TipoTarea.entries.toList(),
                seleccionada = estado.tipo,
                textoDe = { it.etiqueta },
                alSeleccionar = { viewModel.cambiarTipo(it) },
                error = estado.errores[Validaciones.CAMPO_TIPO]
            )

            CampoTexto(
                valor = estado.descripcion,
                alCambiar = { viewModel.cambiarDescripcion(it) },
                etiqueta = "Descripción de la tarea",
                error = estado.errores[Validaciones.CAMPO_DESCRIPCION]
            )

            CampoTexto(
                valor = estado.instrucciones,
                alCambiar = { viewModel.cambiarInstrucciones(it) },
                etiqueta = "Instrucciones para el estudiante",
                error = estado.errores[Validaciones.CAMPO_INSTRUCCIONES],
                lineaUnica = false,
                minimoDeLineas = 3
            )

            SelectorDesplegable<Sector>(
                etiqueta = "Sector",
                opciones = sectores,
                seleccionada = sectores.firstOrNull { it.id == estado.sectorId },
                textoDe = { it.codigo + " - " + it.nombre },
                alSeleccionar = { viewModel.cambiarSector(it.id) },
                error = estado.errores[Validaciones.CAMPO_SECTOR]
            )

            SelectorDesplegable(
                etiqueta = "Cultivo relacionado (opcional)",
                opciones = opcionesCultivo,
                seleccionada = opcionesCultivo.firstOrNull { it.id == estado.cultivoId }
                    .takeIf { estado.cultivoId != null },
                textoDe = { it.texto },
                alSeleccionar = { viewModel.cambiarCultivo(it.id) },
                habilitado = estado.sectorId != null,
                ayuda = when {
                    estado.sectorId == null -> "Primero elige un sector"
                    cultivosDelSector.isEmpty() -> "Este sector no tiene cultivos registrados"
                    else -> null
                }
            )

            SelectorDesplegable<Usuario>(
                etiqueta = "Estudiante responsable",
                opciones = estudiantes,
                seleccionada = estudiantes.firstOrNull { it.id == estado.responsableId },
                textoDe = { it.nombre + " - " + it.taller },
                alSeleccionar = { viewModel.cambiarResponsable(it.id) },
                error = estado.errores[Validaciones.CAMPO_RESPONSABLE],
                textoVacio = "No hay estudiantes activos"
            )

            CampoFecha(
                valor = estado.fechaProgramada,
                alCambiar = { viewModel.cambiarFecha(it) },
                etiqueta = "Fecha programada",
                error = estado.errores[Validaciones.CAMPO_FECHA_PROGRAMADA]
            )

            Text(
                text = "La tarea se crea en estado Pendiente y debe ser validada por un " +
                        "docente o tallerista cuando el estudiante la entregue.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Button(
                onClick = { viewModel.guardar(usuarioId) },
                enabled = !estado.guardando,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (estado.esEdicion) "Guardar cambios" else "Crear y asignar tarea")
            }
        }
    }
}
