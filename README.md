# Huerto Escolar - App móvil de gestión colaborativa

Aplicación Android para organizar el trabajo del huerto de una escuela pública de
San Bernardo: sectores, cultivos, tareas asignadas a estudiantes, evidencias
fotográficas, validación docente, reportes de problemas, historial, datos simulados de
sensores y clima del día. Los datos viven en **un microservicio Spring Boot con MySQL**
(`ms-huerto-escolar`), y la app consume además una **API externa de clima**.

> **Cambio en esta versión:** por recomendación del docente, los tres microservicios
> anteriores (`ms-usuarios`, `ms-huerto` y `ms-actividades`) se unificaron en **un solo
> microservicio** con una sola base de datos. Se mantienen **todos los endpoints con las
> mismas rutas**, así que no se perdió ninguna funcionalidad. Además, ahora el backend y la
> app tienen pruebas unitarias con **Kover** (herramienta de cobertura de JetBrains para Kotlin) que verifican una cobertura mínima de **90 %**.

Asignatura: **DSY1105 - Desarrollo de Aplicaciones Móviles** · Evaluación Parcial 4 (encargo)

## Docente

CHRISTIAN EDUARDO ACUÑA SANDOVAL

## 1. Nombre del proyecto

**Huerto Escolar** (app móvil `cl.duoc.huertoescolar` + microservicio `ms-huerto-escolar`).

## 2. Integrantes

| Nombre        | Rol en el equipo                  |
|---------------|-----------------------------------|
| Antonia Palma | Desarrollador y Líder Técnico     |
| Andrés Vargas | Desarrollador y Líder de Producto |

### Planificación y control de versiones

- **Trello (planificación y distribución de tareas):** https://trello.com/invite/b/6ad54aa2f16ded88b5ffb604/ATTI5a9375fae546a3506fb33c17659c1e79E820D002/proyectohuertoapmoviles
- **GitHub (código y commits):** https://github.com/antoniadelourdespalmavega/apps_moviles_segundaEntrega.git

## Arquitectura

```
        ┌──────────────── App Android (Kotlin + Compose) ────────────────┐
        │  Pantallas (Composables) → ViewModel (StateFlow) → Repository  │
        │                                    │               │          │
        │                          Room (copia local)    Retrofit       │
        └────────────────────────────────────────────────────┼──────────┘
                                                             │ HTTP / JSON
                         ┌───────────────────────────────────┴──────────────┐
                         ▼                                                  ▼
          ms-huerto-escolar :8080                                 API externa Open-Meteo
          usuarios, login, perfil, sectores, cultivos,            clima y pronóstico
          lecturas simuladas, tareas, evidencias,                 (https, sin clave)
          validaciones y reportes
          BD MySQL: huerto_escolar
```

- **Microservicio** (proyecto aparte, carpeta `huerto_escolar_backend`): misma estructura que el ejemplo
  `miapi` visto en clase: `controller` → `service` → `repository` (Spring Data JPA) → `model`,
  más `dto`, `config/DataInitializer` (datos de prueba) y `exception` (errores JSON
  `{"mensaje": "..."}`). Usa una sola base de datos MySQL (`huerto_escolar`). Los permisos
  por rol (por ejemplo, que solo un docente valide tareas) se revisan con `PermisoService`
  leyendo directamente la tabla de usuarios; antes se consultaba por HTTP a otro servicio.
- **App** (este proyecto, `huerto_escolar_frontend`): Retrofit (como en el ejemplo `consumiendo-api-usuarios`) en `data/remote/`.
  Todas las llamadas van a una sola URL (`ApiConfig.URL_BACKEND`). El microservicio es la fuente de los datos; **Room guarda una copia local** que se
  sincroniza al iniciar sesión y luego cada 15 segundos. Las pantallas leen de Room, así se
  actualizan solas y, si se corta la conexión, siguen mostrando la última información con un
  aviso "Sin conexión con el servidor".
- Cada escritura (crear, editar, eliminar, entregar, validar...) se envía primero al
  microservicio; si responde bien, se refresca la copia local.

### Proyectos

