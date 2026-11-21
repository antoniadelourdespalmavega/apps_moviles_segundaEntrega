package cl.duoc.huertoescolar.domain.clima

/** Clima de San Bernardo, ya traducido desde la API externa Open-Meteo. */
data class Clima(
    val actual: ClimaActual,
    val dias: List<PronosticoDia>
)

data class ClimaActual(
    val temperatura: Double,
    val humedad: Int,
    val precipitacion: Double,
    val viento: Double,
    val condicion: CondicionClima
)

/** fecha: texto aaaa-mm-dd tal como lo entrega la API. */
data class PronosticoDia(
    val fecha: String,
    val condicion: CondicionClima,
    val maxima: Double,
    val minima: Double,
    val lluviaMm: Double,
    val probabilidadLluvia: Int
)

/**
 * Condicion del cielo segun el codigo WMO que usa Open-Meteo
 * (0 despejado, 3 nublado, 61 lluvia, 95 tormenta...).
 */
enum class CondicionClima(val descripcion: String) {
    DESPEJADO("Despejado"),
    PARCIAL("Parcialmente nublado"),
    NUBLADO("Nublado"),
    NIEBLA("Niebla"),
    LLOVIZNA("Llovizna"),
    LLUVIA("Lluvia"),
    NIEVE("Nieve"),
    TORMENTA("Tormenta"),
    DESCONOCIDO("Sin datos");

    companion object {
        fun desdeCodigo(codigo: Int?): CondicionClima {
            val valor = codigo ?: return DESCONOCIDO
            return when (valor) {
                0 -> DESPEJADO
                1, 2 -> PARCIAL
                3 -> NUBLADO
                45, 48 -> NIEBLA
                in 51..57 -> LLOVIZNA
                in 61..67, in 80..82 -> LLUVIA
                in 71..77, 85, 86 -> NIEVE
                in 95..99 -> TORMENTA
                else -> DESCONOCIDO
            }
        }
    }
}
