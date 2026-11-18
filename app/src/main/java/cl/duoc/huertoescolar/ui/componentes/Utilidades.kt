package cl.duoc.huertoescolar.ui.componentes

import androidx.compose.runtime.Composable

/** Devuelve el texto o un guion cuando esta vacio. */
@Composable
fun textoODefecto(valor: String?, defecto: String = "Sin información"): String {
    return if (valor.isNullOrBlank()) defecto else valor
}
