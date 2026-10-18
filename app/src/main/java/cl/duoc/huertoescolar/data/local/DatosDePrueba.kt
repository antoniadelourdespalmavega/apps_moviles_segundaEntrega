package cl.duoc.huertoescolar.data.local

import android.content.Context
import android.graphics.Color
import cl.duoc.huertoescolar.core.Fechas
import cl.duoc.huertoescolar.core.Imagenes
import cl.duoc.huertoescolar.data.local.entity.Cultivo
import cl.duoc.huertoescolar.data.local.entity.LecturaSensor
import cl.duoc.huertoescolar.data.local.entity.Sector
import cl.duoc.huertoescolar.data.local.entity.Usuario
import cl.duoc.huertoescolar.domain.Cargo
import cl.duoc.huertoescolar.domain.EstadoCultivo
import cl.duoc.huertoescolar.domain.EstadoLectura
import cl.duoc.huertoescolar.domain.EstadoSector

/**
 * Datos ficticios de demostracion.
 *
 * Todos los usuarios, sectores, cultivos y fotografias son inventados: no hay
 * nombres reales, RUT, correos reales ni imagenes de personas. Se cargan una
 * sola vez, cuando la base de datos esta vacia.
 *
 * Las tareas, evidencias, validaciones y reportes NO se precargan: la app parte
 * con el historial limpio y se va llenando a medida que se usa (por ejemplo,
 * durante la presentacion).
 */
object DatosDePrueba {

    const val CLAVE_DEMO = "huerto123"

