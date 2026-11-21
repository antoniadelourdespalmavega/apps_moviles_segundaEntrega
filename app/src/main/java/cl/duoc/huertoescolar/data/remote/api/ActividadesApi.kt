package cl.duoc.huertoescolar.data.remote.api

import cl.duoc.huertoescolar.data.remote.dto.AccionRequest
import cl.duoc.huertoescolar.data.remote.dto.EntregaRequest
import cl.duoc.huertoescolar.data.remote.dto.EstadoReporteRequest
import cl.duoc.huertoescolar.data.remote.dto.EvidenciaDto
import cl.duoc.huertoescolar.data.remote.dto.ReporteDto
import cl.duoc.huertoescolar.data.remote.dto.ReporteRequest
import cl.duoc.huertoescolar.data.remote.dto.RevisionRequest
import cl.duoc.huertoescolar.data.remote.dto.TareaDto
import cl.duoc.huertoescolar.data.remote.dto.TareaRequest
import cl.duoc.huertoescolar.data.remote.dto.ValidacionDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

/** Endpoints de ms-actividades (puerto 8083). */
interface ActividadesApi {

    // ------------------------------ Tareas ------------------------------
    @GET("tareas")
    suspend fun listarTareas(): List<TareaDto>

    @POST("tareas")
    suspend fun crearTarea(@Body solicitud: TareaRequest): TareaDto

    @PUT("tareas/{id}")
    suspend fun actualizarTarea(@Path("id") id: Long, @Body solicitud: TareaRequest): TareaDto

    @DELETE("tareas/{id}")
    suspend fun eliminarTarea(@Path("id") id: Long, @Query("usuarioId") usuarioId: Long): Response<Void>

    @POST("tareas/{id}/iniciar")
    suspend fun iniciarTarea(@Path("id") id: Long, @Body solicitud: AccionRequest): TareaDto

    @POST("tareas/{id}/entregar")
    suspend fun entregarTarea(@Path("id") id: Long, @Body solicitud: EntregaRequest): TareaDto

    @POST("tareas/{id}/revisar")
    suspend fun revisarTarea(@Path("id") id: Long, @Body solicitud: RevisionRequest): TareaDto

    // ------------------- Historial de evidencias y revisiones ------------
    @GET("evidencias")
    suspend fun listarEvidencias(): List<EvidenciaDto>

    @GET("validaciones")
    suspend fun listarValidaciones(): List<ValidacionDto>

    // ----------------------------- Reportes -----------------------------
    @GET("reportes")
    suspend fun listarReportes(): List<ReporteDto>

    @POST("reportes")
    suspend fun registrarReporte(@Body solicitud: ReporteRequest): ReporteDto

    @PUT("reportes/{id}/estado")
    suspend fun actualizarEstadoReporte(
        @Path("id") id: Long,
        @Body solicitud: EstadoReporteRequest
    ): ReporteDto

    @DELETE("reportes/{id}")
    suspend fun eliminarReporte(@Path("id") id: Long): Response<Void>
}
