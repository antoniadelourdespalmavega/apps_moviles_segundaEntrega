package cl.duoc.huertoescolar

import cl.duoc.huertoescolar.core.Fechas
import cl.duoc.huertoescolar.data.local.entity.Tarea
import cl.duoc.huertoescolar.data.local.query.TareaDetalle
import cl.duoc.huertoescolar.domain.Alertas
import cl.duoc.huertoescolar.domain.Cargo
import cl.duoc.huertoescolar.domain.CategoriaTarea
import cl.duoc.huertoescolar.domain.EstadoTarea
import cl.duoc.huertoescolar.domain.TipoTarea
import cl.duoc.huertoescolar.domain.Validaciones
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Pruebas del registro, edicion de perfil y conteo de tareas por categoria. */
class CuentasYCategoriasTest {

    @Test
    fun correoValidoNoTieneError() {
        assertNull(Validaciones.validarCorreo("ana.rojas@escuela.cl"))
    }

    @Test
    fun correoSinArrobaOSinDominioEsInvalido() {
        assertNotNull(Validaciones.validarCorreo("ana.rojas"))
        assertNotNull(Validaciones.validarCorreo("ana@escuela"))
        assertNotNull(Validaciones.validarCorreo(""))
    }

    @Test
    fun nombreConNumerosOSinApellidoEsInvalido() {
        assertNotNull(Validaciones.validarNombre("Ana123 Rojas"))
        assertNotNull(Validaciones.validarNombre("Ana"))
        assertNull(Validaciones.validarNombre("Ana Rojas"))
    }

    @Test
    fun claveDebeTenerLetrasNumerosYOchoCaracteres() {
        assertNotNull(Validaciones.validarClave("abc12"))
        assertNotNull(Validaciones.validarClave("soloLetras"))
        assertNotNull(Validaciones.validarClave("12345678"))
        assertNotNull(Validaciones.validarClave("con espacio1"))
        assertNull(Validaciones.validarClave("huerto123"))
    }

    @Test
    fun registroDeDocenteExigeCodigoDeAutorizacion() {
        val errores = Validaciones.registro(
            nombre = "Pedro Soto",
            correo = "pedro@escuela.cl",
            usuario = "profe.pedro",
            clave = "huerto123",
            confirmar = "huerto123",
            cargo = Cargo.DOCENTE,
            taller = "Taller A",
            codigoAutorizacion = "otro"
        )
        assertTrue(errores.containsKey(Validaciones.CAMPO_CODIGO_AUTORIZACION))
    }

    @Test
    fun registroConClavesDistintasDevuelveError() {
        val errores = Validaciones.registro(
            nombre = "Pedro Soto",
            correo = "pedro@escuela.cl",
            usuario = "est.pedro",
            clave = "huerto123",
            confirmar = "huerto124",
            cargo = Cargo.ESTUDIANTE,
            taller = "Taller A",
            codigoAutorizacion = ""
        )
        assertEquals(setOf(Validaciones.CAMPO_CONFIRMAR), errores.keys)
    }

    @Test
    fun perfilSinCambioDeClaveNoPideClaveActual() {
        val errores = Validaciones.perfil("Ana Rojas", "ana@escuela.cl", "Taller", "", "", "")
        assertTrue(errores.isEmpty())
    }

    private fun tarea(estado: EstadoTarea, dias: Int): TareaDetalle {
        val hoy = Fechas.hoy()
        return TareaDetalle(
            tarea = Tarea(
                tipo = TipoTarea.RIEGO,
                descripcion = "Regar",
                instrucciones = "Regar con cuidado",
                sectorId = 1,
                cultivoId = null,
                responsableId = 3,
                creadaPorId = 1,
                fechaCreacion = hoy,
                fechaProgramada = Fechas.sumarDias(hoy, dias),
                estado = estado,
                fechaRealizacion = null,
                comentarioEstudiante = null
            ),
            sectorNombre = "Huerto Norte",
            cultivoNombre = null,
            responsableNombre = "Camila Soto",
            creadorNombre = "Ana Rojas"
        )
    }

    @Test
    fun cadaTareaSeCuentaEnUnaSolaCategoria() {
        val tareas = listOf(
            tarea(EstadoTarea.PENDIENTE, 1),
            tarea(EstadoTarea.EN_DESARROLLO, 0),
            tarea(EstadoTarea.PENDIENTE, -2),
            tarea(EstadoTarea.PENDIENTE_VALIDACION, -1),
            tarea(EstadoTarea.VALIDADA, -5),
            tarea(EstadoTarea.RECHAZADA, 2)
        )
        val conteo = Alertas.contarPorCategoria(tareas)
        assertEquals(2, conteo[CategoriaTarea.PENDIENTE])
        assertEquals(1, conteo[CategoriaTarea.ATRASADA])
        assertEquals(1, conteo[CategoriaTarea.POR_VALIDAR])
        assertEquals(1, conteo[CategoriaTarea.VALIDADA])
        assertEquals(1, conteo[CategoriaTarea.RECHAZADA])
        assertEquals(tareas.size, conteo.values.sum())
    }
}
