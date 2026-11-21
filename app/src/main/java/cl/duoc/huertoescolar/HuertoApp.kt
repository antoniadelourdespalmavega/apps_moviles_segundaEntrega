package cl.duoc.huertoescolar

import android.app.Application
import android.content.Context
import cl.duoc.huertoescolar.data.local.HuertoDatabase
import cl.duoc.huertoescolar.data.remote.RetrofitClient
import cl.duoc.huertoescolar.data.repository.ClimaRepository
import cl.duoc.huertoescolar.data.repository.HuertoRepository
import cl.duoc.huertoescolar.data.repository.ReporteRepository
import cl.duoc.huertoescolar.data.repository.SensorRepository
import cl.duoc.huertoescolar.data.repository.SesionRepository
import cl.duoc.huertoescolar.data.repository.SincronizacionRepository
import cl.duoc.huertoescolar.data.repository.TareaRepository

/**
 * Punto unico donde se crean la base de datos local, los clientes Retrofit y
 * los repositorios. Los ViewModel los obtienen desde aqui, de modo que la UI
 * nunca conoce a Room ni a Retrofit.
 */
object Contenedor {

    lateinit var baseDatos: HuertoDatabase
        private set
    lateinit var sincronizacionRepository: SincronizacionRepository
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
    lateinit var climaRepository: ClimaRepository
        private set

    private var iniciado = false

    fun iniciar(context: Context) {
        if (iniciado) return
        val db = HuertoDatabase.obtener(context)
        baseDatos = db

        val sincronizacion = SincronizacionRepository(
            usuariosApi = RetrofitClient.usuariosApi,
            huertoApi = RetrofitClient.huertoApi,
            actividadesApi = RetrofitClient.actividadesApi,
            dao = db.sincronizacionDao()
        )
        sincronizacionRepository = sincronizacion
        sesionRepository = SesionRepository(RetrofitClient.usuariosApi, db.usuarioDao(), sincronizacion)
        huertoRepository = HuertoRepository(
            RetrofitClient.huertoApi,
            db.sectorDao(),
            db.cultivoDao(),
            db.tareaDao(),
            sincronizacion
        )
        tareaRepository = TareaRepository(
            RetrofitClient.actividadesApi,
            db.tareaDao(),
            db.evidenciaDao(),
            db.validacionDao(),
            db.usuarioDao(),
            sincronizacion
        )
        reporteRepository = ReporteRepository(
            RetrofitClient.actividadesApi,
            db.reporteDao(),
            db.usuarioDao(),
            sincronizacion
        )
        sensorRepository = SensorRepository(RetrofitClient.huertoApi, db.sensorDao(), sincronizacion)
        climaRepository = ClimaRepository(RetrofitClient.climaApi)
        iniciado = true
    }
}

/**
 * Los datos de prueba (usuarios, sectores, cultivos y lecturas) ya no se
 * cargan aqui: ahora los crea el microservicio al arrancar por primera vez
 * (clase DataInitializer).
 */
class HuertoApp : Application() {

    override fun onCreate() {
        super.onCreate()
        Contenedor.iniciar(this)
    }
}
