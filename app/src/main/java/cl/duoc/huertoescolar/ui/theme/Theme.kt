package cl.duoc.huertoescolar.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import cl.duoc.huertoescolar.domain.EstadoCultivo
import cl.duoc.huertoescolar.domain.EstadoLectura
import cl.duoc.huertoescolar.domain.EstadoReporte
import cl.duoc.huertoescolar.domain.EstadoSector
import cl.duoc.huertoescolar.domain.EstadoTarea
import cl.duoc.huertoescolar.domain.NivelAlerta

/**
 * Colores semanticos (exito, advertencia, informacion, error) y el color que
 * corresponde a cada estado de tarea, cultivo, sector y reporte.
 * Se exponen a traves del tema para no repetir hexadecimales en las pantallas.
 */
data class ColoresSemanticos(
    val exito: Color,
    val advertencia: Color,
    val informacion: Color,
    val error: Color,
    val neutro: Color,
    val contenedorSuave: Color
)

private val ColoresSemanticosClaros = ColoresSemanticos(
    exito = ExitoClaro,
    advertencia = AdvertenciaClaro,
    informacion = InformacionClaro,
    error = ErrorClaro,
    neutro = NeutroClaro,
    contenedorSuave = VerdeNiebla
)

private val ColoresSemanticosOscuros = ColoresSemanticos(
    exito = ExitoOscuro,
    advertencia = AdvertenciaOscuro,
    informacion = InformacionOscuro,
    error = ErrorOscuro,
    neutro = NeutroOscuro,
    contenedorSuave = SuperficieOscura
)

val LocalColoresSemanticos = staticCompositionLocalOf { ColoresSemanticosClaros }

/** Acceso corto: ColoresHuerto.exito dentro de cualquier Composable. */
object ColoresHuerto {
    val actual: ColoresSemanticos
        @Composable
        @ReadOnlyComposable
        get() = LocalColoresSemanticos.current
}

@Composable
fun colorDeEstadoTarea(estado: EstadoTarea, atrasada: Boolean = false): Color {
    val colores = ColoresHuerto.actual
    if (atrasada) return colores.error
    return when (estado) {
        EstadoTarea.PENDIENTE -> colores.informacion
        EstadoTarea.EN_DESARROLLO -> colores.advertencia
        EstadoTarea.PENDIENTE_VALIDACION -> colores.advertencia
        EstadoTarea.VALIDADA -> colores.exito
        EstadoTarea.RECHAZADA -> colores.error
    }
}

@Composable
fun colorDeEstadoCultivo(estado: EstadoCultivo): Color {
    val colores = ColoresHuerto.actual
    return when (estado) {
        EstadoCultivo.PLANIFICADO -> colores.neutro
        EstadoCultivo.SEMBRADO -> colores.informacion
        EstadoCultivo.EN_CRECIMIENTO -> colores.exito
        EstadoCultivo.LISTO_PARA_COSECHAR -> colores.advertencia
        EstadoCultivo.COSECHADO -> colores.exito
        EstadoCultivo.CON_PROBLEMAS -> colores.error
        EstadoCultivo.FINALIZADO -> colores.neutro
    }
}

@Composable
fun colorDeEstadoSector(estado: EstadoSector): Color {
    val colores = ColoresHuerto.actual
    return when (estado) {
        EstadoSector.DISPONIBLE -> colores.informacion
        EstadoSector.EN_USO -> colores.exito
        EstadoSector.EN_MANTENIMIENTO -> colores.advertencia
        EstadoSector.FUERA_DE_USO -> colores.error
    }
}

@Composable
fun colorDeEstadoReporte(estado: EstadoReporte): Color {
    val colores = ColoresHuerto.actual
    return when (estado) {
        EstadoReporte.INFORMADO -> colores.error
        EstadoReporte.EN_REVISION -> colores.advertencia
        EstadoReporte.ATENDIDO -> colores.exito
        EstadoReporte.CERRADO -> colores.neutro
    }
}

