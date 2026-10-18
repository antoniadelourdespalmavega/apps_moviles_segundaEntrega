package cl.duoc.huertoescolar.ui.componentes

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.dp

/**
 * Encabezado verde con hojas decorativas, usado en el inicio de sesion y en
 * el panel principal. Las formas se dibujan con Canvas (sin imagenes), con
 * tonos del tema para que funcionen en modo claro y oscuro.
 */
@Composable
fun EncabezadoHoja(
    modifier: Modifier = Modifier,
    forma: Shape = RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp),
    contenido: @Composable ColumnScope.() -> Unit
) {
    val fondo = MaterialTheme.colorScheme.primary
    val hoja = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.14f)
    val acento = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.85f)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(forma)
            .background(fondo)
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            // Manchas organicas grandes
            drawCircle(hoja, radius = size.width * 0.38f, center = Offset(size.width * 0.95f, size.height * 0.05f))
            drawCircle(hoja, radius = size.width * 0.22f, center = Offset(-size.width * 0.04f, size.height * 0.9f))
            // Hojas pequenas
            dibujarHoja(Offset(size.width * 0.80f, size.height * 0.62f), 26.dp.toPx(), hoja)
            dibujarHoja(Offset(size.width * 0.66f, size.height * 0.25f), 16.dp.toPx(), hoja)
            dibujarHoja(Offset(size.width * 0.12f, size.height * 0.18f), 14.dp.toPx(), hoja)
            // Puntos de acento naranjo, como semillas
            drawCircle(acento, radius = 5.dp.toPx(), center = Offset(size.width * 0.88f, size.height * 0.38f))
            drawCircle(acento, radius = 3.dp.toPx(), center = Offset(size.width * 0.58f, size.height * 0.82f))
        }
        Column(
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 28.dp),
            content = contenido
        )
    }
}

/** Hoja simple: dos curvas que se juntan en las puntas. */
private fun DrawScope.dibujarHoja(centro: Offset, largo: Float, color: androidx.compose.ui.graphics.Color) {
    val ruta = Path().apply {
        moveTo(centro.x, centro.y - largo)
        quadraticTo(centro.x + largo * 0.8f, centro.y, centro.x, centro.y + largo)
        quadraticTo(centro.x - largo * 0.8f, centro.y, centro.x, centro.y - largo)
        close()
    }
    drawPath(ruta, color)
}
