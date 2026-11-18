package cl.duoc.huertoescolar.domain.validacion

import cl.duoc.huertoescolar.core.Fechas

/**
 * Reglas pequenas y reutilizables que comparten varios formularios
 * (texto obligatorio, correo, contrasena, fechas...).
 *
 * Cada regla revisa UN valor y devuelve el mensaje de error, o null si el
 * valor es correcto. Los validadores de cada formulario combinan estas reglas.
 * No conocen Compose ni Android, por eso se prueban con JUnit.
 */
object ReglasComunes {

    private val REGEX_CORREO =
        Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9-]+(\\.[A-Za-z0-9-]+)*\\.[A-Za-z]{2,}$")
    private val REGEX_NOMBRE = Regex("^[A-Za-zÀ-ÿ' -]+$")
    private val REGEX_USUARIO = Regex("^[a-z0-9._]{4,20}$")

    /** Texto obligatorio con largo minimo y maximo. */
    fun textoObligatorio(
        valor: String,
        etiqueta: String,
        minimo: Int = 3,
        maximo: Int = 120
    ): String? {
        val limpio = valor.trim()
        return when {
            limpio.isEmpty() -> "$etiqueta es obligatorio"
            limpio.length < minimo -> "$etiqueta debe tener al menos $minimo caracteres"
            limpio.length > maximo -> "$etiqueta no puede superar los $maximo caracteres"
            else -> null
        }
    }

    /** Seleccion obligatoria en un menu desplegable (sector, tipo, cargo...). */
    fun seleccionObligatoria(valor: Any?, mensaje: String): String? =
        if (valor == null) mensaje else null

    /** Nombre y apellido: solo letras y espacios, entre 5 y 60 caracteres. */
    fun nombrePersona(nombre: String): String? {
        val limpio = nombre.trim()
        return when {
            limpio.isEmpty() -> "El nombre es obligatorio"
            limpio.length < 5 -> "El nombre debe tener al menos 5 caracteres"
            limpio.length > 60 -> "El nombre no puede superar los 60 caracteres"
            !REGEX_NOMBRE.matches(limpio) -> "El nombre solo puede tener letras y espacios"
            limpio.split(Regex("\\s+")).size < 2 -> "Ingresa nombre y apellido"
            else -> null
        }
    }

    /** Correo con formato nombre@dominio.ext. */
    fun correo(correo: String): String? {
        val limpio = correo.trim()
        return when {
            limpio.isEmpty() -> "El correo es obligatorio"
            limpio.length > 80 -> "El correo no puede superar los 80 caracteres"
            !REGEX_CORREO.matches(limpio) -> "Correo inválido. Ejemplo: nombre@escuela.cl"
            else -> null
        }
    }

    /** Usuario: 4 a 20 caracteres en minuscula, numeros, punto o guion bajo. */
    fun nombreUsuario(usuario: String): String? {
        val limpio = usuario.trim()
        return when {
            limpio.isEmpty() -> "El usuario es obligatorio"
            !REGEX_USUARIO.matches(limpio) ->
                "Usa de 4 a 20 caracteres: minúsculas, números, punto o guion bajo"
            else -> null
        }
    }

    /** Clave: 8 a 30 caracteres, sin espacios, con al menos una letra y un numero. */
    fun clave(clave: String): String? = when {
        clave.isEmpty() -> "La contraseña es obligatoria"
        clave.length < 8 -> "La contraseña debe tener al menos 8 caracteres"
        clave.length > 30 -> "La contraseña no puede superar los 30 caracteres"
        clave.any { it.isWhitespace() } -> "La contraseña no puede tener espacios"
        clave.none { it.isLetter() } -> "La contraseña debe incluir al menos una letra"
        clave.none { it.isDigit() } -> "La contraseña debe incluir al menos un número"
        else -> null
    }

    /** La confirmacion debe ser igual a la contrasena escrita. */
    fun confirmacion(clave: String, confirmar: String): String? =
        if (confirmar != clave) "Las contraseñas no coinciden" else null

    /** Fecha con formato dd/mm/aaaa que corresponda a un dia real. */
    fun fecha(texto: String, nombre: String): String? =
        if (Fechas.parsear(texto) == null) "Fecha de $nombre inválida (dd/mm/aaaa)" else null
}
