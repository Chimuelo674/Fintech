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

abstract class BaseViewModel : ViewModel() {
    private val _mensajes = MutableSharedFlow<String>(extraBufferCapacity = 8)

    val mensajes: SharedFlow<String> = _mensajes.asSharedFlow()

    protected fun avisar(mensaje: String) {
        _mensajes.tryEmit(mensaje)
    }

    protected fun <T> Flow<T>.comoEstado(inicial: T): StateFlow<T> =
        stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), inicial)

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

@Composable
inline fun <reified VM : ViewModel> vm(key: String? = null, crossinline crear: () -> VM): VM =
    viewModel(key = key, factory = viewModelFactory { initializer { crear() } })

@Composable
fun MensajesEnSnackbar(vm: BaseViewModel, snackbar: SnackbarHostState) {
    LaunchedEffect(vm) {
        vm.mensajes.collect { snackbar.showSnackbar(it) }
    }
}
