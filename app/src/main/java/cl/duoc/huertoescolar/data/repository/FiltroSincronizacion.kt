package cl.duoc.huertoescolar.data.repository

import cl.duoc.huertoescolar.data.local.dao.DatosActividades
import cl.duoc.huertoescolar.data.local.dao.DatosHuerto

/**
 * Room exige claves foraneas validas. Aunque ahora todo el backend vive en un
 * solo microservicio y una sola base de datos, los listados se piden en
 * varias llamadas, por lo que entre una y otra un registro puede quedar
 * apuntando a algo recien eliminado (por ejemplo, un cultivo borrado desde
 * otro telefono). Por eso antes de guardar se descartan los registros
 * "huerfanos" y se limpian las referencias opcionales. Es logica pura: se
 * prueba con JUnit.
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
