package cl.duoc.huertoescolar.repository

import cl.duoc.huertoescolar.data.local.dao.ReporteDao
import cl.duoc.huertoescolar.data.local.dao.SensorDao
import cl.duoc.huertoescolar.data.local.dao.UsuarioDao
import cl.duoc.huertoescolar.data.remote.api.ActividadesApi
import cl.duoc.huertoescolar.data.remote.api.ClimaApi
import cl.duoc.huertoescolar.data.remote.api.HuertoApi
import cl.duoc.huertoescolar.data.remote.dto.ClimaActualDto
import cl.duoc.huertoescolar.data.remote.dto.ClimaDto
import cl.duoc.huertoescolar.data.repository.ClimaRepository
import cl.duoc.huertoescolar.data.repository.ReporteRepository
import cl.duoc.huertoescolar.data.repository.SensorRepository
import cl.duoc.huertoescolar.data.repository.SincronizacionRepository
import cl.duoc.huertoescolar.domain.EstadoReporte
import cl.duoc.huertoescolar.domain.Operacion
import cl.duoc.huertoescolar.domain.Resultado
import cl.duoc.huertoescolar.domain.Rol
import cl.duoc.huertoescolar.domain.TipoProblema
import cl.duoc.huertoescolar.util.Fabrica
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

/** Reportes de problemas, lecturas de sensores y API externa de clima. */
class ReporteSensorClimaRepositoryTest {

    private val sincronizacion = mockk<SincronizacionRepository>().also {
        coEvery { it.sincronizarActividades() } returns Operacion.Exito("ok")
        coEvery { it.sincronizarHuerto() } returns Operacion.Exito("ok")
    }

    // ------------------------------ Reportes ------------------------------
    private val actividadesApi = mockk<ActividadesApi>()
    private val reporteDao = mockk<ReporteDao>()
    private val usuarioDao = mockk<UsuarioDao>().also {
        coEvery { it.existeConRol(3, Rol.ESTUDIANTE) } returns 1
        coEvery { it.existeConRol(3, Rol.ADMINISTRADOR) } returns 0
        coEvery { it.existeConRol(1, Rol.ESTUDIANTE) } returns 0
        coEvery { it.existeConRol(1, Rol.ADMINISTRADOR) } returns 1
    }
    private val reportes = ReporteRepository(actividadesApi, reporteDao, usuarioDao, sincronizacion)

    @Test
    fun soloElEstudianteReportaProblemas() = runTest {
        coEvery { actividadesApi.registrarReporte(any()) } returns Fabrica.reporteDto(1)

        assertEquals(
            Operacion.Error("Los problemas los reporta el estudiante; el docente o tallerista los soluciona"),
            reportes.registrar(1, null, TipoProblema.FALTA_DE_AGUA, "Tierra muy seca", 1, null)
        )
        assertEquals(
            Operacion.Exito("Problema reportado correctamente"),
            reportes.registrar(1, null, TipoProblema.FALTA_DE_AGUA, " Tierra muy seca ", 3, null)
        )
        coVerify { actividadesApi.registrarReporte(match { it.tipo == "FALTA_DE_AGUA" && it.descripcion == "Tierra muy seca" }) }
    }

    @Test
    fun soloElAdministradorCambiaElEstadoDelReporte() = runTest {
        coEvery { actividadesApi.actualizarEstadoReporte(1, any()) } returns Fabrica.reporteDto(1)

        assertEquals(
            Operacion.Error("Solo un docente o tallerista puede cambiar el estado del reporte"),
            reportes.actualizarEstado(1, 3, EstadoReporte.ATENDIDO, "")
        )
        assertEquals(
            Operacion.Exito("Reporte actualizado a Atendido"),
            reportes.actualizarEstado(1, 1, EstadoReporte.ATENDIDO, "Se regó el sector")
        )
    }

    @Test
    fun reportesLocalesSeDelegaAlDao() {
        every { reporteDao.observarTodos() } returns flowOf(emptyList())
        every { reporteDao.observarPorUsuario(3) } returns flowOf(emptyList())
        reportes.observarTodos()
        reportes.observarDeUsuario(3)
    }

    // ------------------------------ Sensores ------------------------------
    private val huertoApi = mockk<HuertoApi>()
    private val sensorDao = mockk<SensorDao>()
    private val sensores = SensorRepository(huertoApi, sensorDao, sincronizacion)

    @Test
    fun simularLecturasPideAlMicroservicioYSincroniza() = runTest {
        coEvery { huertoApi.simularLecturas() } returns listOf(Fabrica.lecturaDto(9))
        every { sensorDao.observarTodas() } returns flowOf(emptyList())

        assertEquals(Operacion.Exito("Lecturas simuladas: 1"), sensores.simularLecturasFaltantes())
        coVerify { sincronizacion.sincronizarHuerto() }
        sensores.observarLecturas()
    }

    @Test
    fun simularLecturasSinConexion() = runTest {
        coEvery { huertoApi.simularLecturas() } throws IOException("sin red")
        assertTrue(sensores.simularLecturasFaltantes() is Operacion.Error)
    }

    // ------------------------------ Clima (API externa) ------------------------------
    private val climaApi = mockk<ClimaApi>()
    private val clima = ClimaRepository(climaApi)

    private fun pronostico(dto: ClimaDto) {
        coEvery { climaApi.pronostico(ClimaRepository.LATITUD, ClimaRepository.LONGITUD, any(), any(), any(), any()) } returns dto
    }

    @Test
    fun climaCorrecto() = runTest {
        pronostico(ClimaDto(ClimaActualDto("hoy", 19.0, 50, 0.0, 0, 3.0), null))

        val resultado = clima.obtenerClima()

        assertTrue(resultado is Resultado.Exito)
        assertEquals(19.0, (resultado as Resultado.Exito).dato.actual.temperatura, 0.001)
    }

    @Test
    fun climaSinDatos() = runTest {
        pronostico(ClimaDto(null, null))
        assertEquals(Resultado.Error("La API de clima no entregó datos"), clima.obtenerClima())
    }

    @Test
    fun climaSinInternet() = runTest {
        coEvery { climaApi.pronostico(any(), any(), any(), any(), any(), any()) } throws IOException("sin red")
        assertEquals(Resultado.Error(ClimaRepository.SIN_INTERNET), clima.obtenerClima())
    }

    @Test
    fun climaConErrorDelServicio() = runTest {
        coEvery { climaApi.pronostico(any(), any(), any(), any(), any(), any()) } throws Fabrica.errorHttp(400, "Parametro invalido")
        assertEquals(Resultado.Error("Parametro invalido"), clima.obtenerClima())
    }
}
