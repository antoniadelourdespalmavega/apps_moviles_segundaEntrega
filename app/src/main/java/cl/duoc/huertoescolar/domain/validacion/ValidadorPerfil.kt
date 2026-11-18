package cl.duoc.huertoescolar.domain.validacion

/**
 * Edicion del perfil propio (PerfilScreen / PerfilViewModel).
 * La clave es opcional: solo se valida si se escribe una nueva, y en ese
 * caso se exige la clave actual.
 */
object ValidadorPerfil {

    fun validar(
        nombre: String,
        correo: String,
        taller: String,
        claveActual: String,
        claveNueva: String,
        confirmar: String
    ): ErroresFormulario = errores {
        agregar(Campos.NOMBRE, ReglasComunes.nombrePersona(nombre))
        agregar(Campos.CORREO, ReglasComunes.correo(correo))
        agregar(Campos.TALLER, ValidadorRegistro.validarTaller(taller))
        if (claveNueva.isNotEmpty() || claveActual.isNotEmpty()) {
            if (claveActual.isEmpty()) agregar(Campos.CLAVE_ACTUAL, "Ingresa tu contraseña actual")
            agregar(Campos.CLAVE, ReglasComunes.clave(claveNueva))
            agregar(Campos.CONFIRMAR, ReglasComunes.confirmacion(claveNueva, confirmar))
        }
    }
}
