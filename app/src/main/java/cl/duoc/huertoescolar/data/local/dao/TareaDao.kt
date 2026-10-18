package cl.duoc.huertoescolar.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import cl.duoc.huertoescolar.data.local.entity.Tarea
import cl.duoc.huertoescolar.data.local.query.TareaDetalle
import kotlinx.coroutines.flow.Flow

@Dao
interface TareaDao {

    @Query(
        """
        SELECT t.*,
               s.nombre AS sectorNombre,
               c.nombre AS cultivoNombre,
               ur.nombre AS responsableNombre,
               uc.nombre AS creadorNombre
        FROM tareas t
        INNER JOIN sectores s ON s.id = t.sectorId
        LEFT JOIN cultivos c ON c.id = t.cultivoId
        INNER JOIN usuarios ur ON ur.id = t.responsableId
        INNER JOIN usuarios uc ON uc.id = t.creadaPorId
        ORDER BY t.fechaProgramada ASC, t.id ASC
        """
    )
    fun observarTodas(): Flow<List<TareaDetalle>>

    @Query(
        """
        SELECT t.*,
               s.nombre AS sectorNombre,
               c.nombre AS cultivoNombre,
               ur.nombre AS responsableNombre,
               uc.nombre AS creadorNombre
        FROM tareas t
        INNER JOIN sectores s ON s.id = t.sectorId
        LEFT JOIN cultivos c ON c.id = t.cultivoId
        INNER JOIN usuarios ur ON ur.id = t.responsableId
        INNER JOIN usuarios uc ON uc.id = t.creadaPorId
        WHERE t.responsableId = :usuarioId
        ORDER BY t.fechaProgramada ASC, t.id ASC
        """
    )
    fun observarPorResponsable(usuarioId: Long): Flow<List<TareaDetalle>>

    @Query(
        """
        SELECT t.*,
               s.nombre AS sectorNombre,
               c.nombre AS cultivoNombre,
               ur.nombre AS responsableNombre,
               uc.nombre AS creadorNombre
        FROM tareas t
        INNER JOIN sectores s ON s.id = t.sectorId
        LEFT JOIN cultivos c ON c.id = t.cultivoId
        INNER JOIN usuarios ur ON ur.id = t.responsableId
        INNER JOIN usuarios uc ON uc.id = t.creadaPorId
        WHERE t.id = :id
        LIMIT 1
        """
    )
    fun observarDetalle(id: Long): Flow<TareaDetalle?>

    @Query("SELECT * FROM tareas WHERE id = :id LIMIT 1")
    suspend fun porId(id: Long): Tarea?

    @Query("SELECT COUNT(*) FROM tareas WHERE sectorId = :sectorId")
    suspend fun contarPorSector(sectorId: Long): Int

    @Query("SELECT COUNT(*) FROM tareas WHERE cultivoId = :cultivoId")
    suspend fun contarPorCultivo(cultivoId: Long): Int

    @Query("SELECT COUNT(*) FROM tareas")
    suspend fun contar(): Int

    @Insert
    suspend fun insertar(tarea: Tarea): Long

    @Update
    suspend fun actualizar(tarea: Tarea)

    @Delete
    suspend fun eliminar(tarea: Tarea)
}
