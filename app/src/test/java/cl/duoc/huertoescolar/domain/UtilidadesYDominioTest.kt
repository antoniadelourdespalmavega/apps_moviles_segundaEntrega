package cl.duoc.huertoescolar.domain

import cl.duoc.huertoescolar.core.Fechas
import cl.duoc.huertoescolar.data.local.Converters
import cl.duoc.huertoescolar.domain.clima.CondicionClima
import cl.duoc.huertoescolar.domain.validacion.Campos
import cl.duoc.huertoescolar.domain.validacion.ReglasComunes
import cl.duoc.huertoescolar.domain.validacion.ValidadorCultivo
import cl.duoc.huertoescolar.domain.validacion.ValidadorEntrega
import cl.duoc.huertoescolar.domain.validacion.ValidadorLogin
import cl.duoc.huertoescolar.domain.validacion.ValidadorRegistro
import cl.duoc.huertoescolar.domain.validacion.ValidadorReporte
import cl.duoc.huertoescolar.domain.validacion.ValidadorRevision
import cl.duoc.huertoescolar.domain.validacion.ValidadorSector
import cl.duoc.huertoescolar.domain.validacion.ValidadorTarea
import cl.duoc.huertoescolar.util.Fabrica
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Utilidades de fecha, conversores de Room, avisos del huerto y reglas de cada formulario. */
class UtilidadesYDominioTest {

    // ------------------------------ Fechas ------------------------------
    @Test
    fun parsearSoloAceptaFechasReales() {
        assertNotNull(Fechas.parsear(" 15/03/2026 "))
        assertNull(Fechas.parsear("31/02/2026"))
        assertNull(Fechas.parsear("1/1/2026"))
        assertNull(Fechas.parsear("hoy"))
    }

    @Test
    fun formatearYParsearSonInversos() {
        val millis = Fechas.parsear("15/03/2026")!!
        assertEquals("15/03/2026", Fechas.formatear(millis))
        assertEquals("Sin registro", Fechas.formatearOpcional(null))
        assertEquals("15/03/2026", Fechas.formatearOpcional(millis))
        assertTrue(Regex("""\d{2}/\d{2}/\d{4} \d{2}:\d{2}""").matches(Fechas.formatearConHora(millis)))
    }

    @Test
    fun fechasEnUtcParaElSelectorDeFecha() {
        val utc = Fechas.parsearAUtc("15/03/2026")!!
        assertEquals(0L, utc % (24L * 60 * 60 * 1000))
        assertEquals("15/03/2026", Fechas.formatearDesdeUtc(utc))
        assertNull(Fechas.parsearAUtc("99/99/2026"))
    }

    @Test
    fun diasEntreEInicioDelDia() {
        val desde = Fechas.parsear("01/12/2026")!!
        val hasta = Fechas.parsear("11/12/2026")!!
        assertEquals(10, Fechas.diasEntre(desde, hasta))
        assertEquals(-10, Fechas.diasEntre(hasta, desde))
        assertEquals(desde, Fechas.inicioDelDia(desde + 5 * 60 * 60 * 1000))
        assertEquals(hasta, Fechas.sumarDias(desde, 10))
        assertTrue(Fechas.ahora() >= Fechas.hoy())
    }

    @Test
    fun nombreDelDiaDelPronostico() {
        val iso = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val hoy = Fechas.hoy()
        assertEquals("Hoy", Fechas.nombreDia(iso.format(Date(hoy)), hoy))
        assertEquals("Mañana", Fechas.nombreDia(iso.format(Date(Fechas.sumarDias(hoy, 1))), hoy))
        val enTresDias = Fechas.nombreDia(iso.format(Date(Fechas.sumarDias(hoy, 3))), hoy)
        assertTrue(enTresDias.endsWith(Fechas.formatear(Fechas.sumarDias(hoy, 3)).substring(0, 5)))
        assertEquals("no-es-fecha", Fechas.nombreDia("no-es-fecha", hoy))
    }

