package cl.duoc.huertoescolar.viewmodel

import cl.duoc.huertoescolar.core.Fechas
import cl.duoc.huertoescolar.data.repository.HuertoRepository
import cl.duoc.huertoescolar.data.repository.SesionRepository
import cl.duoc.huertoescolar.data.repository.TareaRepository
import cl.duoc.huertoescolar.domain.CategoriaTarea
import cl.duoc.huertoescolar.domain.EstadoTarea
import cl.duoc.huertoescolar.domain.Operacion
import cl.duoc.huertoescolar.domain.Rol
import cl.duoc.huertoescolar.domain.TipoTarea
import cl.duoc.huertoescolar.domain.validacion.Campos
import cl.duoc.huertoescolar.ui.tareas.FiltroFecha
import cl.duoc.huertoescolar.ui.tareas.TareaFormViewModel
import cl.duoc.huertoescolar.ui.tareas.TareasViewModel
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** Listado de tareas con filtros y formulario para crear o editar tareas. */
class TareasYFormularioViewModelTest {

    @get:Rule
    val reglaCorrutinas = ReglaCorrutinas()

    private val tareas = mockk<TareaRepository>()
    private val huerto = mockk<HuertoRepository>()
    private val sesion = mockk<SesionRepository>()

    // ------------------------------ Listado de tareas ------------------------------
    private fun listado(): TareasViewModel {
        every { tareas.observarTodas() } returns flowOf(
            listOf(
                Fabrica.tareaDetalle(Fabrica.tarea(1, responsableId = 3, estado = EstadoTarea.PENDIENTE, diasHastaFecha = 0)),
                Fabrica.tareaDetalle(Fabrica.tarea(2, responsableId = 3, estado = EstadoTarea.PENDIENTE_VALIDACION, diasHastaFecha = 5)),
                Fabrica.tareaDetalle(Fabrica.tarea(3, responsableId = 4, estado = EstadoTarea.PENDIENTE, diasHastaFecha = -3)),
                Fabrica.tareaDetalle(Fabrica.tarea(4, responsableId = 4, estado = EstadoTarea.RECHAZADA, diasHastaFecha = 20))
            )
        )
        every { sesion.observarEstudiantes() } returns flowOf(
            listOf(Fabrica.usuario(3, Rol.ESTUDIANTE), Fabrica.usuario(4, Rol.ESTUDIANTE))
        )
        return TareasViewModel(tareas, sesion)
    }

    @Test
    fun administradorVeTodasYFiltra() = runTest {
        val vm = listado()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.ui.collect {} }

        vm.establecerUsuario(Fabrica.usuario(1, Rol.ADMINISTRADOR))
        assertTrue(vm.ui.value.esAdministrador)
        assertEquals(4, vm.ui.value.total)
        assertEquals(4, vm.ui.value.tareas.size)
        assertEquals(2, vm.ui.value.estudiantes.size)
        assertEquals(1, vm.ui.value.conteo[CategoriaTarea.ATRASADA])

        vm.filtrarPorCategoria(CategoriaTarea.POR_VALIDAR)
        assertEquals(listOf(2L), vm.ui.value.tareas.map { it.tarea.id })

        vm.limpiarFiltros()
        vm.filtrarPorResponsable(4)
        assertEquals(listOf(3L, 4L), vm.ui.value.tareas.map { it.tarea.id })

        vm.limpiarFiltros()
        vm.filtrarPorFecha(FiltroFecha.HOY)
        assertEquals(listOf(1L), vm.ui.value.tareas.map { it.tarea.id })

        vm.filtrarPorFecha(FiltroFecha.SEMANA)
        assertEquals(listOf(1L, 2L), vm.ui.value.tareas.map { it.tarea.id })

