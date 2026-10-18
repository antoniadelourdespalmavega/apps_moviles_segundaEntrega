package cl.duoc.huertoescolar.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import cl.duoc.huertoescolar.data.local.entity.Usuario
import cl.duoc.huertoescolar.domain.Rol
import kotlinx.coroutines.flow.Flow

@Dao
interface UsuarioDao {

    /** Permite ingresar con el nombre de usuario o con el correo. */
    @Query(
        """
        SELECT * FROM usuarios
        WHERE (nombreUsuario = :identificador OR LOWER(correo) = :identificador)
          AND clave = :clave AND activo = 1
        LIMIT 1
        """
    )
    suspend fun autenticar(identificador: String, clave: String): Usuario?

    @Query("SELECT COUNT(*) FROM usuarios WHERE nombreUsuario = :nombreUsuario")
    suspend fun contarNombreUsuario(nombreUsuario: String): Int

    @Query("SELECT COUNT(*) FROM usuarios WHERE LOWER(correo) = :correo AND id != :excluirId")
    suspend fun contarCorreo(correo: String, excluirId: Long = 0): Int

    @Query("SELECT * FROM usuarios WHERE id = :id LIMIT 1")
    suspend fun porId(id: Long): Usuario?

    @Query("SELECT * FROM usuarios WHERE rol = :rol AND activo = 1 ORDER BY nombre")
    fun observarPorRol(rol: Rol): Flow<List<Usuario>>

    @Query("SELECT * FROM usuarios ORDER BY rol, nombre")
    fun observarTodos(): Flow<List<Usuario>>

    @Query("SELECT COUNT(*) FROM usuarios")
    suspend fun contar(): Int

    @Query("SELECT COUNT(*) FROM usuarios WHERE id = :id AND activo = 1 AND rol = :rol")
    suspend fun existeConRol(id: Long, rol: Rol): Int

    @Insert
    suspend fun insertar(usuario: Usuario): Long

    @Update
    suspend fun actualizar(usuario: Usuario)
}