El sistema está dividido en **dos carpetas independientes**:

```
huerto_escolar_frontend/   App Android (este proyecto; se abre en Android Studio)
├── app/                   Código de la app y pruebas unitarias
├── keystore/              Llave .jks para firmar el APK
├── apk/                   APK firmado para la entrega
└── docs/                  Capturas del README

huerto_escolar_backend/    Microservicio ms-huerto-escolar (se abre en IntelliJ IDEA)
```

**¿Cómo se conectan?** No dependen de dónde estén guardadas las carpetas: la app llama al
microservicio por HTTP. El backend escucha en el puerto **8080** y la app apunta a
`http://10.0.2.2:8080/` (`ApiConfig.URL_BACKEND`), que es el computador visto desde el
emulador. Basta con que el backend esté corriendo en el mismo computador que el emulador.

### Estructura de la app

```
app/src/main/java/cl/duoc/huertoescolar/
├── HuertoApp.kt            Contenedor: crea Room, clientes Retrofit y repositorios
├── core/                   Utilidades (fechas, imágenes)
├── domain/                 Modelos y reglas sin Android (enums, alertas, Resultado)
│   ├── validacion/         Un validador por formulario + reglas comunes
│   └── clima/              Modelo del clima y recomendación de riego
├── data/
│   ├── remote/             Retrofit: ApiConfig, RetrofitClient, api/, dto/, mapeadores, errores
│   ├── local/              Room: entidades, DAOs (incl. SincronizacionDao), converters
│   └── repository/         Repositorios (microservicio + copia local) y sincronización
└── ui/
    ├── animaciones/        Todas las animaciones de la app (Animaciones.kt)
    ├── clima/              Tarjeta del panel y pantalla de pronóstico (API externa)
    ├── sincronizacion/     Sincronización periódica y aviso sin conexión
    ├── navegacion/         NavHost y rutas con control de acceso
    └── <funcionalidad>/    Pantalla + ViewModel de cada módulo
```

## 3. Funcionalidades

**Perfil administrador (docente o tallerista)**
- Crear, editar y cambiar el estado de sectores. Desde el mismo formulario del sector
  se agregan y editan sus cultivos; los cambios se reflejan al instante en el selector
  de cultivo de las tareas y en el de los reportes de problemas.
- Consultar *Informacion de cultivos* (tarjeta o acceso *Cultivos*): que cultivo va en
  cada sector, con fechas, riego y estado.
- Registrar cultivos con fecha de siembra, trasplante, cosecha estimada y frecuencia de riego.
- Actualizar el estado de desarrollo de cada cultivo.
- Crear tareas (boton *Nueva tarea* del panel), asignarlas a un estudiante y programar
  su fecha con calendario.
- Revisar actividades entregadas en la pantalla de revision (tarjeta *Por validar* del
  panel) o desde el detalle de la tarea: validar o rechazar con motivo obligatorio.
- Las tarjetas del panel (Pendientes, Atrasadas, Por validar, Validadas, Rechazadas)
  abren el listado de tareas con el mismo filtro, por lo que los numeros coinciden.
- Solucionar los problemas reportados por los estudiantes (*Marcar como solucionado* o
  cambiar el estado de atencion). El docente y el tallerista no reportan problemas.
- Consultar el historial con filtros (tipo, fecha y persona, con *Limpiar filtros*) y
  los datos simulados de sensores (con filtro por sector).

**Perfil colaborador (estudiante)**
- Ver solo las tareas que tiene asignadas, con instrucciones y fecha programada.
- Iniciar una tarea y entregarla con comentario y fotografia de evidencia.
- Reportar problemas del huerto con descripcion y foto opcional.
- Consultar el resultado de la revision y su propio historial.
- Ver avisos y recordatorios de sus tareas (proximas, para corregir, riegos, cosechas).
- No puede crear ni editar sectores, cultivos o tareas, ni validar sus propias actividades.

**Flujo de estados de una tarea**

```
PENDIENTE -> EN_DESARROLLO -> PENDIENTE_VALIDACION -> VALIDADA
                                      |
                                      +-> RECHAZADA -> (se corrige) -> PENDIENTE_VALIDACION
```

