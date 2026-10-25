package cl.duocuc.pagoflex.ui.common

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Base de los ViewModels de la app (arquitectura MVVM):
 * - la pantalla (View) solo dibuja el estado y avisa las acciones de la persona;
 * - el ViewModel combina los datos del repositorio en un estado (StateFlow) y ejecuta
 *   las acciones en viewModelScope (sobreviven a una rotación de pantalla);
 * - el repositorio (Model) es la única puerta a Room y a las reglas de negocio.
 */
abstract class BaseViewModel : ViewModel() {

    private val _mensajes = MutableSharedFlow<String>(extraBufferCapacity = 8)

    /** Mensajes de una sola vez para mostrar en un Snackbar (éxito o error de una acción). */
    val mensajes: SharedFlow<String> = _mensajes.asSharedFlow()

    protected fun avisar(mensaje: String) {
        _mensajes.tryEmit(mensaje)
    }

    /** Convierte un Flow en StateFlow ligado a la vida del ViewModel. */
    protected fun <T> Flow<T>.comoEstado(inicial: T): StateFlow<T> =
        stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), inicial)

    /**
     * Ejecuta una acción del repositorio que devuelve Result: si sale bien muestra [ok] y llama
     * a [onExito]; si falla muestra el mensaje del error (las reglas de negocio lo redactan).
     */
    protected fun <T> ejecutar(
        accion: suspend () -> Result<T>,
        ok: (T) -> String?,
        onExito: (T) -> Unit = {}
    ) {
        viewModelScope.launch {
            accion()
                .onSuccess { valor ->
                    ok(valor)?.let { avisar(it) }
                    onExito(valor)
                }
                .onFailure { avisar(it.message ?: "No pudimos completar la acción.") }
        }
    }
}

/** Obtiene (o crea) el ViewModel de la pantalla con sus dependencias. */
@Composable
inline fun <reified VM : ViewModel> vm(key: String? = null, crossinline crear: () -> VM): VM =
    viewModel(key = key, factory = viewModelFactory { initializer { crear() } })

/** Muestra en el Snackbar los mensajes que emite el ViewModel. */
@Composable
fun MensajesEnSnackbar(vm: BaseViewModel, snackbar: SnackbarHostState) {
    LaunchedEffect(vm) {
        vm.mensajes.collect { snackbar.showSnackbar(it) }
    }
}
