package cl.duoc.huertoescolar.ui.navegacion

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.delay
import cl.duoc.huertoescolar.ui.clima.ClimaScreen
import cl.duoc.huertoescolar.ui.sincronizacion.AvisoSinConexion
import cl.duoc.huertoescolar.ui.sincronizacion.SincronizacionViewModel
import androidx.compose.animation.AnimatedContent
import cl.duoc.huertoescolar.data.local.entity.Usuario
import cl.duoc.huertoescolar.ui.animaciones.Animaciones
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import cl.duoc.huertoescolar.domain.CategoriaTarea
import cl.duoc.huertoescolar.domain.Rol
import cl.duoc.huertoescolar.ui.componentes.BarraSuperior
import cl.duoc.huertoescolar.ui.componentes.EstadoVacio
import cl.duoc.huertoescolar.ui.cultivos.CultivoFormScreen
import cl.duoc.huertoescolar.ui.cultivos.CultivosInfoScreen
import cl.duoc.huertoescolar.ui.dashboard.DashboardScreen
import cl.duoc.huertoescolar.ui.historial.HistorialScreen
import cl.duoc.huertoescolar.ui.login.LoginScreen
import cl.duoc.huertoescolar.ui.problemas.ProblemaFormScreen
import cl.duoc.huertoescolar.ui.problemas.ProblemasScreen
import cl.duoc.huertoescolar.ui.login.RegistroScreen
import cl.duoc.huertoescolar.ui.perfil.PerfilScreen
import cl.duoc.huertoescolar.ui.revision.RevisionScreen
import cl.duoc.huertoescolar.ui.sectores.SectorDetalleScreen
import cl.duoc.huertoescolar.ui.sectores.SectorFormScreen
import cl.duoc.huertoescolar.ui.sectores.SectoresScreen
import cl.duoc.huertoescolar.ui.sensores.SensoresScreen
import cl.duoc.huertoescolar.ui.sesion.SesionViewModel
import cl.duoc.huertoescolar.ui.tareas.TareaDetalleScreen
import cl.duoc.huertoescolar.ui.tareas.TareaFormScreen
import cl.duoc.huertoescolar.ui.tareas.TareasScreen

/**
 * Navegacion de la aplicacion y control de acceso.
 *
 * Si no hay sesion iniciada, lo unico accesible es el login y el registro.
 * Las pantallas exclusivas del administrador (crear o editar sectores,
 * cultivos y tareas, y la revision de entregas) se bloquean para el perfil
 * estudiante, aunque se intente llegar a ellas por la ruta.
 */
@Composable
fun NavegacionHuerto(
    sesionViewModel: SesionViewModel = viewModel(factory = SesionViewModel.Factory)
) {
    val sesion by sesionViewModel.ui.collectAsStateWithLifecycle()
    var mostrarRegistro by rememberSaveable { mutableStateOf(false) }

    // Vista actual: login, registro o la app con sesion iniciada. El cambio
    // entre ellas se anima con un fundido y acercamiento (animacion 2).
    val vista = when {
        sesion.usuarioActual != null -> VistaPrincipal.APP
        mostrarRegistro -> VistaPrincipal.REGISTRO
        else -> VistaPrincipal.LOGIN
    }

    AnimatedContent(
        targetState = vista,
        transitionSpec = { Animaciones.cambioDeSesion() },
        label = "cambioDeSesion"
    ) { vistaActual ->
        when (vistaActual) {
            VistaPrincipal.LOGIN -> LoginScreen(
                sesionViewModel = sesionViewModel,
                alCrearCuenta = { mostrarRegistro = true }
            )

            VistaPrincipal.REGISTRO -> {
                BackHandler { mostrarRegistro = false }
                RegistroScreen(
                    alVolver = { mostrarRegistro = false },
                    alRegistrar = { nombreUsuario ->
                        sesionViewModel.cuentaCreada(nombreUsuario)
                        mostrarRegistro = false
                    }
                )
            }

            VistaPrincipal.APP -> {
                // Al cerrar sesion, la app sigue visible unos milisegundos
                // mientras se desvanece; en ese momento el usuario ya es null.
                val usuario = sesion.usuarioActual
                if (usuario != null) {
                    AppConSesion(usuario = usuario, sesionViewModel = sesionViewModel)
                } else {
                    Box(modifier = Modifier.fillMaxSize())
                }
            }
        }
    }
}

