package cl.duoc.huertoescolar.data.remote

import cl.duoc.huertoescolar.data.remote.api.ActividadesApi
import cl.duoc.huertoescolar.data.remote.api.ClimaApi
import cl.duoc.huertoescolar.data.remote.api.HuertoApi
import cl.duoc.huertoescolar.data.remote.api.UsuariosApi
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Crea un cliente Retrofit por cada servicio (cada microservicio tiene su
 * propio puerto). Igual que en el ejemplo del docente, se usa Gson para
 * convertir el JSON a objetos de Kotlin.
 */
object RetrofitClient {

    // Si un microservicio esta apagado, se informa en pocos segundos en vez de esperar.
    private val clienteHttp: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .build()
    }

    private fun crear(urlBase: String): Retrofit =
        Retrofit.Builder()
            .baseUrl(urlBase)
            .client(clienteHttp)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

    val usuariosApi: UsuariosApi by lazy {
        crear(ApiConfig.URL_USUARIOS).create(UsuariosApi::class.java)
    }

    val huertoApi: HuertoApi by lazy {
        crear(ApiConfig.URL_HUERTO).create(HuertoApi::class.java)
    }

    val actividadesApi: ActividadesApi by lazy {
        crear(ApiConfig.URL_ACTIVIDADES).create(ActividadesApi::class.java)
    }

    val climaApi: ClimaApi by lazy {
        crear(ApiConfig.URL_CLIMA).create(ClimaApi::class.java)
    }
}
