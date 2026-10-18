package cl.duoc.huertoescolar.domain

/**
 * Enumeraciones del dominio del huerto escolar.
 *
 * Se guardan en la base de datos con su nombre (name) a traves de los
 * TypeConverters de Room, y cada una expone una etiqueta legible para la UI.
 */

enum class Rol(val etiqueta: String) {
    ADMINISTRADOR("Administrador"),
    ESTUDIANTE("Colaborador")
}

/**
 * Cargo de la persona dentro del taller. Docente y tallerista comparten el rol
 * ADMINISTRADOR (mismos permisos); el cargo solo cambia como se presentan.
 */
enum class Cargo(val etiqueta: String, val rol: Rol) {
    DOCENTE("Docente", Rol.ADMINISTRADOR),
    TALLERISTA("Tallerista", Rol.ADMINISTRADOR),
    ESTUDIANTE("Estudiante", Rol.ESTUDIANTE)
}

enum class EstadoSector(val etiqueta: String) {
    DISPONIBLE("Disponible"),
    EN_USO("En uso"),
    EN_MANTENIMIENTO("En mantenimiento"),
    FUERA_DE_USO("Fuera de uso")
}

enum class EstadoCultivo(val etiqueta: String) {
    PLANIFICADO("Planificado"),
    SEMBRADO("Sembrado"),
    EN_CRECIMIENTO("En crecimiento"),
    LISTO_PARA_COSECHAR("Listo para cosechar"),
    COSECHADO("Cosechado"),
    CON_PROBLEMAS("Con problemas"),
    FINALIZADO("Finalizado")
}

enum class TipoTarea(val etiqueta: String) {
    PREPARACION_SECTOR("Preparacion del sector"),
    SIEMBRA("Siembra"),
    TRASPLANTE("Trasplante"),
    RIEGO("Riego"),
    LIMPIEZA("Limpieza"),
    RETIRO_MALEZAS("Retiro de malezas"),
    OBSERVACION("Observacion del cultivo"),
    MANTENIMIENTO("Mantenimiento"),
    REVISION_SUELO("Revision del suelo"),
    REGISTRO_CRECIMIENTO("Registro del crecimiento"),
    COSECHA("Cosecha"),
    OTRA("Otra actividad autorizada")
}

/**
 * Flujo oficial de estados de una tarea:
 * PENDIENTE -> EN_DESARROLLO -> PENDIENTE_VALIDACION -> VALIDADA / RECHAZADA.
 * ATRASADA no se guarda: se calcula comparando la fecha programada con hoy.
 */
enum class EstadoTarea(val etiqueta: String) {
    PENDIENTE("Pendiente"),
    EN_DESARROLLO("En desarrollo"),
    PENDIENTE_VALIDACION("Pendiente de validacion"),
    VALIDADA("Validada"),
    RECHAZADA("Rechazada")
}

/**
 * Agrupacion de tareas que usan el panel principal y el listado de tareas.
 * Cada tarea cae en UNA sola categoria, asi los numeros del panel coinciden
 * siempre con lo que muestra cada filtro del listado.
 */
enum class CategoriaTarea(val etiqueta: String) {
    PENDIENTE("Pendientes"),
    ATRASADA("Atrasadas"),
    POR_VALIDAR("Por validar"),
    VALIDADA("Validadas"),
    RECHAZADA("Rechazadas")
}

enum class ResultadoValidacion(val etiqueta: String) {
    VALIDADA("Validada"),
    RECHAZADA("Rechazada")
}

enum class TipoProblema(val etiqueta: String) {
    FALTA_DE_AGUA("Falta de agua"),
    DANO_EN_PLANTA("Dano en la planta"),
    PLAGA_SIMULADA("Plaga simulada"),
    DETERIORO_SECTOR("Deterioro del sector"),
    MANTENIMIENTO("Problema de mantenimiento"),
    OTRO("Otro problema observado")
}

enum class EstadoReporte(val etiqueta: String) {
    INFORMADO("Informado"),
    EN_REVISION("En revision"),
    ATENDIDO("Atendido"),
    CERRADO("Cerrado")
}

enum class EstadoLectura(val etiqueta: String) {
    NORMAL("Normal"),
    BAJO("Bajo"),
    ALTO("Alto"),
    PENDIENTE_REVISION("Pendiente de revision")
}
