package cl.duoc.huertoescolar.ui.perfil

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import cl.duoc.huertoescolar.data.local.entity.Usuario
import cl.duoc.huertoescolar.domain.Validaciones
import cl.duoc.huertoescolar.ui.componentes.BarraSuperior
import cl.duoc.huertoescolar.ui.componentes.CampoClave
import cl.duoc.huertoescolar.ui.componentes.CampoTexto
import cl.duoc.huertoescolar.ui.componentes.FilaDeDato

/** Mi perfil: el usuario edita su nombre, correo, taller y contrasena. */
@Composable
fun PerfilScreen(
    usuario: Usuario,
    alVolver: () -> Unit,
    alActualizar: (Usuario) -> Unit,
    viewModel: PerfilViewModel = viewModel(factory = PerfilViewModel.Factory)
) {
    val estado by viewModel.ui.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(usuario.id) {
        viewModel.cargar(usuario)
    }

    LaunchedEffect(estado.mensaje) {
        val aviso = estado.mensaje
        if (aviso != null) {
            snackbar.showSnackbar(aviso)
            viewModel.limpiarMensaje()
        }
    }

    Scaffold(
        topBar = { BarraSuperior(titulo = "Mi perfil", alVolver = alVolver) },
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
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp)) {
                    FilaDeDato("Usuario", usuario.nombreUsuario)
                    FilaDeDato("Cargo", usuario.cargo.etiqueta)
                    FilaDeDato("Perfil", usuario.rol.etiqueta)
                }
            }

            Text("Datos personales", style = MaterialTheme.typography.titleMedium)
            CampoTexto(
                valor = estado.nombre,
                alCambiar = { viewModel.cambiarNombre(it) },
                etiqueta = "Nombre y apellido",
                error = estado.errores[Validaciones.CAMPO_NOMBRE]
            )
            CampoTexto(
                valor = estado.correo,
                alCambiar = { viewModel.cambiarCorreo(it) },
                etiqueta = "Correo",
                error = estado.errores[Validaciones.CAMPO_CORREO],
                teclado = KeyboardType.Email
            )
            CampoTexto(
                valor = estado.taller,
                alCambiar = { viewModel.cambiarTaller(it) },
                etiqueta = "Taller o grupo",
                error = estado.errores[Validaciones.CAMPO_TALLER]
            )

            Text("Cambiar contraseña (opcional)", style = MaterialTheme.typography.titleMedium)
            CampoClave(
                valor = estado.claveActual,
                alCambiar = { viewModel.cambiarClaveActual(it) },
                etiqueta = "Contraseña actual",
                error = estado.errores[Validaciones.CAMPO_CLAVE_ACTUAL]
            )
            CampoClave(
                valor = estado.claveNueva,
                alCambiar = { viewModel.cambiarClaveNueva(it) },
                etiqueta = "Nueva contraseña",
                error = estado.errores[Validaciones.CAMPO_CLAVE],
                ayuda = "Mínimo 8 caracteres, con letras y números, sin espacios"
            )
            CampoClave(
                valor = estado.confirmar,
                alCambiar = { viewModel.cambiarConfirmar(it) },
                etiqueta = "Repetir nueva contraseña",
                error = estado.errores[Validaciones.CAMPO_CONFIRMAR]
            )

            Button(
                onClick = { viewModel.guardar(alActualizar) },
                enabled = !estado.guardando,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Guardar cambios")
            }
        }
    }
}
