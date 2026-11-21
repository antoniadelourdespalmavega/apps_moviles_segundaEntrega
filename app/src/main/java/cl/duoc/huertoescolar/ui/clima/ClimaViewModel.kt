package cl.duoc.huertoescolar.ui.clima

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import cl.duoc.huertoescolar.Contenedor
import cl.duoc.huertoescolar.data.repository.ClimaRepository
import cl.duoc.huertoescolar.domain.Resultado
import cl.duoc.huertoescolar.domain.clima.Clima
import cl.duoc.huertoescolar.domain.clima.Recomendacion
import cl.duoc.huertoescolar.domain.clima.RecomendacionRiego
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Los tres estados posibles de la consulta a la API externa (como en el ejemplo del docente). */
sealed interface ClimaUiState {
    data object Cargando : ClimaUiState

    data class Exito(
        val clima: Clima,
        val recomendacion: Recomendacion
    ) : ClimaUiState

    data class Error(val mensaje: String) : ClimaUiState
}

/** Consulta el clima de San Bernardo y calcula la recomendacion de riego. */
class ClimaViewModel(
    private val repositorio: ClimaRepository
) : ViewModel() {

    private val _ui = MutableStateFlow<ClimaUiState>(ClimaUiState.Cargando)
    val ui: StateFlow<ClimaUiState> = _ui.asStateFlow()

    init {
        cargar()
    }

    fun cargar() {
        _ui.value = ClimaUiState.Cargando
        viewModelScope.launch {
            _ui.value = when (val resultado = repositorio.obtenerClima()) {
                is Resultado.Exito -> ClimaUiState.Exito(
                    clima = resultado.dato,
                    recomendacion = RecomendacionRiego.calcular(resultado.dato)
                )
                is Resultado.Error -> ClimaUiState.Error(resultado.mensaje)
            }
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer { ClimaViewModel(Contenedor.climaRepository) }
        }
    }
}
