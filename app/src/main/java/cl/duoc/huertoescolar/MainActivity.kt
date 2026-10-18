package cl.duoc.huertoescolar

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import cl.duoc.huertoescolar.ui.navegacion.NavegacionHuerto
import cl.duoc.huertoescolar.ui.theme.HuertoEscolarTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HuertoEscolarTheme {
                NavegacionHuerto()
            }
        }
    }
}
