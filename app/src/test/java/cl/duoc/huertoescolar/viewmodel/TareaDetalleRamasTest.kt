package cl.duoc.huertoescolar.viewmodel

import cl.duoc.huertoescolar.data.repository.TareaRepository
import cl.duoc.huertoescolar.domain.EstadoTarea
import cl.duoc.huertoescolar.domain.Operacion
import cl.duoc.huertoescolar.domain.ResultadoValidacion
import cl.duoc.huertoescolar.ui.tareas.TareaDetalleViewModel
import cl.duoc.huertoescolar.util.Fabrica
import cl.duoc.huertoescolar.util.ReglaCorrutinas
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

/** Casos del detalle de tarea en que el servidor rechaza la accion o no hay tarea elegida. */
class TareaDetalleRamasTest {

    @get:Rule
    val reglaCorrutinas = ReglaCorrutinas()

    private val repositorio = mockk<TareaRepository>()

    private fun viewModel(): TareaDetalleViewModel {
        val tarea = Fabrica.tarea(1, responsableId = 3, estado = EstadoTarea.EN_DESARROLLO)
        every { repositorio.observarDetalle(1) } returns flowOf(Fabrica.tareaDetalle(tarea))
        every { repositorio.observarEvidencias(1) } returns flowOf(emptyList())
        every { repositorio.observarValidacionesDeTarea(1) } returns flowOf(emptyList())
        return TareaDetalleViewModel(repositorio)
    }

    @Test
    fun sinTareaElegidaNoSeConsultaNada() = runTest {
        val vm = viewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.ui.collect {} }

        vm.establecerTarea(0)

        assertFalse(vm.ui.value.cargando)
        assertNull(vm.ui.value.detalle)
    }

    @Test
    fun entregaRechazadaPorElServidorMantieneLoEscrito() = runTest {
        coEvery { repositorio.entregarTarea(1, 3, "Regué las camas", "/foto.jpg") } returns
                Operacion.Error("Solo el estudiante responsable puede entregar esta tarea")
        val vm = viewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.ui.collect {} }
        vm.establecerTarea(1)

        vm.cambiarComentario("Regué las camas")
        vm.cambiarEvidencia("/foto.jpg")
        vm.entregar(usuarioId = 3)

        assertEquals("Solo el estudiante responsable puede entregar esta tarea", vm.ui.value.mensaje)
        assertEquals("Regué las camas", vm.ui.value.comentario)
        assertEquals("/foto.jpg", vm.ui.value.rutaEvidencia)
        assertFalse(vm.ui.value.procesando)
    }

    @Test
    fun rechazoConErrorDelServidorMantieneElMotivo() = runTest {
        coEvery { repositorio.revisarTarea(1, 1, ResultadoValidacion.RECHAZADA, "Falta la foto") } returns
                Operacion.Error("Solo se revisan actividades pendientes de validación")
        val vm = viewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.ui.collect {} }
        vm.establecerTarea(1)

        vm.cambiarMotivo("Falta la foto")
        vm.rechazar(validadorId = 1)

        assertEquals("Solo se revisan actividades pendientes de validación", vm.ui.value.mensaje)
        assertEquals("Falta la foto", vm.ui.value.motivoRechazo)

        vm.avisar("No se pudo abrir la cámara")
        assertEquals("No se pudo abrir la cámara", vm.ui.value.mensaje)
    }
}
