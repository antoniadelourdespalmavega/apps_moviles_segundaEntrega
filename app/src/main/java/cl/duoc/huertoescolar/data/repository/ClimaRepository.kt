package cl.duoc.huertoescolar.data.repository

import cl.duoc.huertoescolar.data.remote.ErroresApi
import cl.duoc.huertoescolar.data.remote.aDominio
import cl.duoc.huertoescolar.data.remote.api.ClimaApi
import cl.duoc.huertoescolar.data.remote.llamarApi
import cl.duoc.huertoescolar.domain.Resultado
import cl.duoc.huertoescolar.domain.clima.Clima

/**
 * API EXTERNA: clima de San Bernardo desde Open-Meteo.
 * Es independiente del microservicio y de Room: no guarda nada, solo
 * consulta y entrega el clima para mostrarlo en la app.
 */
class ClimaRepository(private val api: ClimaApi) {

    suspend fun obtenerClima(): Resultado<Clima> =
        when (val resultado = llamarApi { api.pronostico(LATITUD, LONGITUD) }) {
            is Resultado.Exito -> resultado.dato.aDominio()
                ?.let { Resultado.Exito(it) }
                ?: Resultado.Error("La API de clima no entregó datos")
            is Resultado.Error -> Resultado.Error(
                if (resultado.mensaje == ErroresApi.SIN_CONEXION) SIN_INTERNET else resultado.mensaje
            )
        }

    companion object {
        const val LUGAR = "San Bernardo"
        const val LATITUD = -33.5922
        const val LONGITUD = -70.6996
        const val SIN_INTERNET = "Sin conexión a internet: no se pudo consultar el clima"
    }
}