Una tarea se marca visualmente como *Atrasada* cuando su fecha programada ya paso y
todavia no ha sido entregada.

**Integración con el microservicio (CRUD completo desde la interfaz)**
- Usuarios: crear cuenta (POST), iniciar sesión (POST /login), ver cuentas (GET) y editar
  el perfil (PUT). Las contraseñas se guardan en el servidor como hash SHA-256.
- Sectores y cultivos: crear, listar, editar, cambiar estado y eliminar. El servidor no deja
  eliminar un sector o cultivo que tenga tareas registradas.
- Tareas: crear, listar, editar, eliminar, iniciar, entregar y validar/rechazar; el
  microservicio revisa el rol de cada persona antes de cada acción.
- Reportes de problemas: crear, listar y cambiar el estado de atención.
- Los cambios hechos en otro teléfono aparecen solos (sincronización cada 15 s).

**API externa (Open-Meteo)**
- Tarjeta "Clima en San Bernardo" en el panel principal con temperatura, humedad y una
  **recomendación de riego** (no regar si llueve, regar temprano si hace calor, riesgo de helada...).
- Al tocarla se abre la pantalla *Clima del huerto* con el pronóstico de 7 días.
- Es independiente del microservicio y de Room: solo se consulta y se muestra.

## 4. Endpoints utilizados

### Microservicio propio: `ms-huerto-escolar` (http://localhost:8080)

| Módulo | Método y ruta | Uso en la app |
|---|---|---|
| Usuarios | `GET /usuarios` | Lista de cuentas (login y sincronización) |
| | `GET /usuarios/{id}` | Consulta de un usuario |
| | `POST /usuarios` | Crear cuenta |
| | `POST /usuarios/login` | Iniciar sesión (usuario o correo + contraseña) |
| | `PUT /usuarios/{id}` | Editar perfil y contraseña |
| | `DELETE /usuarios/{id}` | Eliminar cuenta (disponible en la API) |
| Huerto | `GET /sectores` · `GET /sectores/{id}` | Listado y detalle de sectores |
| | `POST /sectores` · `PUT /sectores/{id}` · `DELETE /sectores/{id}` | Crear, editar y eliminar sector |
| | `GET /cultivos` (opcional `?sectorId=`) · `GET /cultivos/{id}` | Listado de cultivos |
| | `POST /cultivos` · `PUT /cultivos/{id}` · `DELETE /cultivos/{id}` | Crear, editar y eliminar cultivo |
| | `PUT /cultivos/{id}/estado` | Cambiar estado de desarrollo |
| | `GET /lecturas` · `POST /lecturas/simular` | Lecturas simuladas de sensores |
| Actividades | `GET /tareas` (opcional `?responsableId=`) · `GET /tareas/{id}` | Listado de tareas |
| | `POST /tareas` · `PUT /tareas/{id}` · `DELETE /tareas/{id}?usuarioId=` | Crear, editar y eliminar tarea |
| | `POST /tareas/{id}/iniciar` | El estudiante inicia la tarea |
| | `POST /tareas/{id}/entregar` | Entrega con comentario y foto (crea la evidencia) |
| | `POST /tareas/{id}/revisar` | Validar o rechazar (crea la validación) |
| | `GET /evidencias` · `GET /validaciones` | Historial |
| | `GET /reportes` · `GET /reportes/{id}` · `POST /reportes` · `PUT /reportes/{id}/estado` · `DELETE /reportes/{id}` | Reportes de problemas |

Los errores de negocio responden con su código HTTP (400, 403, 404, 409) y el cuerpo
`{"mensaje": "..."}`, que la app muestra tal cual al usuario.

### API externa

| API | Endpoint | Uso |
|---|---|---|
| Open-Meteo | `GET https://api.open-meteo.com/v1/forecast?latitude=-33.5922&longitude=-70.6996&current=temperature_2m,relative_humidity_2m,precipitation,weather_code,wind_speed_10m&daily=weather_code,temperature_2m_max,temperature_2m_min,precipitation_sum,precipitation_probability_max&timezone=America/Santiago&forecast_days=7` | Clima actual, pronóstico de 7 días y recomendación de riego |

