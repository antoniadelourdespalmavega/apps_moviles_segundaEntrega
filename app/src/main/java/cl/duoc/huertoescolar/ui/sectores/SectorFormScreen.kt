package cl.duoc.huertoescolar.ui.sectores

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
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
import cl.duoc.huertoescolar.core.Fechas
import cl.duoc.huertoescolar.ui.componentes.ChipDeEstado
import cl.duoc.huertoescolar.ui.theme.colorDeEstadoCultivo
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import cl.duoc.huertoescolar.data.local.entity.Usuario
import cl.duoc.huertoescolar.domain.EstadoSector
import cl.duoc.huertoescolar.domain.Validaciones
import cl.duoc.huertoescolar.ui.componentes.BarraSuperior
import cl.duoc.huertoescolar.ui.componentes.CampoTexto
import cl.duoc.huertoescolar.ui.componentes.SelectorDesplegable

/**
 * Pantalla 4: formulario para crear o editar un sector (solo administrador).
 * Incluye los cultivos del sector, que se pueden agregar o editar desde aqui.
 */
@Composable
fun SectorFormScreen(
    sectorId: Long,
    alVolver: () -> Unit,
    alGuardado: (mensaje: String) -> Unit,
    alCrearCultivo: (sectorId: Long) -> Unit,
    alEditarCultivo: (sectorId: Long, cultivoId: Long) -> Unit,
    viewModel: SectorFormViewModel = viewModel(factory = SectorFormViewModel.Factory)
) {
    val estado by viewModel.ui.collectAsStateWithLifecycle()
    val responsables by viewModel.responsables.collectAsStateWithLifecycle()
    val cultivos by viewModel.cultivos.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(sectorId) {
        viewModel.cargar(sectorId)
    }

    LaunchedEffect(estado.mensaje) {
        val aviso = estado.mensaje
        if (aviso != null) {
            snackbar.showSnackbar(aviso)
            viewModel.limpiarMensaje()
        }
    }

    // Cuando el guardado termina bien, se vuelve al listado.
    // Al guardar bien se vuelve atras y la navegacion muestra la confirmacion.
    LaunchedEffect(estado.guardado) {
        if (estado.guardado) {
            alGuardado(estado.mensaje ?: "Guardado correctamente")
        }
    }

    Scaffold(
        topBar = {
            BarraSuperior(
                titulo = if (estado.esEdicion) "Editar sector" else "Nuevo sector",
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
            Text(
                text = "Los campos marcados con error deben corregirse antes de guardar.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            CampoTexto(
                valor = estado.codigo,
                alCambiar = { viewModel.cambiarCodigo(it) },
                etiqueta = "Código del sector",
                error = estado.errores[Validaciones.CAMPO_CODIGO],
                ayuda = "Entre 3 y 10 caracteres. Ejemplo: S-01"
            )
            CampoTexto(
                valor = estado.nombre,
                alCambiar = { viewModel.cambiarNombre(it) },
                etiqueta = "Nombre",
                error = estado.errores[Validaciones.CAMPO_NOMBRE]
            )
            CampoTexto(
                valor = estado.descripcion,
                alCambiar = { viewModel.cambiarDescripcion(it) },
                etiqueta = "Descripción general",
                error = estado.errores[Validaciones.CAMPO_DESCRIPCION],
                lineaUnica = false,
                minimoDeLineas = 2
            )
            CampoTexto(
                valor = estado.ubicacion,
                alCambiar = { viewModel.cambiarUbicacion(it) },
                etiqueta = "Ubicación dentro del huerto",
                error = estado.errores[Validaciones.CAMPO_UBICACION]
            )

            SelectorDesplegable(
                etiqueta = "Estado del sector",
                opciones = EstadoSector.entries.toList(),
                seleccionada = estado.estado,
                textoDe = { it.etiqueta },
                alSeleccionar = { viewModel.cambiarEstado(it) }
            )

            SelectorDesplegable<Usuario>(
                etiqueta = "Responsable (docente o tallerista)",
                opciones = responsables,
                seleccionada = responsables.firstOrNull { it.id == estado.responsableId },
                textoDe = { it.nombre },
                alSeleccionar = { viewModel.cambiarResponsable(it.id) },
                textoVacio = "No hay docentes registrados"
            )

            Button(
                onClick = { viewModel.guardar() },
                enabled = !estado.guardando,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (estado.esEdicion) "Guardar cambios" else "Crear sector")
            }

            // ------------------- Cultivos del sector -------------------
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
            Text(
                text = "Cultivos del sector (" + cultivos.size + ")",
                style = MaterialTheme.typography.titleMedium
            )
            if (!estado.esEdicion) {
                Text(
                    text = "Primero crea el sector; luego podrás agregarle cultivos aquí mismo.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                if (cultivos.isEmpty()) {
                    Text(
                        text = "Este sector todavía no tiene cultivos.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                cultivos.forEach { cultivo ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 14.dp, top = 6.dp, bottom = 6.dp, end = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(cultivo.nombre, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    text = "Riego cada " + cultivo.frecuenciaRiegoDias + " días - cosecha " +
                                            Fechas.formatear(cultivo.fechaCosechaEstimada),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            ChipDeEstado(
                                texto = cultivo.estado.etiqueta,
                                color = colorDeEstadoCultivo(cultivo.estado)
                            )
                            IconButton(onClick = { alEditarCultivo(estado.id, cultivo.id) }) {
                                Icon(Icons.Filled.Edit, contentDescription = "Editar " + cultivo.nombre)
                            }
                        }
                    }
                }
                OutlinedButton(
                    onClick = { alCrearCultivo(estado.id) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                    Text("Agregar cultivo", modifier = Modifier.padding(start = 6.dp))
                }
            }
        }
    }
}
