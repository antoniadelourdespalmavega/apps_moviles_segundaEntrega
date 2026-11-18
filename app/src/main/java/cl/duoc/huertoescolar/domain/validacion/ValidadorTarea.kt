package cl.duoc.huertoescolar.domain.validacion

import cl.duoc.huertoescolar.core.Fechas
import cl.duoc.huertoescolar.domain.TipoTarea

/** Formulario para crear o editar tareas (TareaFormScreen / TareaFormViewModel). */
object ValidadorTarea {

    /** Una tarea nueva no puede programarse en el pasado. */
    fun validarFechaProgramada(texto: String, esNueva: Boolean): String? {
        val fecha = Fechas.parsear(texto)
        return when {
            fecha == null -> "Fecha programada inválida (dd/mm/aaaa)"
            esNueva && fecha < Fechas.hoy() -> "Una tarea nueva no puede programarse en el pasado"
            else -> null
        }
    }

    fun validarDescripcion(descripcion: String): String? =
        ReglasComunes.textoObligatorio(descripcion, "La descripción", 5, 120)

    fun validarInstrucciones(instrucciones: String): String? =
        ReglasComunes.textoObligatorio(instrucciones, "Las instrucciones", 5, 300)

    fun validar(
        descripcion: String,
        instrucciones: String,
        tipo: TipoTarea?,
        sectorId: Long?,
        responsableId: Long?,
        fechaProgramada: String,
        esNueva: Boolean = false
    ): ErroresFormulario = errores {
        agregar(Campos.DESCRIPCION, validarDescripcion(descripcion))
        agregar(Campos.INSTRUCCIONES, validarInstrucciones(instrucciones))
        agregar(Campos.TIPO, ReglasComunes.seleccionObligatoria(tipo, "Selecciona el tipo de tarea"))
        agregar(Campos.SECTOR, ReglasComunes.seleccionObligatoria(sectorId, "Selecciona el sector"))
        agregar(
            Campos.RESPONSABLE,
            ReglasComunes.seleccionObligatoria(responsableId, "Asigna un estudiante responsable")
        )
        agregar(Campos.FECHA_PROGRAMADA, validarFechaProgramada(fechaProgramada, esNueva))
    }
}
