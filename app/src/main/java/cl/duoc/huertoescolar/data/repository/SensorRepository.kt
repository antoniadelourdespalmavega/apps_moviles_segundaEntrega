package cl.duoc.huertoescolar.data.repository

import cl.duoc.huertoescolar.core.Fechas
import cl.duoc.huertoescolar.data.local.dao.SensorDao
import cl.duoc.huertoescolar.data.local.entity.LecturaSensor
import cl.duoc.huertoescolar.domain.EstadoLectura
import kotlinx.coroutines.flow.Flow
import kotlin.random.Random

/**
 * Lecturas SIMULADAS de sensores.
 * No existe conexion con hardware: los valores son ficticios y solo sirven para
 * demostrar una posible integracion futura.
 */
class SensorRepository(private val sensorDao: SensorDao) {

    fun observarLecturas(): Flow<List<LecturaSensor>> = sensorDao.observarTodas()

    /**
     * Cada sector debe tener al menos una lectura simulada. Cuando se crea un
     * sector nuevo, aqui se le genera su primera lectura de prueba, asi aparece
     * de inmediato en la pantalla de sensores.
     */
    suspend fun simularLecturasFaltantes(sectorIds: List<Long>) {
        val conLecturas = sensorDao.sectoresConLecturas().toSet()
        sectorIds.filter { it !in conLecturas }.forEach { sensorDao.insertar(lecturaSimulada(it)) }
    }

    /** Valores aleatorios dentro de rangos realistas para un huerto escolar. */
    private fun lecturaSimulada(sectorId: Long): LecturaSensor {
        val humedadSuelo = Random.nextInt(20, 80)
        val estado = when {
            humedadSuelo < 30 -> EstadoLectura.BAJO
            humedadSuelo > 65 -> EstadoLectura.ALTO
            else -> EstadoLectura.NORMAL
        }
        return LecturaSensor(
            sectorId = sectorId,
            fechaLectura = Fechas.ahora(),
            humedadSuelo = humedadSuelo,
            temperatura = Random.nextInt(150, 290) / 10.0,
            humedadAmbiente = Random.nextInt(40, 85),
            estado = estado
        )
    }
}