    // ------------------------------ Conversores de Room ------------------------------
    @Test
    fun conversoresGuardanYRecuperanTodosLosEnum() {
        val c = Converters()
        Rol.entries.forEach { assertEquals(it, c.datoARol(c.rolADato(it))) }
        Cargo.entries.forEach { assertEquals(it, c.datoACargo(c.cargoADato(it))) }
        EstadoSector.entries.forEach { assertEquals(it, c.datoAEstadoSector(c.estadoSectorADato(it))) }
        EstadoCultivo.entries.forEach { assertEquals(it, c.datoAEstadoCultivo(c.estadoCultivoADato(it))) }
        TipoTarea.entries.forEach { assertEquals(it, c.datoATipoTarea(c.tipoTareaADato(it))) }
        EstadoTarea.entries.forEach { assertEquals(it, c.datoAEstadoTarea(c.estadoTareaADato(it))) }
        ResultadoValidacion.entries.forEach { assertEquals(it, c.datoAResultado(c.resultadoADato(it))) }
        TipoProblema.entries.forEach { assertEquals(it, c.datoATipoProblema(c.tipoProblemaADato(it))) }
        EstadoReporte.entries.forEach { assertEquals(it, c.datoAEstadoReporte(c.estadoReporteADato(it))) }
        EstadoLectura.entries.forEach { assertEquals(it, c.datoAEstadoLectura(c.estadoLecturaADato(it))) }
        assertEquals("EN_USO", c.estadoSectorADato(EstadoSector.EN_USO))
    }

    // ------------------------------ Clima ------------------------------
    @Test
    fun condicionDelCieloSegunCodigoWmo() {
        assertEquals(CondicionClima.DESCONOCIDO, CondicionClima.desdeCodigo(null))
        assertEquals(CondicionClima.DESPEJADO, CondicionClima.desdeCodigo(0))
        assertEquals(CondicionClima.PARCIAL, CondicionClima.desdeCodigo(2))
        assertEquals(CondicionClima.NUBLADO, CondicionClima.desdeCodigo(3))
        assertEquals(CondicionClima.NIEBLA, CondicionClima.desdeCodigo(45))
        assertEquals(CondicionClima.LLOVIZNA, CondicionClima.desdeCodigo(53))
        assertEquals(CondicionClima.LLUVIA, CondicionClima.desdeCodigo(63))
        assertEquals(CondicionClima.LLUVIA, CondicionClima.desdeCodigo(81))
        assertEquals(CondicionClima.NIEVE, CondicionClima.desdeCodigo(73))
        assertEquals(CondicionClima.NIEVE, CondicionClima.desdeCodigo(86))
        assertEquals(CondicionClima.TORMENTA, CondicionClima.desdeCodigo(96))
        assertEquals(CondicionClima.DESCONOCIDO, CondicionClima.desdeCodigo(500))
    }

    // ------------------------------ Avisos del huerto ------------------------------
    @Test
    fun avisosCubrenCadaTipoDeRecordatorio() {
        val tareas = listOf(
            Fabrica.tarea(1, estado = EstadoTarea.PENDIENTE, diasHastaFecha = -2),
            Fabrica.tarea(2, estado = EstadoTarea.PENDIENTE, diasHastaFecha = 1),
            Fabrica.tarea(3, estado = EstadoTarea.RECHAZADA, diasHastaFecha = 3),
            Fabrica.tarea(4, estado = EstadoTarea.PENDIENTE, diasHastaFecha = 1, tipo = TipoTarea.RIEGO),
            Fabrica.tarea(5, estado = EstadoTarea.EN_DESARROLLO, diasHastaFecha = 5, tipo = TipoTarea.MANTENIMIENTO),
            Fabrica.tarea(6, estado = EstadoTarea.PENDIENTE_VALIDACION, diasHastaFecha = 10)
        ).map { Fabrica.tareaDetalle(it) }
        val cultivos = listOf(Fabrica.cultivo(1, cosechaEnDias = 5), Fabrica.cultivo(2, cosechaEnDias = 60))

        val avisos = Alertas.calcular(tareas, cultivos)
        val titulos = avisos.map { it.titulo }

        assertEquals(7, avisos.size)
        assertEquals("Tareas atrasadas: 1", titulos[0])
        assertEquals(NivelAlerta.ERROR, avisos[0].nivel)
        assertTrue(titulos.contains("Tareas para los próximos días: 1"))
        assertTrue(titulos.contains("Tareas para corregir y reenviar: 1"))
        assertTrue(titulos.contains("Riegos programados: 1"))
        assertTrue(titulos.contains("Mantenimientos programados: 1"))
        assertTrue(titulos.contains("Entregas esperando revisión: 1"))
        assertTrue(titulos.contains("Próximas cosechas: 1"))
        assertTrue(Alertas.calcular(emptyList(), emptyList()).isEmpty())
    }

