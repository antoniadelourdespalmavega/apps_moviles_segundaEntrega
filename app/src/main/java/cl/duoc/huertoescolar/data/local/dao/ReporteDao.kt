package cl.duoc.huertoescolar.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import cl.duoc.huertoescolar.data.local.entity.ReporteProblema
import cl.duoc.huertoescolar.data.local.query.ReporteDetalle
import kotlinx.coroutines.flow.Flow

@Dao
interface ReporteDao {

    @Query(
        """
        SELECT r.*,
               s.nombre AS sectorNombre,
               c.nombre AS cultivoNombre,
               u.nombre AS reportanteNombre
        FROM reportes_problema r
        INNER JOIN sectores s ON s.id = r.sectorId
        LEFT JOIN cultivos c ON c.id = r.cultivoId
        INNER JOIN usuarios u ON u.id = r.reportadoPorId
        ORDER BY r.fechaReporte DESC
        """
    )
    fun observarTodos(): Flow<List<ReporteDetalle>>

    @Query(
        """
        SELECT r.*,
               s.nombre AS sectorNombre,
               c.nombre AS cultivoNombre,
               u.nombre AS reportanteNombre
        FROM reportes_problema r
        INNER JOIN sectores s ON s.id = r.sectorId
        LEFT JOIN cultivos c ON c.id = r.cultivoId
        INNER JOIN usuarios u ON u.id = r.reportadoPorId
        WHERE r.reportadoPorId = :usuarioId
        ORDER BY r.fechaReporte DESC
        """
    )
    fun observarPorUsuario(usuarioId: Long): Flow<List<ReporteDetalle>>

    @Query("SELECT * FROM reportes_problema WHERE id = :id LIMIT 1")
    suspend fun porId(id: Long): ReporteProblema?

    @Query("SELECT COUNT(*) FROM reportes_problema")
    suspend fun contar(): Int

    @Insert
    suspend fun insertar(reporte: ReporteProblema): Long

    @Update
    suspend fun actualizar(reporte: ReporteProblema)
}
