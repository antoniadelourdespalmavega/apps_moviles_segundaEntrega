package cl.duoc.huertoescolar.data.remote

/**
 * Direcciones del microservicio propio y de la API externa.
 *
 * 10.0.2.2 es el computador visto desde el emulador de Android. Si prueban en
 * un celular real, cambien HOST por la IP del computador en la misma red
 * (por ejemplo "http://192.168.1.10") y agreguen esa IP en
 * res/xml/network_security_config.xml.
 */
object ApiConfig {
    private const val HOST = "http://10.0.2.2"

    /**
     * ms-huerto-escolar: microservicio unico con usuarios, sectores, cultivos,
     * lecturas simuladas, tareas, evidencias, validaciones y reportes.
     */
    const val URL_BACKEND = "$HOST:8080/"

    /** API externa de clima Open-Meteo (gratis, sin clave). */
    const val URL_CLIMA = "https://api.open-meteo.com/"
}
