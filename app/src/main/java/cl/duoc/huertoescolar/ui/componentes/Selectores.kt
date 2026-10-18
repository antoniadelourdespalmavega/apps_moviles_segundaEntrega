package cl.duoc.huertoescolar.ui.componentes

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Lista desplegable generica (sectores, cultivos, estudiantes, estados...).
 * Se construye con componentes estables de Material 3 para que se vea igual en
 * toda la aplicacion.
 */
@Composable
fun <T> SelectorDesplegable(
    etiqueta: String,
    opciones: List<T>,
    seleccionada: T?,
    textoDe: (T) -> String,
    alSeleccionar: (T) -> Unit,
    modifier: Modifier = Modifier,
    error: String? = null,
    textoVacio: String = "Sin opciones disponibles",
    habilitado: Boolean = true,
    ayuda: String? = null
) {
    var expandido by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxWidth()) {
        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = if (seleccionada != null) textoDe(seleccionada) else "",
                onValueChange = { },
                readOnly = true,
                enabled = habilitado,
                isError = error != null,
                label = { Text(etiqueta) },
                placeholder = { Text(if (opciones.isEmpty()) textoVacio else "Selecciona una opción") },
                trailingIcon = {
                    IconButton(
                        onClick = { if (habilitado && opciones.isNotEmpty()) expandido = true }
                    ) {
                        Icon(Icons.Filled.ArrowDropDown, contentDescription = "Desplegar " + etiqueta)
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
            // Capa transparente para abrir el menu tocando todo el campo.
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clickableSinRipple(habilitado && opciones.isNotEmpty()) { expandido = true }
            )
            DropdownMenu(
                expanded = expandido,
                onDismissRequest = { expandido = false },
                modifier = Modifier.heightIn(max = 320.dp)
            ) {
                opciones.forEach { opcion ->
                    DropdownMenuItem(
                        text = { Text(textoDe(opcion), style = MaterialTheme.typography.bodyLarge) },
                        onClick = {
                            alSeleccionar(opcion)
                            expandido = false
                        }
                    )
                }
            }
        }
        if (ayuda != null && error == null) {
            Text(
                text = ayuda,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 16.dp, top = 4.dp)
            )
        }
        MensajeDeError(error)
    }
}
