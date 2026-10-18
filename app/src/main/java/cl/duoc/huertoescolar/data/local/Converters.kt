package cl.duoc.huertoescolar.data.local

import androidx.room.TypeConverter
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

/** Guarda los enum del dominio como texto en SQLite y los recupera de vuelta. */
class Converters {

    @TypeConverter
    fun rolADato(valor: Rol): String = valor.name

    @TypeConverter
    fun datoARol(valor: String): Rol = Rol.valueOf(valor)

    @TypeConverter
    fun cargoADato(valor: Cargo): String = valor.name

    @TypeConverter
    fun datoACargo(valor: String): Cargo = Cargo.valueOf(valor)

    @TypeConverter
    fun estadoSectorADato(valor: EstadoSector): String = valor.name

    @TypeConverter
    fun datoAEstadoSector(valor: String): EstadoSector = EstadoSector.valueOf(valor)

    @TypeConverter
    fun estadoCultivoADato(valor: EstadoCultivo): String = valor.name

    @TypeConverter
    fun datoAEstadoCultivo(valor: String): EstadoCultivo = EstadoCultivo.valueOf(valor)

    @TypeConverter
    fun tipoTareaADato(valor: TipoTarea): String = valor.name

    @TypeConverter
    fun datoATipoTarea(valor: String): TipoTarea = TipoTarea.valueOf(valor)

    @TypeConverter
    fun estadoTareaADato(valor: EstadoTarea): String = valor.name

    @TypeConverter
    fun datoAEstadoTarea(valor: String): EstadoTarea = EstadoTarea.valueOf(valor)

    @TypeConverter
    fun resultadoADato(valor: ResultadoValidacion): String = valor.name

    @TypeConverter
    fun datoAResultado(valor: String): ResultadoValidacion = ResultadoValidacion.valueOf(valor)

    @TypeConverter
    fun tipoProblemaADato(valor: TipoProblema): String = valor.name

    @TypeConverter
    fun datoATipoProblema(valor: String): TipoProblema = TipoProblema.valueOf(valor)

    @TypeConverter
    fun estadoReporteADato(valor: EstadoReporte): String = valor.name

    @TypeConverter
    fun datoAEstadoReporte(valor: String): EstadoReporte = EstadoReporte.valueOf(valor)

    @TypeConverter
    fun estadoLecturaADato(valor: EstadoLectura): String = valor.name

    @TypeConverter
    fun datoAEstadoLectura(valor: String): EstadoLectura = EstadoLectura.valueOf(valor)
}
