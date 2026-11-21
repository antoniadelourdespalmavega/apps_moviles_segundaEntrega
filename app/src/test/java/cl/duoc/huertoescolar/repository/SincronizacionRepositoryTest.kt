package cl.duoc.huertoescolar.repository

import cl.duoc.huertoescolar.data.local.dao.DatosActividades
import cl.duoc.huertoescolar.data.local.dao.DatosHuerto
import cl.duoc.huertoescolar.data.local.dao.SincronizacionDao
import cl.duoc.huertoescolar.data.remote.ErroresApi
import cl.duoc.huertoescolar.data.remote.api.ActividadesApi
import cl.duoc.huertoescolar.data.remote.api.HuertoApi
import cl.duoc.huertoescolar.data.remote.api.UsuariosApi
import cl.duoc.huertoescolar.data.repository.SincronizacionRepository
import cl.duoc.huertoescolar.domain.Operacion
import cl.duoc.huertoescolar.util.Fabrica
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import io.mockk.Runs
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException

class SincronizacionRepositoryTest {

    private val usuariosApi = mockk<UsuariosApi>()
    private val huertoApi = mockk<HuertoApi>()
    private val actividadesApi = mockk<ActividadesApi>()
    private val dao = mockk<SincronizacionDao>()
    private lateinit var repositorio: SincronizacionRepository

    @Before
    fun preparar() {
        repositorio = SincronizacionRepository(usuariosApi, huertoApi, actividadesApi, dao)
        coEvery { dao.aplicarUsuarios(any()) } just Runs
        coEvery { dao.aplicarHuerto(any()) } just Runs
        coEvery { dao.aplicarActividades(any()) } just Runs
        coEvery { dao.idsUsuarios() } returns listOf(1L, 3L)
        coEvery { dao.idsSectores() } returns listOf(1L)
        coEvery { dao.idsCultivos() } returns emptyList()
        coEvery { usuariosApi.listar() } returns listOf(Fabrica.usuarioDto(1), Fabrica.usuarioDto(3, "ESTUDIANTE", "ESTUDIANTE"))
        coEvery { huertoApi.listarSectores() } returns listOf(Fabrica.sectorDto(1))
        coEvery { huertoApi.listarCultivos() } returns listOf(Fabrica.cultivoDto(1, 1), Fabrica.cultivoDto(2, 8))
        coEvery { huertoApi.listarLecturas() } returns listOf(Fabrica.lecturaDto(1, 1))
        coEvery { actividadesApi.listarTareas() } returns listOf(Fabrica.tareaDto(1))
        coEvery { actividadesApi.listarEvidencias() } returns listOf(Fabrica.evidenciaDto(1))
        coEvery { actividadesApi.listarValidaciones() } returns emptyList()
        coEvery { actividadesApi.listarReportes() } returns listOf(Fabrica.reporteDto(1))
    }

    @Test
    fun sincronizarUsuariosGuardaLosUsuariosSinContrasena() = runTest {
        val resultado = repositorio.sincronizarUsuarios()
        assertTrue(resultado is Operacion.Exito)
        coVerify { dao.aplicarUsuarios(match { lista -> lista.size == 2 && lista.all { it.clave == "" } }) }
    }

    @Test
    fun sincronizarHuertoDescartaCultivosHuerfanos() = runTest {
        val guardado = slot<DatosHuerto>()
        coEvery { dao.aplicarHuerto(capture(guardado)) } just Runs

        repositorio.sincronizarHuerto()

        assertEquals(listOf(1L), guardado.captured.cultivos.map { it.id })
        assertEquals(1, guardado.captured.lecturas.size)
    }

    @Test
    fun sincronizarActividadesGuardaTareasYReportes() = runTest {
        val guardado = slot<DatosActividades>()
        coEvery { dao.aplicarActividades(capture(guardado)) } just Runs

        assertTrue(repositorio.sincronizarActividades() is Operacion.Exito)
        assertEquals(1, guardado.captured.tareas.size)
        assertEquals(1, guardado.captured.evidencias.size)
        assertEquals(1, guardado.captured.reportes.size)
    }

    @Test
    fun sincronizarTodoRecorreLosTresServicios() = runTest {
        val resultado = repositorio.sincronizarTodo()
        assertEquals(Operacion.Exito("Datos sincronizados con el microservicio"), resultado)
        coVerify(exactly = 1) { dao.aplicarUsuarios(any()) }
        coVerify(exactly = 1) { dao.aplicarHuerto(any()) }
        coVerify(exactly = 1) { dao.aplicarActividades(any()) }
    }

    @Test
    fun sincronizarTodoSeDetieneEnElPrimerError() = runTest {
        coEvery { huertoApi.listarSectores() } throws IOException("sin red")

        val resultado = repositorio.sincronizarTodo()

        assertEquals(Operacion.Error(ErroresApi.SIN_CONEXION), resultado)
        coVerify(exactly = 0) { dao.aplicarActividades(any()) }
    }
}
