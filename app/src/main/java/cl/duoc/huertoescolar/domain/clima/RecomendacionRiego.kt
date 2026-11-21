package cl.duoc.huertoescolar.domain.clima

import cl.duoc.huertoescolar.domain.NivelAlerta
import kotlin.math.roundToInt

data class Recomendacion(
    val titulo: String,
    val detalle: String,
    val nivel: NivelAlerta
)

/**
 * Convierte el clima del dia en un consejo para el huerto. Es logica pura (no
 * conoce Android ni la API), por eso se prueba con JUnit. Las reglas van de la
 * mas urgente a la menos urgente.
 */
object RecomendacionRiego {

    const val TEMPERATURA_HELADA = 3.0
    const val TEMPERATURA_CALUROSA = 28.0
    const val LLUVIA_SUFICIENTE_MM = 5.0
    const val PROBABILIDAD_LLUVIA_ALTA = 70
    const val HUMEDAD_BAJA = 40

    fun calcular(clima: Clima): Recomendacion {
        val hoy = clima.dias.firstOrNull()
        val actual = clima.actual
        return when {
            hoy != null && hoy.minima <= TEMPERATURA_HELADA -> Recomendacion(
                titulo = "Riesgo de helada",
                detalle = "La mínima será de ${hoy.minima.roundToInt()} °C. Cubre los almácigos y " +
                        "riega a media mañana, nunca al atardecer.",
                nivel = NivelAlerta.ERROR
            )
            hoy != null && (hoy.lluviaMm >= LLUVIA_SUFICIENTE_MM || hoy.probabilidadLluvia >= PROBABILIDAD_LLUVIA_ALTA) ||
                    actual.precipitacion > 0.5 -> Recomendacion(
                titulo = "No es necesario regar",
                detalle = "Se espera lluvia hoy" +
                        (hoy?.let { " (${it.probabilidadLluvia}% de probabilidad, ${it.lluviaMm} mm)" } ?: "") +
                        ". Revisa que las camas no se aneguen.",
                nivel = NivelAlerta.INFORMACION
            )
            hoy != null && hoy.maxima >= TEMPERATURA_CALUROSA -> Recomendacion(
                titulo = "Día caluroso: riega temprano",
                detalle = "La máxima llegará a ${hoy.maxima.roundToInt()} °C. Riega antes de las 10:00 o " +
                        "después de las 19:00 para que el agua no se evapore.",
                nivel = NivelAlerta.ADVERTENCIA
            )
            actual.humedad < HUMEDAD_BAJA -> Recomendacion(
                titulo = "Ambiente seco: conviene regar",
                detalle = "La humedad del aire es de ${actual.humedad} %. Revisa el suelo y riega los " +
                        "cultivos que lo necesiten.",
                nivel = NivelAlerta.ADVERTENCIA
            )
            else -> Recomendacion(
                titulo = "Riego normal",
                detalle = "El clima está estable. Sigue la frecuencia de riego de cada cultivo.",
                nivel = NivelAlerta.INFORMACION
            )
        }
    }
}
