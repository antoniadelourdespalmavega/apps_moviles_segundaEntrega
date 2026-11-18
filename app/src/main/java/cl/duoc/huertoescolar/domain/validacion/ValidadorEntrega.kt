package cl.duoc.huertoescolar.domain.validacion

/** Entrega de una tarea por el estudiante (TareaDetalleScreen / TareaDetalleViewModel). */
object ValidadorEntrega {

    fun validarComentario(comentario: String): String? =
        ReglasComunes.textoObligatorio(comentario, "El comentario", 5, 300)

    fun validarEvidencia(rutaEvidencia: String?): String? =
        if (rutaEvidencia.isNullOrBlank()) "Adjunta una fotografía del trabajo realizado" else null

    fun validar(comentario: String, rutaEvidencia: String?): ErroresFormulario = errores {
        agregar(Campos.COMENTARIO, validarComentario(comentario))
        agregar(Campos.EVIDENCIA, validarEvidencia(rutaEvidencia))
    }
}
