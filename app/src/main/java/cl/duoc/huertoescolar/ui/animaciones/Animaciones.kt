package cl.duoc.huertoescolar.ui.animaciones

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavBackStackEntry
import kotlinx.coroutines.delay

/**
 * ANIMACIONES DE LA APLICACION (todas en este archivo).
 *
 * Cada animacion tiene un nombre que dice lo que hace y un comentario que
 * explica la retroalimentacion que le da al usuario. Las pantallas solo las
 * llaman; asi es facil encontrarlas, explicarlas y reutilizarlas.
 *
 *  1. Transicion entre pantallas ........ entradaPantalla / salidaPantalla ...
 *  2. Cambio login / registro / panel ... cambioDeSesion
 *  3. Sacudida del campo con error ...... Modifier.sacudirConError
 *  4. Rebote al tocar tarjetas .......... Modifier.tocableConRebote
 *  5. Aparicion en cascada de listas .... Modifier.aparecerEnLista
 *  6. Contador animado del panel ........ NumeroAnimado
 *  7. Pulso de atencion ................. Modifier.pulsoDeAtencion
 *  8. Boton con indicador de carga ...... ContenidoConCarga
 *
 * Otras animaciones simples de Compose que tambien se usan en las pantallas:
 *  - AnimatedVisibility: mensajes de error, avisos, foto elegida, campo del
 *    codigo de autorizacion, formulario de rechazo.
 *  - animateColorAsState: color del chip de estado (ChipDeEstado).
 *  - animateContentSize: tarjetas que se expanden (reportes, revision).
 *  - Crossfade: listado de tareas al cambiar el filtro.
 */
object Animaciones {

    /** Duraciones comunes, en milisegundos. */
    const val DURACION_CORTA = 200
    const val DURACION_MEDIA = 350
    const val DURACION_PANTALLA = 400

    // ------------------------------------------------------------------
    // 1. Transicion entre pantallas (se usa en NavegacionHuerto/NavHost).
    //    Al avanzar, la pantalla nueva entra deslizandose desde la derecha;
    //    al volver, la anterior regresa desde la izquierda. Le muestra al
    //    usuario hacia donde se mueve dentro de la app.
    // ------------------------------------------------------------------

    val entradaPantalla: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
        slideIntoContainer(
            towards = AnimatedContentTransitionScope.SlideDirection.Start,
            animationSpec = tween(DURACION_PANTALLA, easing = FastOutSlowInEasing)
        ) + fadeIn(tween(DURACION_PANTALLA))
    }

    val salidaPantalla: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
        slideOutOfContainer(
            towards = AnimatedContentTransitionScope.SlideDirection.Start,
            animationSpec = tween(DURACION_PANTALLA, easing = FastOutSlowInEasing),
            targetOffset = { ancho -> ancho / 4 }
        ) + fadeOut(tween(DURACION_PANTALLA))
    }

    val entradaAlVolver: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
        slideIntoContainer(
            towards = AnimatedContentTransitionScope.SlideDirection.End,
            animationSpec = tween(DURACION_PANTALLA, easing = FastOutSlowInEasing),
            initialOffset = { ancho -> ancho / 4 }
        ) + fadeIn(tween(DURACION_PANTALLA))
    }

    val salidaAlVolver: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
        slideOutOfContainer(
            towards = AnimatedContentTransitionScope.SlideDirection.End,
            animationSpec = tween(DURACION_PANTALLA, easing = FastOutSlowInEasing)
        ) + fadeOut(tween(DURACION_PANTALLA))
    }

    // ------------------------------------------------------------------
    // 2. Cambio entre login, registro y panel principal.
    //    La vista nueva aparece con un fundido y un leve acercamiento,
    //    asi iniciar o cerrar sesion no se siente como un salto brusco.
    // ------------------------------------------------------------------

    fun cambioDeSesion(): ContentTransform =
        (fadeIn(tween(DURACION_PANTALLA)) +
                scaleIn(initialScale = 0.92f, animationSpec = tween(DURACION_PANTALLA)))
            .togetherWith(
                fadeOut(tween(DURACION_CORTA)) +
                        scaleOut(targetScale = 1.04f, animationSpec = tween(DURACION_CORTA))
            )
}

// ----------------------------------------------------------------------
// 3. Sacudida del campo con error.
//    Cuando un campo pasa a tener error (por ejemplo al presionar Guardar),
//    se mueve de lado a lado como un "no". Llama la atencion sobre el campo
//    exacto que hay que corregir. Se usa en CampoTexto, CampoClave y CampoFecha.
// ----------------------------------------------------------------------

@Composable
fun Modifier.sacudirConError(error: String?): Modifier {
    val desplazamiento = remember { Animatable(0f) }
    val tieneError = error != null
    LaunchedEffect(tieneError) {
        if (tieneError) {
            for (paso in listOf(10f, -9f, 7f, -5f, 3f, 0f)) {
                desplazamiento.animateTo(paso, tween(durationMillis = 45))
            }
        }
    }
    return this.graphicsLayer { translationX = desplazamiento.value.dp.toPx() }
}

