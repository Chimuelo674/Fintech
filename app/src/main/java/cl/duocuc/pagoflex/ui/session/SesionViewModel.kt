package cl.duocuc.pagoflex.ui.session

import androidx.lifecycle.viewModelScope
import cl.duocuc.pagoflex.data.local.entities.UsuarioEntity
import cl.duocuc.pagoflex.repository.PagoFlexRepository
import cl.duocuc.pagoflex.ui.common.BaseViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/** ViewModel de la selección de sesión simulada (RF-01). */
class SesionViewModel(private val repo: PagoFlexRepository) : BaseViewModel() {

    /** Solo cuentas activas (AG-03 está inactivo y las personas de prueba no inician sesión). */
    val usuarios: StateFlow<List<UsuarioEntity>> =
        repo.getAllUsuarios().map { lista -> lista.filter { it.activo } }.comoEstado(emptyList())

    /** Nombre de fantasía de cada empresa, para mostrar a qué panel pertenece cada ejecutivo. */
    val empresas: StateFlow<Map<String, String>> =
        repo.getAllEmpresas().map { lista -> lista.associate { it.idEmpresa to it.nombreFantasia } }.comoEstado(emptyMap())

    /** Deja los estados al día por fecha (RN-02, RN-03) y luego abre la sesión. */
    fun entrar(usuario: UsuarioEntity, onListo: () -> Unit) {
        viewModelScope.launch {
            repo.sincronizarEstados()
            UserSession.setSession(usuario)
            onListo()
        }
    }
}
