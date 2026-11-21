package cl.duoc.huertoescolar.repository

import cl.duoc.huertoescolar.data.local.dao.EvidenciaDao
import cl.duoc.huertoescolar.data.local.dao.TareaDao
import cl.duoc.huertoescolar.data.local.dao.UsuarioDao
import cl.duoc.huertoescolar.data.local.dao.ValidacionDao
import cl.duoc.huertoescolar.data.remote.api.ActividadesApi
import cl.duoc.huertoescolar.data.remote.dto.AccionRequest
import cl.duoc.huertoescolar.data.repository.SincronizacionRepository
import cl.duoc.huertoescolar.data.repository.TareaRepository
import cl.duoc.huertoescolar.domain.EstadoTarea
import cl.duoc.huertoescolar.domain.Operacion
import cl.duoc.huertoescolar.domain.ResultadoValidacion
import cl.duoc.huertoescolar.domain.Rol
import cl.duoc.huertoescolar.domain.TipoTarea
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

class TareaRepositoryTest {

    private val api = mockk<ActividadesApi>()
    private val tareaDao = mockk<TareaDao>()
    private val evidenciaDao = mockk<EvidenciaDao>()
    private val validacionDao = mockk<ValidacionDao>()
    private val usuarioDao = mockk<UsuarioDao>()
    private val sincronizacion = mockk<SincronizacionRepository>()
    private lateinit var repositorio: TareaRepository

    private val docente = 1L
    private val estudiante = 3L

    @Before
    fun preparar() {
        repositorio = TareaRepository(api, tareaDao, evidenciaDao, validacionDao, usuarioDao, sincronizacion)
        coEvery { sincronizacion.sincronizarActividades() } returns Operacion.Exito("ok")
        coEvery { usuarioDao.existeConRol(docente, Rol.ADMINISTRADOR) } returns 1
        coEvery { usuarioDao.existeConRol(docente, Rol.ESTUDIANTE) } returns 0
        coEvery { usuarioDao.existeConRol(estudiante, Rol.ESTUDIANTE) } returns 1
        coEvery { usuarioDao.existeConRol(estudiante, Rol.ADMINISTRADOR) } returns 0
    }

    private suspend fun guardar(id: Long, editor: Long = docente, responsable: Long = estudiante) =
        repositorio.guardarTarea(id, TipoTarea.RIEGO, "Regar", "Con regadera", 1, null, responsable, 100, editor)

    @Test
    fun soloUnAdministradorCreaTareas() = runTest {
        assertEquals(Operacion.Error("Solo un administrador puede crear o editar tareas"), guardar(0, editor = estudiante))
        coVerify(exactly = 0) { api.crearTarea(any()) }
    }

    @Test
    fun elResponsableDebeSerEstudiante() = runTest {
        assertEquals(
            Operacion.Error("El responsable debe ser un estudiante activo del taller"),
            guardar(0, responsable = docente)
        )
    }

    @Test
    fun crearYEditarTarea() = runTest {
        coEvery { api.crearTarea(any()) } returns Fabrica.tareaDto(1)
        coEvery { api.actualizarTarea(1, any()) } returns Fabrica.tareaDto(1)

        assertEquals(Operacion.Exito("Tarea creada y asignada correctamente"), guardar(0))
        assertEquals(Operacion.Exito("Tarea actualizada correctamente"), guardar(1))
        coVerify { api.crearTarea(match { it.tipo == "RIEGO" && it.usuarioId == docente }) }
        coVerify(exactly = 2) { sincronizacion.sincronizarActividades() }
    }

    @Test
    fun iniciarTareaInexistenteOAjena() = runTest {
        coEvery { tareaDao.porId(9) } returns null
        coEvery { tareaDao.porId(1) } returns Fabrica.tarea(1, responsableId = 4)

        assertEquals(Operacion.Error("La tarea ya no existe"), repositorio.iniciarTarea(9, estudiante))
        assertEquals(
            Operacion.Error("Solo el responsable asignado puede iniciar esta tarea"),
            repositorio.iniciarTarea(1, estudiante)
        )
    }

    @Test
    fun iniciarTareaPropia() = runTest {
        coEvery { tareaDao.porId(1) } returns Fabrica.tarea(1, responsableId = estudiante)
        coEvery { api.iniciarTarea(1, AccionRequest(estudiante)) } returns Fabrica.tareaDto(1, estado = "EN_DESARROLLO")

        assertEquals(Operacion.Exito("Tarea iniciada"), repositorio.iniciarTarea(1, estudiante))
    }

