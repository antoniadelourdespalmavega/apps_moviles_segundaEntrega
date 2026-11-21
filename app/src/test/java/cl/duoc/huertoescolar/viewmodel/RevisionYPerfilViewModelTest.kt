package cl.duoc.huertoescolar.viewmodel

import cl.duoc.huertoescolar.data.local.entity.Evidencia
import cl.duoc.huertoescolar.data.local.entity.Usuario
import cl.duoc.huertoescolar.data.local.query.EvidenciaDetalle
import cl.duoc.huertoescolar.data.repository.SesionRepository
import cl.duoc.huertoescolar.data.repository.TareaRepository
import cl.duoc.huertoescolar.domain.EstadoTarea
import cl.duoc.huertoescolar.domain.Operacion
import cl.duoc.huertoescolar.domain.ResultadoValidacion
import cl.duoc.huertoescolar.domain.Rol
import cl.duoc.huertoescolar.domain.validacion.Campos
import cl.duoc.huertoescolar.ui.perfil.PerfilViewModel
import cl.duoc.huertoescolar.ui.revision.RevisionViewModel
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

/** Revision docente de entregas y edicion del perfil propio. */
class RevisionYPerfilViewModelTest {

    @get:Rule
    val reglaCorrutinas = ReglaCorrutinas()

    private val tareas = mockk<TareaRepository>()
    private val sesion = mockk<SesionRepository>()

    // ------------------------------ Revision de entregas ------------------------------
    private fun evidencia(id: Long, tareaId: Long) =
        EvidenciaDetalle(Evidencia(id, tareaId, 3, "/foto$id.jpg", "Listo", id), "Camila Soto")

    private fun revision(): RevisionViewModel {
        every { tareas.observarTodas() } returns flowOf(
            listOf(
                Fabrica.tareaDetalle(Fabrica.tarea(1, estado = EstadoTarea.PENDIENTE_VALIDACION)),
                Fabrica.tareaDetalle(Fabrica.tarea(2, estado = EstadoTarea.PENDIENTE)),
                Fabrica.tareaDetalle(Fabrica.tarea(3, estado = EstadoTarea.PENDIENTE_VALIDACION))
            )
        )
        // Ordenadas de la mas nueva a la mas antigua, como las entrega Room.
        every { tareas.observarTodasLasEvidencias() } returns flowOf(listOf(evidencia(9, 1), evidencia(5, 1)))
        return RevisionViewModel(tareas)
    }

    @Test
    fun soloSeMuestranEntregasPendientesConSuUltimaEvidencia() = runTest {
        val vm = revision()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.ui.collect {} }

