package cl.duoc.huertoescolar.data.remote

import cl.duoc.huertoescolar.domain.Resultado
import com.google.gson.Gson
import kotlinx.coroutines.CancellationException
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

/** Cuerpo de error que devuelven los microservicios: {"mensaje": "..."}. */
data class ErrorDto(val mensaje: String?)

/**
 * Traduce los errores de red a mensajes claros para el usuario.
 * - HttpException: el servidor respondio con error (400, 404, 409...). Se usa
 *   el mensaje que envia el microservicio.
 * - IOException: no hubo conexion (microservicio apagado, sin internet...).
 */
object ErroresApi {

    const val SIN_CONEXION =
        "No se pudo conectar con el servidor. Revisa que los microservicios estén encendidos."

    private val gson = Gson()

    fun mensaje(error: Throwable): String = when (error) {
        is HttpException -> mensajeDelServidor(error)
            ?: "El servidor respondió con un error (código ${error.code()})"
        is IOException -> SIN_CONEXION
        else -> "Ocurrió un error inesperado: " + (error.message ?: error.javaClass.simpleName)
    }

    private fun mensajeDelServidor(error: HttpException): String? = try {
        val cuerpo = error.response()?.errorBody()?.string()
        cuerpo?.let { gson.fromJson(it, ErrorDto::class.java)?.mensaje }?.takeIf { it.isNotBlank() }
    } catch (e: Exception) {
        null
    }
}

/**
 * Ejecuta una llamada a la API y la envuelve en un Resultado, de modo que los
 * repositorios nunca dejen escapar una excepcion hacia los ViewModel.
 */
suspend fun <T> llamarApi(bloque: suspend () -> T): Resultado<T> = try {
    Resultado.Exito(bloque())
} catch (e: CancellationException) {
    throw e
} catch (e: Exception) {
    Resultado.Error(ErroresApi.mensaje(e))
}

/**
 * Para los endpoints que no devuelven cuerpo (DELETE): Retrofit entrega la
 * respuesta aunque sea 404 o 500, por eso se revisa isSuccessful (igual que en
 * el ejemplo del docente).
 */
fun Response<*>.comprobar() {
    if (!isSuccessful) throw HttpException(this)
}
