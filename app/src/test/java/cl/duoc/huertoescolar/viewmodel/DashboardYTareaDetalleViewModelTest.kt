package cl.duoc.huertoescolar.viewmodel

import cl.duoc.huertoescolar.data.repository.HuertoRepository
import cl.duoc.huertoescolar.data.repository.ReporteRepository
import cl.duoc.huertoescolar.data.repository.TareaRepository
import cl.duoc.huertoescolar.domain.CategoriaTarea
import cl.duoc.huertoescolar.domain.EstadoReporte
import cl.duoc.huertoescolar.domain.EstadoTarea
import cl.duoc.huertoescolar.domain.Operacion
import cl.duoc.huertoescolar.domain.ResultadoValidacion
import cl.duoc.huertoescolar.domain.Rol
import cl.duoc.huertoescolar.domain.validacion.Campos
import cl.duoc.huertoescolar.ui.dashboard.DashboardViewModel
import cl.duoc.huertoescolar.ui.tareas.TareaDetalleViewModel
import cl.duoc.huertoescolar.util.Fabrica
import cl.duoc.huertoescolar.util.ReglaCorrutinas
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class DashboardYTareaDetalleViewModelTest {

    @get:Rule
    val reglaCorrutinas = ReglaCorrutinas()

    private val tareaRepository = mockk<TareaRepository>()
    private val huertoRepository = mockk<HuertoRepository>()
    private val reporteRepository = mockk<ReporteRepository>()

    // ------------------------------ Panel principal ------------------------------
    private fun prepararPanel() {
        every { tareaRepository.observarTodas() } returns flowOf(
            listOf(
                Fabrica.tareaDetalle(Fabrica.tarea(1, responsableId = 3, estado = EstadoTarea.PENDIENTE)),
                Fabrica.tareaDetalle(Fabrica.tarea(2, responsableId = 3, estado = EstadoTarea.PENDIENTE_VALIDACION)),
                Fabrica.tareaDetalle(Fabrica.tarea(3, responsableId = 4, estado = EstadoTarea.PENDIENTE, diasHastaFecha = -2)),
                Fabrica.tareaDetalle(Fabrica.tarea(4, responsableId = 4, estado = EstadoTarea.VALIDADA))
            )
        )
        every { huertoRepository.observarCultivos() } returns flowOf(listOf(Fabrica.cultivo(1), Fabrica.cultivo(2, cosechaEnDias = 5)))
        every { huertoRepository.observarSectoresSimples() } returns flowOf(listOf(Fabrica.sector(1)))
        every { reporteRepository.observarTodos() } returns flowOf(
            listOf(
                Fabrica.reporteDetalle(1, reportadoPorId = 3, estado = EstadoReporte.INFORMADO),
                Fabrica.reporteDetalle(2, reportadoPorId = 4, estado = EstadoReporte.CERRADO)
            )
        )
    }

    @Test
    fun elAdministradorVeTodasLasTareas() = runTest {
        prepararPanel()
        val vm = DashboardViewModel(tareaRepository, huertoRepository, reporteRepository)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.ui.collect {} }

        vm.establecerUsuario(Fabrica.usuario(1, Rol.ADMINISTRADOR))
        val estado = vm.ui.value

        assertEquals(1, estado.conteo[CategoriaTarea.PENDIENTE])
        assertEquals(1, estado.conteo[CategoriaTarea.ATRASADA])
        assertEquals(1, estado.conteo[CategoriaTarea.POR_VALIDAR])
        assertEquals(1, estado.conteo[CategoriaTarea.VALIDADA])
        assertEquals(1, estado.problemasAbiertos)
        assertEquals(2, estado.totalCultivos)
        assertTrue(estado.alertas.isEmpty())
        assertEquals(3, estado.tareasDelUsuario.size)
    }

    @Test
    fun elEstudianteSoloVeSusTareasYRecibeAvisos() = runTest {
        prepararPanel()
        val vm = DashboardViewModel(tareaRepository, huertoRepository, reporteRepository)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.ui.collect {} }

        vm.establecerUsuario(Fabrica.usuario(3, Rol.ESTUDIANTE))
        val estado = vm.ui.value

        assertEquals(2, estado.tareasDelUsuario.size)
        assertEquals(0, estado.conteo[CategoriaTarea.ATRASADA])
        assertEquals(1, estado.problemasAbiertos)
        assertTrue(estado.alertas.isNotEmpty())
    }

    // ------------------------------ Detalle de una tarea ------------------------------
    private fun detalleViewModel(): TareaDetalleViewModel {
        val tarea = Fabrica.tarea(1, responsableId = 3, estado = EstadoTarea.EN_DESARROLLO)
        every { tareaRepository.observarDetalle(1) } returns flowOf(Fabrica.tareaDetalle(tarea))
        every { tareaRepository.observarEvidencias(1) } returns flowOf(emptyList())
        every { tareaRepository.observarValidacionesDeTarea(1) } returns flowOf(emptyList())
        return TareaDetalleViewModel(tareaRepository).also { it.establecerTarea(1) }
    }

    @Test
    fun entregaSinComentarioNiFotoMuestraErrores() = runTest {
        val vm = detalleViewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.ui.collect {} }

        vm.entregar(usuarioId = 3)

        assertTrue(vm.ui.value.errores.containsKey(Campos.COMENTARIO))
        assertTrue(vm.ui.value.errores.containsKey(Campos.EVIDENCIA))
        coVerify(exactly = 0) { tareaRepository.entregarTarea(any(), any(), any(), any()) }
    }

    @Test
    fun entregaCompletaSeEnviaYLimpiaElFormulario() = runTest {
        coEvery { tareaRepository.entregarTarea(1, 3, "Regué las camas", "/foto.jpg") } returns
                Operacion.Exito("Actividad entregada. Queda pendiente de validación")
        val vm = detalleViewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.ui.collect {} }

        vm.cambiarComentario("Regué las camas")
        vm.cambiarEvidencia("/foto.jpg")
        vm.entregar(usuarioId = 3)

        assertEquals("Actividad entregada. Queda pendiente de validación", vm.ui.value.mensaje)
        assertEquals("", vm.ui.value.comentario)
        assertEquals("Tarea 1", vm.ui.value.detalle?.tarea?.descripcion)
    }

    @Test
    fun rechazoExigeMotivoYValidarEnviaElResultado() = runTest {
        coEvery { tareaRepository.revisarTarea(1, 1, ResultadoValidacion.VALIDADA, any()) } returns
                Operacion.Exito("Actividad validada")
        coEvery { tareaRepository.revisarTarea(1, 1, ResultadoValidacion.RECHAZADA, any()) } returns
                Operacion.Exito("Actividad rechazada. El estudiante puede corregirla y reenviarla")
        val vm = detalleViewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.ui.collect {} }

        vm.rechazar(validadorId = 1)
        assertTrue(vm.ui.value.errores.containsKey(Campos.MOTIVO))

        vm.cambiarMotivo("Falta la foto del sector")
        vm.rechazar(validadorId = 1)
        assertEquals("Actividad rechazada. El estudiante puede corregirla y reenviarla", vm.ui.value.mensaje)

        vm.validar(validadorId = 1)
        assertEquals("Actividad validada", vm.ui.value.mensaje)
    }

    @Test
    fun iniciarYEliminarTarea() = runTest {
        coEvery { tareaRepository.iniciarTarea(1, 3) } returns Operacion.Exito("Tarea iniciada")
        coEvery { tareaRepository.eliminarTarea(1, 1) } returns Operacion.Error("No se elimina una tarea validada")
        coEvery { tareaRepository.eliminarTarea(1, 2) } returns Operacion.Exito("Tarea eliminada")
        val vm = detalleViewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.ui.collect {} }

        vm.iniciar(usuarioId = 3)
        assertEquals("Tarea iniciada", vm.ui.value.mensaje)

        vm.eliminar(usuarioId = 1) { }
        assertEquals("No se elimina una tarea validada", vm.ui.value.mensaje)

        var aviso = ""
        vm.eliminar(usuarioId = 2) { aviso = it }
        assertEquals("Tarea eliminada", aviso)

        vm.limpiarMensaje()
        assertEquals(null, vm.ui.value.mensaje)
    }
}
