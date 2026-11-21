package cl.duoc.huertoescolar.util

import cl.duoc.huertoescolar.data.local.entity.Cultivo
import cl.duoc.huertoescolar.data.local.entity.ReporteProblema
import cl.duoc.huertoescolar.data.local.entity.Sector
import cl.duoc.huertoescolar.data.local.entity.Tarea
import cl.duoc.huertoescolar.data.local.query.ReporteDetalle
import cl.duoc.huertoescolar.data.local.query.TareaDetalle
import cl.duoc.huertoescolar.data.remote.dto.CultivoDto
import cl.duoc.huertoescolar.data.remote.dto.EvidenciaDto
import cl.duoc.huertoescolar.data.remote.dto.LecturaDto
import cl.duoc.huertoescolar.data.remote.dto.ReporteDto
import cl.duoc.huertoescolar.data.remote.dto.SectorDto
import cl.duoc.huertoescolar.data.remote.dto.TareaDto
import cl.duoc.huertoescolar.data.remote.dto.UsuarioDto
import cl.duoc.huertoescolar.data.remote.dto.ValidacionDto
import cl.duoc.huertoescolar.data.local.entity.Usuario
import cl.duoc.huertoescolar.core.Fechas
import cl.duoc.huertoescolar.domain.Cargo
import cl.duoc.huertoescolar.domain.EstadoCultivo
import cl.duoc.huertoescolar.domain.EstadoReporte
import cl.duoc.huertoescolar.domain.EstadoSector
import cl.duoc.huertoescolar.domain.EstadoTarea
import cl.duoc.huertoescolar.domain.Rol
import cl.duoc.huertoescolar.domain.TipoProblema
import cl.duoc.huertoescolar.domain.TipoTarea
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import retrofit2.HttpException
import retrofit2.Response

/** Datos de prueba reutilizables para todas las pruebas unitarias. */
object Fabrica {

    // ------------------------------ DTO del servidor ------------------------------
    fun usuarioDto(id: Long = 1, rol: String = "ADMINISTRADOR", cargo: String = "DOCENTE") = UsuarioDto(
        id = id, nombreUsuario = "usuario$id", correo = "usuario$id@escuela.cl", nombre = "Nombre Apellido",
        rol = rol, cargo = cargo, taller = "Taller A", activo = true
    )

    fun sectorDto(id: Long = 1, responsableId: Long? = 1) = SectorDto(
        id = id, codigo = "S-0$id", nombre = "Sector $id", descripcion = "Descripción del sector",
        ubicacion = "Patio", estado = "EN_USO", responsableId = responsableId, fechaCreacion = 1L, fechaActualizacion = 2L
    )

    fun cultivoDto(id: Long = 1, sectorId: Long = 1) = CultivoDto(
        id = id, nombre = "Lechuga", descripcion = "Cama 1", sectorId = sectorId, fechaSiembra = 10L,
        fechaTrasplante = null, fechaCosechaEstimada = 20L, frecuenciaRiegoDias = 2, estado = "SEMBRADO",
        observaciones = null, rutaFoto = null
    )

    fun lecturaDto(id: Long = 1, sectorId: Long = 1) = LecturaDto(
        id = id, sectorId = sectorId, fechaLectura = 5L, humedadSuelo = 40, temperatura = 20.5,
        humedadAmbiente = 50, origen = null, estado = "NORMAL"
    )

    fun tareaDto(
        id: Long = 1, sectorId: Long = 1, cultivoId: Long? = null, responsableId: Long = 3,
        creadaPorId: Long = 1, estado: String = "PENDIENTE"
    ) = TareaDto(
        id = id, tipo = "RIEGO", descripcion = "Regar el sector", instrucciones = "Usar regadera",
        sectorId = sectorId, cultivoId = cultivoId, responsableId = responsableId, creadaPorId = creadaPorId,
        fechaCreacion = 1L, fechaProgramada = 2L, estado = estado, fechaRealizacion = null, comentarioEstudiante = null
    )

    fun evidenciaDto(id: Long = 1, tareaId: Long = 1, usuarioId: Long = 3) =
        EvidenciaDto(id, tareaId, usuarioId, "/fotos/1.jpg", "Listo", 3L)

    fun validacionDto(id: Long = 1, tareaId: Long = 1, validadorId: Long = 1) =
        ValidacionDto(id, tareaId, validadorId, "VALIDADA", "Bien hecho", 4L)

