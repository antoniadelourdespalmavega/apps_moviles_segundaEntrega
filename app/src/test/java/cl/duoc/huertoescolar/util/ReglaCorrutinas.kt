package cl.duoc.huertoescolar.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.rules.TestWatcher
import org.junit.runner.Description

/**
 * Reemplaza el hilo principal de Android por un despachador de prueba, para
 * que los ViewModel (viewModelScope) se puedan probar con JUnit en el PC.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ReglaCorrutinas(
    val despachador: TestDispatcher = UnconfinedTestDispatcher()
) : TestWatcher() {

    override fun starting(description: Description) {
        Dispatchers.setMain(despachador)
    }

    override fun finished(description: Description) {
        Dispatchers.resetMain()
    }
}
