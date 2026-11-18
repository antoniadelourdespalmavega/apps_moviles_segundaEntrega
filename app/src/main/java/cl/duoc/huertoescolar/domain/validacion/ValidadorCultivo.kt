package cl.duoc.huertoescolar.domain.validacion

import cl.duoc.huertoescolar.core.Fechas

/** Formulario de cultivos (CultivoFormScreen / CultivoFormViewModel). */
object ValidadorCultivo {

    fun validarNombre(nombre: String): String? =
        ReglasComunes.textoObligatorio(nombre, "El nombre del cultivo", 3, 60)

    fun validarDescripcion(descripcion: String): String? =
        ReglasComunes.textoObligatorio(descripcion, "La descripción", 5, 200)

    /** La cosecha estimada no puede quedar antes de la siembra. */
    fun validarCosecha(fechaSiembra: String, fechaCosecha: String): String? {
        val siembra = Fechas.parsear(fechaSiembra)
        val cosecha = Fechas.parsear(fechaCosecha)
        return when {
            cosecha == null -> ReglasComunes.fecha(fechaCosecha, "cosecha")
            siembra != null && cosecha < siembra ->
                "La cosecha estimada no puede ser anterior a la siembra"
            else -> null
        }
    }

    /** El trasplante es opcional, pero si se escribe debe caer entre siembra y cosecha. */
    fun validarTrasplante(fechaSiembra: String, fechaTrasplante: String, fechaCosecha: String): String? {
        if (fechaTrasplante.trim().isEmpty()) return null
        val trasplante = Fechas.parsear(fechaTrasplante)
            ?: return ReglasComunes.fecha(fechaTrasplante, "trasplante")
        val siembra = Fechas.parsear(fechaSiembra)
        val cosecha = Fechas.parsear(fechaCosecha)
        return when {
            siembra != null && trasplante < siembra -> "El trasplante no puede ser anterior a la siembra"
            cosecha != null && trasplante > cosecha -> "El trasplante no puede ser posterior a la cosecha"
            else -> null
        }
    }

    /** Frecuencia de riego en dias: numero entre 1 y 60. */
    fun validarFrecuenciaRiego(texto: String): String? {
        val dias = texto.trim().toIntOrNull()
        return when {
            dias == null -> "La frecuencia de riego debe ser un número"
            dias <= 0 -> "La frecuencia de riego debe ser mayor que cero"
            dias > 60 -> "La frecuencia de riego no puede superar los 60 días"
            else -> null
        }
    }

    fun validar(
        nombre: String,
        descripcion: String,
        sectorId: Long?,
        fechaSiembra: String,
        fechaTrasplante: String,
        fechaCosecha: String,
        frecuenciaRiego: String
    ): ErroresFormulario = errores {
        agregar(Campos.NOMBRE, validarNombre(nombre))
        agregar(Campos.DESCRIPCION, validarDescripcion(descripcion))
        agregar(
            Campos.SECTOR,
            ReglasComunes.seleccionObligatoria(sectorId, "Selecciona el sector donde está el cultivo")
        )
        agregar(Campos.SIEMBRA, ReglasComunes.fecha(fechaSiembra, "siembra"))
        agregar(Campos.COSECHA, validarCosecha(fechaSiembra, fechaCosecha))
        agregar(Campos.TRASPLANTE, validarTrasplante(fechaSiembra, fechaTrasplante, fechaCosecha))
        agregar(Campos.RIEGO, validarFrecuenciaRiego(frecuenciaRiego))
    }
}
