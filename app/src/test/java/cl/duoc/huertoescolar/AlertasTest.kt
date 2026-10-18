package cl.duoc.huertoescolar

import cl.duoc.huertoescolar.core.Fechas
import cl.duoc.huertoescolar.data.local.entity.Tarea
import cl.duoc.huertoescolar.data.local.query.TareaDetalle
import cl.duoc.huertoescolar.domain.Alertas
import cl.duoc.huertoescolar.domain.EstadoTarea
import cl.duoc.huertoescolar.domain.TipoTarea
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Pruebas unitarias del calculo de tareas atrasadas. */
class AlertasTest {

    private fun tareaDePrueba(estado: EstadoTarea, diasDesdeHoy: Int): TareaDetalle {
        val hoy = Fechas.hoy()
        return TareaDetalle(
            tarea = Tarea(
                id = 1,
                tipo = TipoTarea.RIEGO,
                descripcion = "Regar la cama 1",
                instrucciones = "Usar la regadera mediana",
                sectorId = 1,
                cultivoId = null,
                responsableId = 3,
                creadaPorId = 1,
                fechaCreacion = hoy,
                fechaProgramada = Fechas.sumarDias(hoy, diasDesdeHoy),
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
    fun tareaPendienteConFechaPasadaEstaAtrasada() {
        val detalle = tareaDePrueba(EstadoTarea.PENDIENTE, -2)
        assertTrue(Alertas.estaAtrasada(detalle))
    }

    @Test
    fun tareaProgramadaParaHoyNoEstaAtrasada() {
        val detalle = tareaDePrueba(EstadoTarea.PENDIENTE, 0)
        assertFalse(Alertas.estaAtrasada(detalle))
    }

    @Test
    fun tareaValidadaNuncaEstaAtrasada() {
        val detalle = tareaDePrueba(EstadoTarea.VALIDADA, -10)
        assertFalse(Alertas.estaAtrasada(detalle))
    }

    @Test
    fun tareaRechazadaConFechaPasadaEstaAtrasada() {
        val detalle = tareaDePrueba(EstadoTarea.RECHAZADA, -1)
        assertTrue(Alertas.estaAtrasada(detalle))
    }
}
