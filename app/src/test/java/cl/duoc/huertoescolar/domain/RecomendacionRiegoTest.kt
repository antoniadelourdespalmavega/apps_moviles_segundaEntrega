package cl.duoc.huertoescolar.domain

import cl.duoc.huertoescolar.domain.clima.Clima
import cl.duoc.huertoescolar.domain.clima.ClimaActual
import cl.duoc.huertoescolar.domain.clima.CondicionClima
import cl.duoc.huertoescolar.domain.clima.PronosticoDia
import cl.duoc.huertoescolar.domain.clima.RecomendacionRiego
import org.junit.Assert.assertEquals
import org.junit.Test

/** Reglas que convierten el clima en un consejo de riego. */
class RecomendacionRiegoTest {

    private fun clima(
        humedad: Int = 60,
        precipitacion: Double = 0.0,
        maxima: Double = 22.0,
        minima: Double = 10.0,
        lluviaMm: Double = 0.0,
        probabilidad: Int = 10
    ) = Clima(
        actual = ClimaActual(20.0, humedad, precipitacion, 5.0, CondicionClima.DESPEJADO),
        dias = listOf(PronosticoDia("2026-11-10", CondicionClima.DESPEJADO, maxima, minima, lluviaMm, probabilidad))
    )

    @Test
    fun heladaTieneLaMayorPrioridad() {
        val r = RecomendacionRiego.calcular(clima(minima = 1.0, lluviaMm = 20.0))
        assertEquals("Riesgo de helada", r.titulo)
        assertEquals(NivelAlerta.ERROR, r.nivel)
    }

    @Test
    fun conLluviaNoEsNecesarioRegar() {
        assertEquals("No es necesario regar", RecomendacionRiego.calcular(clima(lluviaMm = 8.0)).titulo)
        assertEquals("No es necesario regar", RecomendacionRiego.calcular(clima(probabilidad = 80)).titulo)
        assertEquals("No es necesario regar", RecomendacionRiego.calcular(clima(precipitacion = 1.2)).titulo)
    }

    @Test
    fun diaCalurosoPideRegarTemprano() {
        val r = RecomendacionRiego.calcular(clima(maxima = 31.0))
        assertEquals("Día caluroso: riega temprano", r.titulo)
        assertEquals(NivelAlerta.ADVERTENCIA, r.nivel)
    }

    @Test
    fun ambienteSecoSugiereRegar() {
        assertEquals("Ambiente seco: conviene regar", RecomendacionRiego.calcular(clima(humedad = 30)).titulo)
    }

    @Test
    fun climaEstableEsRiegoNormal() {
        val r = RecomendacionRiego.calcular(clima())
        assertEquals("Riego normal", r.titulo)
        assertEquals(NivelAlerta.INFORMACION, r.nivel)
    }

    @Test
    fun sinPronosticoSeUsaSoloElClimaActual() {
        val sinDias = clima(humedad = 35).copy(dias = emptyList())
        assertEquals("Ambiente seco: conviene regar", RecomendacionRiego.calcular(sinDias).titulo)
    }

    @Test
    fun codigosDelClimaSeTraducen() {
        assertEquals(CondicionClima.DESPEJADO, CondicionClima.desdeCodigo(0))
        assertEquals(CondicionClima.NUBLADO, CondicionClima.desdeCodigo(3))
        assertEquals(CondicionClima.NIEBLA, CondicionClima.desdeCodigo(45))
        assertEquals(CondicionClima.LLOVIZNA, CondicionClima.desdeCodigo(53))
        assertEquals(CondicionClima.LLUVIA, CondicionClima.desdeCodigo(81))
        assertEquals(CondicionClima.NIEVE, CondicionClima.desdeCodigo(73))
        assertEquals(CondicionClima.TORMENTA, CondicionClima.desdeCodigo(95))
        assertEquals(CondicionClima.DESCONOCIDO, CondicionClima.desdeCodigo(null))
        assertEquals(CondicionClima.DESCONOCIDO, CondicionClima.desdeCodigo(500))
    }
}