    @Test
    fun categoriasYProximoRiego() {
        val hoy = Fechas.hoy()
        assertEquals(CategoriaTarea.PENDIENTE, Alertas.categoria(Fabrica.tareaDetalle(Fabrica.tarea(1, estado = EstadoTarea.EN_DESARROLLO)), hoy))
        assertEquals(CategoriaTarea.POR_VALIDAR, Alertas.categoria(Fabrica.tareaDetalle(Fabrica.tarea(1, estado = EstadoTarea.PENDIENTE_VALIDACION)), hoy))
        assertEquals(CategoriaTarea.VALIDADA, Alertas.categoria(Fabrica.tareaDetalle(Fabrica.tarea(1, estado = EstadoTarea.VALIDADA, diasHastaFecha = -9)), hoy))
        assertEquals(CategoriaTarea.RECHAZADA, Alertas.categoria(Fabrica.tareaDetalle(Fabrica.tarea(1, estado = EstadoTarea.RECHAZADA)), hoy))
        assertEquals(CategoriaTarea.ATRASADA, Alertas.categoria(Fabrica.tareaDetalle(Fabrica.tarea(1, estado = EstadoTarea.RECHAZADA, diasHastaFecha = -1)), hoy))

        val cultivo = Fabrica.cultivo(1)
        assertEquals(Fechas.sumarDias(cultivo.fechaSiembra, 2), Alertas.proximaFechaRiego(cultivo, null))
        assertEquals(Fechas.sumarDias(hoy, 2), Alertas.proximaFechaRiego(cultivo, hoy))
    }

    // ------------------------------ Reglas de los formularios ------------------------------
    @Test
    fun reglasComunes() {
        assertEquals("El campo es obligatorio", ReglasComunes.textoObligatorio("  ", "El campo"))
        assertEquals("El campo no puede superar los 5 caracteres", ReglasComunes.textoObligatorio("abcdefg", "El campo", 1, 5))
        assertNull(ReglasComunes.textoObligatorio("abc", "El campo"))
        assertEquals("Selecciona", ReglasComunes.seleccionObligatoria(null, "Selecciona"))
        assertNull(ReglasComunes.seleccionObligatoria(1L, "Selecciona"))
        assertEquals("El usuario es obligatorio", ReglasComunes.nombreUsuario(""))
        assertNotNull(ReglasComunes.nombreUsuario("Con Mayus"))
        assertNull(ReglasComunes.nombreUsuario("est.camila"))
        assertEquals("Las contraseñas no coinciden", ReglasComunes.confirmacion("a1234567", "b1234567"))
        assertNull(ReglasComunes.confirmacion("a1234567", "a1234567"))
        assertEquals("Fecha de siembra inválida (dd/mm/aaaa)", ReglasComunes.fecha("x", "siembra"))
        assertNull(ReglasComunes.fecha("01/01/2026", "siembra"))
    }

    @Test
    fun validadorSectorNormalizaElCodigo() {
        assertTrue(ValidadorSector.validar("s-01", "Huerto", "Cama del patio", "Patio").isEmpty())
        assertEquals("El código es obligatorio", ValidadorSector.validarCodigo(" "))
        assertNotNull(ValidadorSector.validarCodigo("S 01 LARGO!!"))
    }

