package cl.duoc.huertoescolar.data.remote.api

import cl.duoc.huertoescolar.data.remote.dto.ClimaDto
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * API externa Open-Meteo. Ejemplo de la URL que se arma:
 * https://api.open-meteo.com/v1/forecast?latitude=-33.59&longitude=-70.70
 *   &current=temperature_2m,...&daily=...&timezone=America/Santiago&forecast_days=7
 */
interface ClimaApi {

    @GET("v1/forecast")
    suspend fun pronostico(
        @Query("latitude") latitud: Double,
        @Query("longitude") longitud: Double,
        @Query("current") actual: String = DATOS_ACTUALES,
        @Query("daily") diario: String = DATOS_DIARIOS,
        @Query("timezone") zonaHoraria: String = "America/Santiago",
        @Query("forecast_days") dias: Int = 7
    ): ClimaDto

    companion object {
        const val DATOS_ACTUALES =
            "temperature_2m,relative_humidity_2m,precipitation,weather_code,wind_speed_10m"
        const val DATOS_DIARIOS =
            "weather_code,temperature_2m_max,temperature_2m_min,precipitation_sum,precipitation_probability_max"
    }
}
