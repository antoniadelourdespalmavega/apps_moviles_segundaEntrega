package cl.duoc.huertoescolar.domain

/** Resultado de una operacion de negocio, para informar a la pantalla. */
sealed class Operacion {
    /** idGenerado: id del registro recien creado (0 si no aplica). */
    data class Exito(val mensaje: String, val idGenerado: Long = 0) : Operacion()
    data class Error(val mensaje: String) : Operacion()
}
