package cl.duoc.huertoescolar.data.repository

import cl.duoc.huertoescolar.data.local.dao.DatosActividades
import cl.duoc.huertoescolar.data.local.dao.DatosHuerto
import cl.duoc.huertoescolar.data.local.dao.SincronizacionDao
import cl.duoc.huertoescolar.data.remote.aEntidad
import cl.duoc.huertoescolar.data.remote.api.ActividadesApi
import cl.duoc.huertoescolar.data.remote.api.HuertoApi
import cl.duoc.huertoescolar.data.remote.api.UsuariosApi
import cl.duoc.huertoescolar.data.remote.llamarApi
import cl.duoc.huertoescolar.domain.Operacion
import cl.duoc.huertoescolar.domain.Resultado

/**
 * Trae los datos del microservicio y actualiza la copia local (Room).
 *
 * El microservicio es la fuente de la verdad. Room funciona como copia
 * local: las pantallas leen de ahi, asi se redibujan solas cuando llegan datos
 * nuevos y siguen mostrando la ultima informacion si se corta la conexion.
 */
class SincronizacionRepository(
    private val usuariosApi: UsuariosApi,
    private val huertoApi: HuertoApi,
    private val actividadesApi: ActividadesApi,
    private val dao: SincronizacionDao
) {

    suspend fun sincronizarUsuarios(): Operacion = aOperacion(
        llamarApi {
            dao.aplicarUsuarios(usuariosApi.listar().map { it.aEntidad() })
        }
    )

    suspend fun sincronizarHuerto(): Operacion = aOperacion(
        llamarApi {
            val datos = DatosHuerto(
                sectores = huertoApi.listarSectores().map { it.aEntidad() },
                cultivos = huertoApi.listarCultivos().map { it.aEntidad() },
                lecturas = huertoApi.listarLecturas().map { it.aEntidad() }
            )
            dao.aplicarHuerto(FiltroSincronizacion.huerto(datos, dao.idsUsuarios().toSet()))
        }
    )

    suspend fun sincronizarActividades(): Operacion = aOperacion(
        llamarApi {
            val datos = DatosActividades(
                tareas = actividadesApi.listarTareas().map { it.aEntidad() },
                evidencias = actividadesApi.listarEvidencias().map { it.aEntidad() },
                validaciones = actividadesApi.listarValidaciones().map { it.aEntidad() },
                reportes = actividadesApi.listarReportes().map { it.aEntidad() }
            )
            dao.aplicarActividades(
                FiltroSincronizacion.actividades(
                    datos = datos,
                    idsUsuarios = dao.idsUsuarios().toSet(),
                    idsSectores = dao.idsSectores().toSet(),
                    idsCultivos = dao.idsCultivos().toSet()
                )
            )
        }
    )

    /**
     * Sincroniza todo en orden: usuarios, huerto y actividades (las tareas
     * necesitan que ya existan sus usuarios y sectores). Se detiene en el
     * primer error y lo informa.
     */
    suspend fun sincronizarTodo(): Operacion {
        for (paso in listOf(::sincronizarUsuarios, ::sincronizarHuerto, ::sincronizarActividades)) {
            val resultado = paso()
            if (resultado is Operacion.Error) return resultado
        }
        return Operacion.Exito("Datos sincronizados con el microservicio")
    }

    private fun aOperacion(resultado: Resultado<Unit>): Operacion = when (resultado) {
        is Resultado.Exito -> Operacion.Exito("Datos actualizados")
        is Resultado.Error -> Operacion.Error(resultado.mensaje)
    }
}
