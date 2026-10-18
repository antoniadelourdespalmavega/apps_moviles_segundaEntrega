package cl.duoc.huertoescolar.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import cl.duoc.huertoescolar.data.local.entity.LecturaSensor
import kotlinx.coroutines.flow.Flow

@Dao
interface SensorDao {

    @Query("SELECT * FROM lecturas_sensor ORDER BY fechaLectura DESC")
    fun observarTodas(): Flow<List<LecturaSensor>>

    @Query("SELECT DISTINCT sectorId FROM lecturas_sensor")
    suspend fun sectoresConLecturas(): List<Long>

    @Query("SELECT COUNT(*) FROM lecturas_sensor")
    suspend fun contar(): Int

    @Insert
    suspend fun insertar(lectura: LecturaSensor): Long

    @Insert
    suspend fun insertarVarias(lecturas: List<LecturaSensor>)
}
