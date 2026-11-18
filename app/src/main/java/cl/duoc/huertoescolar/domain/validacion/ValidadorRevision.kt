package cl.duoc.huertoescolar.domain.validacion

/**
 * Revision docente de una entrega (RevisionScreen y TareaDetalleScreen).
 * Validar no pide datos; rechazar exige explicar el motivo al estudiante.
 */
object ValidadorRevision {

    fun validarMotivo(motivo: String): String? =
        ReglasComunes.textoObligatorio(motivo, "El motivo del rechazo", 5, 300)

    fun validarRechazo(motivo: String): ErroresFormulario = errores {
        agregar(Campos.MOTIVO, validarMotivo(motivo))
    }
}
