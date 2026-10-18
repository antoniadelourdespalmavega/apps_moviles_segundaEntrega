package cl.duoc.huertoescolar.data.local.query

import androidx.room.Embedded
import cl.duoc.huertoescolar.data.local.entity.Cultivo
import cl.duoc.huertoescolar.data.local.entity.Evidencia
import cl.duoc.huertoescolar.data.local.entity.ReporteProblema
import cl.duoc.huertoescolar.data.local.entity.Sector
import cl.duoc.huertoescolar.data.local.entity.Tarea
import cl.duoc.huertoescolar.data.local.entity.Validacion

/**
 * Objetos de solo lectura que devuelven los DAO cuando una pantalla necesita
 * datos de varias tablas (por ejemplo, la tarea con el nombre de su sector).
 * Evitan que la UI tenga que consultar la base de datos por su cuenta.
 */

data class SectorResumen(
    @Embedded val sector: Sector,
    val responsableNombre: String?,
    val totalCultivos: Int,
    val tareasAbiertas: Int
)

data class CultivoDetalle(
    @Embedded val cultivo: Cultivo,
    val sectorNombre: String
)

data class TareaDetalle(
    @Embedded val tarea: Tarea,
    val sectorNombre: String,
    val cultivoNombre: String?,
    val responsableNombre: String,
    val creadorNombre: String
)

data class EvidenciaDetalle(
    @Embedded val evidencia: Evidencia,
    val usuarioNombre: String
)

data class ValidacionDetalle(
    @Embedded val validacion: Validacion,
    val validadorNombre: String,
    val tareaDescripcion: String,
    val tareaResponsableId: Long
)

data class ReporteDetalle(
    @Embedded val reporte: ReporteProblema,
    val sectorNombre: String,
    val cultivoNombre: String?,
    val reportanteNombre: String
)
