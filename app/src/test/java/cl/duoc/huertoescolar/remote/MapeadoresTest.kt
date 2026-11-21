package cl.duoc.huertoescolar.remote

import cl.duoc.huertoescolar.data.remote.aEntidad
import cl.duoc.huertoescolar.data.remote.enumSeguro
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
import cl.duoc.huertoescolar.util.Fabrica
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Conversion de los datos del microservicio a las entidades de Room. */
class MapeadoresTest {

    @Test
    fun enumSeguroConvierteValoresConocidos() {
        assertEquals(EstadoTarea.VALIDADA, enumSeguro("VALIDADA", EstadoTarea.PENDIENTE))
    }

    @Test
    fun enumSeguroUsaElRespaldoSiElValorEsDesconocido() {
        assertEquals(EstadoTarea.PENDIENTE, enumSeguro("OTRO_ESTADO", EstadoTarea.PENDIENTE))
        assertEquals(EstadoTarea.PENDIENTE, enumSeguro(null, EstadoTarea.PENDIENTE))
    }

    @Test
    fun usuarioLocalNuncaGuardaLaContrasena() {
        val usuario = Fabrica.usuarioDto(id = 3, rol = "ESTUDIANTE", cargo = "ESTUDIANTE").aEntidad()
        assertEquals("", usuario.clave)
        assertEquals(3L, usuario.id)
        assertEquals(Rol.ESTUDIANTE, usuario.rol)
        assertEquals(Cargo.ESTUDIANTE, usuario.cargo)
    }

    @Test
    fun sectorConservaSusDatos() {
        val sector = Fabrica.sectorDto(id = 2, responsableId = null).aEntidad()
        assertEquals("S-02", sector.codigo)
        assertEquals(EstadoSector.EN_USO, sector.estado)
        assertNull(sector.responsableId)
    }

    @Test
    fun cultivoSinObservacionesQuedaConTextoVacio() {
        val cultivo = Fabrica.cultivoDto().aEntidad()
        assertEquals("", cultivo.observaciones)
        assertEquals(EstadoCultivo.SEMBRADO, cultivo.estado)
    }

    @Test
    fun lecturaSinOrigenSeMarcaComoSimulada() {
        val lectura = Fabrica.lecturaDto().aEntidad()
        assertEquals("Simulación académica", lectura.origen)
        assertEquals(EstadoLectura.NORMAL, lectura.estado)
    }

    @Test
    fun tareaEvidenciaValidacionYReporte() {
        val tarea = Fabrica.tareaDto(estado = "EN_DESARROLLO").aEntidad()
        assertEquals(TipoTarea.RIEGO, tarea.tipo)
        assertEquals(EstadoTarea.EN_DESARROLLO, tarea.estado)

        assertEquals("Listo", Fabrica.evidenciaDto().aEntidad().comentario)
        assertEquals(ResultadoValidacion.VALIDADA, Fabrica.validacionDto().aEntidad().resultado)

        val reporte = Fabrica.reporteDto().aEntidad()
        assertEquals(TipoProblema.FALTA_DE_AGUA, reporte.tipo)
        assertEquals(EstadoReporte.INFORMADO, reporte.estado)
        assertEquals("", reporte.comentarioDocente)
    }
}
