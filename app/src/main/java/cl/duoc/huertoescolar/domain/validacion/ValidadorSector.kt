package cl.duoc.huertoescolar.domain.validacion

/** Formulario de sectores del huerto (SectorFormScreen / SectoresViewModel). */
object ValidadorSector {

    private val REGEX_CODIGO = Regex("^[A-Z0-9-]{3,10}$")

    /** Codigo del sector, por ejemplo S-01. Se compara en mayusculas. */
    fun validarCodigo(codigo: String): String? {
        val limpio = codigo.trim().uppercase()
        return when {
            limpio.isEmpty() -> "El código es obligatorio"
            !REGEX_CODIGO.matches(limpio) -> "Usa entre 3 y 10 caracteres: letras, números o guion"
            else -> null
        }
    }

    fun validarNombre(nombre: String): String? =
        ReglasComunes.textoObligatorio(nombre, "El nombre", 3, 60)

    fun validarDescripcion(descripcion: String): String? =
        ReglasComunes.textoObligatorio(descripcion, "La descripción", 5, 200)

    fun validarUbicacion(ubicacion: String): String? =
        ReglasComunes.textoObligatorio(ubicacion, "La ubicación", 3, 80)

    fun validar(
        codigo: String,
        nombre: String,
        descripcion: String,
        ubicacion: String
    ): ErroresFormulario = errores {
        agregar(Campos.CODIGO, validarCodigo(codigo))
        agregar(Campos.NOMBRE, validarNombre(nombre))
        agregar(Campos.DESCRIPCION, validarDescripcion(descripcion))
        agregar(Campos.UBICACION, validarUbicacion(ubicacion))
    }
}
