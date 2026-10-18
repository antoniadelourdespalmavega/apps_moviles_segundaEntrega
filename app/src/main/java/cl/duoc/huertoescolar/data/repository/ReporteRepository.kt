package cl.duoc.huertoescolar.data.repository

import cl.duoc.huertoescolar.core.Fechas
import cl.duoc.huertoescolar.data.local.dao.ReporteDao
import cl.duoc.huertoescolar.data.local.dao.UsuarioDao
import cl.duoc.huertoescolar.data.local.entity.ReporteProblema
import cl.duoc.huertoescolar.data.local.query.ReporteDetalle
import cl.duoc.huertoescolar.domain.EstadoReporte
import cl.duoc.huertoescolar.domain.Operacion
import cl.duoc.huertoescolar.domain.Rol
import cl.duoc.huertoescolar.domain.TipoProblema
import kotlinx.coroutines.flow.Flow

/** Reportes de problemas del huerto y su seguimiento. */
class ReporteRepository(
    private val reporteDao: ReporteDao,
    private val usuarioDao: UsuarioDao
) {

    fun observarTodos(): Flow<List<ReporteDetalle>> = reporteDao.observarTodos()

    fun observarDeUsuario(usuarioId: Long): Flow<List<ReporteDetalle>> =
        reporteDao.observarPorUsuario(usuarioId)

    suspend fun registrar(
        sectorId: Long,
        cultivoId: Long?,
        tipo: TipoProblema,
        descripcion: String,
        usuarioId: Long,
        rutaFoto: String?
    ): Operacion {
        // El estudiante reporta; el docente o tallerista soluciona.
        if (usuarioDao.existeConRol(usuarioId, Rol.ESTUDIANTE) == 0) {
            return Operacion.Error("Los problemas los reporta el estudiante; el docente o tallerista los soluciona")
        }
        reporteDao.insertar(
            ReporteProblema(
                sectorId = sectorId,
                cultivoId = cultivoId,
                tipo = tipo,
                descripcion = descripcion.trim(),
                fechaReporte = Fechas.ahora(),
                reportadoPorId = usuarioId,
                rutaFoto = rutaFoto,
                estado = EstadoReporte.INFORMADO,
                comentarioDocente = "",
                fechaCierre = null
            )
        )
        return Operacion.Exito("Problema reportado correctamente")
    }

    /** Solo el administrador cambia el estado de atencion de un problema. */
    suspend fun actualizarEstado(
        reporteId: Long,
        usuarioId: Long,
        estado: EstadoReporte,
        comentario: String
    ): Operacion {
        if (usuarioDao.existeConRol(usuarioId, Rol.ADMINISTRADOR) == 0) {
            return Operacion.Error("Solo un docente o tallerista puede cambiar el estado del reporte")
        }
        val reporte = reporteDao.porId(reporteId) ?: return Operacion.Error("El reporte ya no existe")
        val cierre = if (estado == EstadoReporte.CERRADO || estado == EstadoReporte.ATENDIDO) {
            Fechas.ahora()
        } else {
            null
        }
        reporteDao.actualizar(
            reporte.copy(
                estado = estado,
                comentarioDocente = comentario.trim(),
                fechaCierre = cierre
            )
        )
        return Operacion.Exito("Reporte actualizado a " + estado.etiqueta)
    }
}
