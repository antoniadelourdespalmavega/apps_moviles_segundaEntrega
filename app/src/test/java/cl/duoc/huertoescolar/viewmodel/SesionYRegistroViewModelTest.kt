package cl.duoc.huertoescolar.viewmodel

import cl.duoc.huertoescolar.data.repository.SesionRepository
import cl.duoc.huertoescolar.domain.Cargo
import cl.duoc.huertoescolar.domain.Operacion
import cl.duoc.huertoescolar.domain.Resultado
import cl.duoc.huertoescolar.domain.Rol
import cl.duoc.huertoescolar.domain.validacion.Campos
import cl.duoc.huertoescolar.ui.login.RegistroViewModel
import cl.duoc.huertoescolar.ui.sesion.SesionViewModel
import cl.duoc.huertoescolar.util.Fabrica
import cl.duoc.huertoescolar.util.ReglaCorrutinas
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class SesionYRegistroViewModelTest {

    @get:Rule
    val reglaCorrutinas = ReglaCorrutinas()

    private val repositorio = mockk<SesionRepository>().also {
        every { it.observarUsuarios() } returns flowOf(emptyList())
        coEvery { it.actualizarCuentas() } returns Operacion.Exito("ok")
    }

    // ------------------------------ Inicio de sesion ------------------------------
    @Test
    fun sinServidorSeAvisaEnElLogin() {
        coEvery { repositorio.actualizarCuentas() } returns Operacion.Error("No se pudo conectar con el servidor")
        val vm = SesionViewModel(repositorio)
        assertEquals("No se pudo conectar con el servidor", vm.ui.value.mensaje)
    }

    @Test
    fun loginVacioMuestraErroresSinLlamarAlServidor() {
        val vm = SesionViewModel(repositorio)
        vm.iniciarSesion()
        assertTrue(vm.ui.value.errores.containsKey(Campos.USUARIO))
        assertTrue(vm.ui.value.errores.containsKey(Campos.CLAVE))
        coVerify(exactly = 0) { repositorio.autenticar(any(), any()) }
    }

    @Test
    fun loginCorrectoIniciaLaSesion() {
        coEvery { repositorio.autenticar("profe.ana", "huerto123") } returns Resultado.Exito(Fabrica.usuario(1))
        val vm = SesionViewModel(repositorio)
        vm.cambiarUsuario("profe.ana")
        vm.cambiarClave("huerto123")

        vm.iniciarSesion()

        assertTrue(vm.ui.value.sesionIniciada)
        assertTrue(vm.ui.value.esAdministrador)
        assertEquals("", vm.ui.value.clave)
    }

    @Test
    fun loginIncorrectoMuestraElMensaje() {
        coEvery { repositorio.autenticar(any(), any()) } returns Resultado.Error("Usuario, correo o contraseña incorrectos")
        val vm = SesionViewModel(repositorio)
        vm.cambiarUsuario("profe.ana")
        vm.cambiarClave("mala")

        vm.iniciarSesion()

        assertFalse(vm.ui.value.sesionIniciada)
        assertEquals("Usuario, correo o contraseña incorrectos", vm.ui.value.mensaje)
        vm.limpiarMensaje()
        assertNull(vm.ui.value.mensaje)
    }

    @Test
    fun cerrarSesionYCuentaCreada() {
        coEvery { repositorio.autenticar(any(), any()) } returns Resultado.Exito(Fabrica.usuario(3, Rol.ESTUDIANTE))
        val vm = SesionViewModel(repositorio)
        vm.cambiarUsuario("est.camila")
        vm.cambiarClave("huerto123")
        vm.iniciarSesion()
        vm.actualizarUsuario(Fabrica.usuario(3, Rol.ESTUDIANTE).copy(nombre = "Camila S."))
        assertEquals("Camila S.", vm.ui.value.usuarioActual?.nombre)

        vm.cerrarSesion()
        assertFalse(vm.ui.value.sesionIniciada)

        vm.cuentaCreada("est.pedro")
        assertEquals("est.pedro", vm.ui.value.usuario)
        assertTrue(vm.ui.value.informacion != null)
    }

    // ------------------------------ Registro ------------------------------
    private fun registroCompleto(vm: RegistroViewModel) {
        vm.cambiarNombre("Pedro Soto")
        vm.cambiarCorreo("pedro@escuela.cl")
        vm.cambiarUsuario("est.pedro")
        vm.cambiarClave("huerto123")
        vm.cambiarConfirmar("huerto123")
        vm.cambiarCargo(Cargo.ESTUDIANTE)
        vm.cambiarTaller("Taller A")
    }

    @Test
    fun registroIncompletoNoLlamaAlServidor() {
        val vm = RegistroViewModel(repositorio)
        vm.registrar()
        assertTrue(vm.ui.value.errores.containsKey(Campos.NOMBRE))
        assertEquals("Revisa los campos marcados", vm.ui.value.mensaje)
        coVerify(exactly = 0) { repositorio.registrar(any(), any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun validacionEnVivoQuitaElErrorAlCorregir() {
        val vm = RegistroViewModel(repositorio)
        vm.registrar()
        assertTrue(vm.ui.value.errores.containsKey(Campos.CORREO))
        vm.cambiarCorreo("pedro@escuela.cl")
        assertFalse(vm.ui.value.errores.containsKey(Campos.CORREO))
    }

    @Test
    fun registroCorrecto() {
        coEvery { repositorio.registrar(any(), any(), any(), any(), any(), any(), any()) } returns
                Operacion.Exito("Cuenta creada. Ya puedes iniciar sesión")
        val vm = RegistroViewModel(repositorio)
        registroCompleto(vm)

        vm.registrar()

        assertTrue(vm.ui.value.registrado)
        assertFalse(vm.ui.value.guardando)
    }

    @Test
    fun registroRechazadoPorElServidor() {
        coEvery { repositorio.registrar(any(), any(), any(), any(), any(), any(), any()) } returns
                Operacion.Error("Ese correo ya está registrado")
        val vm = RegistroViewModel(repositorio)
        registroCompleto(vm)

        vm.registrar()

        assertFalse(vm.ui.value.registrado)
        assertEquals("Ese correo ya está registrado", vm.ui.value.mensaje)
    }

    @Test
    fun docentePideCodigoDeAutorizacion() {
        val vm = RegistroViewModel(repositorio)
        registroCompleto(vm)
        vm.cambiarCargo(Cargo.DOCENTE)
        assertTrue(vm.ui.value.pideCodigo)
        vm.registrar()
        assertTrue(vm.ui.value.errores.containsKey(Campos.CODIGO_AUTORIZACION))
        vm.cambiarCodigo("HUERTO2026")
        assertFalse(vm.ui.value.errores.containsKey(Campos.CODIGO_AUTORIZACION))
    }
}
