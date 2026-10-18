package cl.duoc.huertoescolar.ui.componentes

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.shrinkVertically
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.TextButton
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import cl.duoc.huertoescolar.core.Imagenes
import java.io.File

/**
 * Vista previa de una fotografia guardada en el almacenamiento interno.
 * Usa AsyncImage de Coil: carga la imagen en segundo plano, la reduce al
 * tamano del recuadro y la muestra con un fundido (crossfade).
 */
@Composable
fun VistaPreviaFoto(
    ruta: String?,
    modifier: Modifier = Modifier,
    altura: Int = 180
) {
    val contexto = LocalContext.current
    val existe = Imagenes.existe(ruta)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(altura.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        // La imagen entra con un fundido y un leve acercamiento (animacion 3).
        AnimatedVisibility(
            visible = existe,
            enter = fadeIn() + scaleIn(initialScale = 0.92f),
            exit = fadeOut()
        ) {
            if (ruta != null) {
                AsyncImage(
                    model = ImageRequest.Builder(contexto)
                        .data(File(ruta))
                        .crossfade(true)
                        .build(),
                    contentDescription = "Fotografía de evidencia",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
        if (!existe) {
            Text(
                text = if (ruta == null) "Sin fotografía adjunta" else "La fotografía no está disponible",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Selector de fotografia con los dos recursos nativos del dispositivo:
 * la camara y el selector de fotos (Photo Picker).
 *
 * La imagen elegida se copia a la carpeta interna de la aplicacion y solo se
 * entrega la ruta guardada, de modo que la evidencia sobreviva al reinicio.
 */
@Composable
fun SelectorDeFoto(
    rutaActual: String?,
    alSeleccionar: (String) -> Unit,
    modifier: Modifier = Modifier,
    error: String? = null,
    alAvisar: (String) -> Unit = {},
    alQuitar: (() -> Unit)? = null
) {
    val contexto = LocalContext.current
    // rememberSaveable (y no remember): mientras la camara esta abierta, Android
    // puede recrear la pantalla; asi no se pierde la ruta del archivo pendiente.
    var rutaPendiente by rememberSaveable { mutableStateOf<String?>(null) }

    val lanzadorCamara = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { exito ->
        val archivo = rutaPendiente?.let { File(it) }
        if (exito && archivo != null && archivo.exists() && archivo.length() > 0) {
            alSeleccionar(archivo.absolutePath)
        } else {
            // La captura fue cancelada: se borra el archivo vacio creado antes.
            archivo?.delete()
            alAvisar("Captura cancelada: no se guardó ninguna fotografía")
        }
        rutaPendiente = null
    }

    val lanzadorGaleria = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri == null) {
            alAvisar("No se seleccionó ninguna fotografía")
        } else {
            val ruta = Imagenes.copiarDesdeUri(contexto, uri)
            if (ruta == null) {
                alAvisar("No se pudo copiar la fotografía seleccionada")
            } else {
                alSeleccionar(ruta)
            }
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        VistaPreviaFoto(ruta = rutaActual)

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = {
                    val archivo = Imagenes.nuevoArchivo(contexto, "camara")
                    rutaPendiente = archivo.absolutePath
                    lanzadorCamara.launch(Imagenes.uriDeArchivo(contexto, archivo))
                },
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Filled.PhotoCamera,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "Cámara",
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(start = 6.dp)
                )
            }
            OutlinedButton(
                onClick = {
                    lanzadorGaleria.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Filled.PhotoLibrary,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "Galería",
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(start = 6.dp)
                )
            }
        }
        // "Quitar foto" aparece con animacion solo cuando hay una foto elegida.
        AnimatedVisibility(
            visible = rutaActual != null && alQuitar != null,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            TextButton(
                onClick = { alQuitar?.invoke() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Filled.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                Text("Quitar foto", modifier = Modifier.padding(start = 6.dp))
            }
        }
        MensajeDeError(error)
    }
}
