package cl.duoc.huertoescolar.data.repository

import cl.duoc.huertoescolar.data.local.dao.SensorDao
import cl.duoc.huertoescolar.data.local.entity.LecturaSensor
import cl.duoc.huertoescolar.data.remote.api.HuertoApi
import cl.duoc.huertoescolar.data.remote.llamarApi
import cl.duoc.huertoescolar.domain.Operacion
import cl.duoc.huertoescolar.domain.Resultado
import kotlinx.coroutines.flow.Flow

/**
 * Lecturas SIMULADAS de sensores. Las genera ms-huerto (no hay hardware): los
 * valores son ficticios y solo demuestran una posible integracion futura.
 */
class SensorRepository(
    private val api: HuertoApi,
    private val sensorDao: SensorDao,
    private val sincronizacion: SincronizacionRepository
) {

    fun observarLecturas(): Flow<List<LecturaSensor>> = sensorDao.observarTodas()

    /** Pide al microservicio una lectura para cada sector que aun no tiene. */
    suspend fun simularLecturasFaltantes(): Operacion =
        when (val resultado = llamarApi { api.simularLecturas() }) {
            is Resultado.Exito -> {
                sincronizacion.sincronizarHuerto()
                Operacion.Exito("Lecturas simuladas: " + resultado.dato.size)
            }
            is Resultado.Error -> Operacion.Error(resultado.mensaje)
        }
}
