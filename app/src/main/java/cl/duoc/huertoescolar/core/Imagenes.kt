package cl.duoc.huertoescolar.core

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

/**
 * Manejo de las fotografias de evidencias y reportes.
 *
 * Todas las imagenes se copian a una carpeta interna de la aplicacion y en la
 * base de datos solo se guarda la ruta. Asi la evidencia sigue disponible
 * despues de reiniciar la aplicacion, aunque el usuario borre la foto original
 * de la galeria.
 */
object Imagenes {

    private const val CARPETA = "evidencias"

    fun carpeta(context: Context): File {
        val dir = File(context.filesDir, CARPETA)
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    /** Archivo vacio donde la camara dejara la fotografia capturada. */
    fun nuevoArchivo(context: Context, prefijo: String = "foto"): File {
        return File(carpeta(context), prefijo + "_" + System.currentTimeMillis() + ".jpg")
    }

    /** Uri compartible con la aplicacion de camara (FileProvider). */
    fun uriDeArchivo(context: Context, archivo: File): Uri {
        return FileProvider.getUriForFile(
            context,
            context.packageName + ".fileprovider",
            archivo
        )
    }

    /**
     * Copia a la carpeta interna la imagen elegida en el selector de fotos.
     * Devuelve la ruta guardada o null si la copia no se pudo realizar.
     */
    fun copiarDesdeUri(context: Context, uri: Uri): String? {
        return try {
            val destino = nuevoArchivo(context, "galeria")
            context.contentResolver.openInputStream(uri).use { entrada ->
                if (entrada == null) return null
                FileOutputStream(destino).use { salida ->
                    entrada.copyTo(salida)
                }
            }
            destino.absolutePath
        } catch (e: Exception) {
            null
        }
    }

    fun existe(ruta: String?): Boolean = ruta != null && File(ruta).exists()

    fun eliminar(ruta: String?) {
        if (ruta == null) return
        val archivo = File(ruta)
        if (archivo.exists()) archivo.delete()
    }
}
