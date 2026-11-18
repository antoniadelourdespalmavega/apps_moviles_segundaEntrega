package cl.duoc.huertoescolar

import cl.duoc.huertoescolar.domain.validacion.Campos
import cl.duoc.huertoescolar.domain.validacion.ReglasComunes
import cl.duoc.huertoescolar.domain.validacion.ValidadorCultivo
import cl.duoc.huertoescolar.domain.validacion.ValidadorEntrega
import cl.duoc.huertoescolar.domain.validacion.ValidadorLogin
import cl.duoc.huertoescolar.domain.validacion.ValidadorRevision
import cl.duoc.huertoescolar.domain.validacion.ValidadorSector
import cl.duoc.huertoescolar.domain.validacion.revalidar
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pruebas de los validadores separados por formulario y de la validacion
 * en vivo (revalidar), que actualiza el error de un campo mientras se escribe.
 */
class ValidadoresPorFormularioTest {

    @Test
    fun loginVacioMarcaUsuarioYClave() {
        val errores = ValidadorLogin.validar(usuario = "", clave = "")
        assertEquals(setOf(Campos.USUARIO, Campos.CLAVE), errores.keys)
    }

    @Test
    fun loginConCorreoMalEscritoMarcaElUsuario() {
        assertNotNull(ValidadorLogin.validarUsuario("ana@"))
        assertNull(ValidadorLogin.validarUsuario("profe.ana"))
    }

    @Test
    fun codigoDeSectorSeRevisaEnMayusculas() {
        assertNull(ValidadorSector.validarCodigo("s-01"))
        assertNotNull(ValidadorSector.validarCodigo("s 01"))
    }

    @Test
    fun trasplanteDebeQuedarEntreSiembraYCosecha() {
        assertNull(ValidadorCultivo.validarTrasplante("01/05/2026", "", "30/05/2026"))
        assertNotNull(ValidadorCultivo.validarTrasplante("10/05/2026", "01/05/2026", "30/05/2026"))
        assertNotNull(ValidadorCultivo.validarTrasplante("01/05/2026", "15/06/2026", "30/05/2026"))
    }

    @Test
    fun entregaCompletaNoTieneErrores() {
        val errores = ValidadorEntrega.validar("Regué las dos camas", "/ruta/foto.jpg")
        assertTrue(errores.isEmpty())
    }

    @Test
    fun motivoDeRechazoCortoEsInvalido() {
        assertNotNull(ValidadorRevision.validarMotivo("no"))
        assertNull(ValidadorRevision.validarMotivo("Falta la foto del sector"))
    }

    @Test
    fun revalidarQuitaElErrorCuandoElCampoQuedaCorrecto() {
        val errores = mapOf(Campos.CORREO to "Correo inválido")
        val despues = errores.revalidar(Campos.CORREO, ReglasComunes.correo("ana@escuela.cl"))
        assertFalse(despues.containsKey(Campos.CORREO))
    }

    @Test
    fun revalidarActualizaElMensajeSiElCampoSigueMal() {
        val errores = mapOf(Campos.CLAVE to "La contraseña es obligatoria")
        val despues = errores.revalidar(Campos.CLAVE, ReglasComunes.clave("abc"))
        assertEquals("La contraseña debe tener al menos 8 caracteres", despues[Campos.CLAVE])
    }

    @Test
    fun revalidarNoMuestraErroresAntesDelPrimerIntento() {
        val sinErrores = emptyMap<String, String>()
        val despues = sinErrores.revalidar(Campos.NOMBRE, ReglasComunes.nombrePersona("A"))
        assertTrue(despues.isEmpty())
    }
}
