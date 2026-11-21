package cl.duoc.huertoescolar.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import cl.duoc.huertoescolar.data.local.entity.Cultivo
import cl.duoc.huertoescolar.data.local.entity.Evidencia
import cl.duoc.huertoescolar.data.local.entity.LecturaSensor
import cl.duoc.huertoescolar.data.local.entity.ReporteProblema
import cl.duoc.huertoescolar.data.local.entity.Sector
import cl.duoc.huertoescolar.data.local.entity.Tarea
import cl.duoc.huertoescolar.data.local.entity.Usuario
import cl.duoc.huertoescolar.data.local.entity.Validacion

/**
 * Guarda en Room la copia de los datos que llegan del microservicio.
 *
 * Cada metodo "aplicar" corre dentro de una transaccion: inserta o actualiza
 * (@Upsert) lo que trae el servidor y borra lo que ya no existe alla. Si algo
 * falla a mitad, no queda nada a medias.
 */
@Dao
abstract class SincronizacionDao {

    // ------------------------- Insertar o actualizar -------------------------
    @Upsert
    abstract suspend fun guardarUsuarios(usuarios: List<Usuario>)

    @Upsert
    abstract suspend fun guardarSectores(sectores: List<Sector>)

    @Upsert
    abstract suspend fun guardarCultivos(cultivos: List<Cultivo>)

    @Upsert
    abstract suspend fun guardarLecturas(lecturas: List<LecturaSensor>)

    @Upsert
    abstract suspend fun guardarTareas(tareas: List<Tarea>)

    @Upsert
    abstract suspend fun guardarEvidencias(evidencias: List<Evidencia>)

    @Upsert
    abstract suspend fun guardarValidaciones(validaciones: List<Validacion>)

    @Upsert
    abstract suspend fun guardarReportes(reportes: List<ReporteProblema>)

    // ------------------ Borrar lo que ya no existe en el servidor ------------
    @Query("DELETE FROM sectores WHERE id NOT IN (:ids)")
    abstract suspend fun borrarSectoresExcepto(ids: List<Long>)

    @Query("DELETE FROM cultivos WHERE id NOT IN (:ids)")
    abstract suspend fun borrarCultivosExcepto(ids: List<Long>)

    @Query("DELETE FROM lecturas_sensor WHERE id NOT IN (:ids)")
    abstract suspend fun borrarLecturasExcepto(ids: List<Long>)

    @Query("DELETE FROM tareas WHERE id NOT IN (:ids)")
    abstract suspend fun borrarTareasExcepto(ids: List<Long>)

    @Query("DELETE FROM evidencias WHERE id NOT IN (:ids)")
    abstract suspend fun borrarEvidenciasExcepto(ids: List<Long>)

    @Query("DELETE FROM validaciones WHERE id NOT IN (:ids)")
    abstract suspend fun borrarValidacionesExcepto(ids: List<Long>)

    @Query("DELETE FROM reportes_problema WHERE id NOT IN (:ids)")
    abstract suspend fun borrarReportesExcepto(ids: List<Long>)

    // ------------------- Ids locales (para descartar huerfanos) ---------------
    @Query("SELECT id FROM usuarios")
    abstract suspend fun idsUsuarios(): List<Long>

    @Query("SELECT id FROM sectores")
    abstract suspend fun idsSectores(): List<Long>

    @Query("SELECT id FROM cultivos")
    abstract suspend fun idsCultivos(): List<Long>

    // ------------------------------ Transacciones -----------------------------

    /** Los usuarios solo se insertan o actualizan (las tareas los referencian). */
    @Transaction
    open suspend fun aplicarUsuarios(usuarios: List<Usuario>) {
        guardarUsuarios(usuarios)
    }

    /** Primero se guardan los padres y despues se borran los hijos que sobran. */
    @Transaction
    open suspend fun aplicarHuerto(datos: DatosHuerto) {
        guardarSectores(datos.sectores)
        guardarCultivos(datos.cultivos)
        guardarLecturas(datos.lecturas)
        borrarLecturasExcepto(datos.lecturas.map { it.id })
        borrarCultivosExcepto(datos.cultivos.map { it.id })
        borrarSectoresExcepto(datos.sectores.map { it.id })
    }

    @Transaction
    open suspend fun aplicarActividades(datos: DatosActividades) {
        guardarTareas(datos.tareas)
        guardarEvidencias(datos.evidencias)
        guardarValidaciones(datos.validaciones)
        guardarReportes(datos.reportes)
        borrarEvidenciasExcepto(datos.evidencias.map { it.id })
        borrarValidacionesExcepto(datos.validaciones.map { it.id })
        borrarReportesExcepto(datos.reportes.map { it.id })
        borrarTareasExcepto(datos.tareas.map { it.id })
    }
}

/** Datos de ms-huerto-escolar listos para guardar en Room. */
data class DatosHuerto(
    val sectores: List<Sector>,
    val cultivos: List<Cultivo>,
    val lecturas: List<LecturaSensor>
)

/** Datos de ms-huerto-escolar listos para guardar en Room. */
data class DatosActividades(
    val tareas: List<Tarea>,
    val evidencias: List<Evidencia>,
    val validaciones: List<Validacion>,
    val reportes: List<ReporteProblema>
)
