package cl.duoc.huertoescolar.domain.validacion

/**
 * Resultado de validar un formulario: campo -> mensaje de error.
 * Si el mapa esta vacio, el formulario es valido.
 */
typealias ErroresFormulario = Map<String, String>

/**
 * Ayuda a armar el mapa de errores dentro de cada validador:
 * se le pasa el resultado de una regla (null = sin error) y solo lo agrega
 * si hay un mensaje.
 */
class ConstructorDeErrores {

    private val errores = linkedMapOf<String, String>()

    fun agregar(campo: String, error: String?) {
        if (error != null && campo !in errores) errores[campo] = error
    }

    fun resultado(): ErroresFormulario = errores.toMap()
}

/** Atajo para escribir un validador: errores { agregar(...) }. */
inline fun errores(bloque: ConstructorDeErrores.() -> Unit): ErroresFormulario =
    ConstructorDeErrores().apply(bloque).resultado()

/**
 * Validacion en vivo: actualiza el error de un solo campo mientras el usuario
 * escribe. Si la regla ya no da error, el mensaje desaparece de la pantalla;
 * si sigue fallando, se muestra el mensaje nuevo.
 */
fun ErroresFormulario.conCampo(campo: String, error: String?): ErroresFormulario =
    if (error == null) this - campo else this + (campo to error)

/**
 * Validacion en vivo despues del primer intento de guardar.
 *
 * Mientras el usuario no haya intentado guardar, no se le muestran errores
 * (seria molesto ver "obligatorio" antes de escribir). Pero si el campo YA
 * tiene un error en pantalla, se vuelve a validar con cada tecla: el mensaje
 * cambia si sigue mal y desaparece en cuanto el valor queda correcto.
 */
fun ErroresFormulario.revalidar(campo: String, error: String?): ErroresFormulario =
    if (campo in this) conCampo(campo, error) else this
