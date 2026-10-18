package cl.duoc.huertoescolar.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import cl.duoc.huertoescolar.data.local.entity.Evidencia
import cl.duoc.huertoescolar.data.local.query.EvidenciaDetalle
import kotlinx.coroutines.flow.Flow

@Dao
interface EvidenciaDao {

    @Query(
        """
        SELECT e.*, u.nombre AS usuarioNombre
        FROM evidencias e
        INNER JOIN usuarios u ON u.id = e.usuarioId
        WHERE e.tareaId = :tareaId
        ORDER BY e.fechaRegistro DESC
        """
    )
    fun observarPorTarea(tareaId: Long): Flow<List<EvidenciaDetalle>>

    @Query(
        """
        SELECT e.*, u.nombre AS usuarioNombre
        FROM evidencias e
        INNER JOIN usuarios u ON u.id = e.usuarioId
        ORDER BY e.fechaRegistro DESC
        """
    )
    fun observarTodas(): Flow<List<EvidenciaDetalle>>

    @Insert
    suspend fun insertar(evidencia: Evidencia): Long

    @Query("DELETE FROM evidencias WHERE id = :id")
    suspend fun eliminarPorId(id: Long)
}
