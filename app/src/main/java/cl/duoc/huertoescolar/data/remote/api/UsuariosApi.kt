package cl.duoc.huertoescolar.data.remote.api

import cl.duoc.huertoescolar.data.remote.dto.LoginRequest
import cl.duoc.huertoescolar.data.remote.dto.PerfilRequest
import cl.duoc.huertoescolar.data.remote.dto.RegistroRequest
import cl.duoc.huertoescolar.data.remote.dto.UsuarioDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

/** Endpoints de ms-usuarios (puerto 8081). */
interface UsuariosApi {

    @GET("usuarios")
    suspend fun listar(): List<UsuarioDto>

    @POST("usuarios")
    suspend fun registrar(@Body solicitud: RegistroRequest): UsuarioDto

    @POST("usuarios/login")
    suspend fun login(@Body solicitud: LoginRequest): UsuarioDto

    @PUT("usuarios/{id}")
    suspend fun actualizarPerfil(
        @Path("id") id: Long,
        @Body solicitud: PerfilRequest
    ): UsuarioDto
}