    @Test
    fun entregarTarea() = runTest {
        coEvery { tareaDao.porId(1) } returns Fabrica.tarea(1, estudiante, EstadoTarea.EN_DESARROLLO)
        coEvery { tareaDao.porId(2) } returns Fabrica.tarea(2, estudiante, EstadoTarea.PENDIENTE_VALIDACION)
        coEvery { tareaDao.porId(3) } returns Fabrica.tarea(3, responsableId = 4)
        coEvery { tareaDao.porId(4) } returns null
        coEvery { api.entregarTarea(1, any()) } returns Fabrica.tareaDto(1, estado = "PENDIENTE_VALIDACION")

        assertEquals(
            Operacion.Exito("Actividad entregada. Queda pendiente de validación"),
            repositorio.entregarTarea(1, estudiante, " Listo ", "/foto.jpg")
        )
        assertEquals(Operacion.Error("La tarea ya está esperando revisión"), repositorio.entregarTarea(2, estudiante, "Listo", "/f.jpg"))
        assertEquals(
            Operacion.Error("Solo el responsable asignado puede entregar esta tarea"),
            repositorio.entregarTarea(3, estudiante, "Listo", "/f.jpg")
        )
        assertEquals(Operacion.Error("La tarea ya no existe"), repositorio.entregarTarea(4, estudiante, "Listo", "/f.jpg"))
        coVerify { api.entregarTarea(1, match { it.comentario == "Listo" && it.rutaFoto == "/foto.jpg" }) }
    }

    @Test
    fun revisarTarea() = runTest {
        coEvery { api.revisarTarea(1, any()) } returns Fabrica.tareaDto(1, estado = "VALIDADA")

        assertEquals(
            Operacion.Error("Solo un docente o tallerista puede validar actividades"),
            repositorio.revisarTarea(1, estudiante, ResultadoValidacion.VALIDADA, "")
        )
        assertEquals(
            Operacion.Error("Debes indicar el motivo del rechazo"),
            repositorio.revisarTarea(1, docente, ResultadoValidacion.RECHAZADA, "no")
        )
        assertEquals(Operacion.Exito("Actividad validada"), repositorio.revisarTarea(1, docente, ResultadoValidacion.VALIDADA, ""))
        assertEquals(
            Operacion.Exito("Actividad rechazada. El estudiante puede corregirla y reenviarla"),
            repositorio.revisarTarea(1, docente, ResultadoValidacion.RECHAZADA, "Falta la foto del sector")
        )
    }

    @Test
    fun elServidorRechazaRevisarUnaTareaYaValidada() = runTest {
        coEvery { api.revisarTarea(1, any()) } throws
                Fabrica.errorHttp(409, "Solo se revisan actividades pendientes de validación")

        assertEquals(
            Operacion.Error("Solo se revisan actividades pendientes de validación"),
            repositorio.revisarTarea(1, docente, ResultadoValidacion.VALIDADA, "")
        )
    }

    @Test
    fun eliminarTarea() = runTest {
        coEvery { api.eliminarTarea(1, docente) } returns Fabrica.respuestaVacia()

        assertEquals(Operacion.Error("Solo un administrador puede eliminar tareas"), repositorio.eliminarTarea(1, estudiante))
        assertEquals(Operacion.Exito("Tarea eliminada"), repositorio.eliminarTarea(1, docente))
    }

    @Test
    fun lecturasLocalesSeDelegaALosDao() = runTest {
        every { tareaDao.observarTodas() } returns flowOf(emptyList())
        every { tareaDao.observarPorResponsable(estudiante) } returns flowOf(emptyList())
        every { tareaDao.observarDetalle(1) } returns flowOf(null)
        every { evidenciaDao.observarPorTarea(1) } returns flowOf(emptyList())
        every { evidenciaDao.observarTodas() } returns flowOf(emptyList())
        every { validacionDao.observarTodas() } returns flowOf(emptyList())
        every { validacionDao.observarPorTarea(1) } returns flowOf(emptyList())
        coEvery { tareaDao.porId(1) } returns Fabrica.tarea(1)

        repositorio.observarTodas()
        repositorio.observarDeEstudiante(estudiante)
        repositorio.observarDetalle(1)
        repositorio.observarEvidencias(1)
        repositorio.observarTodasLasEvidencias()
        repositorio.observarValidaciones()
        repositorio.observarValidacionesDeTarea(1)
        assertEquals(1L, repositorio.porId(1)?.id)
    }
}
