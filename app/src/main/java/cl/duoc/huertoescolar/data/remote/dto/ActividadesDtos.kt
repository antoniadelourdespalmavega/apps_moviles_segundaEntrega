package cl.duoc.huertoescolar.data.remote.dto

/* Modelos de ms-actividades: tareas, evidencias, validaciones y reportes. */

data class TareaDto(
    val id: Long,
    val tipo: String,
    val descripcion: String,
    val instrucciones: String,
    val sectorId: Long,
    val cultivoId: Long?,
    val responsableId: Long,
    val creadaPorId: Long,
    val fechaCreacion: Long,
    val fechaProgramada: Long,
    val estado: String,
    val fechaRealizacion: Long?,
    val comentarioEstudiante: String?
)

/** usuarioId: el docente o tallerista que crea o edita la tarea. */
data class TareaRequest(
    val tipo: String,
    val descripcion: String,
    val instrucciones: String,
    val sectorId: Long,
    val cultivoId: Long?,
    val responsableId: Long,
    val fechaProgramada: Long,
    val usuarioId: Long
)

data class AccionRequest(val usuarioId: Long)

data class EntregaRequest(
    val usuarioId: Long,
    val comentario: String,
    val rutaFoto: String
)

data class RevisionRequest(
    val validadorId: Long,
    val resultado: String,
    val motivo: String
)

data class EvidenciaDto(
    val id: Long,
    val tareaId: Long,
    val usuarioId: Long,
    val rutaFoto: String,
    val comentario: String,
    val fechaRegistro: Long
)

data class ValidacionDto(
    val id: Long,
    val tareaId: Long,
    val validadorId: Long,
    val resultado: String,
    val motivo: String,
    val fechaValidacion: Long
)

data class ReporteDto(
    val id: Long,
    val sectorId: Long,
    val cultivoId: Long?,
    val tipo: String,
    val descripcion: String,
    val fechaReporte: Long,
    val reportadoPorId: Long,
    val rutaFoto: String?,
    val estado: String,
    val comentarioDocente: String?,
    val fechaCierre: Long?
)

data class ReporteRequest(
    val sectorId: Long,
    val cultivoId: Long?,
    val tipo: String,
    val descripcion: String,
    val usuarioId: Long,
    val rutaFoto: String?
)

data class EstadoReporteRequest(
    val usuarioId: Long,
    val estado: String,
    val comentario: String
)
