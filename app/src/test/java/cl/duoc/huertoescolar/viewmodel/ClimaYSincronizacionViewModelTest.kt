package cl.duoc.huertoescolar.viewmodel

import cl.duoc.huertoescolar.data.repository.ClimaRepository
import cl.duoc.huertoescolar.data.repository.SincronizacionRepository
import cl.duoc.huertoescolar.domain.Operacion
import cl.duoc.huertoescolar.domain.Resultado
import cl.duoc.huertoescolar.domain.clima.Clima
import cl.duoc.huertoescolar.domain.clima.ClimaActual
import cl.duoc.huertoescolar.domain.clima.CondicionClima
import cl.duoc.huertoescolar.ui.clima.ClimaUiState
import cl.duoc.huertoescolar.ui.clima.ClimaViewModel
import cl.duoc.huertoescolar.ui.sincronizacion.SincronizacionViewModel
import cl.duoc.huertoescolar.util.ReglaCorrutinas
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ClimaYSincronizacionViewModelTest {

    @get:Rule
    val reglaCorrutinas = ReglaCorrutinas()

    private val climaDePrueba = Clima(ClimaActual(18.0, 30, 0.0, 4.0, CondicionClima.DESPEJADO), emptyList())

    // ------------------------------ Clima ------------------------------
    @Test
    fun climaCorrectoIncluyeLaRecomendacion() {
        val repositorio = mockk<ClimaRepository>()
        coEvery { repositorio.obtenerClima() } returns Resultado.Exito(climaDePrueba)

        val estado = ClimaViewModel(repositorio).ui.value

        assertTrue(estado is ClimaUiState.Exito)
        assertEquals("Ambiente seco: conviene regar", (estado as ClimaUiState.Exito).recomendacion.titulo)
    }

    @Test
    fun climaConErrorYReintento() {
        val repositorio = mockk<ClimaRepository>()
        coEvery { repositorio.obtenerClima() } returns Resultado.Error(ClimaRepository.SIN_INTERNET)
        val vm = ClimaViewModel(repositorio)
        assertEquals(ClimaUiState.Error(ClimaRepository.SIN_INTERNET), vm.ui.value)

        coEvery { repositorio.obtenerClima() } returns Resultado.Exito(climaDePrueba)
        vm.cargar()

        assertTrue(vm.ui.value is ClimaUiState.Exito)
        coVerify(exactly = 2) { repositorio.obtenerClima() }
    }

    // ------------------------------ Sincronizacion ------------------------------
    @Test
    fun sincronizacionCorrectaMarcaConectado() {
        val repositorio = mockk<SincronizacionRepository>()
        coEvery { repositorio.sincronizarTodo() } returns Operacion.Exito("ok")
        val vm = SincronizacionViewModel(repositorio)

        vm.sincronizar()

        assertTrue(vm.ui.value.conectado)
        assertNotNull(vm.ui.value.ultimaSincronizacion)
    }

    @Test
    fun sinConexionSeMuestraElAviso() {
        val repositorio = mockk<SincronizacionRepository>()
        coEvery { repositorio.sincronizarTodo() } returns Operacion.Error("No se pudo conectar con el servidor")
        val vm = SincronizacionViewModel(repositorio)

        vm.sincronizar()

        assertFalse(vm.ui.value.conectado)
        assertEquals("No se pudo conectar con el servidor", vm.ui.value.mensaje)
    }

    @Test
    fun noSeLanzanDosSincronizacionesALaVez() {
        val repositorio = mockk<SincronizacionRepository>()
        val espera = CompletableDeferred<Operacion>()
        coEvery { repositorio.sincronizarTodo() } coAnswers { espera.await() }
        val vm = SincronizacionViewModel(repositorio)

        vm.sincronizar()
        vm.sincronizar()
        assertTrue(vm.ui.value.sincronizando)
        espera.complete(Operacion.Exito("ok"))

        assertFalse(vm.ui.value.sincronizando)
        coVerify(exactly = 1) { repositorio.sincronizarTodo() }
    }
}
