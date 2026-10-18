package cl.duoc.huertoescolar.data.repository

import cl.duoc.huertoescolar.core.Fechas
import cl.duoc.huertoescolar.data.local.dao.EvidenciaDao
import cl.duoc.huertoescolar.data.local.dao.TareaDao
import cl.duoc.huertoescolar.data.local.dao.UsuarioDao
import cl.duoc.huertoescolar.data.local.dao.ValidacionDao
import cl.duoc.huertoescolar.data.local.entity.Evidencia
import cl.duoc.huertoescolar.data.local.entity.Tarea
import cl.duoc.huertoescolar.data.local.entity.Validacion
import cl.duoc.huertoescolar.data.local.query.EvidenciaDetalle
import cl.duoc.huertoescolar.data.local.query.TareaDetalle
import cl.duoc.huertoescolar.data.local.query.ValidacionDetalle
import cl.duoc.huertoescolar.domain.EstadoTarea
import cl.duoc.huertoescolar.domain.Operacion
import cl.duoc.huertoescolar.domain.ResultadoValidacion
import cl.duoc.huertoescolar.domain.Rol
import cl.duoc.huertoescolar.domain.TipoTarea
import kotlinx.coroutines.flow.Flow

/**
 * Tareas del huerto, evidencias y validaciones.
 *
 * Controla el flujo de estados obligatorio:
 * PENDIENTE -> EN_DESARROLLO -> PENDIENTE_VALIDACION -> VALIDADA / RECHAZADA
 * y las reglas de permisos (quien puede crear, entregar y validar).
 */
