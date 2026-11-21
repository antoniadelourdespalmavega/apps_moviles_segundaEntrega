package cl.duoc.huertoescolar.data.repository

import cl.duoc.huertoescolar.data.local.dao.ReporteDao
import cl.duoc.huertoescolar.data.local.dao.UsuarioDao
import cl.duoc.huertoescolar.data.local.query.ReporteDetalle
import cl.duoc.huertoescolar.data.remote.api.ActividadesApi
import cl.duoc.huertoescolar.data.remote.dto.EstadoReporteRequest
import cl.duoc.huertoescolar.data.remote.dto.ReporteRequest
import cl.duoc.huertoescolar.data.remote.llamarApi
import cl.duoc.huertoescolar.domain.EstadoReporte
import cl.duoc.huertoescolar.domain.Operacion
import cl.duoc.huertoescolar.domain.Resultado
import cl.duoc.huertoescolar.domain.Rol
import cl.duoc.huertoescolar.domain.TipoProblema
import kotlinx.coroutines.flow.Flow

/** Reportes de problemas del huerto y su seguimiento, contra ms-actividades. */
class ReporteRepository(
    private val api: ActividadesApi,
    private val reporteDao: ReporteDao,
    private val usuarioDao: UsuarioDao,
    private val sincronizacion: SincronizacionRepository
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
        val solicitud = ReporteRequest(sectorId, cultivoId, tipo.name, descripcion.trim(), usuarioId, rutaFoto)
        return escribir("Problema reportado correctamente") { api.registrarReporte(solicitud) }
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
        return escribir("Reporte actualizado a " + estado.etiqueta) {
            api.actualizarEstadoReporte(reporteId, EstadoReporteRequest(usuarioId, estado.name, comentario.trim()))
        }
    }

    private suspend fun escribir(mensajeExito: String, llamada: suspend () -> Any?): Operacion =
        when (val resultado = llamarApi { llamada() }) {
            is Resultado.Exito -> {
                sincronizacion.sincronizarActividades()
                Operacion.Exito(mensajeExito)
            }
            is Resultado.Error -> Operacion.Error(resultado.mensaje)
        }
}
