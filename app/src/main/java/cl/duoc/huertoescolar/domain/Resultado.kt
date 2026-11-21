package cl.duoc.huertoescolar.domain

/**
 * Resultado de una operacion que devuelve un dato (por ejemplo, el usuario al
 * iniciar sesion o el clima). Exito trae el dato; Error trae un mensaje listo
 * para mostrar en la pantalla.
 */
sealed interface Resultado<out T> {
    data class Exito<T>(val dato: T) : Resultado<T>
    data class Error(val mensaje: String) : Resultado<Nothing>
}