    suspend fun cargarSiEstaVacia(context: Context, db: HuertoDatabase) {
        if (db.usuarioDao().contar() > 0) return

        val usuarioDao = db.usuarioDao()
        val sectorDao = db.sectorDao()
        val cultivoDao = db.cultivoDao()
        val sensorDao = db.sensorDao()

        val hoy = Fechas.hoy()

        // ----------------------------- Usuarios -----------------------------
        val idDocente = usuarioDao.insertar(
            Usuario(
                nombreUsuario = "profe.ana",
                correo = "ana.rojas@escuela.cl",
                clave = CLAVE_DEMO,
                nombre = "Ana Rojas",
                rol = Cargo.DOCENTE.rol,
                cargo = Cargo.DOCENTE,
                taller = "Taller de Huerto"
            )
        )
        val idTallerista = usuarioDao.insertar(
            Usuario(
                nombreUsuario = "taller.luis",
                correo = "luis.pinto@escuela.cl",
                clave = CLAVE_DEMO,
                nombre = "Luis Pinto",
                rol = Cargo.TALLERISTA.rol,
                cargo = Cargo.TALLERISTA,
                taller = "Taller de Huerto"
            )
        )
        usuarioDao.insertar(
            Usuario(
                nombreUsuario = "est.camila",
                correo = "camila.soto@escuela.cl",
                clave = CLAVE_DEMO,
                nombre = "Camila Soto",
                rol = Cargo.ESTUDIANTE.rol,
                cargo = Cargo.ESTUDIANTE,
                taller = "Taller A"
            )
        )
        usuarioDao.insertar(
            Usuario(
                nombreUsuario = "est.benjamin",
                correo = "benjamin.diaz@escuela.cl",
                clave = CLAVE_DEMO,
                nombre = "Benjamin Diaz",
                rol = Cargo.ESTUDIANTE.rol,
                cargo = Cargo.ESTUDIANTE,
                taller = "Taller A"
            )
        )
        usuarioDao.insertar(
            Usuario(
                nombreUsuario = "est.martina",
                correo = "martina.rivas@escuela.cl",
                clave = CLAVE_DEMO,
                nombre = "Martina Rivas",
                rol = Cargo.ESTUDIANTE.rol,
                cargo = Cargo.ESTUDIANTE,
                taller = "Taller B"
            )
        )

        // ----------------------------- Sectores -----------------------------
        val idNorte = sectorDao.insertar(
            Sector(
                codigo = "S-01",
                nombre = "Huerto Norte",
                descripcion = "Cuatro camas de cultivo junto al patio techado",
                ubicacion = "Patio norte, costado del comedor",
                estado = EstadoSector.EN_USO,
                responsableId = idDocente,
                fechaCreacion = Fechas.sumarDias(hoy, -60),
                fechaActualizacion = Fechas.sumarDias(hoy, -5)
            )
        )
        val idInvernadero = sectorDao.insertar(
            Sector(
                codigo = "S-02",
                nombre = "Invernadero",
                descripcion = "Estructura cubierta para almacigos y plantas delicadas",
                ubicacion = "Costado del gimnasio",
                estado = EstadoSector.EN_USO,
                responsableId = idTallerista,
                fechaCreacion = Fechas.sumarDias(hoy, -55),
                fechaActualizacion = Fechas.sumarDias(hoy, -2)
            )
        )
        val idCompostera = sectorDao.insertar(
            Sector(
                codigo = "S-03",
                nombre = "Compostera",
                descripcion = "Zona de compostaje y preparacion de tierra",
                ubicacion = "Fondo del patio trasero",
                estado = EstadoSector.EN_MANTENIMIENTO,
                responsableId = idTallerista,
                fechaCreacion = Fechas.sumarDias(hoy, -40),
                fechaActualizacion = Fechas.sumarDias(hoy, -10)
            )
        )
        val idSur = sectorDao.insertar(
            Sector(
                codigo = "S-04",
                nombre = "Huerto Sur",
                descripcion = "Cama nueva disponible para la siembra de primavera",
                ubicacion = "Patio sur, junto a la reja",
                estado = EstadoSector.DISPONIBLE,
                responsableId = null,
                fechaCreacion = Fechas.sumarDias(hoy, -15),
                fechaActualizacion = Fechas.sumarDias(hoy, -15)
            )
        )

        // ----------------------------- Cultivos -----------------------------
        val fotoLechuga = Imagenes.crearImagenDemo(context, "Lechuga - Huerto Norte", Color.rgb(200, 226, 196))
        val fotoTomate = Imagenes.crearImagenDemo(context, "Tomate - Invernadero", Color.rgb(214, 226, 196))

        cultivoDao.insertar(
            Cultivo(
                nombre = "Lechuga costina",
                descripcion = "Almacigo trasplantado en la cama 1 del sector norte",
                sectorId = idNorte,
                fechaSiembra = Fechas.sumarDias(hoy, -35),
                fechaTrasplante = Fechas.sumarDias(hoy, -20),
                fechaCosechaEstimada = Fechas.sumarDias(hoy, 10),
                frecuenciaRiegoDias = 2,
                estado = EstadoCultivo.EN_CRECIMIENTO,
                observaciones = "Crecimiento parejo, hojas sanas",
                rutaFoto = fotoLechuga
            )
        )
        cultivoDao.insertar(
            Cultivo(
                nombre = "Tomate cherry",
                descripcion = "Plantas en maceteros altos dentro del invernadero",
                sectorId = idInvernadero,
                fechaSiembra = Fechas.sumarDias(hoy, -50),
                fechaTrasplante = Fechas.sumarDias(hoy, -30),
                fechaCosechaEstimada = Fechas.sumarDias(hoy, 7),
                frecuenciaRiegoDias = 3,
                estado = EstadoCultivo.LISTO_PARA_COSECHAR,
                observaciones = "Primeros frutos maduros",
                rutaFoto = fotoTomate
            )
        )
        cultivoDao.insertar(
            Cultivo(
                nombre = "Acelga",
                descripcion = "Siembra directa en la cama 2 del sector norte",
                sectorId = idNorte,
                fechaSiembra = Fechas.sumarDias(hoy, -18),
                fechaTrasplante = null,
                fechaCosechaEstimada = Fechas.sumarDias(hoy, 25),
                frecuenciaRiegoDias = 2,
                estado = EstadoCultivo.SEMBRADO,
                observaciones = "Germinacion despareja en el borde",
                rutaFoto = null
            )
        )
        cultivoDao.insertar(
            Cultivo(
                nombre = "Cilantro",
                descripcion = "Almacigo en bandeja dentro del invernadero",
                sectorId = idInvernadero,
                fechaSiembra = Fechas.sumarDias(hoy, -12),
                fechaTrasplante = null,
                fechaCosechaEstimada = Fechas.sumarDias(hoy, 30),
                frecuenciaRiegoDias = 1,
                estado = EstadoCultivo.EN_CRECIMIENTO,
                observaciones = "Requiere riego diario",
                rutaFoto = null
            )
        )
        cultivoDao.insertar(
            Cultivo(
                nombre = "Zanahoria",
                descripcion = "Siembra planificada para la cama del sector sur",
                sectorId = idSur,
                fechaSiembra = Fechas.sumarDias(hoy, 5),
                fechaTrasplante = null,
                fechaCosechaEstimada = Fechas.sumarDias(hoy, 95),
                frecuenciaRiegoDias = 3,
                estado = EstadoCultivo.PLANIFICADO,
                observaciones = "Pendiente preparar la tierra",
                rutaFoto = null
            )
        )
        cultivoDao.insertar(
            Cultivo(
                nombre = "Albahaca",
                descripcion = "Macetero de aromaticas junto a los tomates",
                sectorId = idInvernadero,
                fechaSiembra = Fechas.sumarDias(hoy, -70),
                fechaTrasplante = Fechas.sumarDias(hoy, -45),
                fechaCosechaEstimada = Fechas.sumarDias(hoy, -5),
                frecuenciaRiegoDias = 2,
                estado = EstadoCultivo.COSECHADO,
                observaciones = "Primera cosecha entregada al taller de cocina",
                rutaFoto = null
            )
        )

        // ------------------- Lecturas simuladas de sensores ------------------
        sensorDao.insertarVarias(
            listOf(
                LecturaSensor(
                    sectorId = idNorte,
                    fechaLectura = Fechas.sumarDias(hoy, -1),
                    humedadSuelo = 46,
                    temperatura = 18.5,
                    humedadAmbiente = 55,
                    estado = EstadoLectura.NORMAL
                ),
                LecturaSensor(
                    sectorId = idNorte,
                    fechaLectura = hoy,
                    humedadSuelo = 28,
                    temperatura = 21.0,
                    humedadAmbiente = 48,
                    estado = EstadoLectura.BAJO
                ),
                LecturaSensor(
                    sectorId = idInvernadero,
                    fechaLectura = hoy,
                    humedadSuelo = 62,
                    temperatura = 26.4,
                    humedadAmbiente = 70,
                    estado = EstadoLectura.ALTO
                ),
                LecturaSensor(
                    sectorId = idInvernadero,
                    fechaLectura = Fechas.sumarDias(hoy, -2),
                    humedadSuelo = 54,
                    temperatura = 24.1,
                    humedadAmbiente = 63,
                    estado = EstadoLectura.NORMAL
                ),
                LecturaSensor(
                    sectorId = idCompostera,
                    fechaLectura = Fechas.sumarDias(hoy, -1),
                    humedadSuelo = 71,
                    temperatura = 19.8,
                    humedadAmbiente = 66,
                    estado = EstadoLectura.PENDIENTE_REVISION
                ),
                LecturaSensor(
                    sectorId = idSur,
                    fechaLectura = hoy,
                    humedadSuelo = 35,
                    temperatura = 20.2,
                    humedadAmbiente = 52,
                    estado = EstadoLectura.NORMAL
                )
            )
        )
    }
}