@Composable
fun colorDeEstadoLectura(estado: EstadoLectura): Color {
    val colores = ColoresHuerto.actual
    return when (estado) {
        EstadoLectura.NORMAL -> colores.exito
        EstadoLectura.BAJO -> colores.advertencia
        EstadoLectura.ALTO -> colores.advertencia
        EstadoLectura.PENDIENTE_REVISION -> colores.informacion
    }
}

@Composable
fun colorDeNivelAlerta(nivel: NivelAlerta): Color {
    val colores = ColoresHuerto.actual
    return when (nivel) {
        NivelAlerta.INFORMACION -> colores.informacion
        NivelAlerta.ADVERTENCIA -> colores.advertencia
        NivelAlerta.ERROR -> colores.error
    }
}

/*
 * Verde lima como color principal (barras, botones), naranjo como acento
 * (botones flotantes y acciones destacadas) y tarjetas blancas sobre un
 * fondo verde muy claro, siguiendo la referencia visual del equipo.
 */
private val EsquemaClaro = lightColorScheme(
    primary = VerdeLima,
    onPrimary = Blanco,
    primaryContainer = Naranja,
    onPrimaryContainer = Blanco,
    secondary = VerdeLimaOscuro,
    onSecondary = Blanco,
    secondaryContainer = VerdeBrote,
    onSecondaryContainer = VerdeBosque,
    tertiary = Naranja,
    onTertiary = Blanco,
    tertiaryContainer = NaranjaClaro,
    onTertiaryContainer = NaranjaOscuro,
    background = FondoClaro,
    onBackground = GrisTexto,
    surface = FondoClaro,
    onSurface = GrisTexto,
    surfaceVariant = VerdeNiebla,
    onSurfaceVariant = GrisSuave,
    // Las Card de Material 3 usan estos tonos: tarjetas blancas.
    surfaceContainerLowest = Blanco,
    surfaceContainerLow = Blanco,
    surfaceContainer = Blanco,
    surfaceContainerHigh = Blanco,
    surfaceContainerHighest = Blanco,
    outline = VerdeLimaClaro,
    error = ErrorClaro,
    onError = Blanco
)

private val EsquemaOscuro = darkColorScheme(
    primary = VerdeLimaClaro,
    onPrimary = VerdeBosque,
    primaryContainer = Naranja,
    onPrimaryContainer = Blanco,
    secondary = VerdeLimaClaro,
    onSecondary = VerdeBosque,
    secondaryContainer = VerdeLimaOscuro,
    onSecondaryContainer = VerdeBrote,
    tertiary = Naranja,
    onTertiary = Blanco,
    tertiaryContainer = NaranjaOscuro,
    onTertiaryContainer = NaranjaClaro,
    background = FondoOscuro,
    onBackground = TextoOscuro,
    surface = FondoOscuro,
    onSurface = TextoOscuro,
    surfaceVariant = SuperficieOscura,
    onSurfaceVariant = NeutroOscuro,
    surfaceContainerLowest = SuperficieOscura,
    surfaceContainerLow = SuperficieOscura,
    surfaceContainer = SuperficieOscura,
    surfaceContainerHigh = SuperficieOscura,
    surfaceContainerHighest = SuperficieOscura,
    error = ErrorOscuro,
    onError = ErrorClaro
)

/** Esquinas bien redondeadas en tarjetas, campos y botones. */
private val FormasHuerto = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

@Composable
fun HuertoEscolarTheme(
    temaOscuro: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val esquema = if (temaOscuro) EsquemaOscuro else EsquemaClaro
    val semanticos = if (temaOscuro) ColoresSemanticosOscuros else ColoresSemanticosClaros

    CompositionLocalProvider(LocalColoresSemanticos provides semanticos) {
        MaterialTheme(
            colorScheme = esquema,
            typography = Typography,
            shapes = FormasHuerto,
            content = content
        )
    }
}