private enum class VistaPrincipal { LOGIN, REGISTRO, APP }

/**
 * Navegacion interna, solo con sesion iniciada. El NavController se crea
 * aqui, de modo que cada inicio de sesion parte limpio desde el panel.
 */
@Composable
private fun AppConSesion(
    usuario: Usuario,
    sesionViewModel: SesionViewModel
) {
    val navController = rememberNavController()
    val esAdministrador = usuario.rol == Rol.ADMINISTRADOR

    // Sincronizacion con los microservicios: al entrar y luego cada 15 segundos,
    // solo mientras la app esta visible (STARTED). Al salir de la app se detiene.
    val sincronizacionViewModel: SincronizacionViewModel =
        viewModel(factory = SincronizacionViewModel.Factory)
    val sincronizacion by sincronizacionViewModel.ui.collectAsStateWithLifecycle()
    val ciclo = LocalLifecycleOwner.current
    LaunchedEffect(ciclo) {
        ciclo.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) {
                sincronizacionViewModel.sincronizar()
                delay(SincronizacionViewModel.INTERVALO_MS)
            }
        }
    }

    // Confirmaciones de exito: un formulario que guardo bien vuelve atras y el
    // mensaje aparece en la pantalla anterior (por ejemplo, "Tarea creada...").
    val avisos = remember { SnackbarHostState() }
    val alcance = rememberCoroutineScope()
    val volverConAviso: (String) -> Unit = { mensaje ->
        navController.popBackStack()
        alcance.launch { avisos.showSnackbar(mensaje) }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        NavHost(
            navController = navController,
            startDestination = Rutas.DASHBOARD,
            // Transicion entre pantallas (animacion 1): ver Animaciones.kt
            enterTransition = Animaciones.entradaPantalla,
            exitTransition = Animaciones.salidaPantalla,
            popEnterTransition = Animaciones.entradaAlVolver,
            popExitTransition = Animaciones.salidaAlVolver
        ) {

            composable(Rutas.DASHBOARD) {
                DashboardScreen(
                    usuario = usuario,
                    alCerrarSesion = { sesionViewModel.cerrarSesion() },
                    alIrASectores = { navController.navigate(Rutas.SECTORES) },
                    alIrATareas = { categoria -> navController.navigate(Rutas.tareas(categoria?.name)) },
                    alCrearTarea = {
                        if (esAdministrador) navController.navigate(Rutas.tareaForm(0))
                    },
                    alIrAPerfil = { navController.navigate(Rutas.PERFIL) },
                    alIrACultivos = { navController.navigate(Rutas.CULTIVOS) },
                    alIrARevision = {
                        if (esAdministrador) navController.navigate(Rutas.REVISION)
                    },
                    alIrAProblemas = { navController.navigate(Rutas.PROBLEMAS) },
                    alIrAHistorial = { navController.navigate(Rutas.HISTORIAL) },
                    alIrASensores = { navController.navigate(Rutas.SENSORES) },
                    alIrAClima = { navController.navigate(Rutas.CLIMA) },
                    alAbrirTarea = { id -> navController.navigate(Rutas.tareaDetalle(id)) }
                )
            }

            composable(Rutas.SECTORES) {
                SectoresScreen(
                    esAdministrador = esAdministrador,
                    alVolver = { navController.popBackStack() },
                    alAbrirSector = { id -> navController.navigate(Rutas.sectorDetalle(id)) },
                    alCrearSector = {
                        if (esAdministrador) navController.navigate(Rutas.sectorForm(0))
                    }
                )
            }

            composable(
                route = Rutas.SECTOR_DETALLE + "/{sectorId}",
                arguments = listOf(navArgument("sectorId") { type = NavType.LongType })
            ) { entrada ->
                val sectorId = entrada.arguments?.getLong("sectorId") ?: 0L
                SectorDetalleScreen(
                    sectorId = sectorId,
                    esAdministrador = esAdministrador,
                    alVolver = { navController.popBackStack() },
                    alEditarSector = { id ->
                        if (esAdministrador) navController.navigate(Rutas.sectorForm(id))
                    },
                    alCrearCultivo = { id ->
                        if (esAdministrador) navController.navigate(Rutas.cultivoForm(id, 0))
                    },
                    alEditarCultivo = { idSector, idCultivo ->
                        if (esAdministrador) {
                            navController.navigate(Rutas.cultivoForm(idSector, idCultivo))
                        }
                    },
                    alEliminado = volverConAviso
                )
            }

            composable(
                route = Rutas.SECTOR_FORM + "/{sectorId}",
                arguments = listOf(navArgument("sectorId") { type = NavType.LongType })
            ) { entrada ->
                if (!esAdministrador) {
                    PantallaSinPermiso { navController.popBackStack() }
                    return@composable
                }
                val sectorId = entrada.arguments?.getLong("sectorId") ?: 0L
                SectorFormScreen(
                    sectorId = sectorId,
                    alVolver = { navController.popBackStack() },
                    alGuardado = volverConAviso,
                    alCrearCultivo = { idSector -> navController.navigate(Rutas.cultivoForm(idSector, 0)) },
                    alEditarCultivo = { idSector, idCultivo ->
                        navController.navigate(Rutas.cultivoForm(idSector, idCultivo))
                    }
                )
            }

            composable(
                route = Rutas.CULTIVO_FORM + "/{sectorId}/{cultivoId}",
                arguments = listOf(
                    navArgument("sectorId") { type = NavType.LongType },
                    navArgument("cultivoId") { type = NavType.LongType }
                )
            ) { entrada ->
                if (!esAdministrador) {
                    PantallaSinPermiso { navController.popBackStack() }
                    return@composable
                }
                val sectorId = entrada.arguments?.getLong("sectorId") ?: 0L
                val cultivoId = entrada.arguments?.getLong("cultivoId") ?: 0L
                CultivoFormScreen(
                    sectorId = sectorId,
                    cultivoId = cultivoId,
                    alVolver = { navController.popBackStack() },
                    alGuardado = volverConAviso,
                    alEliminado = volverConAviso
                )
            }

            composable(
                route = Rutas.TAREAS + "?categoria={categoria}",
                arguments = listOf(
                    navArgument("categoria") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    }
                )
            ) { entrada ->
                val categoria = entrada.arguments?.getString("categoria")
                    ?.let { nombre -> CategoriaTarea.entries.firstOrNull { it.name == nombre } }
                TareasScreen(
                    usuario = usuario,
                    categoriaInicial = categoria,
                    alVolver = { navController.popBackStack() },
                    alAbrirTarea = { id -> navController.navigate(Rutas.tareaDetalle(id)) },
                    alCrearTarea = {
                        if (esAdministrador) navController.navigate(Rutas.tareaForm(0))
                    }
                )
            }

            composable(
                route = Rutas.TAREA_FORM + "/{tareaId}",
                arguments = listOf(navArgument("tareaId") { type = NavType.LongType })
            ) { entrada ->
                if (!esAdministrador) {
                    PantallaSinPermiso { navController.popBackStack() }
                    return@composable
                }
                val tareaId = entrada.arguments?.getLong("tareaId") ?: 0L
                TareaFormScreen(
                    tareaId = tareaId,
                    usuarioId = usuario.id,
                    alVolver = { navController.popBackStack() },
                    alGuardado = volverConAviso
                )
            }

            composable(
                route = Rutas.TAREA_DETALLE + "/{tareaId}",
                arguments = listOf(navArgument("tareaId") { type = NavType.LongType })
            ) { entrada ->
                val tareaId = entrada.arguments?.getLong("tareaId") ?: 0L
                TareaDetalleScreen(
                    tareaId = tareaId,
                    usuario = usuario,
                    alVolver = { navController.popBackStack() },
                    alEditar = { id ->
                        if (esAdministrador) navController.navigate(Rutas.tareaForm(id))
                    },
                    alEliminada = volverConAviso
                )
            }

            composable(Rutas.CULTIVOS) {
                CultivosInfoScreen(
                    esAdministrador = esAdministrador,
                    alVolver = { navController.popBackStack() },
                    alEditarCultivo = { idSector, idCultivo ->
                        if (esAdministrador) navController.navigate(Rutas.cultivoForm(idSector, idCultivo))
                    },
                    // Sector 0: el formulario muestra el selector de sector.
                    alCrearCultivo = {
                        if (esAdministrador) navController.navigate(Rutas.cultivoForm(0, 0))
                    }
                )
            }

            composable(Rutas.REVISION) {
                if (!esAdministrador) {
                    PantallaSinPermiso { navController.popBackStack() }
                    return@composable
                }
                RevisionScreen(
                    usuario = usuario,
                    alVolver = { navController.popBackStack() },
                    alAbrirTarea = { id -> navController.navigate(Rutas.tareaDetalle(id)) }
                )
            }

            composable(Rutas.PERFIL) {
                PerfilScreen(
                    usuario = usuario,
                    alVolver = { navController.popBackStack() },
                    alActualizar = { actualizado -> sesionViewModel.actualizarUsuario(actualizado) }
                )
            }

            composable(Rutas.PROBLEMAS) {
                ProblemasScreen(
                    usuario = usuario,
                    alVolver = { navController.popBackStack() },
                    alReportar = {
                        if (!esAdministrador) navController.navigate(Rutas.PROBLEMA_FORM)
                    }
                )
            }

            composable(Rutas.PROBLEMA_FORM) {
                if (esAdministrador) {
                    PantallaSinPermiso { navController.popBackStack() }
                    return@composable
                }
                ProblemaFormScreen(
                    usuarioId = usuario.id,
                    alVolver = { navController.popBackStack() },
                    alGuardado = volverConAviso
                )
            }

            composable(Rutas.HISTORIAL) {
                HistorialScreen(
                    usuario = usuario,
                    alVolver = { navController.popBackStack() }
                )
            }

            composable(Rutas.SENSORES) {
                SensoresScreen(
                    alVolver = { navController.popBackStack() }
                )
            }

            // API externa: clima de San Bernardo (Open-Meteo)
            composable(Rutas.CLIMA) {
                ClimaScreen(
                    alVolver = { navController.popBackStack() }
                )
            }
        }
        AvisoSinConexion(
            visible = !sincronizacion.conectado,
            mensaje = sincronizacion.mensaje,
            alReintentar = { sincronizacionViewModel.sincronizar() },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
        )
        // Se ubica sobre los botones flotantes para no taparlos.
        SnackbarHost(
            hostState = avisos,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 88.dp)
        )
    }
}

/**
 * Pantalla de bloqueo: se muestra cuando un perfil sin permisos intenta entrar
 * a una pantalla reservada para el administrador.
 */
@Composable
private fun PantallaSinPermiso(alVolver: () -> Unit) {
    Scaffold(
        topBar = { BarraSuperior(titulo = "Acceso restringido", alVolver = alVolver) }
    ) { innerPadding ->
        EstadoVacio(
            titulo = "No tienes permiso para esta pantalla",
            detalle = "Esta función está disponible solo para docentes y talleristas autorizados.",
            icono = Icons.Filled.Lock,
            modifier = Modifier.padding(innerPadding)
        )
    }
}
