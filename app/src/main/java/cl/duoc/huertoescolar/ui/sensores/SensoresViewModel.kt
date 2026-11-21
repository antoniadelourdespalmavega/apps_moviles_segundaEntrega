package cl.duoc.huertoescolar.ui.sensores

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import cl.duoc.huertoescolar.Contenedor
import cl.duoc.huertoescolar.data.local.entity.LecturaSensor
import cl.duoc.huertoescolar.data.local.entity.Sector
import cl.duoc.huertoescolar.data.repository.HuertoRepository
import cl.duoc.huertoescolar.data.repository.SensorRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Una lectura con el nombre de su sector y los cultivos que hay en ese sector. */
data class LecturaConSector(
    val lectura: LecturaSensor,
    val sectorNombre: String,
    val cultivos: List<String>
)

data class SensoresUiState(
    val cargando: Boolean = true,
    val lecturas: List<LecturaConSector> = emptyList(),
    val sectores: List<Sector> = emptyList(),
    /** Sector por el que se filtra el listado; null = todos los sectores. */
    val filtroSectorId: Long? = null
)

/**
 * Lecturas simuladas de sensores (solo consulta).
 * Los valores los genera ms-huerto-escolar: no hay sensores, placas
 * Arduino o ESP32 conectados, y asi se indica siempre en la pantalla.
 */
class SensoresViewModel(
    sensorRepository: SensorRepository,
    huertoRepository: HuertoRepository
) : ViewModel() {

    private val filtroSectorId = MutableStateFlow<Long?>(null)

    init {
        // ms-huerto-escolar genera una lectura simulada para los sectores que aun no
        // tienen ninguna, y luego se actualiza la copia local.
        viewModelScope.launch {
            sensorRepository.simularLecturasFaltantes()
        }
    }

    val ui: StateFlow<SensoresUiState> = combine(
        sensorRepository.observarLecturas(),
        huertoRepository.observarSectoresSimples(),
        huertoRepository.observarCultivos(),
        filtroSectorId
    ) { lecturas, sectores, cultivos, filtro ->
        val cultivosPorSector = cultivos.groupBy({ it.sectorId }, { it.nombre })
        SensoresUiState(
            cargando = false,
            lecturas = lecturas
                .filter { filtro == null || it.sectorId == filtro }
                .map { lectura ->
                    LecturaConSector(
                        lectura = lectura,
                        sectorNombre = sectores.firstOrNull { it.id == lectura.sectorId }?.nombre
                            ?: "Sector eliminado",
                        cultivos = cultivosPorSector[lectura.sectorId].orEmpty()
                    )
                },
            sectores = sectores,
            filtroSectorId = filtro
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SensoresUiState()
    )

    fun filtrarPorSector(sectorId: Long?) {
        filtroSectorId.value = sectorId
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                SensoresViewModel(Contenedor.sensorRepository, Contenedor.huertoRepository)
            }
        }
    }
}
