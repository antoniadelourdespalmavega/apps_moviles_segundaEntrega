# Huerto Escolar - App movil de gestion colaborativa

Aplicacion Android (MVP academico) para organizar el trabajo del huerto de una escuela
publica de San Bernardo: sectores, cultivos, tareas asignadas a estudiantes, evidencias
fotograficas, validacion docente, reportes de problemas, historial y datos simulados de
sensores.

Asignatura: **008D - Desarrollo de Aplicaciones Moviles**
Caso: Aplicacion movil para la gestion colaborativa del huerto escolar.
## Docente

CHRISTIAN EDUARDO ACUNA SANDOVAL

## Integrantes

| Nombre        | Rol en el equipo                  |
|---------------|-----------------------------------|
| Antonia Palma | Desarrollador y Líder Técnico     |
| Andrés Vargas | Desarrollador y Líder de Producto |


## Planificación y control de versiones

- **Trello (planificación y distribución de tareas):** https://trello.com/invite/b/6ad54aa2f16ded88b5ffb604/ATTI5a9375fae546a3506fb33c17659c1e79E820D002/proyectohuertoapmoviles
- **GitHub (código y commits):** https://github.com/antoniadelourdespalmavega/apps_moviles_segundaEntrega.git



## Como ejecutar

1. Abrir la carpeta `huerto-escolar` desde Android Studio (*File > Open*).
2. Esperar la sincronizacion de Gradle (descarga AGP 9.0.1, Kotlin 2.2.10, Room y KSP).
3. Elegir un emulador o dispositivo con **Android 7.0 (API 24) o superior**.
4. Ejecutar `app` (boton Run).
5. Iniciar sesion con alguna de las cuentas ficticias de la tabla siguiente.

La base de datos se crea sola la primera vez que se abre la aplicacion, con usuarios,
sectores, cultivos y lecturas de sensores de prueba. **Las tareas, el historial y los
reportes parten vacios**: se llenan a medida que se usa la app (por ejemplo, en la
presentacion), y todas las cuentas que entren en el mismo dispositivo ven los cambios al
instante. Para volver al estado inicial basta con desinstalar la app y volver a ejecutarla.

### Cuentas ficticias de prueba

Contrasena para todas las cuentas: **huerto123**. Se puede ingresar con el usuario o
con el correo.

| Usuario | Correo ficticio | Nombre | Cargo / Perfil |
|---|---|---|---|
| `profe.ana` | ana.rojas@escuela.cl | Ana Rojas | Docente / Administrador |
| `taller.luis` | luis.pinto@escuela.cl | Luis Pinto | Tallerista / Administrador |
| `est.camila` | camila.soto@escuela.cl | Camila Soto | Estudiante / Colaborador |
| `est.benjamin` | benjamin.diaz@escuela.cl | Benjamin Diaz | Estudiante / Colaborador |
| `est.martina` | martina.rivas@escuela.cl | Martina Rivas | Estudiante / Colaborador |

### Registro de cuentas nuevas

Desde el login, *Crear una cuenta nueva*. Se valida:

- Nombre y apellido: solo letras y espacios, 5 a 60 caracteres.
- Correo: formato `nombre@dominio.ext`, unico.
- Usuario: 4 a 20 caracteres (minusculas, numeros, punto o guion bajo), unico.
- Contrasena: 8 a 30 caracteres, con letras y numeros, sin espacios, repetida igual.
- Docente o tallerista: requiere el codigo de autorizacion **HUERTO2026**.

Cada usuario puede editar su nombre, correo, taller y contrasena en *Mi perfil*
(icono de persona arriba a la derecha del panel).

Todos los usuarios, nombres, sectores, cultivos y fotografias son **ficticios**. No se usan
RUT, correos reales, cuentas reales ni imagenes de personas.

## Funcionalidades

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

## Tecnologias

- Kotlin 2.2.10 y Android Studio (AGP 9.0.1, Gradle 9.5).
- Jetpack Compose + Material Design 3.
- Arquitectura MVVM: UI (Composables) -> ViewModel (StateFlow) -> Repository -> Room.
- Room / SQLite para persistencia local, con KSP como procesador de anotaciones.
- Navigation Compose para la navegacion y el control de acceso por rol.
- Recursos nativos: camara (TakePicture + FileProvider) y Photo Picker (PickVisualMedia).
- JUnit para pruebas unitarias de las reglas de validacion.

## Estructura del proyecto

```
app/src/main/java/cl/duoc/huertoescolar/
├── HuertoApp.kt            Application + contenedor de repositorios
├── MainActivity.kt         Punto de entrada de Compose
├── core/                   Utilidades (fechas, manejo de imagenes)
├── domain/                 Enums y calculo de alertas (sin Android)
│   └── validacion/         Un validador por formulario + reglas comunes
├── data/
│   ├── local/              Room: entidades, DAOs, converters, datos de prueba
│   └── repository/         Reglas de negocio y acceso a datos
└── ui/
    ├── theme/              Colores, tipografia y colores semanticos de estado
    ├── componentes/        Campos, chips, selectores, fotos, estados vacios
    ├── animaciones/        Todas las animaciones de la app (Animaciones.kt)
    ├── navegacion/         NavHost y rutas con control de acceso
    └── <funcionalidad>/    Pantalla + ViewModel de cada modulo
```

### Modelo de datos (8 entidades)

| Entidad | Relaciones |
|---|---|
| `usuarios` | - |
| `sectores` | responsable -> usuarios |
| `cultivos` | sector -> sectores |
| `tareas` | sector, cultivo, responsable y creador |
| `evidencias` | tarea, usuario |
| `validaciones` | tarea, validador |
| `reportes_problema` | sector, cultivo, usuario que reporta |
| `lecturas_sensor` | sector (datos simulados) |

Todas las tablas usan clave primaria propia y claves foraneas; ningun dato descriptivo se
repite entre tablas (referencia 3FN).

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

## Pruebas unitarias

```
./gradlew test
```

Cubren cada validador por formulario (fechas coherentes, frecuencia de riego positiva,
campos obligatorios, motivo de rechazo, login, registro y perfil), la validacion en vivo
(`revalidar`) y el calculo de tareas atrasadas. Archivos: `ValidacionesTest`,
`ValidadoresPorFormularioTest`, `CuentasYCategoriasTest` y `AlertasTest`.

## Limites del MVP

La aplicacion no se conecta con sensores fisicos, placas Arduino o ESP32, bombas, valvulas
ni sistemas de riego. Las lecturas de humedad y temperatura son **simulaciones academicas**
generadas dentro de la app y se muestran siempre identificadas como datos de prueba. La
aplicacion tampoco usa GPS, microfono ni datos biometricos, y no toma decisiones
automaticas sobre el cuidado del huerto.

## Nota sobre versiones

El proyecto usa las mismas versiones del proyecto base de la asignatura (AGP 9.0.1,
Kotlin 2.2.10, Gradle 9.5) y agrega Room 2.8.0 con KSP 2.3.6, que es la version de KSP
compatible con el Kotlin integrado de AGP 9. Si Gradle reclamara por la version de KSP,
el unico cambio necesario esta en `gradle/libs.versions.toml`, en la linea `ksp = "..."`.
