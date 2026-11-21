package cl.duoc.huertoescolar.remote

import cl.duoc.huertoescolar.data.remote.ErroresApi
import cl.duoc.huertoescolar.data.remote.comprobar
import cl.duoc.huertoescolar.data.remote.llamarApi
import cl.duoc.huertoescolar.domain.Resultado
import cl.duoc.huertoescolar.util.Fabrica
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import java.io.IOException

/** Traduccion de errores de red a mensajes para el usuario. */
class ErroresApiTest {

    @Test
    fun usaElMensajeQueEnviaElMicroservicio() {
        val error = Fabrica.errorHttp(409, "Ya existe otro sector con el código S-01")
        assertEquals("Ya existe otro sector con el código S-01", ErroresApi.mensaje(error))
    }

    @Test
    fun sinMensajeDelServidorInformaElCodigo() {
        val error = Fabrica.errorHttp(500)
        assertEquals("El servidor respondió con un error (código 500)", ErroresApi.mensaje(error))
    }

    @Test
    fun sinConexionDaUnMensajeClaro() {
        assertEquals(ErroresApi.SIN_CONEXION, ErroresApi.mensaje(IOException("Connection refused")))
    }

    @Test
    fun otrosErroresIncluyenSuDescripcion() {
        val mensaje = ErroresApi.mensaje(IllegalStateException("dato raro"))
        assertTrue(mensaje.contains("dato raro"))
    }

    @Test
    fun llamarApiEnvuelveElResultadoCorrecto() = runTest {
        val resultado = llamarApi { 42 }
        assertEquals(Resultado.Exito(42), resultado)
    }

    @Test
    fun llamarApiConvierteLaExcepcionEnError() = runTest {
        val resultado = llamarApi<Int> { throw IOException("sin red") }
        assertEquals(Resultado.Error(ErroresApi.SIN_CONEXION), resultado)
    }

    @Test
    fun comprobarNoHaceNadaSiLaRespuestaEsCorrecta() {
        Fabrica.respuestaVacia().comprobar()
    }

    @Test(expected = HttpException::class)
    fun comprobarLanzaErrorSiLaRespuestaFalla() {
        Fabrica.respuestaError(404, "No existe").comprobar()
    }
}
