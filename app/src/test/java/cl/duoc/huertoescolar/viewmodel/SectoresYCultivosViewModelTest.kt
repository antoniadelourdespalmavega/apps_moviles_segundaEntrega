package cl.duoc.huertoescolar.viewmodel

import cl.duoc.huertoescolar.core.Fechas
import cl.duoc.huertoescolar.data.local.query.SectorResumen
import cl.duoc.huertoescolar.data.repository.HuertoRepository
import cl.duoc.huertoescolar.data.repository.SesionRepository
import cl.duoc.huertoescolar.domain.EstadoCultivo
import cl.duoc.huertoescolar.domain.EstadoSector
import cl.duoc.huertoescolar.domain.Operacion
import cl.duoc.huertoescolar.domain.Rol
import cl.duoc.huertoescolar.domain.validacion.Campos
import cl.duoc.huertoescolar.ui.cultivos.CultivoFormViewModel
import cl.duoc.huertoescolar.ui.cultivos.CultivosInfoViewModel
import cl.duoc.huertoescolar.ui.sectores.SectorDetalleViewModel
import cl.duoc.huertoescolar.ui.sectores.SectorFormViewModel
import cl.duoc.huertoescolar.ui.sectores.SectoresViewModel
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

/** Listado, detalle y formulario de sectores; informacion y formulario de cultivos. */
class SectoresYCultivosViewModelTest {

    @get:Rule
    val reglaCorrutinas = ReglaCorrutinas()

    private val huerto = mockk<HuertoRepository>()
    private val sesion = mockk<SesionRepository>()

    // ------------------------------ Listado de sectores ------------------------------
    @Test
    fun listadoMuestraSectoresYMensajeAlEliminar() = runTest {
        every { huerto.observarSectores() } returns flowOf(
            listOf(SectorResumen(Fabrica.sector(1), "Ana Rojas", 2, 1))
        )
        coEvery { huerto.eliminarSector(1) } returns Operacion.Error("No se puede eliminar: el sector tiene tareas registradas")
        coEvery { huerto.eliminarSector(2) } returns Operacion.Exito("Sector eliminado")
        val vm = SectoresViewModel(huerto)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.ui.collect {} }

        assertFalse(vm.ui.value.cargando)
        assertEquals(1, vm.ui.value.sectores.size)
        assertEquals("Ana Rojas", vm.ui.value.sectores[0].responsableNombre)

        vm.eliminarSector(1)
        assertEquals("No se puede eliminar: el sector tiene tareas registradas", vm.ui.value.mensaje)

        vm.eliminarSector(2)
        assertEquals("Sector eliminado", vm.ui.value.mensaje)

