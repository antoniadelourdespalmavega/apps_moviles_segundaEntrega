package cl.duoc.huertoescolar.data.remote

import cl.duoc.huertoescolar.data.remote.dto.ClimaDto
import cl.duoc.huertoescolar.domain.clima.Clima
import cl.duoc.huertoescolar.domain.clima.ClimaActual
import cl.duoc.huertoescolar.domain.clima.CondicionClima
import cl.duoc.huertoescolar.domain.clima.PronosticoDia

/**
 * Convierte la respuesta de Open-Meteo en el modelo de la app.
 * Devuelve null si la respuesta no trae los datos minimos.
 */
fun ClimaDto.aDominio(): Clima? {
    val datosActuales = actual ?: return null
    val temperatura = datosActuales.temperatura ?: return null
    val fechas = diario?.fechas.orEmpty()
    val dias = fechas.mapIndexed { i, fecha ->
        PronosticoDia(
            fecha = fecha,
            condicion = CondicionClima.desdeCodigo(diario?.codigosClima?.getOrNull(i)),
            maxima = diario?.maximas?.getOrNull(i) ?: temperatura,
            minima = diario?.minimas?.getOrNull(i) ?: temperatura,
            lluviaMm = diario?.lluvia?.getOrNull(i) ?: 0.0,
            probabilidadLluvia = diario?.probabilidadLluvia?.getOrNull(i) ?: 0
        )
    }
    return Clima(
        actual = ClimaActual(
            temperatura = temperatura,
            humedad = datosActuales.humedad ?: 0,
            precipitacion = datosActuales.precipitacion ?: 0.0,
            viento = datosActuales.viento ?: 0.0,
            condicion = CondicionClima.desdeCodigo(datosActuales.codigoClima)
        ),
        dias = dias
    )
}
