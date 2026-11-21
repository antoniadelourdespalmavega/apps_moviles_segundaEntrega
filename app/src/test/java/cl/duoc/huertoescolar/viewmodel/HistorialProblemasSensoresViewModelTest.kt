package cl.duoc.huertoescolar.viewmodel

import cl.duoc.huertoescolar.core.Fechas
import cl.duoc.huertoescolar.data.local.entity.Evidencia
import cl.duoc.huertoescolar.data.local.entity.LecturaSensor
import cl.duoc.huertoescolar.data.local.entity.Validacion
import cl.duoc.huertoescolar.data.local.query.EvidenciaDetalle
import cl.duoc.huertoescolar.data.local.query.ValidacionDetalle
import cl.duoc.huertoescolar.data.repository.HuertoRepository
import cl.duoc.huertoescolar.data.repository.ReporteRepository
import cl.duoc.huertoescolar.data.repository.SensorRepository
import cl.duoc.huertoescolar.data.repository.TareaRepository
import cl.duoc.huertoescolar.domain.EstadoLectura
import cl.duoc.huertoescolar.domain.EstadoReporte
import cl.duoc.huertoescolar.domain.Operacion
import cl.duoc.huertoescolar.domain.ResultadoValidacion
import cl.duoc.huertoescolar.domain.Rol
import cl.duoc.huertoescolar.domain.TipoProblema
import cl.duoc.huertoescolar.domain.validacion.Campos
import cl.duoc.huertoescolar.ui.historial.HistorialViewModel
import cl.duoc.huertoescolar.ui.historial.PeriodoHistorial
import cl.duoc.huertoescolar.ui.historial.TipoRegistro
import cl.duoc.huertoescolar.ui.problemas.ProblemaFormViewModel
import cl.duoc.huertoescolar.ui.problemas.ProblemasViewModel
import cl.duoc.huertoescolar.ui.sensores.SensoresViewModel
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

/** Historial con filtros, reportes de problemas y lecturas simuladas de sensores. */
class HistorialProblemasSensoresViewModelTest {

    @get:Rule
    val reglaCorrutinas = ReglaCorrutinas()

    private val tareas = mockk<TareaRepository>()
    private val reportes = mockk<ReporteRepository>()
    private val huerto = mockk<HuertoRepository>()
    private val sensores = mockk<SensorRepository>()

    // ------------------------------ Historial ------------------------------
    private fun historial(): HistorialViewModel {
        every { tareas.observarTodas() } returns flowOf(listOf(Fabrica.tareaDetalle(Fabrica.tarea(1, responsableId = 3))))
        every { tareas.observarTodasLasEvidencias() } returns flowOf(
            listOf(EvidenciaDetalle(Evidencia(5, 1, 3, "/f.jpg", "Regué las camas", Fechas.ahora()), "Camila Soto"))
        )
        every { tareas.observarValidaciones() } returns flowOf(
            listOf(
                ValidacionDetalle(
                    Validacion(7, 1, 1, ResultadoValidacion.VALIDADA, "Bien hecho", Fechas.sumarDias(Fechas.hoy(), -3)),
                    validadorNombre = "Ana Rojas", tareaDescripcion = "Tarea 1", tareaResponsableId = 3
                )
            )
        )
        every { reportes.observarTodos() } returns flowOf(
            listOf(Fabrica.reporteDetalle(1, reportadoPorId = 3), Fabrica.reporteDetalle(2, reportadoPorId = 4))
        )
        return HistorialViewModel(tareas, reportes)
    }

    @Test
    fun administradorVeTodoOrdenadoDelMasNuevoAlMasAntiguo() = runTest {
        val vm = historial()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.ui.collect {} }

        vm.establecerUsuario(Fabrica.usuario(1, Rol.ADMINISTRADOR))
        val estado = vm.ui.value