// ----------------------------------------------------------------------
// 4. Rebote al tocar.
//    La tarjeta se achica un poco mientras se presiona y vuelve con un
//    rebote al soltarla. Confirma que el toque fue recibido. Reemplaza al
//    antiguo clickableSinRipple en tarjetas, accesos y selectores.
// ----------------------------------------------------------------------

@Composable
fun Modifier.tocableConRebote(habilitado: Boolean = true, alTocar: () -> Unit): Modifier {
    val interaccion = remember { MutableInteractionSource() }
    val presionado by interaccion.collectIsPressedAsState()
    val escala by animateFloatAsState(
        targetValue = if (presionado && habilitado) 0.95f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "reboteAlTocar"
    )
    return this
        .graphicsLayer {
            scaleX = escala
            scaleY = escala
        }
        .clickable(
            interactionSource = interaccion,
            indication = null,
            enabled = habilitado,
            onClick = alTocar
        )
}

// ----------------------------------------------------------------------
// 5. Aparicion en cascada de las listas.
//    Cada tarjeta entra subiendo y apareciendo, una tras otra (con un
//    pequeno retraso segun su posicion). La lista se "arma" frente al
//    usuario en vez de aparecer de golpe. Se combina con animateItem(),
//    que anima las tarjetas que se agregan, se quitan o cambian de lugar.
// ----------------------------------------------------------------------

@Composable
fun Modifier.aparecerEnLista(indice: Int): Modifier {
    // rememberSaveable: la lista recuerda que la tarjeta ya aparecio, asi no
    // se vuelve a animar cada vez que sale y entra de la pantalla al hacer scroll.
    var yaAparecio by rememberSaveable { mutableStateOf(false) }
    val progreso = remember { Animatable(if (yaAparecio) 1f else 0f) }
    LaunchedEffect(Unit) {
        if (!yaAparecio) {
            // Solo las primeras tarjetas esperan su turno (efecto cascada).
            if (indice < 8) delay(indice * 50L)
            progreso.animateTo(1f, tween(Animaciones.DURACION_MEDIA, easing = FastOutSlowInEasing))
            yaAparecio = true
        }
    }
    return this.graphicsLayer {
        alpha = progreso.value
        translationY = (1f - progreso.value) * 28.dp.toPx()
    }
}

// ----------------------------------------------------------------------
// 6. Contador animado.
//    Los numeros del panel (Pendientes, Por validar, Cultivos...) cuentan
//    desde 0 hasta su valor, y vuelven a contar cuando el valor cambia
//    (por ejemplo, al validar una tarea). Hace notar que el dato cambio.
// ----------------------------------------------------------------------

@Composable
fun NumeroAnimado(
    valor: Int,
    estilo: TextStyle,
    color: Color,
    modifier: Modifier = Modifier
) {
    var objetivo by remember { mutableIntStateOf(0) }
    LaunchedEffect(valor) { objetivo = valor }
    val mostrado by animateIntAsState(
        targetValue = objetivo,
        animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing),
        label = "contadorPanel"
    )
    Text(text = mostrado.toString(), style = estilo, color = color, modifier = modifier)
}

// ----------------------------------------------------------------------
// 7. Pulso de atencion.
//    Un "latido" suave y continuo que se activa solo cuando hay algo que
//    atender: entregas por validar o tareas atrasadas.
// ----------------------------------------------------------------------

@Composable
fun Modifier.pulsoDeAtencion(activo: Boolean): Modifier {
    if (!activo) return this
    val transicion = rememberInfiniteTransition(label = "pulso")
    val escala by transicion.animateFloat(
        initialValue = 1f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "escalaPulso"
    )
    return this.graphicsLayer {
        scaleX = escala
        scaleY = escala
    }
}

// ----------------------------------------------------------------------
// 8. Contenido de boton con indicador de carga.
//    Mientras se guarda, el texto del boton se transforma en una rueda de
//    carga (y vuelve al terminar). El usuario sabe que su accion se esta
//    procesando y no presiona dos veces.
// ----------------------------------------------------------------------

@Composable
fun ContenidoConCarga(
    cargando: Boolean,
    texto: String,
    negrita: Boolean = false
) {
    val colorContenido = LocalContentColor.current
    AnimatedContent(
        targetState = cargando,
        transitionSpec = {
            (fadeIn(tween(Animaciones.DURACION_CORTA)) + scaleIn(initialScale = 0.6f))
                .togetherWith(fadeOut(tween(Animaciones.DURACION_CORTA)) + scaleOut(targetScale = 0.6f))
        },
        contentAlignment = Alignment.Center,
        label = "botonConCarga"
    ) { guardando ->
        if (guardando) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp,
                color = colorContenido
            )
        } else {
            Text(
                text = texto,
                fontWeight = if (negrita) FontWeight.Bold else null
            )
        }
    }
}
