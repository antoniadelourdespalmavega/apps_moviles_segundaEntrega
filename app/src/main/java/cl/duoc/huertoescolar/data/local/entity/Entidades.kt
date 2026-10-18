package cl.duoc.huertoescolar.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import cl.duoc.huertoescolar.domain.Cargo
import cl.duoc.huertoescolar.domain.EstadoCultivo
import cl.duoc.huertoescolar.domain.EstadoLectura
import cl.duoc.huertoescolar.domain.EstadoReporte
import cl.duoc.huertoescolar.domain.EstadoSector
import cl.duoc.huertoescolar.domain.EstadoTarea
import cl.duoc.huertoescolar.domain.ResultadoValidacion
import cl.duoc.huertoescolar.domain.Rol
import cl.duoc.huertoescolar.domain.TipoProblema
import cl.duoc.huertoescolar.domain.TipoTarea

/**
 * Modelo relacional del huerto escolar (referencia 3FN).
 *
 * Cada entidad tiene clave primaria propia y las relaciones se expresan solo
 * con claves foraneas, sin repetir datos descriptivos de otra tabla.
 */

@Entity(
    tableName = "usuarios",
    indices = [
        Index(value = ["nombreUsuario"], unique = true),
        Index(value = ["correo"], unique = true)
    ]
)
data class Usuario(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nombreUsuario: String,
    val correo: String,
    val clave: String,
    val nombre: String,
    val rol: Rol,
    val cargo: Cargo,
    val taller: String,
    val activo: Boolean = true
)

@Entity(
    tableName = "sectores",
    indices = [
        Index(value = ["codigo"], unique = true),
        Index(value = ["responsableId"])
    ],
    foreignKeys = [
        ForeignKey(
            entity = Usuario::class,
            parentColumns = ["id"],
            childColumns = ["responsableId"],
            onDelete = ForeignKey.SET_NULL
        )
    ]
)
data class Sector(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val codigo: String,
    val nombre: String,
    val descripcion: String,
    val ubicacion: String,
    val estado: EstadoSector,
    val responsableId: Long?,
    val fechaCreacion: Long,
    val fechaActualizacion: Long
)

@Entity(
    tableName = "cultivos",
    indices = [Index(value = ["sectorId"])],
    foreignKeys = [
        ForeignKey(
            entity = Sector::class,
            parentColumns = ["id"],
            childColumns = ["sectorId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class Cultivo(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nombre: String,
    val descripcion: String,
    val sectorId: Long,
    val fechaSiembra: Long,
    val fechaTrasplante: Long?,
    val fechaCosechaEstimada: Long,
    val frecuenciaRiegoDias: Int,
    val estado: EstadoCultivo,
    val observaciones: String,
    val rutaFoto: String?
)

@Entity(
    tableName = "tareas",
    indices = [
        Index(value = ["sectorId"]),
        Index(value = ["cultivoId"]),
        Index(value = ["responsableId"]),
        Index(value = ["creadaPorId"])
    ],
    foreignKeys = [
        ForeignKey(
            entity = Sector::class,
            parentColumns = ["id"],
            childColumns = ["sectorId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Cultivo::class,
            parentColumns = ["id"],
            childColumns = ["cultivoId"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = Usuario::class,
            parentColumns = ["id"],
            childColumns = ["responsableId"],
            onDelete = ForeignKey.RESTRICT
        ),
        ForeignKey(
            entity = Usuario::class,
            parentColumns = ["id"],
            childColumns = ["creadaPorId"],
            onDelete = ForeignKey.RESTRICT
        )
    ]
)
data class Tarea(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tipo: TipoTarea,
    val descripcion: String,
    val instrucciones: String,
    val sectorId: Long,
    val cultivoId: Long?,
    val responsableId: Long,
    val creadaPorId: Long,
    val fechaCreacion: Long,
    val fechaProgramada: Long,
    val estado: EstadoTarea,
    val fechaRealizacion: Long?,
    val comentarioEstudiante: String?
)

@Entity(
    tableName = "evidencias",
    indices = [
        Index(value = ["tareaId"]),
        Index(value = ["usuarioId"])
    ],
    foreignKeys = [
        ForeignKey(
            entity = Tarea::class,
            parentColumns = ["id"],
            childColumns = ["tareaId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Usuario::class,
            parentColumns = ["id"],
            childColumns = ["usuarioId"],
            onDelete = ForeignKey.RESTRICT
        )
    ]
)
data class Evidencia(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tareaId: Long,
    val usuarioId: Long,
    val rutaFoto: String,
    val comentario: String,
    val fechaRegistro: Long
)

@Entity(
    tableName = "validaciones",
    indices = [
        Index(value = ["tareaId"]),
        Index(value = ["validadorId"])
    ],
    foreignKeys = [
        ForeignKey(
            entity = Tarea::class,
            parentColumns = ["id"],
            childColumns = ["tareaId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Usuario::class,
            parentColumns = ["id"],
            childColumns = ["validadorId"],
            onDelete = ForeignKey.RESTRICT
        )
    ]
)
data class Validacion(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tareaId: Long,
    val validadorId: Long,
    val resultado: ResultadoValidacion,
    val motivo: String,
    val fechaValidacion: Long
)

@Entity(
    tableName = "reportes_problema",
    indices = [
        Index(value = ["sectorId"]),
        Index(value = ["cultivoId"]),
        Index(value = ["reportadoPorId"])
    ],
    foreignKeys = [
        ForeignKey(
            entity = Sector::class,
            parentColumns = ["id"],
            childColumns = ["sectorId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Cultivo::class,
            parentColumns = ["id"],
            childColumns = ["cultivoId"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = Usuario::class,
            parentColumns = ["id"],
            childColumns = ["reportadoPorId"],
            onDelete = ForeignKey.RESTRICT
        )
    ]
)
data class ReporteProblema(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sectorId: Long,
    val cultivoId: Long?,
    val tipo: TipoProblema,
    val descripcion: String,
    val fechaReporte: Long,
    val reportadoPorId: Long,
    val rutaFoto: String?,
    val estado: EstadoReporte,
    val comentarioDocente: String,
    val fechaCierre: Long?
)

/**
 * Lecturas SIMULADAS de sensores. No provienen de hardware: el campo origen
 * siempre indica que se trata de una simulacion academica.
 */
@Entity(
    tableName = "lecturas_sensor",
    indices = [Index(value = ["sectorId"])],
    foreignKeys = [
        ForeignKey(
            entity = Sector::class,
            parentColumns = ["id"],
            childColumns = ["sectorId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class LecturaSensor(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sectorId: Long,
    val fechaLectura: Long,
    val humedadSuelo: Int,
    val temperatura: Double,
    val humedadAmbiente: Int,
    val origen: String = "Simulación académica",
    val estado: EstadoLectura
)