        assertTrue(estado.esAdministrador)
        assertEquals(5, estado.total)
        assertEquals("evidencia-5", estado.registros[0].id)
        assertEquals("validacion-7", estado.registros[1].id)
        assertEquals("Validada: Tarea 1", estado.registros[1].titulo)
        assertEquals(listOf("Ana Rojas", "Camila Soto"), estado.autores)
        assertTrue(estado.registros.any { it.tipo == TipoRegistro.TAREA && it.titulo == "Siembra: Tarea 1" })
        assertTrue(estado.registros.any { it.tipo == TipoRegistro.REPORTE && it.titulo == "Falta de agua en Sector 1" })
        assertFalse(estado.filtros.activos)
    }

    @Test
    fun filtrosPorTipoPeriodoYAutor() = runTest {
        val vm = historial()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.ui.collect {} }
        vm.establecerUsuario(Fabrica.usuario(1, Rol.ADMINISTRADOR))

        vm.filtrarPorTipo(TipoRegistro.EVIDENCIA)
        assertEquals(listOf("evidencia-5"), vm.ui.value.registros.map { it.id })
        assertTrue(vm.ui.value.filtros.activos)

        vm.limpiarFiltros()
        vm.filtrarPorPeriodo(PeriodoHistorial.HOY)
        assertEquals(listOf("evidencia-5"), vm.ui.value.registros.map { it.id })

        vm.filtrarPorPeriodo(PeriodoHistorial.SEMANA)
        assertEquals(listOf("evidencia-5", "validacion-7"), vm.ui.value.registros.map { it.id })

        vm.limpiarFiltros()
        vm.filtrarPorAutor("Ana Rojas")
        assertEquals(setOf("tarea-1", "validacion-7"), vm.ui.value.registros.map { it.id }.toSet())

        vm.filtrarPorAutor(null)
        vm.filtrarPorPeriodo(PeriodoHistorial.TODO)
        assertEquals(5, vm.ui.value.registros.size)
        assertFalse(vm.ui.value.filtros.activos)
    }

    @Test
    fun estudianteSoloVeLoSuyo() = runTest {
        val vm = historial()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.ui.collect {} }

        vm.establecerUsuario(Fabrica.usuario(3, Rol.ESTUDIANTE))
        assertFalse(vm.ui.value.esAdministrador)
        assertEquals(4, vm.ui.value.total)

        vm.establecerUsuario(Fabrica.usuario(4, Rol.ESTUDIANTE))
        assertEquals(listOf("reporte-2"), vm.ui.value.registros.map { it.id })
    }

    // ------------------------------ Problemas ------------------------------
    private fun problemas(): ProblemasViewModel {
        every { reportes.observarTodos() } returns flowOf(
            listOf(Fabrica.reporteDetalle(1, reportadoPorId = 3), Fabrica.reporteDetalle(2, reportadoPorId = 4))
        )
        return ProblemasViewModel(reportes)
    }

    @Test
    fun administradorVeTodosLosReportesYCambiaSuEstado() = runTest {
        coEvery { reportes.actualizarEstado(1, 1, EstadoReporte.ATENDIDO, "Regado") } returns
                Operacion.Exito("Reporte actualizado a Atendido")
        coEvery { reportes.actualizarEstado(2, 1, EstadoReporte.CERRADO, "") } returns
                Operacion.Error("Sin conexión con el servidor")
        val vm = problemas()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.ui.collect {} }

        vm.establecerUsuario(Fabrica.usuario(1, Rol.ADMINISTRADOR))
        assertTrue(vm.ui.value.esAdministrador)
        assertEquals(2, vm.ui.value.reportes.size)

        vm.cambiarEstado(1, EstadoReporte.ATENDIDO, "Regado")
        assertEquals("Reporte actualizado a Atendido", vm.ui.value.mensaje)
        vm.cambiarEstado(2, EstadoReporte.CERRADO, "")
        assertEquals("Sin conexión con el servidor", vm.ui.value.mensaje)
        vm.limpiarMensaje()
        assertNull(vm.ui.value.mensaje)
    }

    @Test
    fun estudianteSoloVeSusReportesYSinUsuarioNoSeCambiaNada() = runTest {
        val vm = problemas()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.ui.collect {} }

        assertTrue(vm.ui.value.reportes.isEmpty())
        vm.cambiarEstado(1, EstadoReporte.CERRADO, "")
        coVerify(exactly = 0) { reportes.actualizarEstado(any(), any(), any(), any()) }

        vm.establecerUsuario(Fabrica.usuario(3, Rol.ESTUDIANTE))
        assertFalse(vm.ui.value.esAdministrador)
        assertEquals(listOf(1L), vm.ui.value.reportes.map { it.reporte.id })
    }

    private fun formularioProblema(): ProblemaFormViewModel {
        every { huerto.observarSectoresSimples() } returns flowOf(listOf(Fabrica.sector(1)))
        every { huerto.observarCultivos() } returns flowOf(listOf(Fabrica.cultivo(1)))
        return ProblemaFormViewModel(reportes, huerto)
    }

    @Test
    fun reporteIncompletoMuestraErrores() = runTest {
        val vm = formularioProblema()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.sectores.collect {} }
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.cultivos.collect {} }
        assertEquals(1, vm.sectores.value.size)
        assertEquals(1, vm.cultivos.value.size)

        vm.registrar(usuarioId = 3)
        assertEquals("Revisa los campos marcados", vm.ui.value.mensaje)
        assertTrue(vm.ui.value.errores.keys.containsAll(listOf(Campos.SECTOR, Campos.TIPO, Campos.DESCRIPCION)))

        vm.cambiarDescripcion("corta")
        assertEquals("La descripción del problema debe tener al menos 10 caracteres", vm.ui.value.errores[Campos.DESCRIPCION])
        vm.cambiarDescripcion("Hay pulgones en las hojas")
        vm.cambiarSector(1)
        vm.cambiarTipo(TipoProblema.PLAGA_SIMULADA)
        assertTrue(vm.ui.value.errores.isEmpty())
        coVerify(exactly = 0) { reportes.registrar(any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun registrarReporteCompleto() = runTest {
        coEvery { reportes.registrar(1, 1, TipoProblema.FALTA_DE_AGUA, "La cama está muy seca", 3, "/f.jpg") } returns
                Operacion.Exito("Problema reportado correctamente")
        val vm = formularioProblema()

        vm.cambiarSector(1)
        vm.cambiarCultivo(1)
        vm.cambiarTipo(TipoProblema.FALTA_DE_AGUA)
        vm.cambiarDescripcion("La cama está muy seca")
        vm.cambiarFoto("/f.jpg")
        vm.registrar(usuarioId = 3)

        assertTrue(vm.ui.value.guardado)
        assertFalse(vm.ui.value.guardando)
        assertEquals("Problema reportado correctamente", vm.ui.value.mensaje)
    }

    @Test
    fun registrarReporteConErrorYAvisos() = runTest {
        coEvery { reportes.registrar(any(), any(), any(), any(), 1, any()) } returns
                Operacion.Error("Los problemas los reporta el estudiante; el docente o tallerista los soluciona")
        val vm = formularioProblema()

        vm.cambiarCultivo(1)
        vm.cambiarSector(1)
        assertNull(vm.ui.value.cultivoId)
        vm.cambiarTipo(TipoProblema.OTRO)
        vm.cambiarDescripcion("Se rompió la manguera")
        vm.registrar(usuarioId = 1)

        assertFalse(vm.ui.value.guardado)
        assertEquals("Los problemas los reporta el estudiante; el docente o tallerista los soluciona", vm.ui.value.mensaje)

        vm.avisar("No se pudo abrir la galería")
        assertEquals("No se pudo abrir la galería", vm.ui.value.mensaje)
        vm.limpiarMensaje()
        assertNull(vm.ui.value.mensaje)
    }

    // ------------------------------ Sensores ------------------------------
    @Test
    fun sensoresSimulaFaltantesYMuestraLecturasConSuSector() = runTest {
        coEvery { sensores.simularLecturasFaltantes() } returns Operacion.Exito("Lecturas simuladas")
        every { sensores.observarLecturas() } returns flowOf(
            listOf(
                LecturaSensor(1, 1, 5L, 40, 20.5, 50, estado = EstadoLectura.NORMAL),
                LecturaSensor(2, 9, 6L, 25, 18.0, 45, estado = EstadoLectura.BAJO)
            )
        )
        every { huerto.observarSectoresSimples() } returns flowOf(listOf(Fabrica.sector(1)))
        every { huerto.observarCultivos() } returns flowOf(listOf(Fabrica.cultivo(1, sectorId = 1)))
        val vm = SensoresViewModel(sensores, huerto)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.ui.collect {} }

        coVerify(exactly = 1) { sensores.simularLecturasFaltantes() }
        val lecturas = vm.ui.value.lecturas
        assertEquals(2, lecturas.size)
        assertEquals("Sector 1", lecturas[0].sectorNombre)
        assertEquals(listOf("Lechuga"), lecturas[0].cultivos)
        assertEquals("Sector eliminado", lecturas[1].sectorNombre)
        assertTrue(lecturas[1].cultivos.isEmpty())
        assertEquals("Simulación académica", lecturas[0].lectura.origen)

        vm.filtrarPorSector(9)
        assertEquals(9L, vm.ui.value.filtroSectorId)
        assertEquals(listOf(2L), vm.ui.value.lecturas.map { it.lectura.id })

        vm.filtrarPorSector(null)
        assertEquals(2, vm.ui.value.lecturas.size)
    }
}