    fun reporteDto(id: Long = 1, sectorId: Long = 1, cultivoId: Long? = null, reportadoPorId: Long = 3) = ReporteDto(
        id = id, sectorId = sectorId, cultivoId = cultivoId, tipo = "FALTA_DE_AGUA", descripcion = "Tierra muy seca",
        fechaReporte = 6L, reportadoPorId = reportadoPorId, rutaFoto = null, estado = "INFORMADO",
        comentarioDocente = null, fechaCierre = null
    )

    // ------------------------------ Entidades locales ------------------------------
    fun usuario(id: Long = 1, rol: Rol = Rol.ADMINISTRADOR) = Usuario(
        id = id, nombreUsuario = "usuario$id", correo = "usuario$id@escuela.cl", clave = "",
        nombre = "Nombre Apellido", rol = rol,
        cargo = if (rol == Rol.ADMINISTRADOR) Cargo.DOCENTE else Cargo.ESTUDIANTE,
        taller = "Taller A"
    )

    fun sector(id: Long = 1) = Sector(
        id = id, codigo = "S-0$id", nombre = "Sector $id", descripcion = "Descripción", ubicacion = "Patio",
        estado = EstadoSector.EN_USO, responsableId = 1, fechaCreacion = 1L, fechaActualizacion = 1L
    )

    fun cultivo(id: Long = 1, sectorId: Long = 1, cosechaEnDias: Int = 40) = Cultivo(
        id = id, nombre = "Lechuga", descripcion = "Cama 1", sectorId = sectorId,
        fechaSiembra = Fechas.sumarDias(Fechas.hoy(), -10), fechaTrasplante = null,
        fechaCosechaEstimada = Fechas.sumarDias(Fechas.hoy(), cosechaEnDias), frecuenciaRiegoDias = 2,
        estado = EstadoCultivo.EN_CRECIMIENTO, observaciones = "", rutaFoto = null
    )

    fun tarea(
        id: Long = 1, responsableId: Long = 3, estado: EstadoTarea = EstadoTarea.PENDIENTE,
        diasHastaFecha: Int = 2, tipo: TipoTarea = TipoTarea.SIEMBRA
    ) = Tarea(
        id = id, tipo = tipo, descripcion = "Tarea $id", instrucciones = "Instrucciones", sectorId = 1,
        cultivoId = null, responsableId = responsableId, creadaPorId = 1, fechaCreacion = 1L,
        fechaProgramada = Fechas.sumarDias(Fechas.hoy(), diasHastaFecha), estado = estado,
        fechaRealizacion = null, comentarioEstudiante = null
    )

    fun tareaDetalle(tarea: Tarea) = TareaDetalle(
        tarea = tarea, sectorNombre = "Sector 1", cultivoNombre = null,
        responsableNombre = "Camila Soto", creadorNombre = "Ana Rojas"
    )

    fun reporteDetalle(id: Long = 1, reportadoPorId: Long = 3, estado: EstadoReporte = EstadoReporte.INFORMADO) =
        ReporteDetalle(
            reporte = ReporteProblema(
                id = id, sectorId = 1, cultivoId = null, tipo = TipoProblema.FALTA_DE_AGUA,
                descripcion = "Tierra seca", fechaReporte = 1L, reportadoPorId = reportadoPorId, rutaFoto = null,
                estado = estado, comentarioDocente = "", fechaCierre = null
            ),
            sectorNombre = "Sector 1", cultivoNombre = null, reportanteNombre = "Camila Soto"
        )

    // ------------------------------ Respuestas HTTP ------------------------------
    private fun cuerpo(json: String) = json.toResponseBody("application/json".toMediaType())

    /** Error HTTP como los que devuelve el microservicio: {"mensaje": "..."}. */
    fun errorHttp(codigo: Int, mensaje: String? = null): HttpException {
        val json = if (mensaje == null) "" else "{\"mensaje\":\"$mensaje\"}"
        return HttpException(Response.error<Any>(codigo, cuerpo(json)))
    }

    fun respuestaVacia(): Response<Void> = Response.success(null)

    fun respuestaError(codigo: Int, mensaje: String): Response<Void> =
        Response.error(codigo, cuerpo("{\"mensaje\":\"$mensaje\"}"))
}
