package cl.duoc.huertoescolar.core

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Utilidades de fecha.
 *
 * Se usa SimpleDateFormat / Calendar (disponibles desde API 1) para no depender
 * de java.time, que exigiria desugaring con minSdk 24.
 */
object Fechas {

    private const val PATRON_FECHA = "dd/MM/yyyy"
    private const val PATRON_FECHA_HORA = "dd/MM/yyyy HH:mm"

    private fun formatoFecha() = SimpleDateFormat(PATRON_FECHA, Locale("es", "CL"))

    private fun formatoFechaHora() = SimpleDateFormat(PATRON_FECHA_HORA, Locale("es", "CL"))

    /** Milisegundos de hoy a las 00:00. */
    fun hoy(): Long = inicioDelDia(System.currentTimeMillis())

    fun ahora(): Long = System.currentTimeMillis()

    fun inicioDelDia(millis: Long): Long {
        val c = Calendar.getInstance()
        c.timeInMillis = millis
        c.set(Calendar.HOUR_OF_DAY, 0)
        c.set(Calendar.MINUTE, 0)
        c.set(Calendar.SECOND, 0)
        c.set(Calendar.MILLISECOND, 0)
        return c.timeInMillis
    }

    fun sumarDias(millis: Long, dias: Int): Long {
        val c = Calendar.getInstance()
        c.timeInMillis = millis
        c.add(Calendar.DAY_OF_YEAR, dias)
        return c.timeInMillis
    }

    fun diasEntre(desde: Long, hasta: Long): Int {
        val unDia = 24L * 60L * 60L * 1000L
        return ((inicioDelDia(hasta) - inicioDelDia(desde)) / unDia).toInt()
    }

    fun formatear(millis: Long): String = formatoFecha().format(Date(millis))

    fun formatearConHora(millis: Long): String = formatoFechaHora().format(Date(millis))

    /**
     * El selector de fecha de Material entrega la medianoche en UTC; se formatea
     * en UTC para no correr el dia segun la zona horaria del telefono.
     */
    fun formatearDesdeUtc(millisUtc: Long): String {
        val formato = formatoFecha()
        formato.timeZone = TimeZone.getTimeZone("UTC")
        return formato.format(Date(millisUtc))
    }

    /** Convierte un texto dd/MM/yyyy en la medianoche UTC (para el selector de fecha). */
    fun parsearAUtc(texto: String): Long? {
        if (parsear(texto) == null) return null
        val formato = formatoFecha()
        formato.timeZone = TimeZone.getTimeZone("UTC")
        return try {
            formato.parse(texto.trim())?.time
        } catch (e: Exception) {
            null
        }
    }

    fun formatearOpcional(millis: Long?): String =
        if (millis == null) "Sin registro" else formatear(millis)

    /**
     * Convierte un texto dd/MM/yyyy en milisegundos.
     * Devuelve null si el texto no corresponde a una fecha real.
     */
    fun parsear(texto: String): Long? {
        val limpio = texto.trim()
        if (!Regex("""^\d{2}/\d{2}/\d{4}$""").matches(limpio)) return null
        return try {
            val formato = formatoFecha()
            formato.isLenient = false
            val fecha = formato.parse(limpio) ?: return null
            inicioDelDia(fecha.time)
        } catch (e: Exception) {
            null
        }
    }
}
