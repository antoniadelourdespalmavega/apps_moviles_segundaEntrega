package cl.duoc.huertoescolar.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.MutableCreationExtras
import cl.duoc.huertoescolar.Contenedor
import cl.duoc.huertoescolar.data.repository.ClimaRepository
import cl.duoc.huertoescolar.data.repository.HuertoRepository
import cl.duoc.huertoescolar.data.repository.ReporteRepository
import cl.duoc.huertoescolar.data.repository.SensorRepository
import cl.duoc.huertoescolar.data.repository.SesionRepository
import cl.duoc.huertoescolar.data.repository.SincronizacionRepository
import cl.duoc.huertoescolar.data.repository.TareaRepository
import cl.duoc.huertoescolar.domain.Operacion
import cl.duoc.huertoescolar.domain.Resultado
import cl.duoc.huertoescolar.ui.clima.ClimaViewModel
import cl.duoc.huertoescolar.ui.cultivos.CultivoFormViewModel
import cl.duoc.huertoescolar.ui.cultivos.CultivosInfoViewModel
import cl.duoc.huertoescolar.ui.dashboard.DashboardViewModel
import cl.duoc.huertoescolar.ui.historial.HistorialViewModel
import cl.duoc.huertoescolar.ui.login.RegistroViewModel
import cl.duoc.huertoescolar.ui.perfil.PerfilViewModel
import cl.duoc.huertoescolar.ui.problemas.ProblemaFormViewModel
import cl.duoc.huertoescolar.ui.problemas.ProblemasViewModel
import cl.duoc.huertoescolar.ui.revision.RevisionViewModel
import cl.duoc.huertoescolar.ui.sectores.SectorDetalleViewModel
import cl.duoc.huertoescolar.ui.sectores.SectorFormViewModel
import cl.duoc.huertoescolar.ui.sectores.SectoresViewModel
import cl.duoc.huertoescolar.ui.sensores.SensoresViewModel
import cl.duoc.huertoescolar.ui.sesion.SesionViewModel
import cl.duoc.huertoescolar.ui.sincronizacion.SincronizacionViewModel
import cl.duoc.huertoescolar.ui.tareas.TareaDetalleViewModel
import cl.duoc.huertoescolar.ui.tareas.TareaFormViewModel
import cl.duoc.huertoescolar.ui.tareas.TareasViewModel
import cl.duoc.huertoescolar.util.ReglaCorrutinas
import io.mockk.coEvery
import io.mockk.mockk
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import kotlin.reflect.KClass

/**
 * Cada pantalla obtiene su ViewModel con la fabrica (Factory) de ese ViewModel,
 * que toma los repositorios desde el Contenedor. Aqui el Contenedor se llena
 * con repositorios simulados y se comprueba que cada fabrica crea el ViewModel
 * correcto.
 */
class FabricasViewModelTest {

    @get:Rule
    val reglaCorrutinas = ReglaCorrutinas()

    @Before
    fun prepararContenedor() {
        val sesion = mockk<SesionRepository>(relaxed = true)
        val sensores = mockk<SensorRepository>(relaxed = true)
        val clima = mockk<ClimaRepository>(relaxed = true)
        // Llamadas que algunos ViewModel hacen apenas se crean
        coEvery { sesion.actualizarCuentas() } returns Operacion.Exito("Cuentas actualizadas")
        coEvery { sensores.simularLecturasFaltantes() } returns Operacion.Exito("Lecturas simuladas")
        coEvery { clima.obtenerClima() } returns Resultado.Error("Sin conexión")

        Contenedor.sesionRepository = sesion
        Contenedor.sensorRepository = sensores
        Contenedor.climaRepository = clima
        Contenedor.huertoRepository = mockk<HuertoRepository>(relaxed = true)
        Contenedor.tareaRepository = mockk<TareaRepository>(relaxed = true)
        Contenedor.reporteRepository = mockk<ReporteRepository>(relaxed = true)
        Contenedor.sincronizacionRepository = mockk<SincronizacionRepository>(relaxed = true)
    }

    private fun <T : ViewModel> crear(fabrica: ViewModelProvider.Factory, clase: KClass<T>): T =
        fabrica.create(clase, MutableCreationExtras())

    @Test
    fun fabricasDeSesionYPanel() {
        assertNotNull(crear(SesionViewModel.Factory, SesionViewModel::class))
        assertNotNull(crear(RegistroViewModel.Factory, RegistroViewModel::class))
        assertNotNull(crear(PerfilViewModel.Factory, PerfilViewModel::class))
        assertNotNull(crear(DashboardViewModel.Factory, DashboardViewModel::class))
        assertNotNull(crear(SincronizacionViewModel.Factory, SincronizacionViewModel::class))
        assertNotNull(crear(ClimaViewModel.Factory, ClimaViewModel::class))
    }

    @Test
    fun fabricasDeSectoresYCultivos() {
        assertNotNull(crear(SectoresViewModel.Factory, SectoresViewModel::class))
        assertNotNull(crear(SectorDetalleViewModel.Factory, SectorDetalleViewModel::class))
        assertNotNull(crear(SectorFormViewModel.Factory, SectorFormViewModel::class))
        assertNotNull(crear(CultivosInfoViewModel.Factory, CultivosInfoViewModel::class))
        assertNotNull(crear(CultivoFormViewModel.Factory, CultivoFormViewModel::class))
        assertNotNull(crear(SensoresViewModel.Factory, SensoresViewModel::class))
    }

    @Test
    fun fabricasDeTareasReportesEHistorial() {
        assertNotNull(crear(TareasViewModel.Factory, TareasViewModel::class))
        assertNotNull(crear(TareaFormViewModel.Factory, TareaFormViewModel::class))
        assertNotNull(crear(TareaDetalleViewModel.Factory, TareaDetalleViewModel::class))
        assertNotNull(crear(RevisionViewModel.Factory, RevisionViewModel::class))
        assertNotNull(crear(ProblemasViewModel.Factory, ProblemasViewModel::class))
        assertNotNull(crear(ProblemaFormViewModel.Factory, ProblemaFormViewModel::class))
        assertNotNull(crear(HistorialViewModel.Factory, HistorialViewModel::class))
    }
}