        vm.filtrarPorFecha(FiltroFecha.TODAS)
        assertEquals(4, vm.ui.value.tareas.size)
    }

    @Test
    fun estudianteSoloVeSusTareas() = runTest {
        val vm = listado()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.ui.collect {} }

        vm.establecerUsuario(Fabrica.usuario(3, Rol.ESTUDIANTE))

        assertFalse(vm.ui.value.esAdministrador)
        assertEquals(2, vm.ui.value.total)
        assertTrue(vm.ui.value.tareas.all { it.tarea.responsableId == 3L })
    }

    @Test
    fun sinUsuarioNoSeMuestranTareasNiSeElimina() = runTest {
        val vm = listado()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.ui.collect {} }

        assertEquals(0, vm.ui.value.total)
        vm.eliminarTarea(1)
        coVerify(exactly = 0) { tareas.eliminarTarea(any(), any()) }
    }

    @Test
    fun categoriaInicialSeAplicaUnaSolaVez() = runTest {
        val vm = listado()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.ui.collect {} }
        vm.establecerUsuario(Fabrica.usuario(1, Rol.ADMINISTRADOR))

        vm.aplicarCategoriaInicial(CategoriaTarea.RECHAZADA)
        assertEquals(CategoriaTarea.RECHAZADA, vm.ui.value.filtros.categoria)
        assertEquals(listOf(4L), vm.ui.value.tareas.map { it.tarea.id })

        vm.filtrarPorCategoria(null)
        vm.aplicarCategoriaInicial(CategoriaTarea.PENDIENTE)
        assertNull(vm.ui.value.filtros.categoria)
    }

    @Test
    fun eliminarTareaMuestraElResultado() = runTest {
        coEvery { tareas.eliminarTarea(1, 1) } returns Operacion.Exito("Tarea eliminada")
        coEvery { tareas.eliminarTarea(2, 1) } returns Operacion.Error("No se elimina una tarea validada")
        val vm = listado()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.ui.collect {} }
        vm.establecerUsuario(Fabrica.usuario(1, Rol.ADMINISTRADOR))

        vm.eliminarTarea(1)
        assertEquals("Tarea eliminada", vm.ui.value.mensaje)
        vm.eliminarTarea(2)
        assertEquals("No se elimina una tarea validada", vm.ui.value.mensaje)
        vm.limpiarMensaje()
        assertNull(vm.ui.value.mensaje)
    }

    // ------------------------------ Formulario de tarea ------------------------------
    private fun formulario(): TareaFormViewModel {
        every { huerto.observarSectoresSimples() } returns flowOf(listOf(Fabrica.sector(1)))
        every { huerto.observarCultivos() } returns flowOf(listOf(Fabrica.cultivo(1)))
        every { sesion.observarEstudiantes() } returns flowOf(listOf(Fabrica.usuario(3, Rol.ESTUDIANTE)))
        return TareaFormViewModel(tareas, huerto, sesion)
    }

    @Test
    fun formularioVacioMuestraTodosLosErrores() = runTest {
        val vm = formulario()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.sectores.collect {} }
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.cultivos.collect {} }
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.estudiantes.collect {} }
        assertEquals(1, vm.sectores.value.size)
        assertEquals(1, vm.cultivos.value.size)
        assertEquals(1, vm.estudiantes.value.size)

        vm.guardar(usuarioId = 1)

        val errores = vm.ui.value.errores
        assertEquals("Revisa los campos marcados", vm.ui.value.mensaje)
        assertTrue(
            errores.keys.containsAll(
                listOf(Campos.DESCRIPCION, Campos.INSTRUCCIONES, Campos.TIPO, Campos.SECTOR, Campos.RESPONSABLE, Campos.FECHA_PROGRAMADA)
            )
        )
        coVerify(exactly = 0) { tareas.guardarTarea(any(), any(), any(), any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun corregirCamposQuitaLosErroresYTareaNuevaNoVaAlPasado() = runTest {
        val vm = formulario()
        vm.guardar(usuarioId = 1)

        vm.cambiarDescripcion("Riego")
        vm.cambiarInstrucciones("Usar regadera")
        vm.cambiarTipo(TipoTarea.RIEGO)
        vm.cambiarResponsable(3)
        vm.cambiarFecha(Fechas.formatear(Fechas.sumarDias(Fechas.hoy(), -1)))
        vm.cambiarSector(1)
        assertTrue(vm.ui.value.errores.isEmpty())

        vm.guardar(usuarioId = 1)
        assertEquals("Una tarea nueva no puede programarse en el pasado", vm.ui.value.errores[Campos.FECHA_PROGRAMADA])
    }

    @Test
    fun cambiarSectorLimpiaElCultivo() = runTest {
        val vm = formulario()

        vm.cambiarCultivo(1)
        assertEquals(1L, vm.ui.value.cultivoId)
        vm.cambiarSector(2)
        assertNull(vm.ui.value.cultivoId)
    }

    @Test
    fun crearTareaEnviaLosDatos() = runTest {
        val manana = Fechas.sumarDias(Fechas.hoy(), 1)
        coEvery {
            tareas.guardarTarea(0, TipoTarea.RIEGO, "Regar el sector", "Usar regadera", 1, 1, 3, manana, 1)
        } returns Operacion.Exito("Tarea creada y asignada")
        val vm = formulario()

        vm.cambiarTipo(TipoTarea.RIEGO)
        vm.cambiarDescripcion("Regar el sector")
        vm.cambiarInstrucciones("Usar regadera")
        vm.cambiarSector(1)
        vm.cambiarCultivo(1)
        vm.cambiarResponsable(3)
        vm.cambiarFecha(Fechas.formatear(manana))
        vm.guardar(usuarioId = 1)

        assertTrue(vm.ui.value.guardado)
        assertFalse(vm.ui.value.guardando)
        assertEquals("Tarea creada y asignada", vm.ui.value.mensaje)
        vm.limpiarMensaje()
        assertNull(vm.ui.value.mensaje)
    }

    @Test
    fun editarTareaPermiteFechaPasadaYMuestraErrores() = runTest {
        val tarea = Fabrica.tarea(7, diasHastaFecha = -5)
        coEvery { tareas.porId(7) } returns tarea
        coEvery { tareas.porId(9) } returns null
        coEvery { tareas.guardarTarea(7, any(), any(), any(), any(), any(), any(), any(), 1) } returns
                Operacion.Error("Solo un administrador puede crear o editar tareas")
        val vm = formulario()

        vm.cargar(9)
        assertFalse(vm.ui.value.esEdicion)

        vm.cargar(7)
        assertTrue(vm.ui.value.esEdicion)
        assertEquals("Tarea 7", vm.ui.value.descripcion)
        assertEquals(TipoTarea.SIEMBRA, vm.ui.value.tipo)
        assertEquals(Fechas.formatear(tarea.fechaProgramada), vm.ui.value.fechaProgramada)
        vm.cargar(7)
        vm.cargar(0)
        coVerify(exactly = 1) { tareas.porId(7) }

        vm.guardar(usuarioId = 1)
        assertEquals("Solo un administrador puede crear o editar tareas", vm.ui.value.mensaje)
        assertFalse(vm.ui.value.guardado)
    }
}
