package cl.duoc.huertoescolar.core

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
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

    /**
     * Genera una fotografia ficticia (solo formas y texto) para los datos de
     * prueba. No se usan imagenes de personas ni datos reales.
     */
    fun crearImagenDemo(context: Context, etiqueta: String, colorFondo: Int): String {
        val ancho = 600
        val alto = 400
        val bitmap = Bitmap.createBitmap(ancho, alto, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(colorFondo)

        val tierra = Paint()
        tierra.color = Color.rgb(123, 94, 59)
        canvas.drawRect(0f, 320f, ancho.toFloat(), alto.toFloat(), tierra)

        val planta = Paint()
        planta.isAntiAlias = true
        planta.color = Color.rgb(46, 107, 62)
        canvas.drawRect(290f, 180f, 310f, 320f, planta)
        planta.color = Color.rgb(76, 154, 91)
        canvas.drawCircle(250f, 200f, 55f, planta)
        canvas.drawCircle(350f, 200f, 55f, planta)
        canvas.drawCircle(300f, 150f, 55f, planta)

        val texto = Paint()
        texto.isAntiAlias = true
        texto.color = Color.WHITE
        texto.textSize = 28f
        canvas.drawText(etiqueta, 24f, 60f, texto)
        texto.textSize = 20f
        canvas.drawText("Imagen ficticia de prueba", 24f, 92f, texto)

        val destino = nuevoArchivo(context, "demo")
        FileOutputStream(destino).use { salida ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 90, salida)
        }
        bitmap.recycle()
        return destino.absolutePath
    }
}
