package cl.duoc.huertoescolar.ui.sincronizacion

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Franja que aparece (deslizandose desde abajo) cuando no hay conexion con
 * los microservicios. La app sigue mostrando la ultima copia local.
 */
@Composable
fun AvisoSinConexion(
    visible: Boolean,
    mensaje: String?,
    alReintentar: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically { alto -> alto },
        exit = slideOutVertically { alto -> alto },
        modifier = modifier
    ) {
        Surface(
            color = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Icon(Icons.Filled.CloudOff, contentDescription = null, modifier = Modifier.size(20.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Sin conexión con el servidor", style = MaterialTheme.typography.titleSmall)
                    Text(
                        text = mensaje ?: "Se muestran los últimos datos guardados.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                TextButton(onClick = alReintentar) { Text("Reintentar") }
            }
        }
    }
}
