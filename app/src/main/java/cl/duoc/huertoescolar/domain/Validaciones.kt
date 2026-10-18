package cl.duoc.huertoescolar.domain

import cl.duoc.huertoescolar.core.Fechas

/**
 * Reglas de validacion de formularios.
 *
 * Son funciones puras: no conocen Compose ni Android. Cada una devuelve un mapa
 * campo -> mensaje de error. Si el mapa esta vacio, el formulario es valido.
 * De esta forma la validacion queda fuera de los Composables y se puede probar
 * con pruebas unitarias.
 */
object Validaciones {

    const val CAMPO_USUARIO = "usuario"
    const val CAMPO_CLAVE = "clave"
    const val CAMPO_CODIGO = "codigo"
    const val CAMPO_NOMBRE = "nombre"
    const val CAMPO_DESCRIPCION = "descripcion"
    const val CAMPO_UBICACION = "ubicacion"
    const val CAMPO_SECTOR = "sector"
    const val CAMPO_SIEMBRA = "siembra"
    const val CAMPO_TRASPLANTE = "trasplante"
    const val CAMPO_COSECHA = "cosecha"
    const val CAMPO_RIEGO = "riego"
    const val CAMPO_TIPO = "tipo"
    const val CAMPO_INSTRUCCIONES = "instrucciones"
    const val CAMPO_RESPONSABLE = "responsable"
    const val CAMPO_FECHA_PROGRAMADA = "fechaProgramada"
    const val CAMPO_COMENTARIO = "comentario"
    const val CAMPO_MOTIVO = "motivo"
    const val CAMPO_EVIDENCIA = "evidencia"
    const val CAMPO_CORREO = "correo"
    const val CAMPO_CONFIRMAR = "confirmar"
    const val CAMPO_CLAVE_ACTUAL = "claveActual"
    const val CAMPO_TALLER = "taller"
    const val CAMPO_CARGO = "cargo"
    const val CAMPO_CODIGO_AUTORIZACION = "codigoAutorizacion"

    /**
     * Codigo que entrega la escuela para registrar cuentas de docente o
     * tallerista. Sin el, cualquiera podria darse permisos de administrador.
     */
    const val CODIGO_AUTORIZACION_ADMIN = "HUERTO2026"

