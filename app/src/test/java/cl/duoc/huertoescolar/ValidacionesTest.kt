package cl.duoc.huertoescolar

import cl.duoc.huertoescolar.core.Fechas
import cl.duoc.huertoescolar.domain.TipoProblema
import cl.duoc.huertoescolar.domain.TipoTarea
import cl.duoc.huertoescolar.domain.Validaciones
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pruebas unitarias de las reglas de validacion.
 * Como son funciones puras (sin Compose ni Android) se pueden probar directo.
 */
class ValidacionesTest {

    @Test
    fun sectorConDatosCorrectosNoTieneErrores() {
        val errores = Validaciones.sector(
            codigo = "S-01",
            nombre = "Huerto Norte",
            descripcion = "Cuatro camas de cultivo",
            ubicacion = "Patio norte"
        )
        assertTrue(errores.isEmpty())
    }

    @Test
    fun sectorSinNombreDevuelveError() {
        val errores = Validaciones.sector(
            codigo = "S-01",
            nombre = "",
            descripcion = "Cuatro camas de cultivo",
            ubicacion = "Patio norte"
        )
        assertTrue(errores.containsKey(Validaciones.CAMPO_NOMBRE))
    }

    @Test
    fun cosechaAnteriorALaSiembraDevuelveError() {
        val errores = Validaciones.cultivo(
            nombre = "Lechuga",
            descripcion = "Cultivo de prueba",
            sectorId = 1L,
            fechaSiembra = "10/05/2026",
            fechaTrasplante = "",
            fechaCosecha = "01/05/2026",
            frecuenciaRiego = "2"
        )
        assertEquals(
            "La cosecha estimada no puede ser anterior a la siembra",
            errores[Validaciones.CAMPO_COSECHA]
        )
    }

    @Test
    fun frecuenciaDeRiegoDebeSerPositiva() {
        val errores = Validaciones.cultivo(
            nombre = "Lechuga",
            descripcion = "Cultivo de prueba",
            sectorId = 1L,
            fechaSiembra = "01/05/2026",
            fechaTrasplante = "",
            fechaCosecha = "20/05/2026",
            frecuenciaRiego = "0"
        )
        assertTrue(errores.containsKey(Validaciones.CAMPO_RIEGO))
    }

    @Test
    fun tareaSinResponsableDevuelveError() {
        val errores = Validaciones.tarea(
            descripcion = "Regar la cama 1",
            instrucciones = "Usar la regadera mediana",
            tipo = TipoTarea.RIEGO,
            sectorId = 1L,
            responsableId = null,
            fechaProgramada = "20/05/2026"
        )
        assertTrue(errores.containsKey(Validaciones.CAMPO_RESPONSABLE))
    }

    @Test
    fun entregaSinFotografiaDevuelveError() {
        val errores = Validaciones.entregaDeTarea(
            comentario = "Termine la actividad",
            rutaEvidencia = null
        )
        assertTrue(errores.containsKey(Validaciones.CAMPO_EVIDENCIA))
    }

    @Test
    fun rechazoSinMotivoDevuelveError() {
        val errores = Validaciones.rechazo(motivo = "")
        assertTrue(errores.containsKey(Validaciones.CAMPO_MOTIVO))
    }

    @Test
    fun reporteSinSectorDevuelveError() {
        val errores = Validaciones.reporte(
            sectorId = null,
            tipo = TipoProblema.FALTA_DE_AGUA,
            descripcion = "La bandeja de almacigos se seco"
        )
        assertTrue(errores.containsKey(Validaciones.CAMPO_SECTOR))
    }

    @Test
    fun fechaInvalidaNoSeConvierte() {
        assertEquals(null, Fechas.parsear("32/13/2026"))
        assertEquals(null, Fechas.parsear("2026-05-20"))
        assertTrue(Fechas.parsear("20/05/2026") != null)
    }
}
