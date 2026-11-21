package cl.duoc.huertoescolar.data.remote.dto

/*
 * Modelos de lectura (Dto) y de escritura (Request) de ms-huerto.
 * Se separan igual que en la clase: lo que se recibe trae id y fechas que
 * genera el servidor; lo que se envia solo trae los datos del formulario.
 */

data class SectorDto(
    val id: Long,
    val codigo: String,
    val nombre: String,
    val descripcion: String,
    val ubicacion: String,
    val estado: String,
    val responsableId: Long?,
    val fechaCreacion: Long,
    val fechaActualizacion: Long
)

data class SectorRequest(
    val codigo: String,
    val nombre: String,
    val descripcion: String,
    val ubicacion: String,
    val estado: String,
    val responsableId: Long?
)

data class CultivoDto(
    val id: Long,
    val nombre: String,
    val descripcion: String,
    val sectorId: Long,
    val fechaSiembra: Long,
    val fechaTrasplante: Long?,
    val fechaCosechaEstimada: Long,
    val frecuenciaRiegoDias: Int,
    val estado: String,
    val observaciones: String?,
    val rutaFoto: String?
)

data class CultivoRequest(
    val nombre: String,
    val descripcion: String,
    val sectorId: Long,
    val fechaSiembra: Long,
    val fechaTrasplante: Long?,
    val fechaCosechaEstimada: Long,
    val frecuenciaRiegoDias: Int,
    val estado: String,
    val observaciones: String,
    val rutaFoto: String?
)

/** Cambio de estado, por ejemplo {"estado": "COSECHADO"}. */
data class EstadoRequest(val estado: String)

data class LecturaDto(
    val id: Long,
    val sectorId: Long,
    val fechaLectura: Long,
    val humedadSuelo: Int,
    val temperatura: Double,
    val humedadAmbiente: Int,
    val origen: String?,
    val estado: String
)
