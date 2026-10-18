package cl.duoc.huertoescolar.ui.dashboard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Agriculture
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AddTask
import androidx.compose.material.icons.filled.Grass
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import cl.duoc.huertoescolar.core.Fechas
import cl.duoc.huertoescolar.data.local.entity.Usuario
import cl.duoc.huertoescolar.domain.Alertas
import cl.duoc.huertoescolar.domain.CategoriaTarea
import cl.duoc.huertoescolar.domain.Rol
import cl.duoc.huertoescolar.ui.componentes.BarraSuperior
import cl.duoc.huertoescolar.ui.componentes.ChipDeEstado
import cl.duoc.huertoescolar.ui.componentes.EncabezadoHoja
import cl.duoc.huertoescolar.ui.componentes.EstadoVacio
import cl.duoc.huertoescolar.ui.componentes.TarjetaAlerta
import cl.duoc.huertoescolar.ui.componentes.clickableSinRipple
import cl.duoc.huertoescolar.ui.theme.ColoresHuerto
import cl.duoc.huertoescolar.ui.theme.colorDeEstadoTarea

/**
 * Pantalla 2: panel principal. Es la misma pantalla reutilizada, pero muestra
 * accesos y datos distintos segun el rol del usuario.
 */
@Composable
fun DashboardScreen(
    usuario: Usuario,
    alCerrarSesion: () -> Unit,
    alIrASectores: () -> Unit,
    alIrATareas: (CategoriaTarea?) -> Unit,
    alCrearTarea: () -> Unit,
    alIrAPerfil: () -> Unit,
    alIrARevision: () -> Unit,
    alIrACultivos: () -> Unit,
    alIrAProblemas: () -> Unit,
    alIrAHistorial: () -> Unit,
    alIrASensores: () -> Unit,
    alAbrirTarea: (Long) -> Unit,
    viewModel: DashboardViewModel = viewModel(factory = DashboardViewModel.Factory)
) {
    val estado by viewModel.ui.collectAsStateWithLifecycle()
    val esAdmin = usuario.rol == Rol.ADMINISTRADOR

    LaunchedEffect(usuario.id) {
        viewModel.establecerUsuario(usuario)
    }

    Scaffold(
        topBar = {
            BarraSuperior(
                titulo = if (esAdmin) "Panel del " + usuario.cargo.etiqueta.lowercase() else "Mi taller",
                acciones = {
                    IconButton(onClick = alIrAPerfil) {
                        Icon(Icons.Filled.AccountCircle, contentDescription = "Mi perfil")
                    }
                    IconButton(onClick = alCerrarSesion) {
                        Icon(Icons.Filled.Logout, contentDescription = "Cerrar sesión")
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                EncabezadoHoja(forma = MaterialTheme.shapes.large) {
                    Text(
                        text = "Hola, " + usuario.nombre,
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Text(
                        text = usuario.cargo.etiqueta + " - " + usuario.taller,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f)
                    )
                }
            }

            // Avisos y recordatorios (aparecen con animacion al cargarse).
            item {
                AnimatedVisibility(
                    visible = estado.alertas.isNotEmpty(),
                    enter = fadeIn() + slideInVertically(),
                    exit = fadeOut()
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Avisos y recordatorios", style = MaterialTheme.typography.titleMedium)
                        estado.alertas.forEach { alerta ->
                            TarjetaAlerta(alerta)
                        }
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Cada tarjeta abre el listado de tareas con el mismo filtro,
                    // por eso el numero coincide con lo que se ve alli.
                    TarjetaResumen(
                        titulo = "Pendientes",
                        valor = (estado.conteo[CategoriaTarea.PENDIENTE] ?: 0).toString(),
                        color = ColoresHuerto.actual.informacion,
                        modifier = Modifier.weight(1f),
                        alTocar = { alIrATareas(CategoriaTarea.PENDIENTE) }
                    )
                    TarjetaResumen(
                        titulo = "Atrasadas",
                        valor = (estado.conteo[CategoriaTarea.ATRASADA] ?: 0).toString(),
                        color = ColoresHuerto.actual.error,
                        modifier = Modifier.weight(1f),
                        alTocar = { alIrATareas(CategoriaTarea.ATRASADA) }
                    )
                    TarjetaResumen(
                        titulo = if (esAdmin) "Por validar" else "En revisión",
                        valor = (estado.conteo[CategoriaTarea.POR_VALIDAR] ?: 0).toString(),
                        color = ColoresHuerto.actual.advertencia,
                        modifier = Modifier.weight(1f),
                        // El administrador va a la pantalla de revision; el
                        // estudiante ve sus entregas en revision.
                        alTocar = {
                            if (esAdmin) alIrARevision() else alIrATareas(CategoriaTarea.POR_VALIDAR)
                        }
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    TarjetaResumen(
                        titulo = "Validadas",
                        valor = (estado.conteo[CategoriaTarea.VALIDADA] ?: 0).toString(),
                        color = ColoresHuerto.actual.exito,
                        modifier = Modifier.weight(1f),
                        alTocar = { alIrATareas(CategoriaTarea.VALIDADA) }
                    )
                    TarjetaResumen(
                        titulo = "Rechazadas",
                        valor = (estado.conteo[CategoriaTarea.RECHAZADA] ?: 0).toString(),
                        color = ColoresHuerto.actual.error,
                        modifier = Modifier.weight(1f),
                        alTocar = { alIrATareas(CategoriaTarea.RECHAZADA) }
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    TarjetaResumen(
                        titulo = "Cultivos",
                        valor = estado.totalCultivos.toString(),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f),
                        alTocar = alIrACultivos
                    )
                    TarjetaResumen(
                        titulo = "Problemas",
                        valor = estado.problemasAbiertos.toString(),
                        color = ColoresHuerto.actual.error,
                        modifier = Modifier.weight(1f),
                        alTocar = alIrAProblemas
                    )
                    TarjetaResumen(
                        titulo = "Sectores",
                        valor = estado.totalSectores.toString(),
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.weight(1f),
                        alTocar = alIrASectores
                    )
                }
            }

            item {
                Text("Accesos", style = MaterialTheme.typography.titleMedium)
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    BotonAcceso(
                        texto = "Sectores",
                        icono = Icons.Filled.Agriculture,
                        alTocar = alIrASectores,
                        modifier = Modifier.weight(1f)
                    )
                    BotonAcceso(
                        texto = "Cultivos",
                        icono = Icons.Filled.Grass,
                        alTocar = alIrACultivos,
                        modifier = Modifier.weight(1f)
                    )
                    BotonAcceso(
                        texto = if (esAdmin) "Tareas" else "Mis tareas",
                        icono = Icons.Filled.Assignment,
                        alTocar = { alIrATareas(null) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    BotonAcceso(
                        texto = "Problemas",
                        icono = Icons.Filled.ReportProblem,
                        alTocar = alIrAProblemas,
                        modifier = Modifier.weight(1f)
                    )
                    BotonAcceso(
                        texto = "Historial",
                        icono = Icons.Filled.History,
                        alTocar = alIrAHistorial,
                        modifier = Modifier.weight(1f)
                    )
                    BotonAcceso(
                        texto = "Sensores",
                        icono = Icons.Filled.Sensors,
                        alTocar = alIrASensores,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Crear tareas es exclusivo del docente o tallerista. Las entregas
            // se revisan desde la tarjeta "Por validar" (pantalla de revision).
            if (esAdmin) {
                item {
                    BotonAcceso(
                        texto = "Nueva tarea",
                        icono = Icons.Filled.AddTask,
                        alTocar = alCrearTarea,
                        modifier = Modifier.fillMaxWidth(),
                        destacado = true
                    )
                }
            }

            item {
                Text(
                    text = if (esAdmin) "Actividades en curso" else "Mis próximas tareas",
                    style = MaterialTheme.typography.titleMedium
                )
            }

            if (estado.tareasDelUsuario.isEmpty()) {
                item {
                    EstadoVacio(
                        titulo = "No hay tareas abiertas",
                        detalle = if (esAdmin) {
                            "Usa el botón Nueva tarea para crear y asignar la primera."
                        } else {
                            "Cuando el docente te asigne una tarea, aparecerá aquí."
                        },
                        icono = Icons.Filled.Grass
                    )
                }
            } else {
                items(estado.tareasDelUsuario, key = { it.tarea.id }) { detalle ->
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
                                    texto = if (atrasada) "Atrasada" else detalle.tarea.estado.etiqueta,
                                    color = colorDeEstadoTarea(detalle.tarea.estado, atrasada)
                                )
                            }
                            Text(
                                text = detalle.sectorNombre + " - " +
                                        Fechas.formatear(detalle.tarea.fechaProgramada),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (esAdmin) {
                                Text(
                                    text = "Responsable: " + detalle.responsableNombre,
                                    style = MaterialTheme.typography.bodyMedium,
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

@Composable
private fun TarjetaResumen(
    titulo: String,
    valor: String,
    color: Color,
    modifier: Modifier = Modifier,
    alTocar: (() -> Unit)? = null
) {
    Card(
        modifier = modifier.clickableSinRipple(alTocar != null) { alTocar?.invoke() },
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.12f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = valor, style = MaterialTheme.typography.headlineMedium, color = color)
            Text(
                text = titulo,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun BotonAcceso(
    texto: String,
    icono: ImageVector,
    alTocar: () -> Unit,
    modifier: Modifier = Modifier,
    destacado: Boolean = false
) {
    // El acceso destacado (Nueva tarea) usa el naranjo de acento.
    val fondo = if (destacado) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.secondaryContainer
    val contenido = if (destacado) MaterialTheme.colorScheme.onTertiary else MaterialTheme.colorScheme.onSecondaryContainer
    Card(
        modifier = modifier.clickableSinRipple(true, alTocar),
        colors = CardDefaults.cardColors(containerColor = fondo)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icono,
                    contentDescription = null,
                    tint = contenido,
                    modifier = Modifier.size(24.dp)
                )
            }
            Text(
                text = texto,
                style = MaterialTheme.typography.labelLarge,
                color = contenido,
                textAlign = TextAlign.Center
            )
        }
    }
}
