package cl.duoc.huertoescolar.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import cl.duoc.huertoescolar.data.local.entity.Sector
import cl.duoc.huertoescolar.data.local.query.SectorResumen
import kotlinx.coroutines.flow.Flow

@Dao
interface SectorDao {

    @Query(
        """
        SELECT s.*,
               u.nombre AS responsableNombre,
               (SELECT COUNT(*) FROM cultivos c WHERE c.sectorId = s.id) AS totalCultivos,
               (SELECT COUNT(*) FROM tareas t WHERE t.sectorId = s.id
                 AND t.estado IN ('PENDIENTE', 'EN_DESARROLLO', 'PENDIENTE_VALIDACION')) AS tareasAbiertas
        FROM sectores s
        LEFT JOIN usuarios u ON u.id = s.responsableId
        ORDER BY s.nombre
        """
    )
    fun observarResumen(): Flow<List<SectorResumen>>

    @Query("SELECT * FROM sectores ORDER BY nombre")
    fun observarTodos(): Flow<List<Sector>>

    @Query("SELECT * FROM sectores WHERE id = :id LIMIT 1")
    fun observarPorId(id: Long): Flow<Sector?>

    @Query("SELECT * FROM sectores WHERE id = :id LIMIT 1")
    suspend fun porId(id: Long): Sector?

    @Query("SELECT COUNT(*) FROM sectores WHERE codigo = :codigo AND id <> :idActual")
    suspend fun existeCodigo(codigo: String, idActual: Long): Int

    @Query("SELECT COUNT(*) FROM sectores")
    suspend fun contar(): Int

    @Insert
    suspend fun insertar(sector: Sector): Long

    @Update
    suspend fun actualizar(sector: Sector)

    @Delete
    suspend fun eliminar(sector: Sector)
}
