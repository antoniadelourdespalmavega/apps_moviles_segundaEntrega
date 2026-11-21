package cl.duoc.huertoescolar.data.remote.api

import cl.duoc.huertoescolar.data.remote.dto.CultivoDto
import cl.duoc.huertoescolar.data.remote.dto.CultivoRequest
import cl.duoc.huertoescolar.data.remote.dto.EstadoRequest
import cl.duoc.huertoescolar.data.remote.dto.LecturaDto
import cl.duoc.huertoescolar.data.remote.dto.SectorDto
import cl.duoc.huertoescolar.data.remote.dto.SectorRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

/** Endpoints de ms-huerto (puerto 8082). */
interface HuertoApi {

    // ----------------------------- Sectores -----------------------------
    @GET("sectores")
    suspend fun listarSectores(): List<SectorDto>

    @POST("sectores")
    suspend fun crearSector(@Body solicitud: SectorRequest): SectorDto

    @PUT("sectores/{id}")
    suspend fun actualizarSector(@Path("id") id: Long, @Body solicitud: SectorRequest): SectorDto

    @DELETE("sectores/{id}")
    suspend fun eliminarSector(@Path("id") id: Long): Response<Void>

    // ----------------------------- Cultivos -----------------------------
    @GET("cultivos")
    suspend fun listarCultivos(): List<CultivoDto>

    @POST("cultivos")
    suspend fun crearCultivo(@Body solicitud: CultivoRequest): CultivoDto

    @PUT("cultivos/{id}")
    suspend fun actualizarCultivo(@Path("id") id: Long, @Body solicitud: CultivoRequest): CultivoDto

    @PUT("cultivos/{id}/estado")
    suspend fun cambiarEstadoCultivo(@Path("id") id: Long, @Body solicitud: EstadoRequest): CultivoDto

    @DELETE("cultivos/{id}")
    suspend fun eliminarCultivo(@Path("id") id: Long): Response<Void>

    // ------------------------- Lecturas de sensores ----------------------
    @GET("lecturas")
    suspend fun listarLecturas(): List<LecturaDto>

    @POST("lecturas/simular")
    suspend fun simularLecturas(): List<LecturaDto>
}
