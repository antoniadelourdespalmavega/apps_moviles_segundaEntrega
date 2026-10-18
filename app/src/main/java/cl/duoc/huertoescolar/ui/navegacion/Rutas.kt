package cl.duoc.huertoescolar.ui.navegacion

/** Rutas de navegacion de la aplicacion. */
object Rutas {
    const val LOGIN = "login"
    const val DASHBOARD = "dashboard"
    const val SECTORES = "sectores"
    const val SECTOR_DETALLE = "sector_detalle"
    const val SECTOR_FORM = "sector_form"
    const val CULTIVO_FORM = "cultivo_form"
    const val TAREAS = "tareas"
    const val TAREA_FORM = "tarea_form"
    const val TAREA_DETALLE = "tarea_detalle"
    const val PERFIL = "perfil"
    const val REVISION = "revision"
    const val CULTIVOS = "cultivos"
    const val PROBLEMAS = "problemas"
    const val PROBLEMA_FORM = "problema_form"
    const val HISTORIAL = "historial"
    const val SENSORES = "sensores"

    /** Listado de tareas, opcionalmente abierto con un filtro de categoria. */
    fun tareas(categoria: String? = null) =
        if (categoria == null) TAREAS else TAREAS + "?categoria=" + categoria

    fun sectorDetalle(sectorId: Long) = SECTOR_DETALLE + "/" + sectorId
    fun sectorForm(sectorId: Long) = SECTOR_FORM + "/" + sectorId
    fun cultivoForm(sectorId: Long, cultivoId: Long) = CULTIVO_FORM + "/" + sectorId + "/" + cultivoId
    fun tareaForm(tareaId: Long) = TAREA_FORM + "/" + tareaId
    fun tareaDetalle(tareaId: Long) = TAREA_DETALLE + "/" + tareaId
}