        val entregas = vm.ui.value.entregas
        assertFalse(vm.ui.value.cargando)
        assertEquals(listOf(1L, 3L), entregas.map { it.detalle.tarea.id })
        assertEquals("/foto9.jpg", entregas[0].evidencia?.evidencia?.rutaFoto)
        assertNull(entregas[1].evidencia)
    }

    @Test
    fun alternarRechazoAbreYCierraElMotivo() = runTest {
        val vm = revision()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.ui.collect {} }

        vm.alternarRechazo(1)
        vm.cambiarMotivo("texto")
        assertEquals(1L, vm.ui.value.formulario.rechazandoId)
        assertEquals("texto", vm.ui.value.formulario.motivo)

        vm.alternarRechazo(3)
        assertEquals(3L, vm.ui.value.formulario.rechazandoId)
        assertEquals("", vm.ui.value.formulario.motivo)

        vm.alternarRechazo(3)
        assertNull(vm.ui.value.formulario.rechazandoId)
    }

    @Test
    fun rechazarSinMotivoMuestraErrorYConMotivoEnvia() = runTest {
        coEvery { tareas.revisarTarea(1, 1, ResultadoValidacion.RECHAZADA, "Falta la foto del sector") } returns
                Operacion.Exito("Actividad rechazada")
        val vm = revision()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.ui.collect {} }

        vm.alternarRechazo(1)
        vm.rechazar(tareaId = 1, validadorId = 1)
        assertEquals("El motivo del rechazo es obligatorio", vm.ui.value.formulario.error)
        coVerify(exactly = 0) { tareas.revisarTarea(any(), any(), any(), any()) }

        vm.cambiarMotivo("Falta la foto del sector")
        assertNull(vm.ui.value.formulario.error)
        vm.rechazar(tareaId = 1, validadorId = 1)

        assertEquals("Actividad rechazada", vm.ui.value.formulario.mensaje)
        assertNull(vm.ui.value.formulario.rechazandoId)
        assertFalse(vm.ui.value.formulario.procesando)
    }

    @Test
    fun validarConErrorMantieneElFormulario() = runTest {
        coEvery { tareas.revisarTarea(3, 1, ResultadoValidacion.VALIDADA, "") } returns
                Operacion.Error("Solo se revisan actividades pendientes de validación")
        val vm = revision()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.ui.collect {} }

        vm.validar(tareaId = 3, validadorId = 1)

        assertEquals("Solo se revisan actividades pendientes de validación", vm.ui.value.formulario.mensaje)
        assertFalse(vm.ui.value.formulario.procesando)
        vm.limpiarMensaje()
        assertNull(vm.ui.value.formulario.mensaje)
    }

    // ------------------------------ Perfil ------------------------------
    private val ana = Fabrica.usuario(1, Rol.ADMINISTRADOR)

    @Test
    fun cargarCopiaLosDatosUnaSolaVez() {
        val vm = PerfilViewModel(sesion)

        vm.cargar(ana)
        assertEquals("Nombre Apellido", vm.ui.value.nombre)
        assertEquals("usuario1@escuela.cl", vm.ui.value.correo)
        assertEquals("Taller A", vm.ui.value.taller)

        vm.cambiarNombre("Otro Nombre")
        vm.cargar(ana)
        assertEquals("Otro Nombre", vm.ui.value.nombre)
    }

    @Test
    fun erroresDelPerfilYValidacionEnVivo() {
        val vm = PerfilViewModel(sesion)
        vm.cargar(ana)

        vm.cambiarNombre("Ana")
        vm.cambiarCorreo("correo-malo")
        vm.cambiarTaller("")
        vm.cambiarClaveNueva("corta")
        vm.guardar { }

        val errores = vm.ui.value.errores
        assertEquals("Revisa los campos marcados", vm.ui.value.mensaje)
        assertTrue(
            errores.keys.containsAll(
                listOf(Campos.NOMBRE, Campos.CORREO, Campos.TALLER, Campos.CLAVE_ACTUAL, Campos.CLAVE, Campos.CONFIRMAR)
            )
        )

        vm.cambiarNombre("Ana Rojas")
        vm.cambiarCorreo("ana@escuela.cl")
        vm.cambiarTaller("Taller de Huerto")
        vm.cambiarClaveActual("huerto123")
        vm.cambiarClaveNueva("nueva1234")
        vm.cambiarConfirmar("nueva1234")
        assertTrue(vm.ui.value.errores.isEmpty())
        coVerify(exactly = 0) { sesion.actualizarPerfil(any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun guardarPerfilLimpiaLasClavesYDevuelveElUsuario() {
        val actualizado = ana.copy(nombre = "Ana Rojas")
        coEvery { sesion.actualizarPerfil(1, "Ana Rojas", "usuario1@escuela.cl", "Taller A", "huerto123", "nueva1234") } returns
                (actualizado to Operacion.Exito("Perfil actualizado"))
        val vm = PerfilViewModel(sesion)
        vm.cargar(ana)

        vm.cambiarNombre("Ana Rojas")
        vm.cambiarClaveActual("huerto123")
        vm.cambiarClaveNueva("nueva1234")
        vm.cambiarConfirmar("nueva1234")
        var recibido: Usuario? = null
        vm.guardar { recibido = it }

        assertEquals("Perfil actualizado", vm.ui.value.mensaje)
        assertEquals("", vm.ui.value.claveActual)
        assertEquals("", vm.ui.value.claveNueva)
        assertEquals("", vm.ui.value.confirmar)
        assertFalse(vm.ui.value.guardando)
        assertEquals("Ana Rojas", recibido?.nombre)
    }

    @Test
    fun guardarPerfilConErrorDelServidor() {
        coEvery { sesion.actualizarPerfil(any(), any(), any(), any(), any(), any()) } returns
                (null to Operacion.Error("Ese correo ya está registrado por otra cuenta"))
        val vm = PerfilViewModel(sesion)
        vm.cargar(ana)

        var llamado = false
        vm.guardar { llamado = true }

        assertEquals("Ese correo ya está registrado por otra cuenta", vm.ui.value.mensaje)
        assertFalse(llamado)
        vm.limpiarMensaje()
        assertNull(vm.ui.value.mensaje)
    }
}
