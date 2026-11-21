package cl.duoc.huertoescolar.repository

import cl.duoc.huertoescolar.data.local.dao.DatosActividades
import cl.duoc.huertoescolar.data.local.dao.DatosHuerto
import cl.duoc.huertoescolar.data.remote.aEntidad
import cl.duoc.huertoescolar.data.repository.FiltroSincronizacion
import cl.duoc.huertoescolar.util.Fabrica
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Descarte de registros que apuntan a datos que ya no existen. */
class FiltroSincronizacionTest {

    @Test
    fun responsableInexistenteQuedaSinAsignar() {
        val datos = DatosHuerto(listOf(Fabrica.sectorDto(responsableId = 99).aEntidad()), emptyList(), emptyList())
        val filtrado = FiltroSincronizacion.huerto(datos, idsUsuarios = setOf(1L))
        assertNull(filtrado.sectores.first().responsableId)
    }

    @Test
    fun cultivosYLecturasDeSectoresInexistentesSeDescartan() {
        val datos = DatosHuerto(
            sectores = listOf(Fabrica.sectorDto(id = 1).aEntidad()),
            cultivos = listOf(Fabrica.cultivoDto(id = 1, sectorId = 1).aEntidad(), Fabrica.cultivoDto(id = 2, sectorId = 7).aEntidad()),
            lecturas = listOf(Fabrica.lecturaDto(id = 1, sectorId = 7).aEntidad())
        )
        val filtrado = FiltroSincronizacion.huerto(datos, idsUsuarios = setOf(1L))
        assertEquals(listOf(1L), filtrado.cultivos.map { it.id })
        assertEquals(0, filtrado.lecturas.size)
        assertEquals(1L, filtrado.sectores.first().responsableId)
    }

    @Test
    fun actividadesHuerfanasSeDescartanYReferenciasOpcionalesSeLimpian() {
        val datos = DatosActividades(
            tareas = listOf(
                Fabrica.tareaDto(id = 1, cultivoId = 50).aEntidad(),       // cultivo inexistente
                Fabrica.tareaDto(id = 2, sectorId = 9).aEntidad(),         // sector inexistente
                Fabrica.tareaDto(id = 3, responsableId = 77).aEntidad()    // estudiante inexistente
            ),
            evidencias = listOf(Fabrica.evidenciaDto(id = 1, tareaId = 1), Fabrica.evidenciaDto(id = 2, tareaId = 2)).map { it.aEntidad() },
            validaciones = listOf(Fabrica.validacionDto(id = 1, tareaId = 1), Fabrica.validacionDto(id = 2, tareaId = 3)).map { it.aEntidad() },
            reportes = listOf(
                Fabrica.reporteDto(id = 1, cultivoId = 50).aEntidad(),
                Fabrica.reporteDto(id = 2, reportadoPorId = 77).aEntidad()
            )
        )
        val filtrado = FiltroSincronizacion.actividades(datos, setOf(1L, 3L), setOf(1L), setOf(1L))

        assertEquals(listOf(1L), filtrado.tareas.map { it.id })
        assertNull(filtrado.tareas.first().cultivoId)
        assertEquals(listOf(1L), filtrado.evidencias.map { it.id })
        assertEquals(listOf(1L), filtrado.validaciones.map { it.id })
        assertEquals(listOf(1L), filtrado.reportes.map { it.id })
        assertNull(filtrado.reportes.first().cultivoId)
    }
}