## 5. Pasos para ejecutar

Se necesitan **tres programas abiertos al mismo tiempo**: XAMPP (MySQL), IntelliJ IDEA
(el microservicio) y Android Studio (la app). El microservicio debe quedar corriendo
mientras se usa la app.

### Paso 1: encender MySQL
1. Abrir **XAMPP Control Panel**.
2. Presionar **Start** en la fila **MySQL** (no hace falta Apache). Debe quedar en verde, puerto 3306.
3. No hay que crear la base de datos: el microservicio crea `huerto_escolar` al arrancar.
   (Si su MySQL tiene contraseña, escríbanla en `spring.datasource.password` del archivo
   `huerto_escolar_backend/src/main/resources/application.properties`.)

### Paso 2: abrir el microservicio en IntelliJ IDEA
1. Abrir IntelliJ IDEA → **File > Open** → elegir la carpeta **`huerto_escolar_backend`** → *Trust Project*.
2. Esperar a que Maven descargue las dependencias (barra de progreso abajo a la derecha).
3. **File > Project Structure > Project > SDK**: elegir un JDK **21** (si no hay, *Add SDK >
   Download JDK* y elegir la versión 21).

### Paso 3: ejecutar el microservicio
1. Abrir `src/main/java/cl/duoc/huerto/MsHuertoEscolarApplication.java` y presionar el
   **▶ verde** junto a `main`.
2. Esperar en la consola `Tomcat started on port 8080` y `Started MsHuertoEscolarApplication`.
3. **No cerrar** la pestaña *Run* mientras se usa la app.

Probar en el navegador del computador (deben responder con datos en JSON):
- http://localhost:8080/usuarios → las 5 cuentas de prueba
- http://localhost:8080/sectores → los 4 sectores
- http://localhost:8080/tareas → `[]` (las tareas parten vacías)

### Paso 4: ejecutar la app en Android Studio
1. Abrir Android Studio → **File > Open** → elegir la carpeta **`huerto_escolar_frontend`**.
2. Esperar la sincronización de Gradle.
3. Elegir un **emulador** y presionar **Run ▶**. La app se conecta al microservicio
   mediante `10.0.2.2:8080`, que es el computador visto desde el emulador.
4. Entrar con `profe.ana` / `huerto123`.

Si la app muestra "No se pudo conectar con el servidor", revisar que el microservicio siga
corriendo en IntelliJ y que MySQL esté encendido en XAMPP.

**Celular real (opcional):** cambiar `HOST` en `app/src/main/java/cl/duoc/huertoescolar/data/remote/ApiConfig.kt`
por la IP del computador (comando `ipconfig` en Windows) y agregar esa IP en
`app/src/main/res/xml/network_security_config.xml`. El celular y el computador
deben estar en la misma red Wi-Fi.

**Sin IntelliJ:** también se puede ejecutar desde una terminal: `cd huerto_escolar_backend`
y luego `.\mvnw.cmd spring-boot:run` (en Mac/Linux `./mvnw spring-boot:run`).

### Cuentas de prueba (las crea el microservicio al arrancar)

Contraseña para todas: **huerto123**. Se puede ingresar con el usuario o con el correo.

| Usuario | Correo ficticio | Nombre | Cargo / Perfil |
|---|---|---|---|
| `profe.ana` | ana.rojas@escuela.cl | Ana Rojas | Docente / Administrador |
| `taller.luis` | luis.pinto@escuela.cl | Luis Pinto | Tallerista / Administrador |
| `est.camila` | camila.soto@escuela.cl | Camila Soto | Estudiante / Colaborador |
| `est.benjamin` | benjamin.diaz@escuela.cl | Benjamín Díaz | Estudiante / Colaborador |
| `est.martina` | martina.rivas@escuela.cl | Martina Rivas | Estudiante / Colaborador |

Docente o tallerista nuevo: requiere el código de autorización **HUERTO2026**.

