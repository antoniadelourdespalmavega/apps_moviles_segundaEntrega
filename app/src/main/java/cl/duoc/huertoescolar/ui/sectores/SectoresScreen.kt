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
import androidx.compose.material.icons.filled.Agriculture
import androidx.compose.material3.Card
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import cl.duoc.huertoescolar.ui.componentes.BarraSuperior
import cl.duoc.huertoescolar.ui.componentes.CargandoPantalla
import cl.duoc.huertoescolar.ui.componentes.ChipDeEstado
import cl.duoc.huertoescolar.ui.componentes.EstadoVacio
import cl.duoc.huertoescolar.ui.componentes.clickableSinRipple
import cl.duoc.huertoescolar.ui.theme.colorDeEstadoSector

/**
 * Pantalla 3: listado de sectores del huerto.
 * El boton para crear un sector solo se muestra al administrador.
 */
@Composable
fun SectoresScreen(
    esAdministrador: Boolean,
    alVolver: () -> Unit,
    alAbrirSector: (Long) -> Unit,
    alCrearSector: () -> Unit,
    viewModel: SectoresViewModel = viewModel(factory = SectoresViewModel.Factory)
) {
    val estado by viewModel.ui.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(estado.mensaje) {
        val aviso = estado.mensaje
        if (aviso != null) {
            snackbar.showSnackbar(aviso)
            viewModel.limpiarMensaje()
        }
    }

    Scaffold(
        topBar = { BarraSuperior(titulo = "Sectores del huerto", alVolver = alVolver) },
        snackbarHost = { SnackbarHost(snackbar) },
        floatingActionButton = {
            if (esAdministrador) {
                ExtendedFloatingActionButton(
                    onClick = alCrearSector,
                    icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                    text = { Text("Nuevo sector") }
                )
            }
        }
    ) { innerPadding ->
        when {
            estado.cargando -> CargandoPantalla(modifier = Modifier.padding(innerPadding))
            estado.sectores.isEmpty() -> EstadoVacio(
                titulo = "Aún no hay sectores",
                detalle = if (esAdministrador) {
                    "Crea el primer sector con el botón Nuevo sector."
                } else {
                    "El docente todavía no registra sectores."
                },
                icono = Icons.Filled.Agriculture,
                modifier = Modifier.padding(innerPadding)
            )
            else -> LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(estado.sectores, key = { it.sector.id }) { resumen ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .animateContentSize()
                            .clickableSinRipple(true) { alAbrirSector(resumen.sector.id) }
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = resumen.sector.codigo + " - " + resumen.sector.nombre,
                                    style = MaterialTheme.typography.titleMedium,
                                    modifier = Modifier.weight(1f)
                                )
                                ChipDeEstado(
                                    texto = resumen.sector.estado.etiqueta,
                                    color = colorDeEstadoSector(resumen.sector.estado)
                                )
                            }
                            Text(
                                text = resumen.sector.ubicacion,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = resumen.totalCultivos.toString() + " cultivos - " +
                                        resumen.tareasAbiertas + " tareas abiertas",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = "Responsable: " + (resumen.responsableNombre ?: "Sin asignar"),
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
