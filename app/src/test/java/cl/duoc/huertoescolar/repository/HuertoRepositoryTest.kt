package cl.duoc.huertoescolar.repository

import cl.duoc.huertoescolar.data.local.dao.CultivoDao
import cl.duoc.huertoescolar.data.local.dao.SectorDao
import cl.duoc.huertoescolar.data.local.dao.TareaDao
import cl.duoc.huertoescolar.data.remote.ErroresApi
import cl.duoc.huertoescolar.data.remote.api.HuertoApi
import cl.duoc.huertoescolar.data.remote.dto.EstadoRequest
import cl.duoc.huertoescolar.data.repository.HuertoRepository
import cl.duoc.huertoescolar.data.repository.SincronizacionRepository
import cl.duoc.huertoescolar.domain.EstadoCultivo
import cl.duoc.huertoescolar.domain.EstadoSector
import cl.duoc.huertoescolar.domain.Operacion
import cl.duoc.huertoescolar.util.Fabrica
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.io.IOException

class HuertoRepositoryTest {

    private val api = mockk<HuertoApi>()
    private val sectorDao = mockk<SectorDao>()
    private val cultivoDao = mockk<CultivoDao>()
    private val tareaDao = mockk<TareaDao>()
    private val sincronizacion = mockk<SincronizacionRepository>()
    private lateinit var repositorio: HuertoRepository

    @Before
    fun preparar() {
        repositorio = HuertoRepository(api, sectorDao, cultivoDao, tareaDao, sincronizacion)
        coEvery { sincronizacion.sincronizarHuerto() } returns Operacion.Exito("ok")
    }

    private suspend fun guardarSector(id: Long) = repositorio.guardarSector(
        id, " s-05 ", "Sector Oeste", "Cama nueva", "Patio oeste", EstadoSector.DISPONIBLE, null
    )

    @Test
    fun crearSectorDevuelveElIdGeneradoPorElServidor() = runTest {
        coEvery { api.crearSector(any()) } returns Fabrica.sectorDto(id = 5)

        val resultado = guardarSector(0)

        assertEquals(Operacion.Exito("Sector creado. Ahora puedes agregarle cultivos", idGenerado = 5), resultado)
        coVerify { api.crearSector(match { it.codigo == "S-05" && it.estado == "DISPONIBLE" }) }
        coVerify { sincronizacion.sincronizarHuerto() }
    }

    @Test
    fun editarSectorUsaPut() = runTest {
        coEvery { api.actualizarSector(5, any()) } returns Fabrica.sectorDto(id = 5)

        assertEquals(Operacion.Exito("Sector actualizado correctamente"), guardarSector(5))
    }

    @Test
    fun codigoRepetidoMuestraElMensajeDelServidor() = runTest {
        coEvery { api.crearSector(any()) } throws Fabrica.errorHttp(409, "Ya existe otro sector con el código S-05")

        assertEquals(Operacion.Error("Ya existe otro sector con el código S-05"), guardarSector(0))
        coVerify(exactly = 0) { sincronizacion.sincronizarHuerto() }
    }

    @Test
    fun noSeEliminaUnSectorConTareas() = runTest {
        coEvery { tareaDao.contarPorSector(1) } returns 2

        val resultado = repositorio.eliminarSector(1)

        assertEquals(Operacion.Error("No se puede eliminar: el sector tiene tareas registradas"), resultado)
        coVerify(exactly = 0) { api.eliminarSector(any()) }
    }

    @Test
    fun eliminarSectorSinTareas() = runTest {
        coEvery { tareaDao.contarPorSector(1) } returns 0
        coEvery { api.eliminarSector(1) } returns Fabrica.respuestaVacia()

        assertEquals(Operacion.Exito("Sector eliminado"), repositorio.eliminarSector(1))
    }

    @Test
    fun eliminarSectorInexistenteInformaElError() = runTest {
        coEvery { tareaDao.contarPorSector(9) } returns 0
        coEvery { api.eliminarSector(9) } returns Fabrica.respuestaError(404, "El sector 9 no existe")

        assertEquals(Operacion.Error("El sector 9 no existe"), repositorio.eliminarSector(9))
    }

    @Test
    fun crearYEditarCultivo() = runTest {
        coEvery { api.crearCultivo(any()) } returns Fabrica.cultivoDto(3)
        coEvery { api.actualizarCultivo(3, any()) } returns Fabrica.cultivoDto(3)

        val crear = repositorio.guardarCultivo(0, "Lechuga", "Cama 1", 1, 10, null, 20, 2, EstadoCultivo.SEMBRADO, "", null)
        val editar = repositorio.guardarCultivo(3, "Lechuga", "Cama 1", 1, 10, null, 20, 2, EstadoCultivo.SEMBRADO, "", null)

        assertEquals(Operacion.Exito("Cultivo registrado correctamente"), crear)
        assertEquals(Operacion.Exito("Cultivo actualizado correctamente"), editar)
    }

    @Test
    fun sinConexionNoSeGuardaElCultivo() = runTest {
        coEvery { api.crearCultivo(any()) } throws IOException("sin red")

        val resultado = repositorio.guardarCultivo(0, "Lechuga", "Cama 1", 1, 10, null, 20, 2, EstadoCultivo.SEMBRADO, "", null)

        assertEquals(Operacion.Error(ErroresApi.SIN_CONEXION), resultado)
    }

    @Test
    fun eliminarCultivoRespetaSusTareas() = runTest {
        coEvery { tareaDao.contarPorCultivo(1) } returns 1
        coEvery { tareaDao.contarPorCultivo(2) } returns 0
        coEvery { api.eliminarCultivo(2) } returns Fabrica.respuestaVacia()

        assertEquals(
            Operacion.Error("No se puede eliminar: el cultivo tiene tareas registradas"),
            repositorio.eliminarCultivo(1)
        )
        assertEquals(Operacion.Exito("Cultivo eliminado"), repositorio.eliminarCultivo(2))
    }

    @Test
    fun cambiarEstadoDelCultivo() = runTest {
        coEvery { api.cambiarEstadoCultivo(1, EstadoRequest("COSECHADO")) } returns Fabrica.cultivoDto(1)

        val resultado = repositorio.cambiarEstadoCultivo(1, EstadoCultivo.COSECHADO)

        assertEquals(Operacion.Exito("Estado del cultivo actualizado a Cosechado"), resultado)
    }

    @Test
    fun lecturasLocalesSeDelegaALosDao() = runTest {
        every { sectorDao.observarResumen() } returns flowOf(emptyList())
        every { sectorDao.observarTodos() } returns flowOf(emptyList())
        every { sectorDao.observarPorId(1) } returns flowOf(null)
        every { cultivoDao.observarPorSector(1) } returns flowOf(emptyList())
        every { cultivoDao.observarTodos() } returns flowOf(emptyList())
        every { cultivoDao.observarDetalle() } returns flowOf(emptyList())
        every { cultivoDao.observarPorId(1) } returns flowOf(null)
        coEvery { sectorDao.porId(1) } returns Fabrica.sector(1)
        coEvery { cultivoDao.porId(1) } returns null

        repositorio.observarSectores()
        repositorio.observarSectoresSimples()
        repositorio.observarSector(1)
        repositorio.observarCultivosDeSector(1)
        repositorio.observarCultivos()
        repositorio.observarCultivosDetalle()
        repositorio.observarCultivo(1)
        assertEquals(1L, repositorio.sectorPorId(1)?.id)
        assertEquals(null, repositorio.cultivoPorId(1))
    }
}
