package cl.duoc.huertoescolar.ui.problemas

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
import cl.duoc.huertoescolar.data.local.entity.Cultivo
import cl.duoc.huertoescolar.data.local.entity.Sector
import cl.duoc.huertoescolar.domain.TipoProblema
import cl.duoc.huertoescolar.domain.Validaciones
import cl.duoc.huertoescolar.ui.componentes.BarraSuperior
import cl.duoc.huertoescolar.ui.componentes.CampoTexto
import cl.duoc.huertoescolar.ui.componentes.SelectorDeFoto
import cl.duoc.huertoescolar.ui.componentes.SelectorDesplegable

/**
 * Pantalla 12: registro de un problema detectado en el huerto.
 * Solo la usa el estudiante: el docente o tallerista soluciona los problemas.
 */
@Composable
fun ProblemaFormScreen(
    usuarioId: Long,
    alVolver: () -> Unit,
    alGuardado: (mensaje: String) -> Unit,
    viewModel: ProblemaFormViewModel = viewModel(factory = ProblemaFormViewModel.Factory)
) {
    val estado by viewModel.ui.collectAsStateWithLifecycle()
    val sectores by viewModel.sectores.collectAsStateWithLifecycle()
    val cultivos by viewModel.cultivos.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    val cultivosDelSector = cultivos.filter { it.sectorId == estado.sectorId }

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
        topBar = { BarraSuperior(titulo = "Reportar problema", alVolver = alVolver) },
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
            SelectorDesplegable<Sector>(
                etiqueta = "Sector afectado",
                opciones = sectores,
                seleccionada = sectores.firstOrNull { it.id == estado.sectorId },
                textoDe = { it.codigo + " - " + it.nombre },
                alSeleccionar = { viewModel.cambiarSector(it.id) },
                error = estado.errores[Validaciones.CAMPO_SECTOR]
            )

            // null representa "sin cultivo", para poder quitar uno elegido por error.
            SelectorDesplegable<Cultivo?>(
                etiqueta = "Cultivo afectado (opcional)",
                opciones = listOf<Cultivo?>(null) + cultivosDelSector,
                seleccionada = cultivosDelSector.firstOrNull { it.id == estado.cultivoId },
                textoDe = { it?.nombre ?: "Sin cultivo especifico" },
                alSeleccionar = { viewModel.cambiarCultivo(it?.id) },
                habilitado = estado.sectorId != null,
                ayuda = when {
                    estado.sectorId == null -> "Primero elige el sector afectado"
                    cultivosDelSector.isEmpty() -> "Este sector no tiene cultivos registrados"
                    else -> null
                }
            )

            SelectorDesplegable(
                etiqueta = "Tipo de problema",
                opciones = TipoProblema.entries.toList(),
                seleccionada = estado.tipo,
                textoDe = { it.etiqueta },
                alSeleccionar = { viewModel.cambiarTipo(it) },
                error = estado.errores[Validaciones.CAMPO_TIPO]
            )

            CampoTexto(
                valor = estado.descripcion,
                alCambiar = { viewModel.cambiarDescripcion(it) },
                etiqueta = "Descripcion breve del problema",
                error = estado.errores[Validaciones.CAMPO_DESCRIPCION],
                lineaUnica = false,
                minimoDeLineas = 3,
                ayuda = "Explica que viste y donde"
            )

            Text(
                text = "Fotografia del problema (opcional)",
                style = MaterialTheme.typography.titleMedium
            )
            SelectorDeFoto(
                rutaActual = estado.rutaFoto,
                alSeleccionar = { viewModel.cambiarFoto(it) },
                alAvisar = { viewModel.avisar(it) },
                alQuitar = { viewModel.cambiarFoto(null) }
            )

            Button(
                onClick = { viewModel.registrar(usuarioId) },
                enabled = !estado.guardando,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Enviar reporte")
            }
        }
    }
}
