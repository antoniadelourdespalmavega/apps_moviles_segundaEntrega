package cl.duoc.huertoescolar.domain.validacion

import cl.duoc.huertoescolar.domain.Cargo
import cl.duoc.huertoescolar.domain.Rol

/** Formulario de creacion de cuenta (RegistroScreen / RegistroViewModel). */
object ValidadorRegistro {

    /**
     * Codigo que entrega la escuela para registrar cuentas de docente o
     * tallerista. Sin el, cualquiera podria darse permisos de administrador.
     */
    const val CODIGO_AUTORIZACION_ADMIN = "HUERTO2026"

    fun validarTaller(taller: String): String? =
        ReglasComunes.textoObligatorio(taller, "El taller", minimo = 2, maximo = 40)

    fun validarCargo(cargo: Cargo?): String? =
        ReglasComunes.seleccionObligatoria(cargo, "Selecciona tu cargo")

    /** Solo docentes y talleristas (rol administrador) necesitan el codigo. */
    fun validarCodigoAutorizacion(cargo: Cargo?, codigo: String): String? =
        if (cargo != null && cargo.rol == Rol.ADMINISTRADOR &&
            codigo.trim() != CODIGO_AUTORIZACION_ADMIN
        ) {
            "Código de autorización incorrecto"
        } else {
            null
        }

    fun validar(
        nombre: String,
        correo: String,
        usuario: String,
        clave: String,
        confirmar: String,
        cargo: Cargo?,
        taller: String,
        codigoAutorizacion: String
    ): ErroresFormulario = errores {
        agregar(Campos.NOMBRE, ReglasComunes.nombrePersona(nombre))
        agregar(Campos.CORREO, ReglasComunes.correo(correo))
        agregar(Campos.USUARIO, ReglasComunes.nombreUsuario(usuario))
        agregar(Campos.CLAVE, ReglasComunes.clave(clave))
        agregar(Campos.CONFIRMAR, ReglasComunes.confirmacion(clave, confirmar))
        agregar(Campos.CARGO, validarCargo(cargo))
        agregar(Campos.TALLER, validarTaller(taller))
        agregar(Campos.CODIGO_AUTORIZACION, validarCodigoAutorizacion(cargo, codigoAutorizacion))
    }
}
