package cl.duoc.huertoescolar.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import cl.duoc.huertoescolar.data.local.dao.CultivoDao
import cl.duoc.huertoescolar.data.local.dao.EvidenciaDao
import cl.duoc.huertoescolar.data.local.dao.ReporteDao
import cl.duoc.huertoescolar.data.local.dao.SectorDao
import cl.duoc.huertoescolar.data.local.dao.SensorDao
import cl.duoc.huertoescolar.data.local.dao.SincronizacionDao
import cl.duoc.huertoescolar.data.local.dao.TareaDao
import cl.duoc.huertoescolar.data.local.dao.UsuarioDao
import cl.duoc.huertoescolar.data.local.dao.ValidacionDao
import cl.duoc.huertoescolar.data.local.entity.Cultivo
import cl.duoc.huertoescolar.data.local.entity.Evidencia
import cl.duoc.huertoescolar.data.local.entity.LecturaSensor
import cl.duoc.huertoescolar.data.local.entity.ReporteProblema
import cl.duoc.huertoescolar.data.local.entity.Sector
import cl.duoc.huertoescolar.data.local.entity.Tarea
import cl.duoc.huertoescolar.data.local.entity.Usuario
import cl.duoc.huertoescolar.data.local.entity.Validacion

/**
 * Base de datos local (Room sobre SQLite).
 *
 * Desde la tercera entrega los datos "oficiales" viven en los microservicios.
 * Room guarda una copia local: las pantallas leen de aqui (se actualizan
 * solas) y se sigue viendo la ultima informacion si se corta la conexion.
 */
@Database(
    entities = [
        Usuario::class,
        Sector::class,
        Cultivo::class,
        Tarea::class,
        Evidencia::class,
        Validacion::class,
        ReporteProblema::class,
        LecturaSensor::class
    ],
    // Version 3: los datos ahora vienen de los microservicios (Room es la copia local).
    version = 3,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class HuertoDatabase : RoomDatabase() {

    abstract fun usuarioDao(): UsuarioDao
    abstract fun sectorDao(): SectorDao
    abstract fun cultivoDao(): CultivoDao
    abstract fun tareaDao(): TareaDao
    abstract fun evidenciaDao(): EvidenciaDao
    abstract fun validacionDao(): ValidacionDao
    abstract fun reporteDao(): ReporteDao
    abstract fun sensorDao(): SensorDao
    abstract fun sincronizacionDao(): SincronizacionDao

    companion object {

        @Volatile
        private var instancia: HuertoDatabase? = null

        fun obtener(context: Context): HuertoDatabase {
            return instancia ?: synchronized(this) {
                val nueva = Room.databaseBuilder(
                    context.applicationContext,
                    HuertoDatabase::class.java,
                    "huerto_escolar.db"
                )
                    // Proyecto academico: si cambia el esquema se recrea la base.
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                instancia = nueva
                nueva
            }
        }
    }
}
