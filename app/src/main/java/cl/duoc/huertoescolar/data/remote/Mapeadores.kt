package cl.duoc.huertoescolar.data.remote

import cl.duoc.huertoescolar.data.local.entity.Cultivo
import cl.duoc.huertoescolar.data.local.entity.Evidencia
import cl.duoc.huertoescolar.data.local.entity.LecturaSensor
import cl.duoc.huertoescolar.data.local.entity.ReporteProblema
import cl.duoc.huertoescolar.data.local.entity.Sector
import cl.duoc.huertoescolar.data.local.entity.Tarea
import cl.duoc.huertoescolar.data.local.entity.Usuario
import cl.duoc.huertoescolar.data.local.entity.Validacion
import cl.duoc.huertoescolar.data.remote.dto.CultivoDto
import cl.duoc.huertoescolar.data.remote.dto.EvidenciaDto
import cl.duoc.huertoescolar.data.remote.dto.LecturaDto
import cl.duoc.huertoescolar.data.remote.dto.ReporteDto
import cl.duoc.huertoescolar.data.remote.dto.SectorDto
import cl.duoc.huertoescolar.data.remote.dto.TareaDto
import cl.duoc.huertoescolar.data.remote.dto.UsuarioDto
import cl.duoc.huertoescolar.data.remote.dto.ValidacionDto
import cl.duoc.huertoescolar.domain.Cargo
import cl.duoc.huertoescolar.domain.EstadoCultivo
import cl.duoc.huertoescolar.domain.EstadoLectura
import cl.duoc.huertoescolar.domain.EstadoReporte
import cl.duoc.huertoescolar.domain.EstadoSector
import cl.duoc.huertoescolar.domain.EstadoTarea
import cl.duoc.huertoescolar.domain.ResultadoValidacion
import cl.duoc.huertoescolar.domain.Rol
import cl.duoc.huertoescolar.domain.TipoProblema
import cl.duoc.huertoescolar.domain.TipoTarea

/*
 * Conversion de lo que llega del microservicio (Dto) a las entidades de
 * Room, que es donde la app guarda su copia local. Las pantallas siguen
 * leyendo de Room, asi funcionan igual que antes y se actualizan solas.
 */

/** Convierte un texto en enum; si el servidor manda un valor desconocido, usa el de respaldo. */
inline fun <reified E : Enum<E>> enumSeguro(valor: String?, respaldo: E): E =
    enumValues<E>().firstOrNull { it.name == valor } ?: respaldo

/** La contrasena nunca baja del servidor: en la copia local queda vacia. */
fun UsuarioDto.aEntidad() = Usuario(
    id = id,
    nombreUsuario = nombreUsuario,
    correo = correo,
    clave = "",
    nombre = nombre,
    rol = enumSeguro(rol, Rol.ESTUDIANTE),
    cargo = enumSeguro(cargo, Cargo.ESTUDIANTE),
    taller = taller,
    activo = activo
)

fun SectorDto.aEntidad() = Sector(
    id = id,
    codigo = codigo,
    nombre = nombre,
    descripcion = descripcion,
    ubicacion = ubicacion,
    estado = enumSeguro(estado, EstadoSector.DISPONIBLE),
    responsableId = responsableId,
    fechaCreacion = fechaCreacion,
    fechaActualizacion = fechaActualizacion
)

fun CultivoDto.aEntidad() = Cultivo(
    id = id,
    nombre = nombre,
    descripcion = descripcion,
    sectorId = sectorId,
    fechaSiembra = fechaSiembra,
    fechaTrasplante = fechaTrasplante,
    fechaCosechaEstimada = fechaCosechaEstimada,
    frecuenciaRiegoDias = frecuenciaRiegoDias,
    estado = enumSeguro(estado, EstadoCultivo.PLANIFICADO),
    observaciones = observaciones.orEmpty(),
    rutaFoto = rutaFoto
)

fun LecturaDto.aEntidad() = LecturaSensor(
    id = id,
    sectorId = sectorId,
    fechaLectura = fechaLectura,
    humedadSuelo = humedadSuelo,
    temperatura = temperatura,
    humedadAmbiente = humedadAmbiente,
    origen = origen ?: "Simulación académica",
    estado = enumSeguro(estado, EstadoLectura.NORMAL)
)

fun TareaDto.aEntidad() = Tarea(
    id = id,
    tipo = enumSeguro(tipo, TipoTarea.OTRA),
    descripcion = descripcion,
    instrucciones = instrucciones,
    sectorId = sectorId,
    cultivoId = cultivoId,
    responsableId = responsableId,
    creadaPorId = creadaPorId,
    fechaCreacion = fechaCreacion,
    fechaProgramada = fechaProgramada,
    estado = enumSeguro(estado, EstadoTarea.PENDIENTE),
    fechaRealizacion = fechaRealizacion,
    comentarioEstudiante = comentarioEstudiante
)

fun EvidenciaDto.aEntidad() = Evidencia(
    id = id,
    tareaId = tareaId,
    usuarioId = usuarioId,
    rutaFoto = rutaFoto,
    comentario = comentario,
    fechaRegistro = fechaRegistro
)

fun ValidacionDto.aEntidad() = Validacion(
    id = id,
    tareaId = tareaId,
    validadorId = validadorId,
    resultado = enumSeguro(resultado, ResultadoValidacion.VALIDADA),
    motivo = motivo,
    fechaValidacion = fechaValidacion
)

fun ReporteDto.aEntidad() = ReporteProblema(
    id = id,
    sectorId = sectorId,
    cultivoId = cultivoId,
    tipo = enumSeguro(tipo, TipoProblema.OTRO),
    descripcion = descripcion,
    fechaReporte = fechaReporte,
    reportadoPorId = reportadoPorId,
    rutaFoto = rutaFoto,
    estado = enumSeguro(estado, EstadoReporte.INFORMADO),
    comentarioDocente = comentarioDocente.orEmpty(),
    fechaCierre = fechaCierre
)
