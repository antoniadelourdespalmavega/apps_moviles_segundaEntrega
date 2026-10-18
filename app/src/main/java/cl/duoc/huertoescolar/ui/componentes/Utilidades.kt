package cl.duoc.huertoescolar.ui.componentes

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed

/** Area tocable sin efecto visual, usada sobre campos de solo lectura. */
fun Modifier.clickableSinRipple(habilitado: Boolean, alTocar: () -> Unit): Modifier = composed {
    val interaccion = remember { MutableInteractionSource() }
    this.clickable(
        interactionSource = interaccion,
        indication = null,
        enabled = habilitado,
        onClick = alTocar
    )
}

/** Devuelve el texto o un guion cuando esta vacio. */
@Composable
fun textoODefecto(valor: String?, defecto: String = "Sin informacion"): String {
    return if (valor.isNullOrBlank()) defecto else valor
}
