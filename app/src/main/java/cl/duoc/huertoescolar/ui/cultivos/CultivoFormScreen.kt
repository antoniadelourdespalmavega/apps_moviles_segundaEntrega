package cl.duoc.huertoescolar.ui.cultivos

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
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import cl.duoc.huertoescolar.ui.componentes.DialogoConfirmacion
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import cl.duoc.huertoescolar.data.local.entity.Sector
import cl.duoc.huertoescolar.domain.EstadoCultivo
import cl.duoc.huertoescolar.domain.Validaciones
import cl.duoc.huertoescolar.ui.componentes.BarraSuperior
import cl.duoc.huertoescolar.ui.componentes.CampoFecha
import cl.duoc.huertoescolar.ui.componentes.CampoTexto
import cl.duoc.huertoescolar.ui.componentes.SelectorDeFoto
import cl.duoc.huertoescolar.ui.componentes.SelectorDesplegable

/**
 * Pantalla 6: formulario de cultivo (solo administrador).
 * Incluye la fotografia referencial tomada con la camara o elegida desde la
 * galeria del dispositivo.
 */
@Composable
fun CultivoFormScreen(
    sectorId: Long,
    cultivoId: Long,
    alVolver: () -> Unit,
    alGuardado: (mensaje: String) -> Unit,
    alEliminado: (mensaje: String) -> Unit,
    viewModel: CultivoFormViewModel = viewModel(factory = CultivoFormViewModel.Factory)
) {
    val estado by viewModel.ui.collectAsStateWithLifecycle()
    val sectores by viewModel.sectores.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    var confirmarEliminar by remember { mutableStateOf(false) }

    if (confirmarEliminar) {
        DialogoConfirmacion(
            titulo = "Eliminar cultivo",
            mensaje = "Se eliminará este cultivo. Un cultivo con tareas registradas no se puede eliminar.",
            textoConfirmar = "Eliminar",
            alConfirmar = {
                confirmarEliminar = false
                viewModel.eliminar(alEliminado)
            },
            alCancelar = { confirmarEliminar = false }
        )
    }

    LaunchedEffect(sectorId, cultivoId) {
        viewModel.preparar(sectorId, cultivoId)
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
                titulo = if (estado.esEdicion) "Editar cultivo" else "Nuevo cultivo",
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
            CampoTexto(
                valor = estado.nombre,
                alCambiar = { viewModel.cambiarNombre(it) },
                etiqueta = "Nombre comun de la planta",
                error = estado.errores[Validaciones.CAMPO_NOMBRE]
            )
            CampoTexto(
                valor = estado.descripcion,
                alCambiar = { viewModel.cambiarDescripcion(it) },
                etiqueta = "Descripción",
                error = estado.errores[Validaciones.CAMPO_DESCRIPCION],
                lineaUnica = false,
                minimoDeLineas = 2
            )

            SelectorDesplegable<Sector>(
                etiqueta = "Sector asignado",
                opciones = sectores,
                seleccionada = sectores.firstOrNull { it.id == estado.sectorId },
                textoDe = { it.codigo + " - " + it.nombre },
                alSeleccionar = { viewModel.cambiarSector(it.id) },
                error = estado.errores[Validaciones.CAMPO_SECTOR],
                textoVacio = "Primero debes crear un sector"
            )

            // Solo se pide la cosecha estimada (con calendario). La siembra se
            // registra automaticamente el dia en que se crea el cultivo.
            CampoFecha(
                valor = estado.fechaCosecha,
                alCambiar = { viewModel.cambiarCosecha(it) },
                etiqueta = "Fecha estimada de cosecha",
                error = estado.errores[Validaciones.CAMPO_COSECHA]
            )
            Text(
                text = "Siembra registrada: " + estado.fechaSiembra +
                        ". La cosecha no puede ser anterior a esa fecha.",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            CampoTexto(
                valor = estado.frecuenciaRiego,
                alCambiar = { viewModel.cambiarRiego(it) },
                etiqueta = "Frecuencia de riego (días)",
                error = estado.errores[Validaciones.CAMPO_RIEGO],
                teclado = KeyboardType.Number,
                ayuda = "Número mayor que cero"
            )

            SelectorDesplegable(
                etiqueta = "Estado de desarrollo",
                opciones = EstadoCultivo.entries.toList(),
                seleccionada = estado.estado,
                textoDe = { it.etiqueta },
                alSeleccionar = { viewModel.cambiarEstado(it) }
            )

            CampoTexto(
                valor = estado.observaciones,
                alCambiar = { viewModel.cambiarObservaciones(it) },
                etiqueta = "Observaciones generales",
                lineaUnica = false,
                minimoDeLineas = 2
            )

            Text(
                text = "Fotografía referencial del cultivo",
                style = MaterialTheme.typography.titleMedium
            )
            SelectorDeFoto(
                rutaActual = estado.rutaFoto,
                alSeleccionar = { viewModel.cambiarFoto(it) },
                alAvisar = { viewModel.avisar(it) },
                alQuitar = { viewModel.cambiarFoto(null) }
            )

            Button(
                onClick = { viewModel.guardar() },
                enabled = !estado.guardando,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (estado.esEdicion) "Guardar cambios" else "Registrar cultivo")
            }

            if (estado.esEdicion) {
                OutlinedButton(
                    onClick = { confirmarEliminar = true },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Eliminar cultivo")
                }
            }
        }
    }
}
