package cl.duoc.huertoescolar.data.remote

/**
 * Direcciones de los microservicios y de la API externa.
 *
 * 10.0.2.2 es el computador visto desde el emulador de Android. Si prueban en
 * un celular real, cambien HOST por la IP del computador en la misma red
 * (por ejemplo "http://192.168.1.10") y agreguen esa IP en
 * res/xml/network_security_config.xml.
 */
object ApiConfig {
    private const val HOST = "http://10.0.2.2"

    /** ms-usuarios: login, registro y perfil. */
    const val URL_USUARIOS = "$HOST:8081/"

    /** ms-huerto: sectores, cultivos y lecturas simuladas de sensores. */
    const val URL_HUERTO = "$HOST:8082/"

    /** ms-actividades: tareas, evidencias, validaciones y reportes. */
    const val URL_ACTIVIDADES = "$HOST:8083/"

    /** API externa de clima Open-Meteo (gratis, sin clave). */
    const val URL_CLIMA = "https://api.open-meteo.com/"
}
