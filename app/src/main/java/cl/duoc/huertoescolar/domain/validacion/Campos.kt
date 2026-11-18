package cl.duoc.huertoescolar.domain.validacion

/**
 * Nombres de los campos de los formularios.
 *
 * Cada validador devuelve un mapa campo -> mensaje de error usando estas
 * claves, y cada pantalla busca en ese mapa el error de su campo. Asi la
 * pantalla y el validador "hablan el mismo idioma" sin conocerse entre si.
 */
object Campos {
    const val USUARIO = "usuario"
    const val CLAVE = "clave"
    const val CLAVE_ACTUAL = "claveActual"
    const val CONFIRMAR = "confirmar"
    const val CORREO = "correo"
    const val NOMBRE = "nombre"
    const val TALLER = "taller"
    const val CARGO = "cargo"
    const val CODIGO_AUTORIZACION = "codigoAutorizacion"

    const val CODIGO = "codigo"
    const val DESCRIPCION = "descripcion"
    const val UBICACION = "ubicacion"
    const val SECTOR = "sector"

    const val SIEMBRA = "siembra"
    const val TRASPLANTE = "trasplante"
    const val COSECHA = "cosecha"
    const val RIEGO = "riego"

    const val TIPO = "tipo"
    const val INSTRUCCIONES = "instrucciones"
    const val RESPONSABLE = "responsable"
    const val FECHA_PROGRAMADA = "fechaProgramada"

    const val COMENTARIO = "comentario"
    const val EVIDENCIA = "evidencia"
    const val MOTIVO = "motivo"
}