    private val REGEX_CORREO =
        Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9-]+(\\.[A-Za-z0-9-]+)*\\.[A-Za-z]{2,}$")
    private val REGEX_NOMBRE = Regex("^[A-Za-z\u00C0-\u00FF' -]+$")
    private val REGEX_USUARIO = Regex("^[a-z0-9._]{4,20}$")

    /** Nombre y apellido: solo letras y espacios, entre 5 y 60 caracteres. */
    fun validarNombre(nombre: String): String? {
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
    fun validarCorreo(correo: String): String? {
        val limpio = correo.trim()
        return when {
            limpio.isEmpty() -> "El correo es obligatorio"
            limpio.length > 80 -> "El correo no puede superar los 80 caracteres"
            !REGEX_CORREO.matches(limpio) -> "Correo invalido. Ejemplo: nombre@escuela.cl"
            else -> null
        }
    }

    /** Usuario: 4 a 20 caracteres en minuscula, numeros, punto o guion bajo. */
    fun validarNombreUsuario(usuario: String): String? {
        val limpio = usuario.trim()
        return when {
            limpio.isEmpty() -> "El usuario es obligatorio"
            !REGEX_USUARIO.matches(limpio) ->
                "Usa de 4 a 20 caracteres: minusculas, numeros, punto o guion bajo"
            else -> null
        }
    }

    /** Clave: 8 a 30 caracteres, sin espacios, con al menos una letra y un numero. */
    fun validarClave(clave: String): String? {
        return when {
            clave.isEmpty() -> "La contrasena es obligatoria"
            clave.length < 8 -> "La contrasena debe tener al menos 8 caracteres"
            clave.length > 30 -> "La contrasena no puede superar los 30 caracteres"
            clave.any { it.isWhitespace() } -> "La contrasena no puede tener espacios"
            clave.none { it.isLetter() } -> "La contrasena debe incluir al menos una letra"
            clave.none { it.isDigit() } -> "La contrasena debe incluir al menos un numero"
            else -> null
        }
    }

    private fun textoObligatorio(
        errores: MutableMap<String, String>,
        campo: String,
        valor: String,
        etiqueta: String,
        minimo: Int = 3,
        maximo: Int = 120
    ) {
        val limpio = valor.trim()
        when {
            limpio.isEmpty() -> errores[campo] = etiqueta + " es obligatorio"
            limpio.length < minimo -> errores[campo] = etiqueta + " debe tener al menos " + minimo + " caracteres"
            limpio.length > maximo -> errores[campo] = etiqueta + " no puede superar los " + maximo + " caracteres"
        }
    }

    fun login(usuario: String, clave: String): Map<String, String> {
        val errores = mutableMapOf<String, String>()
        val limpio = usuario.trim()
        if (limpio.isEmpty()) {
            errores[CAMPO_USUARIO] = "Ingresa tu usuario o correo"
        } else if (limpio.contains("@")) {
            validarCorreo(limpio)?.let { errores[CAMPO_USUARIO] = it }
        }
        if (clave.isEmpty()) errores[CAMPO_CLAVE] = "Ingresa tu contrasena"
        return errores
    }

    fun registro(
        nombre: String,
        correo: String,
        usuario: String,
        clave: String,
        confirmar: String,
        cargo: Cargo?,
        taller: String,
        codigoAutorizacion: String
    ): Map<String, String> {
        val errores = mutableMapOf<String, String>()
        validarNombre(nombre)?.let { errores[CAMPO_NOMBRE] = it }
        validarCorreo(correo)?.let { errores[CAMPO_CORREO] = it }
        validarNombreUsuario(usuario)?.let { errores[CAMPO_USUARIO] = it }
        validarClave(clave)?.let { errores[CAMPO_CLAVE] = it }
        if (confirmar != clave) errores[CAMPO_CONFIRMAR] = "Las contrasenas no coinciden"
        if (cargo == null) errores[CAMPO_CARGO] = "Selecciona tu cargo"
        textoObligatorio(errores, CAMPO_TALLER, taller, "El taller", minimo = 2, maximo = 40)
        if (cargo != null && cargo.rol == Rol.ADMINISTRADOR &&
            codigoAutorizacion.trim() != CODIGO_AUTORIZACION_ADMIN
        ) {
            errores[CAMPO_CODIGO_AUTORIZACION] = "Codigo de autorizacion incorrecto"
        }
        return errores
    }

    /**
     * Edicion del perfil propio. La clave es opcional: solo se valida si se
     * escribe una nueva, y en ese caso se exige la clave actual.
     */
    fun perfil(
        nombre: String,
        correo: String,
        taller: String,
        claveActual: String,
        claveNueva: String,
        confirmar: String
    ): Map<String, String> {
        val errores = mutableMapOf<String, String>()
        validarNombre(nombre)?.let { errores[CAMPO_NOMBRE] = it }
        validarCorreo(correo)?.let { errores[CAMPO_CORREO] = it }
        textoObligatorio(errores, CAMPO_TALLER, taller, "El taller", minimo = 2, maximo = 40)
        if (claveNueva.isNotEmpty() || claveActual.isNotEmpty()) {
            if (claveActual.isEmpty()) errores[CAMPO_CLAVE_ACTUAL] = "Ingresa tu contrasena actual"
            validarClave(claveNueva)?.let { errores[CAMPO_CLAVE] = it }
            if (confirmar != claveNueva) errores[CAMPO_CONFIRMAR] = "Las contrasenas no coinciden"
        }
        return errores
    }

    fun sector(
        codigo: String,
        nombre: String,
        descripcion: String,
        ubicacion: String
    ): Map<String, String> {
        val errores = mutableMapOf<String, String>()
        val codigoLimpio = codigo.trim().uppercase()
        if (codigoLimpio.isEmpty()) {
            errores[CAMPO_CODIGO] = "El codigo es obligatorio"
        } else if (!Regex("^[A-Z0-9-]{3,10}$").matches(codigoLimpio)) {
            errores[CAMPO_CODIGO] = "Usa entre 3 y 10 caracteres: letras, numeros o guion"
        }
        textoObligatorio(errores, CAMPO_NOMBRE, nombre, "El nombre", minimo = 3, maximo = 60)
        textoObligatorio(errores, CAMPO_DESCRIPCION, descripcion, "La descripcion", minimo = 5, maximo = 200)
        textoObligatorio(errores, CAMPO_UBICACION, ubicacion, "La ubicacion", minimo = 3, maximo = 80)
        return errores
    }

    fun cultivo(
        nombre: String,
        descripcion: String,
        sectorId: Long?,
        fechaSiembra: String,
        fechaTrasplante: String,
        fechaCosecha: String,
        frecuenciaRiego: String
    ): Map<String, String> {
        val errores = mutableMapOf<String, String>()
        textoObligatorio(errores, CAMPO_NOMBRE, nombre, "El nombre del cultivo", minimo = 3, maximo = 60)
        textoObligatorio(errores, CAMPO_DESCRIPCION, descripcion, "La descripcion", minimo = 5, maximo = 200)
        if (sectorId == null) errores[CAMPO_SECTOR] = "Selecciona el sector donde esta el cultivo"

        val siembra = Fechas.parsear(fechaSiembra)
        if (siembra == null) {
            errores[CAMPO_SIEMBRA] = "Fecha de siembra invalida (dd/mm/aaaa)"
        }

        val cosecha = Fechas.parsear(fechaCosecha)
        if (cosecha == null) {
            errores[CAMPO_COSECHA] = "Fecha de cosecha invalida (dd/mm/aaaa)"
        } else if (siembra != null && cosecha < siembra) {
            errores[CAMPO_COSECHA] = "La cosecha estimada no puede ser anterior a la siembra"
        }

        if (fechaTrasplante.trim().isNotEmpty()) {
            val trasplante = Fechas.parsear(fechaTrasplante)
            if (trasplante == null) {
                errores[CAMPO_TRASPLANTE] = "Fecha de trasplante invalida (dd/mm/aaaa)"
            } else if (siembra != null && trasplante < siembra) {
                errores[CAMPO_TRASPLANTE] = "El trasplante no puede ser anterior a la siembra"
            } else if (cosecha != null && trasplante > cosecha) {
                errores[CAMPO_TRASPLANTE] = "El trasplante no puede ser posterior a la cosecha"
            }
        }

        val riego = frecuenciaRiego.trim().toIntOrNull()
        if (riego == null) {
            errores[CAMPO_RIEGO] = "La frecuencia de riego debe ser un numero"
        } else if (riego <= 0) {
            errores[CAMPO_RIEGO] = "La frecuencia de riego debe ser mayor que cero"
        } else if (riego > 60) {
            errores[CAMPO_RIEGO] = "La frecuencia de riego no puede superar los 60 dias"
        }
        return errores
    }

    fun tarea(
        descripcion: String,
        instrucciones: String,
        tipo: TipoTarea?,
        sectorId: Long?,
        responsableId: Long?,
        fechaProgramada: String,
        esNueva: Boolean = false
    ): Map<String, String> {
        val errores = mutableMapOf<String, String>()
        textoObligatorio(errores, CAMPO_DESCRIPCION, descripcion, "La descripcion", minimo = 5, maximo = 120)
        textoObligatorio(errores, CAMPO_INSTRUCCIONES, instrucciones, "Las instrucciones", minimo = 5, maximo = 300)
        if (tipo == null) errores[CAMPO_TIPO] = "Selecciona el tipo de tarea"
        if (sectorId == null) errores[CAMPO_SECTOR] = "Selecciona el sector"
        if (responsableId == null) errores[CAMPO_RESPONSABLE] = "Asigna un estudiante responsable"
        val fecha = Fechas.parsear(fechaProgramada)
        if (fecha == null) {
            errores[CAMPO_FECHA_PROGRAMADA] = "Fecha programada invalida (dd/mm/aaaa)"
        } else if (esNueva && fecha < Fechas.hoy()) {
            errores[CAMPO_FECHA_PROGRAMADA] = "Una tarea nueva no puede programarse en el pasado"
        }
        return errores
    }

    fun entregaDeTarea(comentario: String, rutaEvidencia: String?): Map<String, String> {
        val errores = mutableMapOf<String, String>()
        textoObligatorio(errores, CAMPO_COMENTARIO, comentario, "El comentario", minimo = 5, maximo = 300)
        if (rutaEvidencia.isNullOrBlank()) {
            errores[CAMPO_EVIDENCIA] = "Adjunta una fotografia del trabajo realizado"
        }
        return errores
    }

    fun rechazo(motivo: String): Map<String, String> {
        val errores = mutableMapOf<String, String>()
        textoObligatorio(errores, CAMPO_MOTIVO, motivo, "El motivo del rechazo", minimo = 5, maximo = 300)
        return errores
    }

    fun reporte(
        sectorId: Long?,
        tipo: TipoProblema?,
        descripcion: String
    ): Map<String, String> {
        val errores = mutableMapOf<String, String>()
        if (sectorId == null) errores[CAMPO_SECTOR] = "Selecciona el sector afectado"
        if (tipo == null) errores[CAMPO_TIPO] = "Selecciona el tipo de problema"
        textoObligatorio(errores, CAMPO_DESCRIPCION, descripcion, "La descripcion del problema", minimo = 10, maximo = 300)
        return errores
    }
}
