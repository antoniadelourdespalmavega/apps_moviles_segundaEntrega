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
 * Clientes Retrofit. Igual que en el ejemplo del docente, se usa Gson para
 * convertir el JSON a objetos de Kotlin.
 *
 * Todo el backend vive en un solo microservicio (ms-huerto-escolar), por eso
 * usuariosApi, huertoApi y actividadesApi comparten la misma instancia de
 * Retrofit y la misma URL. Se mantienen como tres interfaces solo para
 * ordenar los endpoints por modulo.
 */
object RetrofitClient {

    // Si el microservicio esta apagado, se informa en pocos segundos en vez de esperar.
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

    private val backend: Retrofit by lazy { crear(ApiConfig.URL_BACKEND) }

    val usuariosApi: UsuariosApi by lazy { backend.create(UsuariosApi::class.java) }

    val huertoApi: HuertoApi by lazy { backend.create(HuertoApi::class.java) }

    val actividadesApi: ActividadesApi by lazy { backend.create(ActividadesApi::class.java) }

    val climaApi: ClimaApi by lazy {
        crear(ApiConfig.URL_CLIMA).create(ClimaApi::class.java)
    }
}
