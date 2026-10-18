package cl.duoc.huertoescolar.ui.cultivos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import cl.duoc.huertoescolar.Contenedor
import cl.duoc.huertoescolar.data.local.entity.Cultivo
import cl.duoc.huertoescolar.data.local.entity.Sector
import cl.duoc.huertoescolar.data.repository.HuertoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/** Un sector con los cultivos que tiene plantados. */
data class SectorConCultivos(
    val sector: Sector,
    val cultivos: List<Cultivo>
)

data class CultivosInfoUiState(
    val cargando: Boolean = true,
    val grupos: List<SectorConCultivos> = emptyList(),
    val totalCultivos: Int = 0,
    val busqueda: String = ""
)

/**
 * Informacion de cultivos: que se cultiva y en que sector va cada cultivo.
 * Se arma con Flows de Room, asi refleja al instante los cultivos creados o
 * editados desde el formulario de sector.
 */
class CultivosInfoViewModel(huertoRepository: HuertoRepository) : ViewModel() {

    private val busqueda = MutableStateFlow("")

    val ui: StateFlow<CultivosInfoUiState> = combine(
        huertoRepository.observarSectoresSimples(),
        huertoRepository.observarCultivos(),
        busqueda
    ) { sectores, cultivos, texto ->
        val filtro = texto.trim().lowercase()
        val grupos = sectores.map { sector ->
            SectorConCultivos(
                sector = sector,
                cultivos = cultivos.filter {
                    it.sectorId == sector.id &&
                        (filtro.isEmpty() || it.nombre.lowercase().contains(filtro))
                }
            )
        }
        CultivosInfoUiState(
            cargando = false,
            // Al buscar se ocultan los sectores sin coincidencias.
            grupos = if (filtro.isEmpty()) grupos else grupos.filter { it.cultivos.isNotEmpty() },
            totalCultivos = cultivos.size,
            busqueda = texto
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = CultivosInfoUiState()
    )

    fun buscar(texto: String) {
        busqueda.value = texto
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer { CultivosInfoViewModel(Contenedor.huertoRepository) }
        }
    }
}
