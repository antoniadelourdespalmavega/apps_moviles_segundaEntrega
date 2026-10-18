package cl.duoc.huertoescolar

import android.app.Application
import android.content.Context
import cl.duoc.huertoescolar.data.local.DatosDePrueba
import cl.duoc.huertoescolar.data.local.HuertoDatabase
import cl.duoc.huertoescolar.data.repository.HuertoRepository
import cl.duoc.huertoescolar.data.repository.ReporteRepository
import cl.duoc.huertoescolar.data.repository.SensorRepository
import cl.duoc.huertoescolar.data.repository.SesionRepository
import cl.duoc.huertoescolar.data.repository.TareaRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Punto unico donde se crean la base de datos y los repositorios.
 * Los ViewModel los obtienen desde aqui, de modo que la UI nunca conoce a Room.
 */
object Contenedor {

    lateinit var baseDatos: HuertoDatabase
        private set
    lateinit var sesionRepository: SesionRepository
        private set
    lateinit var huertoRepository: HuertoRepository
        private set
    lateinit var tareaRepository: TareaRepository
        private set
    lateinit var reporteRepository: ReporteRepository
        private set
    lateinit var sensorRepository: SensorRepository
        private set

    private var iniciado = false

    fun iniciar(context: Context) {
        if (iniciado) return
        val db = HuertoDatabase.obtener(context)
        baseDatos = db
        sesionRepository = SesionRepository(db.usuarioDao())
        huertoRepository = HuertoRepository(db.sectorDao(), db.cultivoDao(), db.tareaDao())
        tareaRepository = TareaRepository(
            db.tareaDao(),
            db.evidenciaDao(),
            db.validacionDao(),
            db.usuarioDao()
        )
        reporteRepository = ReporteRepository(db.reporteDao(), db.usuarioDao())
        sensorRepository = SensorRepository(db.sensorDao())
        iniciado = true
    }
}

class HuertoApp : Application() {

    private val alcance = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        Contenedor.iniciar(this)
        // Carga los datos ficticios la primera vez que se abre la aplicacion.
        alcance.launch {
            DatosDePrueba.cargarSiEstaVacia(applicationContext, Contenedor.baseDatos)
        }
    }
}
