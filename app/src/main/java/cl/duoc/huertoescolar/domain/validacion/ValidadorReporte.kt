package cl.duoc.huertoescolar.domain.validacion

import cl.duoc.huertoescolar.domain.TipoProblema

/** Reporte de problemas del huerto (ProblemaFormScreen / ProblemasViewModel). */
object ValidadorReporte {

    fun validarDescripcion(descripcion: String): String? =
        ReglasComunes.textoObligatorio(descripcion, "La descripción del problema", 10, 300)

    fun validar(
        sectorId: Long?,
        tipo: TipoProblema?,
        descripcion: String
    ): ErroresFormulario = errores {
        agregar(Campos.SECTOR, ReglasComunes.seleccionObligatoria(sectorId, "Selecciona el sector afectado"))
        agregar(Campos.TIPO, ReglasComunes.seleccionObligatoria(tipo, "Selecciona el tipo de problema"))
        agregar(Campos.DESCRIPCION, validarDescripcion(descripcion))
    }
}
