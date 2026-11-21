package cl.duoc.huertoescolar.data.remote.dto

import com.google.gson.annotations.SerializedName

/*
 * Respuesta de la API externa Open-Meteo (https://open-meteo.com).
 * @SerializedName conecta los nombres del JSON (temperature_2m) con nombres
 * claros en Kotlin.
 */

data class ClimaDto(
    @SerializedName("current") val actual: ClimaActualDto?,
    @SerializedName("daily") val diario: ClimaDiarioDto?
)

data class ClimaActualDto(
    @SerializedName("time") val hora: String?,
    @SerializedName("temperature_2m") val temperatura: Double?,
    @SerializedName("relative_humidity_2m") val humedad: Int?,
    @SerializedName("precipitation") val precipitacion: Double?,
    @SerializedName("weather_code") val codigoClima: Int?,
    @SerializedName("wind_speed_10m") val viento: Double?
)

data class ClimaDiarioDto(
    @SerializedName("time") val fechas: List<String>?,
    @SerializedName("weather_code") val codigosClima: List<Int?>?,
    @SerializedName("temperature_2m_max") val maximas: List<Double?>?,
    @SerializedName("temperature_2m_min") val minimas: List<Double?>?,
    @SerializedName("precipitation_sum") val lluvia: List<Double?>?,
    @SerializedName("precipitation_probability_max") val probabilidadLluvia: List<Int?>?
)
