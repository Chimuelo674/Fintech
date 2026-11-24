package cl.duocuc.pagoflex.ui.usuario

import androidx.lifecycle.viewModelScope
import cl.duocuc.pagoflex.data.remote.Indicador
import cl.duocuc.pagoflex.data.remote.IndicadoresRepository
import cl.duocuc.pagoflex.ui.common.BaseViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Indicadores económicos del día (API externa mindicador.cl) para la tarjeta del inicio. */
class IndicadoresViewModel(private val repo: IndicadoresRepository) : BaseViewModel() {

    data class Estado(
        val cargando: Boolean = true,
        val indicadores: List<Indicador> = emptyList(),
        val error: String? = null
    )

    private val _estado = MutableStateFlow(Estado())
    val estado: StateFlow<Estado> = _estado.asStateFlow()

    init {
        cargar(forzar = false)
    }

    fun recargar() = cargar(forzar = true)

    private fun cargar(forzar: Boolean) {
        _estado.update { it.copy(cargando = true, error = null) }
        viewModelScope.launch {
            repo.obtener(forzar)
                .onSuccess { lista -> _estado.value = Estado(cargando = false, indicadores = lista) }
                .onFailure { e -> _estado.update { it.copy(cargando = false, error = e.message) } }
        }
    }
}