    @Test
    fun validadorCultivoFechas() {
        assertEquals(
            "La cosecha estimada no puede ser anterior a la siembra",
            ValidadorCultivo.validarCosecha("10/03/2026", "01/03/2026")
        )
        assertNotNull(ValidadorCultivo.validarCosecha("10/03/2026", "x"))
        assertNull(ValidadorCultivo.validarCosecha("x", "01/03/2026"))
        assertNull(ValidadorCultivo.validarTrasplante("01/03/2026", " ", "01/05/2026"))
        assertNotNull(ValidadorCultivo.validarTrasplante("01/03/2026", "x", "01/05/2026"))
        assertEquals(
            "El trasplante no puede ser anterior a la siembra",
            ValidadorCultivo.validarTrasplante("01/03/2026", "01/02/2026", "01/05/2026")
        )
        assertEquals(
            "El trasplante no puede ser posterior a la cosecha",
            ValidadorCultivo.validarTrasplante("01/03/2026", "01/06/2026", "01/05/2026")
        )
        assertNull(ValidadorCultivo.validarTrasplante("01/03/2026", "01/04/2026", "01/05/2026"))
        assertEquals("La frecuencia de riego debe ser mayor que cero", ValidadorCultivo.validarFrecuenciaRiego("0"))
        assertEquals("La frecuencia de riego no puede superar los 60 días", ValidadorCultivo.validarFrecuenciaRiego("61"))
    }

    @Test
    fun validadorTareaFechaProgramada() {
        val ayer = Fechas.formatear(Fechas.sumarDias(Fechas.hoy(), -1))
        assertEquals("Fecha programada inválida (dd/mm/aaaa)", ValidadorTarea.validarFechaProgramada("x", true))
        assertEquals("Una tarea nueva no puede programarse en el pasado", ValidadorTarea.validarFechaProgramada(ayer, true))
        assertNull(ValidadorTarea.validarFechaProgramada(ayer, false))
        assertNull(ValidadorTarea.validarFechaProgramada(Fechas.formatear(Fechas.hoy()), true))
    }

    @Test
    fun validadoresDeEntregaRevisionYReporte() {
        assertEquals(setOf(Campos.COMENTARIO, Campos.EVIDENCIA), ValidadorEntrega.validar("", " ").keys)
        assertTrue(ValidadorEntrega.validar("Regué las camas", "/f.jpg").isEmpty())
        assertEquals(setOf(Campos.MOTIVO), ValidadorRevision.validarRechazo("no").keys)
        assertTrue(ValidadorRevision.validarRechazo("Falta la foto").isEmpty())
        assertEquals(setOf(Campos.SECTOR, Campos.TIPO, Campos.DESCRIPCION), ValidadorReporte.validar(null, null, "").keys)
        assertTrue(ValidadorReporte.validar(1, TipoProblema.OTRO, "Se rompió la manguera").isEmpty())
    }

    @Test
    fun validadoresDeLoginYRegistro() {
        assertEquals("Ingresa tu usuario o correo", ValidadorLogin.validarUsuario(" "))
        assertNotNull(ValidadorLogin.validarUsuario("malo@correo"))
        assertNull(ValidadorLogin.validarUsuario("ana@escuela.cl"))
        assertNull(ValidadorLogin.validarUsuario("profe.ana"))
        assertEquals("Ingresa tu contraseña", ValidadorLogin.validarClave(""))
        assertEquals("Código de autorización incorrecto", ValidadorRegistro.validarCodigoAutorizacion(Cargo.DOCENTE, "x"))
        assertNull(ValidadorRegistro.validarCodigoAutorizacion(Cargo.TALLERISTA, " HUERTO2026 "))
        assertNull(ValidadorRegistro.validarCodigoAutorizacion(Cargo.ESTUDIANTE, ""))
        assertNull(ValidadorRegistro.validarCodigoAutorizacion(null, ""))
        assertEquals("Selecciona tu cargo", ValidadorRegistro.validarCargo(null))
    }
}
