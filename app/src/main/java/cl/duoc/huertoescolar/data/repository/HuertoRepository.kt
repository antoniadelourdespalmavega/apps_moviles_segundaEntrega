package cl.duoc.huertoescolar.data.repository

import cl.duoc.huertoescolar.core.Fechas
import cl.duoc.huertoescolar.data.local.dao.CultivoDao
import cl.duoc.huertoescolar.data.local.dao.SectorDao
import cl.duoc.huertoescolar.data.local.dao.TareaDao
import cl.duoc.huertoescolar.data.local.entity.Cultivo
import cl.duoc.huertoescolar.data.local.entity.Sector
import cl.duoc.huertoescolar.data.local.query.CultivoDetalle
import cl.duoc.huertoescolar.data.local.query.SectorResumen
import cl.duoc.huertoescolar.domain.EstadoCultivo
import cl.duoc.huertoescolar.domain.EstadoSector
import cl.duoc.huertoescolar.domain.Operacion
import kotlinx.coroutines.flow.Flow

/**
 * Sectores y cultivos del huerto.
 * Aqui viven las reglas de negocio: la UI nunca consulta la base de datos.
 */
class HuertoRepository(
    private val sectorDao: SectorDao,
    private val cultivoDao: CultivoDao,
    private val tareaDao: TareaDao
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
        val codigoLimpio = codigo.trim().uppercase()
        if (sectorDao.existeCodigo(codigoLimpio, id) > 0) {
            return Operacion.Error("Ya existe otro sector con el codigo " + codigoLimpio)
        }
        val ahora = Fechas.ahora()
        return if (id == 0L) {
            val nuevoId = sectorDao.insertar(
                Sector(
                    codigo = codigoLimpio,
                    nombre = nombre.trim(),
                    descripcion = descripcion.trim(),
                    ubicacion = ubicacion.trim(),
                    estado = estado,
                    responsableId = responsableId,
                    fechaCreacion = ahora,
                    fechaActualizacion = ahora
                )
            )
            Operacion.Exito("Sector creado. Ahora puedes agregarle cultivos", idGenerado = nuevoId)
        } else {
            val actual = sectorDao.porId(id) ?: return Operacion.Error("El sector ya no existe")
            sectorDao.actualizar(
                actual.copy(
                    codigo = codigoLimpio,
                    nombre = nombre.trim(),
                    descripcion = descripcion.trim(),
                    ubicacion = ubicacion.trim(),
                    estado = estado,
                    responsableId = responsableId,
                    fechaActualizacion = ahora
                )
            )
            Operacion.Exito("Sector actualizado correctamente")
        }
    }

    /** Solo se elimina un sector sin tareas asociadas, para no perder historial. */
    suspend fun eliminarSector(id: Long): Operacion {
        val sector = sectorDao.porId(id) ?: return Operacion.Error("El sector ya no existe")
        if (tareaDao.contarPorSector(id) > 0) {
            return Operacion.Error("No se puede eliminar: el sector tiene tareas registradas")
        }
        sectorDao.eliminar(sector)
        return Operacion.Exito("Sector eliminado")
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
        if (sectorDao.porId(sectorId) == null) {
            return Operacion.Error("El sector seleccionado no existe")
        }
        if (fechaCosechaEstimada < fechaSiembra) {
            return Operacion.Error("La cosecha estimada no puede ser anterior a la siembra")
        }
        if (frecuenciaRiegoDias <= 0) {
            return Operacion.Error("La frecuencia de riego debe ser mayor que cero")
        }
        return if (id == 0L) {
            cultivoDao.insertar(
                Cultivo(
                    nombre = nombre.trim(),
                    descripcion = descripcion.trim(),
                    sectorId = sectorId,
                    fechaSiembra = fechaSiembra,
                    fechaTrasplante = fechaTrasplante,
                    fechaCosechaEstimada = fechaCosechaEstimada,
                    frecuenciaRiegoDias = frecuenciaRiegoDias,
                    estado = estado,
                    observaciones = observaciones.trim(),
                    rutaFoto = rutaFoto
                )
            )
            Operacion.Exito("Cultivo registrado correctamente")
        } else {
            val actual = cultivoDao.porId(id) ?: return Operacion.Error("El cultivo ya no existe")
            cultivoDao.actualizar(
                actual.copy(
                    nombre = nombre.trim(),
                    descripcion = descripcion.trim(),
                    sectorId = sectorId,
                    fechaSiembra = fechaSiembra,
                    fechaTrasplante = fechaTrasplante,
                    fechaCosechaEstimada = fechaCosechaEstimada,
                    frecuenciaRiegoDias = frecuenciaRiegoDias,
                    estado = estado,
                    observaciones = observaciones.trim(),
                    rutaFoto = rutaFoto
                )
            )
            Operacion.Exito("Cultivo actualizado correctamente")
        }
    }

    /** Un cultivo con tareas no se elimina, para no perder su historial. */
    suspend fun eliminarCultivo(id: Long): Operacion {
        val cultivo = cultivoDao.porId(id) ?: return Operacion.Error("El cultivo ya no existe")
        if (tareaDao.contarPorCultivo(id) > 0) {
            return Operacion.Error("No se puede eliminar: el cultivo tiene tareas registradas")
        }
        cultivoDao.eliminar(cultivo)
        return Operacion.Exito("Cultivo eliminado")
    }

    suspend fun cambiarEstadoCultivo(id: Long, estado: EstadoCultivo): Operacion {
        val cultivo = cultivoDao.porId(id) ?: return Operacion.Error("El cultivo ya no existe")
        cultivoDao.actualizar(cultivo.copy(estado = estado))
        return Operacion.Exito("Estado del cultivo actualizado a " + estado.etiqueta)
    }
}