        vm.limpiarMensaje()
        assertNull(vm.ui.value.mensaje)
    }

    // ------------------------------ Detalle del sector ------------------------------
    @Test
    fun detalleSinSectorNoConsultaNada() = runTest {
        val vm = SectorDetalleViewModel(huerto)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.ui.collect {} }

        assertFalse(vm.ui.value.cargando)
        assertNull(vm.ui.value.sector)
    }

    @Test
    fun detalleMuestraCultivosYCambiaEstado() = runTest {
        every { huerto.observarSector(1) } returns flowOf(Fabrica.sector(1))
        every { huerto.observarCultivosDeSector(1) } returns flowOf(listOf(Fabrica.cultivo(1), Fabrica.cultivo(2)))
        coEvery { huerto.cambiarEstadoCultivo(1, EstadoCultivo.COSECHADO) } returns
                Operacion.Exito("Estado del cultivo actualizado a Cosechado")
        coEvery { huerto.cambiarEstadoCultivo(2, EstadoCultivo.FINALIZADO) } returns Operacion.Error("Sin conexión")
        val vm = SectorDetalleViewModel(huerto)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.ui.collect {} }

        vm.establecerSector(1)
        assertEquals("Sector 1", vm.ui.value.sector?.nombre)
        assertEquals(2, vm.ui.value.cultivos.size)

        vm.cambiarEstadoCultivo(1, EstadoCultivo.COSECHADO)
        assertEquals("Estado del cultivo actualizado a Cosechado", vm.ui.value.mensaje)

        vm.cambiarEstadoCultivo(2, EstadoCultivo.FINALIZADO)
        assertEquals("Sin conexión", vm.ui.value.mensaje)

        vm.limpiarMensaje()
        assertNull(vm.ui.value.mensaje)
    }

    @Test
    fun detalleEliminaSectorOAvisaElError() = runTest {
        every { huerto.observarSector(1) } returns flowOf(Fabrica.sector(1))
        every { huerto.observarCultivosDeSector(1) } returns flowOf(emptyList())
        coEvery { huerto.eliminarSector(1) } returnsMany listOf(
            Operacion.Error("No se puede eliminar: el sector tiene tareas registradas"),
            Operacion.Exito("Sector eliminado")
        )
        val vm = SectorDetalleViewModel(huerto)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.ui.collect {} }
        vm.establecerSector(1)

        var aviso = ""
        vm.eliminarSector { aviso = it }
        assertEquals("", aviso)
        assertEquals("No se puede eliminar: el sector tiene tareas registradas", vm.ui.value.mensaje)

        vm.eliminarSector { aviso = it }
        assertEquals("Sector eliminado", aviso)
    }

    // ------------------------------ Formulario de sector ------------------------------
    private fun formularioSector(): SectorFormViewModel {
        every { sesion.observarUsuarios() } returns flowOf(
            listOf(Fabrica.usuario(1, Rol.ADMINISTRADOR), Fabrica.usuario(3, Rol.ESTUDIANTE))
        )
        every { huerto.observarCultivosDeSector(any()) } returns flowOf(listOf(Fabrica.cultivo(1)))
        return SectorFormViewModel(huerto, sesion)
    }

    @Test
    fun formularioSectorVacioMuestraErroresYLosCorrigeEnVivo() = runTest {
        val vm = formularioSector()

        vm.guardar()
        val errores = vm.ui.value.errores
        assertEquals("Revisa los campos marcados", vm.ui.value.mensaje)
        assertTrue(errores.keys.containsAll(listOf(Campos.CODIGO, Campos.NOMBRE, Campos.DESCRIPCION, Campos.UBICACION)))

        vm.cambiarCodigo("S-09")
        vm.cambiarNombre("Huerto Este")
        vm.cambiarDescripcion("Cama nueva del patio")
        vm.cambiarUbicacion("Patio")
        assertTrue(vm.ui.value.errores.isEmpty())

        vm.limpiarMensaje()
        assertNull(vm.ui.value.mensaje)
        coVerify(exactly = 0) { huerto.guardarSector(any(), any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun crearSectorQuedaAbiertoEnEdicionConSusCultivos() = runTest {
        coEvery {
            huerto.guardarSector(0, "S-09", "Huerto Este", "Cama nueva del patio", "Patio", EstadoSector.EN_USO, 1)
        } returns Operacion.Exito("Sector creado correctamente", idGenerado = 9)
        val vm = formularioSector()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.cultivos.collect {} }
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.responsables.collect {} }

        assertTrue(vm.cultivos.value.isEmpty())
        assertEquals(1, vm.responsables.value.size)

        vm.cambiarCodigo("S-09")
        vm.cambiarNombre("Huerto Este")
        vm.cambiarDescripcion("Cama nueva del patio")
        vm.cambiarUbicacion("Patio")
        vm.cambiarEstado(EstadoSector.EN_USO)
        vm.cambiarResponsable(1)
        vm.guardar()

        assertEquals(9L, vm.ui.value.id)
        assertTrue(vm.ui.value.esEdicion)
        assertFalse(vm.ui.value.guardado)
        assertFalse(vm.ui.value.guardando)
        assertEquals("Sector creado correctamente", vm.ui.value.mensaje)
        assertEquals(1, vm.cultivos.value.size)
    }

    @Test
    fun editarSectorCargaDatosYVuelveAlGuardar() = runTest {
        coEvery { huerto.sectorPorId(1) } returns Fabrica.sector(1)
        coEvery { huerto.sectorPorId(5) } returns null
        coEvery { huerto.guardarSector(1, any(), any(), any(), any(), any(), any()) } returnsMany listOf(
            Operacion.Error("Ya existe otro sector con el código S-01"),
            Operacion.Exito("Sector actualizado correctamente")
        )
        val vm = formularioSector()

        vm.cargar(5)
        assertEquals(0L, vm.ui.value.id)

        vm.cargar(1)
        assertEquals("S-01", vm.ui.value.codigo)
        assertEquals("Patio", vm.ui.value.ubicacion)
        vm.cargar(1)
        vm.cargar(0)
        coVerify(exactly = 1) { huerto.sectorPorId(1) }

        vm.guardar()
        assertEquals("Ya existe otro sector con el código S-01", vm.ui.value.mensaje)
        assertFalse(vm.ui.value.guardado)

        vm.guardar()
        assertTrue(vm.ui.value.guardado)
    }

    // ------------------------------ Informacion de cultivos ------------------------------
    @Test
    fun informacionAgrupaCultivosPorSectorYFiltra() = runTest {
        every { huerto.observarSectoresSimples() } returns flowOf(listOf(Fabrica.sector(1), Fabrica.sector(2)))
        every { huerto.observarCultivos() } returns flowOf(
            listOf(Fabrica.cultivo(1, sectorId = 1), Fabrica.cultivo(2, sectorId = 1).copy(nombre = "Tomate"))
        )
        val vm = CultivosInfoViewModel(huerto)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.ui.collect {} }

        assertEquals(2, vm.ui.value.grupos.size)
        assertEquals(2, vm.ui.value.grupos[0].cultivos.size)
        assertTrue(vm.ui.value.grupos[1].cultivos.isEmpty())
        assertEquals(2, vm.ui.value.totalCultivos)

        vm.buscar(" TOMA ")
        assertEquals(1, vm.ui.value.grupos.size)
        assertEquals("Tomate", vm.ui.value.grupos[0].cultivos.single().nombre)
        assertEquals(" TOMA ", vm.ui.value.busqueda)
    }

    // ------------------------------ Formulario de cultivo ------------------------------
    private fun formularioCultivo(): CultivoFormViewModel {
        every { huerto.observarSectoresSimples() } returns flowOf(listOf(Fabrica.sector(1)))
        return CultivoFormViewModel(huerto)
    }

    @Test
    fun cultivoNuevoUsaLaFechaDeHoyYValida() = runTest {
        val vm = formularioCultivo()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.sectores.collect {} }
        assertEquals(1, vm.sectores.value.size)

        vm.preparar(sectorId = 1, cultivoId = 0)
        assertEquals(Fechas.formatear(Fechas.hoy()), vm.ui.value.fechaSiembra)
        assertEquals(1L, vm.ui.value.sectorId)

        vm.guardar()
        val errores = vm.ui.value.errores
        assertTrue(errores.keys.containsAll(listOf(Campos.NOMBRE, Campos.DESCRIPCION, Campos.COSECHA, Campos.RIEGO)))
        assertEquals("Revisa los campos marcados", vm.ui.value.mensaje)

        vm.cambiarNombre("Lechuga")
        vm.cambiarDescripcion("Cama 1 del sector")
        vm.cambiarRiego("abc")
        assertEquals("La frecuencia de riego debe ser un número", vm.ui.value.errores[Campos.RIEGO])
        vm.cambiarRiego("2")
        vm.cambiarCosecha("01/01/2099")
        vm.cambiarSector(1)
        assertTrue(vm.ui.value.errores.isEmpty())
        coVerify(exactly = 0) { huerto.guardarCultivo(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun cultivoNuevoSinSectorNoLoPreselecciona() = runTest {
        val vm = formularioCultivo()

        vm.preparar(sectorId = 0, cultivoId = 0)
        assertNull(vm.ui.value.sectorId)

        // Si ya tiene fecha no se vuelve a preparar.
        vm.cambiarSector(1)
        vm.preparar(sectorId = 0, cultivoId = 0)
        assertEquals(1L, vm.ui.value.sectorId)
    }

    @Test
    fun guardarCultivoNuevoEnviaLosDatos() = runTest {
        coEvery {
            huerto.guardarCultivo(
                0, "Lechuga", "Cama 1 del sector", 1, Fechas.hoy(), null,
                Fechas.parsear("01/01/2099")!!, 2, EstadoCultivo.SEMBRADO, "Sin novedad", "/foto.jpg"
            )
        } returns Operacion.Exito("Cultivo registrado correctamente")
        val vm = formularioCultivo()

        vm.preparar(sectorId = 1, cultivoId = 0)
        vm.cambiarNombre("Lechuga")
        vm.cambiarDescripcion("Cama 1 del sector")
        vm.cambiarCosecha("01/01/2099")
        vm.cambiarRiego("2")
        vm.cambiarEstado(EstadoCultivo.SEMBRADO)
        vm.cambiarObservaciones("Sin novedad")
        vm.cambiarFoto("/foto.jpg")
        vm.guardar()

        assertTrue(vm.ui.value.guardado)
        assertFalse(vm.ui.value.guardando)
        assertEquals("Cultivo registrado correctamente", vm.ui.value.mensaje)
    }

    @Test
    fun editarCultivoCargaDatosGuardaYElimina() = runTest {
        val cultivo = Fabrica.cultivo(4).copy(fechaTrasplante = Fechas.hoy(), rutaFoto = "/f.jpg")
        coEvery { huerto.cultivoPorId(4) } returns cultivo
        coEvery { huerto.cultivoPorId(8) } returns null
        coEvery { huerto.guardarCultivo(4, any(), any(), any(), any(), any(), any(), any(), any(), any(), any()) } returns
                Operacion.Error("El sector seleccionado no existe")
        coEvery { huerto.eliminarCultivo(4) } returnsMany listOf(
            Operacion.Error("No se puede eliminar: el cultivo tiene tareas registradas"),
            Operacion.Exito("Cultivo eliminado")
        )
        val vm = formularioCultivo()

        vm.preparar(sectorId = 1, cultivoId = 8)
        assertEquals(0L, vm.ui.value.id)

        vm.preparar(sectorId = 1, cultivoId = 4)
        assertTrue(vm.ui.value.esEdicion)
        assertEquals("Lechuga", vm.ui.value.nombre)
        assertEquals(Fechas.formatear(Fechas.hoy()), vm.ui.value.fechaTrasplante)
        assertEquals("2", vm.ui.value.frecuenciaRiego)
        assertEquals("/f.jpg", vm.ui.value.rutaFoto)
        vm.preparar(sectorId = 1, cultivoId = 4)
        coVerify(exactly = 1) { huerto.cultivoPorId(4) }

        vm.guardar()
        assertEquals("El sector seleccionado no existe", vm.ui.value.mensaje)
        assertFalse(vm.ui.value.guardado)

        var aviso = ""
        vm.eliminar { aviso = it }
        assertEquals("No se puede eliminar: el cultivo tiene tareas registradas", vm.ui.value.mensaje)
        vm.eliminar { aviso = it }
        assertEquals("Cultivo eliminado", aviso)

        vm.avisar("No se pudo abrir la cámara")
        assertEquals("No se pudo abrir la cámara", vm.ui.value.mensaje)
        vm.limpiarMensaje()
        assertNull(vm.ui.value.mensaje)
    }

    @Test
    fun cultivoSinTrasplanteSeMuestraVacio() = runTest {
        coEvery { huerto.cultivoPorId(1) } returns Fabrica.cultivo(1)
        val vm = formularioCultivo()

        vm.preparar(sectorId = 1, cultivoId = 1)

        assertEquals("", vm.ui.value.fechaTrasplante)
    }
}
