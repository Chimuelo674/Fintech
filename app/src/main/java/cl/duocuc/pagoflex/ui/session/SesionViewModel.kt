package cl.duocuc.pagoflex.ui.session

import androidx.lifecycle.viewModelScope
import cl.duocuc.pagoflex.data.local.entities.UsuarioEntity
import cl.duocuc.pagoflex.repository.PagoFlexRepository
import cl.duocuc.pagoflex.ui.common.BaseViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class SesionViewModel(private val repo: PagoFlexRepository) : BaseViewModel() {
    val usuarios: StateFlow<List<UsuarioEntity>> =
        repo.getAllUsuarios().map { lista -> lista.filter { it.activo } }.comoEstado(emptyList())

    val empresas: StateFlow<Map<String, String>> =
        repo.getAllEmpresas().map { lista -> lista.associate { it.idEmpresa to it.nombreFantasia } }.comoEstado(emptyMap())

    fun entrar(usuario: UsuarioEntity, onListo: () -> Unit) {
        viewModelScope.launch {
            repo.sincronizarEstados()
            UserSession.setSession(usuario)
            onListo()
        }
    }
}
