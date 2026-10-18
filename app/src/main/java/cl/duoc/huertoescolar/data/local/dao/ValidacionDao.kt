package cl.duoc.huertoescolar.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import cl.duoc.huertoescolar.data.local.query.ValidacionDetalle
import kotlinx.coroutines.flow.Flow

@Dao
interface ValidacionDao {

    @Query(
        """
        SELECT v.*, u.nombre AS validadorNombre, t.descripcion AS tareaDescripcion,
               t.responsableId AS tareaResponsableId
        FROM validaciones v
        INNER JOIN usuarios u ON u.id = v.validadorId
        INNER JOIN tareas t ON t.id = v.tareaId
        ORDER BY v.fechaValidacion DESC
        """
    )
    fun observarTodas(): Flow<List<ValidacionDetalle>>

    @Query(
        """
        SELECT v.*, u.nombre AS validadorNombre, t.descripcion AS tareaDescripcion,
               t.responsableId AS tareaResponsableId
        FROM validaciones v
        INNER JOIN usuarios u ON u.id = v.validadorId
        INNER JOIN tareas t ON t.id = v.tareaId
        WHERE v.tareaId = :tareaId
        ORDER BY v.fechaValidacion DESC
        """
    )
    fun observarPorTarea(tareaId: Long): Flow<List<ValidacionDetalle>>

    @Insert
    suspend fun insertar(validacion: cl.duoc.huertoescolar.data.local.entity.Validacion): Long
}
