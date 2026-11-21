package cl.duoc.huertoescolar.data.repository

import cl.duoc.huertoescolar.data.local.dao.EvidenciaDao
import cl.duoc.huertoescolar.data.local.dao.TareaDao
import cl.duoc.huertoescolar.data.local.dao.UsuarioDao
import cl.duoc.huertoescolar.data.local.dao.ValidacionDao
import cl.duoc.huertoescolar.data.local.entity.Tarea
import cl.duoc.huertoescolar.data.local.query.EvidenciaDetalle
import cl.duoc.huertoescolar.data.local.query.TareaDetalle
import cl.duoc.huertoescolar.data.local.query.ValidacionDetalle
import cl.duoc.huertoescolar.data.remote.api.ActividadesApi
import cl.duoc.huertoescolar.data.remote.comprobar
import cl.duoc.huertoescolar.data.remote.dto.AccionRequest
import cl.duoc.huertoescolar.data.remote.dto.EntregaRequest
import cl.duoc.huertoescolar.data.remote.dto.RevisionRequest
import cl.duoc.huertoescolar.data.remote.dto.TareaRequest
import cl.duoc.huertoescolar.data.remote.llamarApi
import cl.duoc.huertoescolar.domain.EstadoTarea
import cl.duoc.huertoescolar.domain.Operacion
import cl.duoc.huertoescolar.domain.Resultado
import cl.duoc.huertoescolar.domain.ResultadoValidacion
import cl.duoc.huertoescolar.domain.Rol
import cl.duoc.huertoescolar.domain.TipoTarea
import kotlinx.coroutines.flow.Flow

/**
 * Tareas del huerto, evidencias y validaciones contra ms-actividades.
 *
 * Flujo de estados: PENDIENTE -> EN_DESARROLLO -> PENDIENTE_VALIDACION ->
 * VALIDADA / RECHAZADA. Los permisos se revisan dos veces: aqui (respuesta
 * inmediata en la pantalla) y en el microservicio (la regla que manda).
 */
class TareaRepository(
    private val api: ActividadesApi,
    private val tareaDao: TareaDao,
    private val evidenciaDao: EvidenciaDao,
    private val validacionDao: ValidacionDao,
    private val usuarioDao: UsuarioDao,
    private val sincronizacion: SincronizacionRepository
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
        val solicitud = TareaRequest(
            tipo = tipo.name,
            descripcion = descripcion.trim(),
            instrucciones = instrucciones.trim(),
            sectorId = sectorId,
            cultivoId = cultivoId,
            responsableId = responsableId,
            fechaProgramada = fechaProgramada,
            usuarioId = usuarioQueEditaId
        )
        return if (id == 0L) {
            escribir("Tarea creada y asignada correctamente") { api.crearTarea(solicitud) }
        } else {
            escribir("Tarea actualizada correctamente") { api.actualizarTarea(id, solicitud) }
        }
    }

    /** El estudiante responsable marca el inicio del trabajo. */
    suspend fun iniciarTarea(tareaId: Long, usuarioId: Long): Operacion {
        val tarea = tareaDao.porId(tareaId) ?: return Operacion.Error("La tarea ya no existe")
        if (tarea.responsableId != usuarioId) {
            return Operacion.Error("Solo el responsable asignado puede iniciar esta tarea")
        }
        return escribir("Tarea iniciada") { api.iniciarTarea(tareaId, AccionRequest(usuarioId)) }
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
        if (tarea.estado == EstadoTarea.PENDIENTE_VALIDACION) {
            return Operacion.Error("La tarea ya está esperando revisión")
        }
        return escribir("Actividad entregada. Queda pendiente de validación") {
            api.entregarTarea(tareaId, EntregaRequest(usuarioId, comentario.trim(), rutaFoto))
        }
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
        if (usuarioDao.existeConRol(validadorId, Rol.ADMINISTRADOR) == 0) {
            return Operacion.Error("Solo un docente o tallerista puede validar actividades")
        }
        if (resultado == ResultadoValidacion.RECHAZADA && motivo.trim().length < 5) {
            return Operacion.Error("Debes indicar el motivo del rechazo")
        }
        val mensaje = if (resultado == ResultadoValidacion.VALIDADA) {
            "Actividad validada"
        } else {
            "Actividad rechazada. El estudiante puede corregirla y reenviarla"
        }
        return escribir(mensaje) {
            api.revisarTarea(tareaId, RevisionRequest(validadorId, resultado.name, motivo.trim()))
        }
    }

    suspend fun eliminarTarea(tareaId: Long, usuarioId: Long): Operacion {
        if (usuarioDao.existeConRol(usuarioId, Rol.ADMINISTRADOR) == 0) {
            return Operacion.Error("Solo un administrador puede eliminar tareas")
        }
        return escribir("Tarea eliminada") { api.eliminarTarea(tareaId, usuarioId).comprobar() }
    }

    /** Envia la accion al microservicio y, si resulta, refresca la copia local. */
    private suspend fun escribir(mensajeExito: String, llamada: suspend () -> Any?): Operacion =
        when (val resultado = llamarApi { llamada() }) {
            is Resultado.Exito -> {
                sincronizacion.sincronizarActividades()
                Operacion.Exito(mensajeExito)
            }
            is Resultado.Error -> Operacion.Error(resultado.mensaje)
        }
}
