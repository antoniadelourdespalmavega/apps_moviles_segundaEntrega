package cl.duoc.huertoescolar.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import cl.duoc.huertoescolar.data.local.entity.Cultivo
import cl.duoc.huertoescolar.data.local.query.CultivoDetalle
import kotlinx.coroutines.flow.Flow

@Dao
interface CultivoDao {

    @Query(
        """
        SELECT c.*, s.nombre AS sectorNombre
        FROM cultivos c
        INNER JOIN sectores s ON s.id = c.sectorId
        ORDER BY c.nombre
        """
    )
    fun observarDetalle(): Flow<List<CultivoDetalle>>

    @Query("SELECT * FROM cultivos WHERE sectorId = :sectorId ORDER BY nombre")
    fun observarPorSector(sectorId: Long): Flow<List<Cultivo>>

    @Query("SELECT * FROM cultivos ORDER BY nombre")
    fun observarTodos(): Flow<List<Cultivo>>

    @Query("SELECT * FROM cultivos WHERE id = :id LIMIT 1")
    fun observarPorId(id: Long): Flow<Cultivo?>

    @Query("SELECT * FROM cultivos WHERE id = :id LIMIT 1")
    suspend fun porId(id: Long): Cultivo?

    @Query("SELECT COUNT(*) FROM cultivos")
    suspend fun contar(): Int

    @Insert
    suspend fun insertar(cultivo: Cultivo): Long

    @Update
    suspend fun actualizar(cultivo: Cultivo)

    @Delete
    suspend fun eliminar(cultivo: Cultivo)
}
