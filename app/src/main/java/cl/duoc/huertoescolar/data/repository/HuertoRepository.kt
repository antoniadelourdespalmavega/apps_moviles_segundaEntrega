package cl.duoc.huertoescolar.data.repository

import cl.duoc.huertoescolar.data.local.dao.CultivoDao
import cl.duoc.huertoescolar.data.local.dao.SectorDao
import cl.duoc.huertoescolar.data.local.dao.TareaDao
import cl.duoc.huertoescolar.data.local.entity.Cultivo
import cl.duoc.huertoescolar.data.local.entity.Sector
import cl.duoc.huertoescolar.data.local.query.CultivoDetalle
import cl.duoc.huertoescolar.data.local.query.SectorResumen
import cl.duoc.huertoescolar.data.remote.api.HuertoApi
import cl.duoc.huertoescolar.data.remote.comprobar
import cl.duoc.huertoescolar.data.remote.dto.CultivoRequest
import cl.duoc.huertoescolar.data.remote.dto.EstadoRequest
import cl.duoc.huertoescolar.data.remote.dto.SectorRequest
import cl.duoc.huertoescolar.data.remote.llamarApi
import cl.duoc.huertoescolar.domain.EstadoCultivo
import cl.duoc.huertoescolar.domain.EstadoSector
import cl.duoc.huertoescolar.domain.Operacion
import cl.duoc.huertoescolar.domain.Resultado
import kotlinx.coroutines.flow.Flow

/**
 * Sectores y cultivos del huerto contra ms-huerto-escolar.
 * Lectura: desde la copia local (Room). Escritura: se envia al microservicio
 * y luego se actualiza la copia local.
 */
class HuertoRepository(
    private val api: HuertoApi,
    private val sectorDao: SectorDao,
    private val cultivoDao: CultivoDao,
    private val tareaDao: TareaDao,
    private val sincronizacion: SincronizacionRepository
) {

    fun observarSectores(): Flow<List<SectorResumen>> = sectorDao.observarResumen()

    fun observarSectoresSimples(): Flow<List<Sector>> = sectorDao.observarTodos()

    fun observarSector(id: Long): Flow<Sector?> = sectorDao.observarPorId(id)

    fun observarCultivosDeSector(sectorId: Long): Flow<List<Cultivo>> =
        cultivoDao.observarPorSector(sectorId)

    fun observarCultivos(): Flow<List<Cultivo>> = cultivoDao.observarTodos()

    fun observarCultivosDetalle(): Flow<List<CultivoDetalle>> = cultivoDao.observarDetalle()

    fun observarCultivo(id: Long): Flow<Cultivo?> = cultivoDao.observarPorId(id)

    suspend fun sectorPorId(id: Long): Sector? = sectorDao.porId(id)

    suspend fun cultivoPorId(id: Long): Cultivo? = cultivoDao.porId(id)

    suspend fun guardarSector(
        id: Long,
        codigo: String,
        nombre: String,
        descripcion: String,
        ubicacion: String,
        estado: EstadoSector,
        responsableId: Long?
    ): Operacion {
        val solicitud = SectorRequest(
            codigo = codigo.trim().uppercase(),
            nombre = nombre.trim(),
            descripcion = descripcion.trim(),
            ubicacion = ubicacion.trim(),
            estado = estado.name,
            responsableId = responsableId
        )
        val resultado = llamarApi {
            if (id == 0L) api.crearSector(solicitud) else api.actualizarSector(id, solicitud)
        }
        return when (resultado) {
            is Resultado.Exito -> {
                sincronizacion.sincronizarHuerto()
                if (id == 0L) {
                    Operacion.Exito("Sector creado. Ahora puedes agregarle cultivos", idGenerado = resultado.dato.id)
                } else {
                    Operacion.Exito("Sector actualizado correctamente")
                }
            }
            is Resultado.Error -> Operacion.Error(resultado.mensaje)
        }
    }

    /**
     * Solo se elimina un sector sin tareas asociadas, para no perder historial.
     * El microservicio tambien la revisa; aqui se adelanta para responder al instante.
     */
    suspend fun eliminarSector(id: Long): Operacion {
        if (tareaDao.contarPorSector(id) > 0) {
            return Operacion.Error("No se puede eliminar: el sector tiene tareas registradas")
        }
        return escribir("Sector eliminado") { api.eliminarSector(id).comprobar() }
    }

    suspend fun guardarCultivo(
        id: Long,
        nombre: String,
        descripcion: String,
        sectorId: Long,
        fechaSiembra: Long,
        fechaTrasplante: Long?,
        fechaCosechaEstimada: Long,
        frecuenciaRiegoDias: Int,
        estado: EstadoCultivo,
        observaciones: String,
        rutaFoto: String?
    ): Operacion {
        val solicitud = CultivoRequest(
            nombre = nombre.trim(),
            descripcion = descripcion.trim(),
            sectorId = sectorId,
            fechaSiembra = fechaSiembra,
            fechaTrasplante = fechaTrasplante,
            fechaCosechaEstimada = fechaCosechaEstimada,
            frecuenciaRiegoDias = frecuenciaRiegoDias,
            estado = estado.name,
            observaciones = observaciones.trim(),
            rutaFoto = rutaFoto
        )
        return if (id == 0L) {
            escribir("Cultivo registrado correctamente") { api.crearCultivo(solicitud) }
        } else {
            escribir("Cultivo actualizado correctamente") { api.actualizarCultivo(id, solicitud) }
        }
    }

    /** Un cultivo con tareas no se elimina, para no perder su historial. */
    suspend fun eliminarCultivo(id: Long): Operacion {
        if (tareaDao.contarPorCultivo(id) > 0) {
            return Operacion.Error("No se puede eliminar: el cultivo tiene tareas registradas")
        }
        return escribir("Cultivo eliminado") { api.eliminarCultivo(id).comprobar() }
    }

    suspend fun cambiarEstadoCultivo(id: Long, estado: EstadoCultivo): Operacion =
        escribir("Estado del cultivo actualizado a " + estado.etiqueta) {
            api.cambiarEstadoCultivo(id, EstadoRequest(estado.name))
        }

    /** Envia el cambio al microservicio y, si resulta, refresca la copia local. */
    private suspend fun escribir(mensajeExito: String, llamada: suspend () -> Any?): Operacion =
        when (val resultado = llamarApi { llamada() }) {
            is Resultado.Exito -> {
                sincronizacion.sincronizarHuerto()
                Operacion.Exito(mensajeExito)
            }
            is Resultado.Error -> Operacion.Error(resultado.mensaje)
        }
}
