package cl.duoc.huertoescolar.domain

import cl.duoc.huertoescolar.core.Fechas
import cl.duoc.huertoescolar.data.local.entity.Cultivo
import cl.duoc.huertoescolar.data.local.query.TareaDetalle

/** Tipo de aviso, usado para elegir el color semantico en la UI. */
enum class NivelAlerta { INFORMACION, ADVERTENCIA, ERROR }

data class Alerta(
    val titulo: String,
    val detalle: String,
    val nivel: NivelAlerta
)

/**
 * Calculo de recordatorios y avisos del huerto.
 * Es logica de negocio pura: recibe los datos y devuelve la lista de avisos.
 * Los recordatorios se muestran solo al estudiante, sobre sus propias tareas.
 */
object Alertas {

    /** Una tarea esta atrasada si su fecha programada ya paso y aun no se entrega. */
    fun estaAtrasada(detalle: TareaDetalle, hoy: Long = Fechas.hoy()): Boolean {
        val estado = detalle.tarea.estado
        val abierta = estado == EstadoTarea.PENDIENTE ||
            estado == EstadoTarea.EN_DESARROLLO ||
            estado == EstadoTarea.RECHAZADA
        return abierta && Fechas.inicioDelDia(detalle.tarea.fechaProgramada) < hoy
    }

    /**
     * Categoria unica de una tarea. Las atrasadas se separan primero, de modo
     * que una tarea atrasada no se cuenta tambien como pendiente o rechazada.
     */
    fun categoria(detalle: TareaDetalle, hoy: Long = Fechas.hoy()): CategoriaTarea {
        if (estaAtrasada(detalle, hoy)) return CategoriaTarea.ATRASADA
        return when (detalle.tarea.estado) {
            EstadoTarea.PENDIENTE, EstadoTarea.EN_DESARROLLO -> CategoriaTarea.PENDIENTE
            EstadoTarea.PENDIENTE_VALIDACION -> CategoriaTarea.POR_VALIDAR
            EstadoTarea.VALIDADA -> CategoriaTarea.VALIDADA
            EstadoTarea.RECHAZADA -> CategoriaTarea.RECHAZADA
        }
    }

    /** Cantidad de tareas por categoria (todas las categorias, aunque sea 0). */
    fun contarPorCategoria(
        tareas: List<TareaDetalle>,
        hoy: Long = Fechas.hoy()
    ): Map<CategoriaTarea, Int> {
        val conteo = tareas.groupingBy { categoria(it, hoy) }.eachCount()
        return CategoriaTarea.entries.associateWith { conteo[it] ?: 0 }
    }

    fun proximaFechaRiego(cultivo: Cultivo, ultimoRiego: Long?): Long {
        val base = ultimoRiego ?: cultivo.fechaSiembra
        return Fechas.sumarDias(base, cultivo.frecuenciaRiegoDias)
    }

    fun calcular(
        tareas: List<TareaDetalle>,
        cultivos: List<Cultivo>,
        hoy: Long = Fechas.hoy()
    ): List<Alerta> {
        val avisos = mutableListOf<Alerta>()

        val atrasadas = tareas.filter { estaAtrasada(it, hoy) }
        if (atrasadas.isNotEmpty()) {
            avisos.add(
                Alerta(
                    titulo = "Tareas atrasadas: " + atrasadas.size,
                    detalle = atrasadas.take(3).joinToString(", ") { it.tarea.descripcion },
                    nivel = NivelAlerta.ERROR
                )
            )
        }

        // Riegos y mantenimientos tienen su propio aviso mas abajo.
        val proximas = tareas.filter {
            it.tarea.tipo != TipoTarea.RIEGO && it.tarea.tipo != TipoTarea.MANTENIMIENTO &&
                (it.tarea.estado == EstadoTarea.PENDIENTE || it.tarea.estado == EstadoTarea.EN_DESARROLLO) &&
                Fechas.diasEntre(hoy, it.tarea.fechaProgramada) in 0..3
        }
        if (proximas.isNotEmpty()) {
            avisos.add(
                Alerta(
                    titulo = "Tareas para los proximos dias: " + proximas.size,
                    detalle = proximas.take(3).joinToString(", ") {
                        it.tarea.descripcion + " (" + Fechas.formatear(it.tarea.fechaProgramada) + ")"
                    },
                    nivel = NivelAlerta.INFORMACION
                )
            )
        }

        val porCorregir = tareas.filter {
            it.tarea.estado == EstadoTarea.RECHAZADA && !estaAtrasada(it, hoy)
        }
        if (porCorregir.isNotEmpty()) {
            avisos.add(
                Alerta(
                    titulo = "Tareas para corregir y reenviar: " + porCorregir.size,
                    detalle = porCorregir.take(3).joinToString(", ") { it.tarea.descripcion },
                    nivel = NivelAlerta.ADVERTENCIA
                )
            )
        }

        val riegosProximos = tareas.filter {
            it.tarea.tipo == TipoTarea.RIEGO &&
                it.tarea.estado == EstadoTarea.PENDIENTE &&
                Fechas.diasEntre(hoy, it.tarea.fechaProgramada) in 0..2
        }
        if (riegosProximos.isNotEmpty()) {
            avisos.add(
                Alerta(
                    titulo = "Riegos programados: " + riegosProximos.size,
                    detalle = riegosProximos.joinToString(", ") {
                        it.sectorNombre + " (" + Fechas.formatear(it.tarea.fechaProgramada) + ")"
                    },
                    nivel = NivelAlerta.INFORMACION
                )
            )
        }

        val mantenimientos = tareas.filter {
            it.tarea.tipo == TipoTarea.MANTENIMIENTO &&
                it.tarea.estado != EstadoTarea.VALIDADA &&
                Fechas.diasEntre(hoy, it.tarea.fechaProgramada) in 0..7
        }
        if (mantenimientos.isNotEmpty()) {
            avisos.add(
                Alerta(
                    titulo = "Mantenimientos programados: " + mantenimientos.size,
                    detalle = mantenimientos.joinToString(", ") {
                        it.sectorNombre + " (" + Fechas.formatear(it.tarea.fechaProgramada) + ")"
                    },
                    nivel = NivelAlerta.ADVERTENCIA
                )
            )
        }

        val porValidar = tareas.filter { it.tarea.estado == EstadoTarea.PENDIENTE_VALIDACION }
        if (porValidar.isNotEmpty()) {
            avisos.add(
                Alerta(
                    titulo = "Entregas esperando revision: " + porValidar.size,
                    detalle = porValidar.joinToString(", ") { it.tarea.descripcion },
                    nivel = NivelAlerta.ADVERTENCIA
                )
            )
        }

        val cosechas = cultivos.filter {
            it.estado != EstadoCultivo.COSECHADO &&
                it.estado != EstadoCultivo.FINALIZADO &&
                Fechas.diasEntre(hoy, it.fechaCosechaEstimada) in 0..14
        }
        if (cosechas.isNotEmpty()) {
            avisos.add(
                Alerta(
                    titulo = "Proximas cosechas: " + cosechas.size,
                    detalle = cosechas.joinToString(", ") {
                        it.nombre + " (" + Fechas.formatear(it.fechaCosechaEstimada) + ")"
                    },
                    nivel = NivelAlerta.INFORMACION
                )
            )
        }

        return avisos
    }
}