### Pruebas unitarias y cobertura (mínimo 90 %)

**App móvil** (JUnit 4, MockK y kotlinx-coroutines-test). En la terminal de Android Studio (carpeta `huerto_escolar_frontend`):
```bash
.\gradlew.bat testDebugUnitTest      # solo correr las pruebas
.\gradlew.bat koverHtmlReportDebug   # pruebas + informe de cobertura (Kover)
.\gradlew.bat koverVerifyDebug       # falla si la cobertura baja del 90 %
```
Informe: `app/build/reports/kover/htmlDebug/index.html`
(en PowerShell hay que escribir `.\` antes de `gradlew.bat`). También se puede usar el panel
**Gradle** de Android Studio: `app > Tasks > verification > koverHtmlReportDebug`.

Kover mide el **código lógico**: ViewModels, repositorios, validaciones, reglas del dominio,
mapeadores y utilidades. Se excluyen las pantallas Compose, el tema, las animaciones, la
navegación, las clases generadas por Room y los modelos de datos (DTO y entidades), porque no
contienen lógica que se pueda probar con JUnit en el computador.

Las pruebas cubren:
- **ViewModels** (`viewmodel/`): todos los ViewModel de la app (login, registro, panel, tareas
  y su formulario, detalle de tarea, revisión, sectores, detalle y formulario de sector,
  información y formulario de cultivos, problemas y su formulario, historial con filtros,
  sensores, perfil, clima y sincronización), y que la fábrica (`Factory`) de cada uno crea el
  ViewModel correcto.
- **Repositorios** (`repository/`): sesión, huerto, tareas, reportes, sensores, clima y
  sincronización, simulando el microservicio con MockK (respuestas correctas, errores HTTP
  con mensaje del servidor y falta de conexión).
- **Lógica pura**: validadores de cada formulario, alertas, fechas, conversores de Room,
  mapeadores DTO→entidad, filtro de sincronización, traducción de errores y recomendación de riego.

**Microservicio** (JUnit 5 y Mockito). En el proyecto `huerto_escolar_backend`:
```bash
.\mvnw.cmd verify   # pruebas + informe Kover en target/site/kover/html/index.html
                    # y falla si la cobertura de líneas baja del 90 %
```
Se prueban todos los servicios (reglas de negocio, permisos por rol y flujo de estados de las
tareas), los controladores, las validaciones, el manejador de errores y la carga de datos de prueba.

## 6. APK firmado y llave (.jks)

- Llave: `keystore/huerto-escolar.jks` (alias `huerto-escolar`).
- Configuración: `keystore.properties` y bloque `signingConfigs` en `app/build.gradle.kts`;
  el build `release` se firma automáticamente.
- Generar el APK firmado: en esta carpeta, `.\gradlew.bat assembleRelease` → `app/build/outputs/apk/release/app-release.apk`
  (o *Build > Generate Signed App Bundle / APK* usando la misma llave).
- Después de generarlo, copien el APK a la carpeta `apk/` del repositorio para entregarlo junto a la llave.

<!-- Reemplazar por las capturas reales del APK firmado y del .jks -->
![APK firmado](docs/captura-apk-firmado.png)
![Llave .jks](docs/captura-jks.png)

## Validaciones (logica centralizada y desacoplada de la interfaz)

Las validaciones estan en el paquete `domain/validacion/`, separadas por
formulario. Son objetos de Kotlin puro: no conocen Compose ni Android, por eso
se prueban con JUnit.

| Archivo | Responsabilidad | Lo usa |
|---|---|---|
| `Campos.kt` | Nombres de los campos (claves del mapa de errores) | Validadores y pantallas |
| `ErroresFormulario.kt` | Tipo del resultado, `errores { }` y `revalidar` (validacion en vivo) | Todos los validadores y ViewModels |
| `ReglasComunes.kt` | Reglas reutilizables: texto obligatorio, correo, clave, nombre, fecha | Varios validadores |
| `ValidadorLogin.kt` | Inicio de sesion | `SesionViewModel` |
| `ValidadorRegistro.kt` | Crear cuenta y codigo de autorizacion | `RegistroViewModel` |
| `ValidadorPerfil.kt` | Editar perfil y cambiar contrasena | `PerfilViewModel` |
| `ValidadorSector.kt` | Formulario de sector | `SectoresViewModel` |
| `ValidadorCultivo.kt` | Formulario de cultivo (fechas y riego) | `CultivoFormViewModel` |
| `ValidadorTarea.kt` | Crear o editar tarea | `TareaFormViewModel` |
| `ValidadorEntrega.kt` | Entrega del estudiante (comentario y foto) | `TareaDetalleViewModel` |
| `ValidadorRevision.kt` | Motivo de rechazo | `TareaDetalleViewModel`, `RevisionViewModel` |
| `ValidadorReporte.kt` | Reporte de problema | `ProblemasViewModel` |

**Flujo:** la pantalla solo avisa cambios al ViewModel (`cambiarNombre(...)`).
El ViewModel llama al validador, guarda el mapa de errores en su `StateFlow` y la
pantalla se redibuja sola mostrando el error bajo cada campo:

```
Pantalla (Composable) -> ViewModel -> Validador (domain/validacion) -> errores en StateFlow -> Pantalla
```

**Validacion en vivo:** despues del primer intento de guardar, cada campo con
error se vuelve a validar con cada tecla (`revalidar`). El mensaje cambia si
sigue mal y desaparece apenas el valor queda correcto.

Ademas, los repositorios (`data/repository/`) repiten las reglas de negocio y
de permisos (por ejemplo, que solo un docente valide tareas), para que se
cumplan aunque se intente saltar la interfaz.

## Animaciones

Todas las animaciones propias estan en
`ui/animaciones/Animaciones.kt`, numeradas y comentadas:

| # | Animacion | Donde se ve | Que retroalimentacion da |
|---|---|---|---|
| 1 | `entradaPantalla` / `salidaPantalla` / `entradaAlVolver` / `salidaAlVolver` | Al navegar entre cualquier pantalla (`NavegacionHuerto`) | Indica si el usuario avanza o vuelve |
| 2 | `cambioDeSesion` | Login, registro y panel al iniciar o cerrar sesion | Transicion suave en vez de un salto |
| 3 | `sacudirConError` | Todos los campos de formulario cuando aparece un error | Señala el campo exacto que hay que corregir |
| 4 | `tocableConRebote` | Tarjetas del panel, sectores, tareas y problemas | Confirma que el toque se recibio |
| 5 | `aparecerEnLista` + `animateItem()` | Listados de tareas, sectores, cultivos, problemas, historial, sensores y revision | La lista se arma en cascada; las tarjetas que se agregan o quitan se animan |
| 6 | `NumeroAnimado` | Numeros del panel principal | Los contadores suben hasta su valor y se nota cuando cambian |
| 7 | `pulsoDeAtencion` | Tarjetas *Atrasadas* y *Por validar* del panel, si son mayores que 0 | Llama la atencion sobre lo pendiente |
| 8 | `ContenidoConCarga` | Boton *Ingresar* y botones de guardar de todos los formularios | Muestra que la accion se esta procesando |

Tambien se usan animaciones estandar de Compose: `AnimatedVisibility` (mensajes
de error, avisos, foto seleccionada, codigo de autorizacion, formulario de
rechazo), `animateColorAsState` (color del chip de estado),
`animateContentSize` (tarjetas que se expanden) y `Crossfade` (listado de
tareas al cambiar de filtro).

## Límites del MVP

- La app no se conecta con sensores físicos, placas Arduino o ESP32 ni sistemas de riego.
  Las lecturas de humedad y temperatura son **simulaciones académicas** generadas por
  `ms-huerto-escolar` y se muestran siempre identificadas como datos de prueba.
- Las fotografías de evidencias se guardan en el teléfono; el microservicio guarda solo su
  ruta, por lo que se ven en el mismo dispositivo donde se tomaron.
- La recomendación de riego es orientativa y se basa solo en el pronóstico del clima.
- Todos los usuarios, nombres, sectores, cultivos y fotografías son **ficticios**.