class TareaRepository(
    private val tareaDao: TareaDao,
    private val evidenciaDao: EvidenciaDao,
    private val validacionDao: ValidacionDao,
    private val usuarioDao: UsuarioDao
) {

    fun observarTodas(): Flow<List<TareaDetalle>> = tareaDao.observarTodas()

    fun observarDeEstudiante(usuarioId: Long): Flow<List<TareaDetalle>> =
        tareaDao.observarPorResponsable(usuarioId)

    fun observarDetalle(tareaId: Long): Flow<TareaDetalle?> = tareaDao.observarDetalle(tareaId)

    fun observarEvidencias(tareaId: Long): Flow<List<EvidenciaDetalle>> =
        evidenciaDao.observarPorTarea(tareaId)

    fun observarTodasLasEvidencias(): Flow<List<EvidenciaDetalle>> = evidenciaDao.observarTodas()

    fun observarValidaciones(): Flow<List<ValidacionDetalle>> = validacionDao.observarTodas()

    fun observarValidacionesDeTarea(tareaId: Long): Flow<List<ValidacionDetalle>> =
        validacionDao.observarPorTarea(tareaId)

    suspend fun porId(id: Long): Tarea? = tareaDao.porId(id)

    /** Crea o edita una tarea. Solo un administrador puede hacerlo. */
    suspend fun guardarTarea(
        id: Long,
        tipo: TipoTarea,
        descripcion: String,
        instrucciones: String,
        sectorId: Long,
        cultivoId: Long?,
        responsableId: Long,
        fechaProgramada: Long,
        usuarioQueEditaId: Long
    ): Operacion {
        if (usuarioDao.existeConRol(usuarioQueEditaId, Rol.ADMINISTRADOR) == 0) {
            return Operacion.Error("Solo un administrador puede crear o editar tareas")
        }
        if (usuarioDao.existeConRol(responsableId, Rol.ESTUDIANTE) == 0) {
            return Operacion.Error("El responsable debe ser un estudiante activo del taller")
        }
        return if (id == 0L) {
            tareaDao.insertar(
                Tarea(
                    tipo = tipo,
                    descripcion = descripcion.trim(),
                    instrucciones = instrucciones.trim(),
                    sectorId = sectorId,
                    cultivoId = cultivoId,
                    responsableId = responsableId,
                    creadaPorId = usuarioQueEditaId,
                    fechaCreacion = Fechas.ahora(),
                    fechaProgramada = fechaProgramada,
                    estado = EstadoTarea.PENDIENTE,
                    fechaRealizacion = null,
                    comentarioEstudiante = null
                )
            )
            Operacion.Exito("Tarea creada y asignada correctamente")
        } else {
            val actual = tareaDao.porId(id) ?: return Operacion.Error("La tarea ya no existe")
            if (actual.estado == EstadoTarea.VALIDADA) {
                return Operacion.Error("Una tarea validada no se puede modificar")
            }
            tareaDao.actualizar(
                actual.copy(
                    tipo = tipo,
                    descripcion = descripcion.trim(),
                    instrucciones = instrucciones.trim(),
                    sectorId = sectorId,
                    cultivoId = cultivoId,
                    responsableId = responsableId,
                    fechaProgramada = fechaProgramada
                )
            )
            Operacion.Exito("Tarea actualizada correctamente")
        }
    }

    /** El estudiante responsable marca el inicio del trabajo. */
    suspend fun iniciarTarea(tareaId: Long, usuarioId: Long): Operacion {
        val tarea = tareaDao.porId(tareaId) ?: return Operacion.Error("La tarea ya no existe")
        if (tarea.responsableId != usuarioId) {
            return Operacion.Error("Solo el responsable asignado puede iniciar esta tarea")
        }
        if (tarea.estado != EstadoTarea.PENDIENTE && tarea.estado != EstadoTarea.RECHAZADA) {
            return Operacion.Error("La tarea no esta pendiente de ejecucion")
        }
        tareaDao.actualizar(tarea.copy(estado = EstadoTarea.EN_DESARROLLO))
        return Operacion.Exito("Tarea iniciada")
    }

    /** El estudiante entrega la tarea con comentario y evidencia fotografica. */
    suspend fun entregarTarea(
        tareaId: Long,
        usuarioId: Long,
        comentario: String,
        rutaFoto: String
    ): Operacion {
        val tarea = tareaDao.porId(tareaId) ?: return Operacion.Error("La tarea ya no existe")
        if (tarea.responsableId != usuarioId) {
            return Operacion.Error("Solo el responsable asignado puede entregar esta tarea")
        }
        if (tarea.estado == EstadoTarea.VALIDADA) {
            return Operacion.Error("La tarea ya fue validada")
        }
        if (tarea.estado == EstadoTarea.PENDIENTE_VALIDACION) {
            return Operacion.Error("La tarea ya esta esperando revision")
        }
        val ahora = Fechas.ahora()
        evidenciaDao.insertar(
            Evidencia(
                tareaId = tareaId,
                usuarioId = usuarioId,
                rutaFoto = rutaFoto,
                comentario = comentario.trim(),
                fechaRegistro = ahora
            )
        )
        tareaDao.actualizar(
            tarea.copy(
                estado = EstadoTarea.PENDIENTE_VALIDACION,
                fechaRealizacion = ahora,
                comentarioEstudiante = comentario.trim()
            )
        )
        return Operacion.Exito("Actividad entregada. Queda pendiente de validacion")
    }

    /**
     * El administrador valida o rechaza. Un estudiante no puede validar, y
     * nadie puede validar una tarea de la que es responsable.
     */
    suspend fun revisarTarea(
        tareaId: Long,
        validadorId: Long,
        resultado: ResultadoValidacion,
        motivo: String
    ): Operacion {
        val tarea = tareaDao.porId(tareaId) ?: return Operacion.Error("La tarea ya no existe")
        if (usuarioDao.existeConRol(validadorId, Rol.ADMINISTRADOR) == 0) {
            return Operacion.Error("Solo un docente o tallerista puede validar actividades")
        }
        if (tarea.responsableId == validadorId) {
            return Operacion.Error("No puedes validar una actividad de la que eres responsable")
        }
        if (tarea.estado != EstadoTarea.PENDIENTE_VALIDACION) {
            return Operacion.Error("Solo se revisan actividades pendientes de validacion")
        }
        if (resultado == ResultadoValidacion.RECHAZADA && motivo.trim().length < 5) {
            return Operacion.Error("Debes indicar el motivo del rechazo")
        }
        val ahora = Fechas.ahora()
        validacionDao.insertar(
            Validacion(
                tareaId = tareaId,
                validadorId = validadorId,
                resultado = resultado,
                motivo = motivo.trim(),
                fechaValidacion = ahora
            )
        )
        val nuevoEstado = if (resultado == ResultadoValidacion.VALIDADA) {
            EstadoTarea.VALIDADA
        } else {
            EstadoTarea.RECHAZADA
        }
        tareaDao.actualizar(tarea.copy(estado = nuevoEstado))
        return if (resultado == ResultadoValidacion.VALIDADA) {
            Operacion.Exito("Actividad validada")
        } else {
            Operacion.Exito("Actividad rechazada. El estudiante puede corregirla y reenviarla")
        }
    }

    suspend fun eliminarTarea(tareaId: Long, usuarioId: Long): Operacion {
        if (usuarioDao.existeConRol(usuarioId, Rol.ADMINISTRADOR) == 0) {
            return Operacion.Error("Solo un administrador puede eliminar tareas")
        }
        val tarea = tareaDao.porId(tareaId) ?: return Operacion.Error("La tarea ya no existe")
        if (tarea.estado == EstadoTarea.VALIDADA) {
            return Operacion.Error("No se elimina una tarea validada: forma parte del historial")
        }
        tareaDao.eliminar(tarea)
        return Operacion.Exito("Tarea eliminada")
    }
}
