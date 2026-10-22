package cl.duocuc.pagoflex.ui.session

import cl.duocuc.pagoflex.data.local.entities.UsuarioEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object UserSession {

    private val _currentUser = MutableStateFlow<UsuarioEntity?>(null)
    val currentUser: StateFlow<UsuarioEntity?> = _currentUser.asStateFlow()

    fun setSession(user: UsuarioEntity) {
        _currentUser.value = user
    }

    fun clearSession() {
        _currentUser.value = null
    }
}
