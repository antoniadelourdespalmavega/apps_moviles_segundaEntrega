package cl.duoc.huertoescolar.data.repository

import cl.duoc.huertoescolar.data.local.dao.DatosActividades
import cl.duoc.huertoescolar.data.local.dao.DatosHuerto

/**
 * Cada microservicio tiene su propia base de datos, asi que un registro puede
 * apuntar a algo que otro servicio ya no tiene (por ejemplo, una tarea de un
 * sector eliminado). Room si exige claves foraneas validas, por eso antes de
 * guardar se descartan los registros "huerfanos" y se limpian las referencias
 * opcionales. Es logica pura: se prueba con JUnit.
 */
object FiltroSincronizacion {

    fun huerto(datos: DatosHuerto, idsUsuarios: Set<Long>): DatosHuerto {
        val sectores = datos.sectores.map { sector ->
            if (sector.responsableId != null && sector.responsableId !in idsUsuarios) {
                sector.copy(responsableId = null)
            } else {
                sector
            }
        }
        val idsSectores = sectores.map { it.id }.toSet()
        return DatosHuerto(
            sectores = sectores,
            cultivos = datos.cultivos.filter { it.sectorId in idsSectores },
            lecturas = datos.lecturas.filter { it.sectorId in idsSectores }
        )
    }

    fun actividades(
        datos: DatosActividades,
        idsUsuarios: Set<Long>,
        idsSectores: Set<Long>,
        idsCultivos: Set<Long>
    ): DatosActividades {
        val tareas = datos.tareas
            .filter {
                it.sectorId in idsSectores &&
                        it.responsableId in idsUsuarios &&
                        it.creadaPorId in idsUsuarios
            }
            .map { if (it.cultivoId != null && it.cultivoId !in idsCultivos) it.copy(cultivoId = null) else it }
        val idsTareas = tareas.map { it.id }.toSet()
        return DatosActividades(
            tareas = tareas,
            evidencias = datos.evidencias.filter { it.tareaId in idsTareas && it.usuarioId in idsUsuarios },
            validaciones = datos.validaciones.filter { it.tareaId in idsTareas && it.validadorId in idsUsuarios },
            reportes = datos.reportes
                .filter { it.sectorId in idsSectores && it.reportadoPorId in idsUsuarios }
                .map { if (it.cultivoId != null && it.cultivoId !in idsCultivos) it.copy(cultivoId = null) else it }
        )
    }
}
