package cl.duoc.huertoescolar.ui.login

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import cl.duoc.huertoescolar.domain.Cargo
import cl.duoc.huertoescolar.domain.Validaciones
import cl.duoc.huertoescolar.ui.componentes.BarraSuperior
import cl.duoc.huertoescolar.ui.componentes.CampoClave
import cl.duoc.huertoescolar.ui.componentes.CampoTexto
import cl.duoc.huertoescolar.ui.componentes.SelectorDesplegable

/**
 * Registro de una cuenta nueva. Todas las reglas (nombre, correo, usuario,
 * contrasena y codigo de autorizacion) estan en Validaciones.registro.
 */
@Composable
fun RegistroScreen(
    alVolver: () -> Unit,
    alRegistrar: (usuario: String) -> Unit,
    viewModel: RegistroViewModel = viewModel(factory = RegistroViewModel.Factory)
) {
    val estado by viewModel.ui.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(estado.mensaje) {
        val aviso = estado.mensaje
        if (aviso != null && !estado.registrado) {
            snackbar.showSnackbar(aviso)
            viewModel.limpiarMensaje()
        }
    }

    LaunchedEffect(estado.registrado) {
        if (estado.registrado) alRegistrar(estado.usuario.trim().lowercase())
    }

    Scaffold(
        topBar = { BarraSuperior(titulo = "Crear cuenta", alVolver = alVolver) },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CampoTexto(
                valor = estado.nombre,
                alCambiar = { viewModel.cambiarNombre(it) },
                etiqueta = "Nombre y apellido",
                error = estado.errores[Validaciones.CAMPO_NOMBRE],
                ayuda = "Solo letras y espacios. Ejemplo: Ana Rojas"
            )
            CampoTexto(
                valor = estado.correo,
                alCambiar = { viewModel.cambiarCorreo(it) },
                etiqueta = "Correo",
                error = estado.errores[Validaciones.CAMPO_CORREO],
                ayuda = "Ejemplo: nombre@escuela.cl",
                teclado = KeyboardType.Email
            )
            CampoTexto(
                valor = estado.usuario,
                alCambiar = { viewModel.cambiarUsuario(it) },
                etiqueta = "Nombre de usuario",
                error = estado.errores[Validaciones.CAMPO_USUARIO],
                ayuda = "4 a 20 caracteres: minusculas, numeros, punto o guion bajo"
            )
            CampoClave(
                valor = estado.clave,
                alCambiar = { viewModel.cambiarClave(it) },
                etiqueta = "Contrasena",
                error = estado.errores[Validaciones.CAMPO_CLAVE],
                ayuda = "Minimo 8 caracteres, con letras y numeros, sin espacios"
            )
            CampoClave(
                valor = estado.confirmar,
                alCambiar = { viewModel.cambiarConfirmar(it) },
                etiqueta = "Repetir contrasena",
                error = estado.errores[Validaciones.CAMPO_CONFIRMAR]
            )
            SelectorDesplegable(
                etiqueta = "Cargo",
                opciones = Cargo.entries.toList(),
                seleccionada = estado.cargo,
                textoDe = { it.etiqueta },
                alSeleccionar = { viewModel.cambiarCargo(it) },
                error = estado.errores[Validaciones.CAMPO_CARGO]
            )
            CampoTexto(
                valor = estado.taller,
                alCambiar = { viewModel.cambiarTaller(it) },
                etiqueta = "Taller o grupo",
                error = estado.errores[Validaciones.CAMPO_TALLER],
                ayuda = "Ejemplo: Taller A"
            )

            // Solo docentes y talleristas autorizados pueden administrar el huerto.
            AnimatedVisibility(visible = estado.pideCodigo) {
                CampoTexto(
                    valor = estado.codigoAutorizacion,
                    alCambiar = { viewModel.cambiarCodigo(it) },
                    etiqueta = "Codigo de autorizacion",
                    error = estado.errores[Validaciones.CAMPO_CODIGO_AUTORIZACION],
                    ayuda = "Lo entrega la escuela a docentes y talleristas"
                )
            }

            Button(
                onClick = { viewModel.registrar() },
                enabled = !estado.guardando,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Crear cuenta")
            }
            TextButton(onClick = alVolver, modifier = Modifier.fillMaxWidth()) {
                Text("Ya tengo cuenta, volver a ingresar")
            }
            Text(
                text = "Usa datos ficticios: el proyecto no guarda informacion personal real.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
