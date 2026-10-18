package cl.duoc.huertoescolar.ui.login

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cl.duoc.huertoescolar.data.local.DatosDePrueba
import cl.duoc.huertoescolar.domain.Validaciones
import cl.duoc.huertoescolar.ui.componentes.CampoClave
import cl.duoc.huertoescolar.ui.componentes.CampoTexto
import cl.duoc.huertoescolar.ui.componentes.EncabezadoHoja
import cl.duoc.huertoescolar.ui.componentes.MensajeDeError
import cl.duoc.huertoescolar.ui.sesion.SesionViewModel

/**
 * Pantalla 1: inicio de sesion. Encabezado verde con hojas y el formulario
 * en una tarjeta blanca, con el boton principal en naranjo.
 */
@Composable
fun LoginScreen(sesionViewModel: SesionViewModel, alCrearCuenta: () -> Unit) {
    val estado by sesionViewModel.ui.collectAsStateWithLifecycle()
    val cuentas by sesionViewModel.cuentas.collectAsStateWithLifecycle()

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
        ) {
            EncabezadoHoja {
                Spacer(modifier = Modifier.height(36.dp))
                Text(
                    text = "HUERTO",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Text(
                    text = "Escolar",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Text(
                    text = "Gestion colaborativa del huerto: sectores, cultivos, tareas y reportes.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f),
                    modifier = Modifier.padding(top = 8.dp, bottom = 32.dp)
                )
            }

            // Tarjeta del formulario, montada sobre el borde del encabezado.
            Card(
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                shape = MaterialTheme.shapes.large,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .offset(y = (-28).dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CampoTexto(
                        valor = estado.usuario,
                        alCambiar = { sesionViewModel.cambiarUsuario(it) },
                        etiqueta = "Usuario o correo",
                        error = estado.errores[Validaciones.CAMPO_USUARIO],
                        ayuda = "Ejemplo: profe.ana o ana.rojas@escuela.cl",
                        teclado = KeyboardType.Email
                    )

                    CampoClave(
                        valor = estado.clave,
                        alCambiar = { sesionViewModel.cambiarClave(it) },
                        etiqueta = "Contrasena",
                        error = estado.errores[Validaciones.CAMPO_CLAVE]
                    )

                    estado.informacion?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }

                    // El mensaje de error de autenticacion aparece con animacion.
                    AnimatedVisibility(
                        visible = estado.mensaje != null,
                        enter = fadeIn() + slideInVertically(),
                        exit = fadeOut()
                    ) {
                        MensajeDeError(estado.mensaje)
                    }

                    Button(
                        onClick = { sesionViewModel.iniciarSesion() },
                        enabled = !estado.cargando,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.tertiary,
                            contentColor = MaterialTheme.colorScheme.onTertiary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    ) {
                        if (estado.cargando) {
                            CircularProgressIndicator(
                                modifier = Modifier.height(18.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onTertiary
                            )
                        } else {
                            Text("INGRESAR", fontWeight = FontWeight.Bold)
                        }
                    }

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "No tienes cuenta?",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        TextButton(onClick = alCrearCuenta, enabled = !estado.cargando) {
                            Text(
                                "Crear una cuenta nueva",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                }
            }

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 20.dp, bottom = 24.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Cuentas registradas",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Text(
                        text = "Contrasena de las cuentas de prueba: " + DatosDePrueba.CLAVE_DEMO,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    // Se lee de la base de datos: incluye las cuentas creadas con el registro.
                    cuentas.forEach { cuenta ->
                        Text(
                            text = cuenta.nombreUsuario + " - " + cuenta.rol.etiqueta.lowercase() +
                                " (" + cuenta.cargo.etiqueta.lowercase() + ")",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }
        }
    }
}
