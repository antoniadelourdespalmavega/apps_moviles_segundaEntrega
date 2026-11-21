package cl.duoc.huertoescolar.repository

import cl.duoc.huertoescolar.data.local.dao.UsuarioDao
import cl.duoc.huertoescolar.data.remote.api.UsuariosApi
import cl.duoc.huertoescolar.data.remote.dto.LoginRequest
import cl.duoc.huertoescolar.data.repository.SesionRepository
import cl.duoc.huertoescolar.data.repository.SincronizacionRepository
import cl.duoc.huertoescolar.domain.Cargo
import cl.duoc.huertoescolar.domain.Operacion
import cl.duoc.huertoescolar.domain.Resultado
import cl.duoc.huertoescolar.domain.Rol
import cl.duoc.huertoescolar.util.Fabrica
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SesionRepositoryTest {

    private val api = mockk<UsuariosApi>()
    private val usuarioDao = mockk<UsuarioDao>()
    private val sincronizacion = mockk<SincronizacionRepository>()
    private lateinit var repositorio: SesionRepository

    @Before
    fun preparar() {
        repositorio = SesionRepository(api, usuarioDao, sincronizacion)
        coEvery { sincronizacion.sincronizarUsuarios() } returns Operacion.Exito("ok")
    }

    @Test
    fun loginCorrectoDevuelveElUsuario() = runTest {
        coEvery { api.login(LoginRequest("profe.ana", "huerto123")) } returns Fabrica.usuarioDto(1)

        val resultado = repositorio.autenticar("  Profe.Ana ", "huerto123")

        assertTrue(resultado is Resultado.Exito)
        assertEquals(1L, (resultado as Resultado.Exito).dato.id)
    }

    @Test
    fun loginIncorrectoDevuelveElMensajeDelServidor() = runTest {
        coEvery { api.login(any()) } throws Fabrica.errorHttp(401, "Usuario, correo o contraseña incorrectos")

        val resultado = repositorio.autenticar("profe.ana", "mala")

        assertEquals(Resultado.Error("Usuario, correo o contraseña incorrectos"), resultado)
    }

    @Test
    fun registroCorrectoActualizaLasCuentas() = runTest {
        coEvery { api.registrar(any()) } returns Fabrica.usuarioDto(6, "ESTUDIANTE", "ESTUDIANTE")

        val resultado = repositorio.registrar(
            "Pedro   Soto", "PEDRO@escuela.cl", "Est.Pedro", "huerto123", Cargo.ESTUDIANTE, "Taller A"
        )

        assertEquals(Operacion.Exito("Cuenta creada. Ya puedes iniciar sesión"), resultado)
        coVerify {
            api.registrar(match { it.nombre == "Pedro Soto" && it.correo == "pedro@escuela.cl" && it.nombreUsuario == "est.pedro" })
        }
        coVerify { sincronizacion.sincronizarUsuarios() }
    }

    @Test
    fun registroRepetidoInformaElConflicto() = runTest {
        coEvery { api.registrar(any()) } throws Fabrica.errorHttp(409, "Ese nombre de usuario ya está en uso")

        val resultado = repositorio.registrar("Pedro Soto", "p@escuela.cl", "est.pedro", "huerto123", Cargo.ESTUDIANTE, "Taller A")

        assertEquals(Operacion.Error("Ese nombre de usuario ya está en uso"), resultado)
    }

    @Test
    fun actualizarPerfilDevuelveElUsuarioActualizado() = runTest {
        coEvery { api.actualizarPerfil(1, any()) } returns Fabrica.usuarioDto(1)

        val (usuario, operacion) = repositorio.actualizarPerfil(1, "Ana Rojas", "ana@escuela.cl", "Taller", "", "")

        assertEquals(1L, usuario?.id)
        assertTrue(operacion is Operacion.Exito)
    }

    @Test
    fun actualizarPerfilConClaveActualIncorrecta() = runTest {
        coEvery { api.actualizarPerfil(1, any()) } throws Fabrica.errorHttp(400, "La contraseña actual no es correcta")

        val (usuario, operacion) = repositorio.actualizarPerfil(1, "Ana Rojas", "ana@escuela.cl", "Taller", "x", "nueva1234")

        assertNull(usuario)
        assertEquals(Operacion.Error("La contraseña actual no es correcta"), operacion)
    }

    @Test
    fun lecturasLocalesSeDelegaAlDao() = runTest {
        val ana = Fabrica.usuario(1)
        coEvery { usuarioDao.porId(1) } returns ana
        every { usuarioDao.observarPorRol(Rol.ESTUDIANTE) } returns flowOf(emptyList())
        every { usuarioDao.observarTodos() } returns flowOf(listOf(ana))

        assertEquals(ana, repositorio.porId(1))
        repositorio.observarEstudiantes()
        repositorio.observarUsuarios()
        assertTrue(repositorio.actualizarCuentas() is Operacion.Exito)
        verify { usuarioDao.observarPorRol(Rol.ESTUDIANTE) }
        verify { usuarioDao.observarTodos() }
    }
}
