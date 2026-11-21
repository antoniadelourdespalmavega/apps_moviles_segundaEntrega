package cl.duoc.huertoescolar.remote

import cl.duoc.huertoescolar.data.remote.aDominio
import cl.duoc.huertoescolar.data.remote.dto.ClimaActualDto
import cl.duoc.huertoescolar.data.remote.dto.ClimaDiarioDto
import cl.duoc.huertoescolar.data.remote.dto.ClimaDto
import cl.duoc.huertoescolar.domain.clima.CondicionClima
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/** Conversion de la respuesta de Open-Meteo al modelo de la app. */
class MapeadoresClimaTest {

    private val actual = ClimaActualDto("2026-11-10T12:00", 22.4, 45, 0.0, 1, 8.0)

    @Test
    fun convierteClimaActualYPronostico() {
        val dto = ClimaDto(
            actual = actual,
            diario = ClimaDiarioDto(
                fechas = listOf("2026-11-10", "2026-11-11"),
                codigosClima = listOf(0, 61),
                maximas = listOf(26.0, 18.0),
                minimas = listOf(10.0, 9.0),
                lluvia = listOf(0.0, 12.5),
                probabilidadLluvia = listOf(5, 90)
            )
        )
        val clima = dto.aDominio()
        assertNotNull(clima)
        assertEquals(22.4, clima!!.actual.temperatura, 0.001)
        assertEquals(CondicionClima.PARCIAL, clima.actual.condicion)
        assertEquals(2, clima.dias.size)
        assertEquals(CondicionClima.LLUVIA, clima.dias[1].condicion)
        assertEquals(90, clima.dias[1].probabilidadLluvia)
    }

    @Test
    fun datosDiariosFaltantesUsanValoresSeguros() {
        val dto = ClimaDto(
            actual = actual,
            diario = ClimaDiarioDto(listOf("2026-11-10"), null, null, null, null, null)
        )
        val dia = dto.aDominio()!!.dias.first()
        assertEquals(22.4, dia.maxima, 0.001)
        assertEquals(0.0, dia.lluviaMm, 0.001)
        assertEquals(CondicionClima.DESCONOCIDO, dia.condicion)
    }

    @Test
    fun sinDatosActualesDevuelveNull() {
        assertNull(ClimaDto(actual = null, diario = null).aDominio())
        assertNull(ClimaDto(actual = actual.copy(temperatura = null), diario = null).aDominio())
    }
}
