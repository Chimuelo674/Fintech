package cl.duocuc.pagoflex.ui.usuario

import android.content.Context
import androidx.lifecycle.viewModelScope
import cl.duocuc.pagoflex.avisos.ResultadoAvisos
import cl.duocuc.pagoflex.avisos.ServicioAvisos
import cl.duocuc.pagoflex.data.local.entities.*
import cl.duocuc.pagoflex.domain.ReglasRecomendacion
import cl.duocuc.pagoflex.repository.Cumplimiento
import cl.duocuc.pagoflex.repository.PagoFlexRepository
import cl.duocuc.pagoflex.ui.common.BaseViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class InicioViewModel(repo: PagoFlexRepository, idUsuario: String) : BaseViewModel() {
    data class Estado(
        val compromisos: List<CompromisoEntity> = emptyList(),
        val empresas: Map<String, EmpresaEntity> = emptyMap(),
        val reconocimientos: List<ReconocimientoEntity> = emptyList(),
        val cumplimiento: Cumplimiento? = null,
        val descripcionReconocimiento: Map<String, String> = emptyMap()
    )

    val estado: StateFlow<Estado> = combine(
        repo.getCompromisosByUsuario(idUsuario),
        repo.getAllEmpresas(),
        repo.getReconocimientosByUsuario(idUsuario),
        repo.getCumplimiento(idUsuario),
        repo.getCatalogoReconocimientos()
    ) { compromisos, empresas, reconocimientos, cumplimiento, catalogo ->
        Estado(compromisos, empresas.associateBy { it.idEmpresa }, reconocimientos, cumplimiento, catalogo)
    }.comoEstado(Estado())

    init {
        viewModelScope.launch { repo.sincronizarEstados() }
    }
}

class DetalleCompromisoViewModel(repo: PagoFlexRepository, idUsuario: String, idCompromiso: String) : BaseViewModel() {
    data class Estado(
        val cargado: Boolean = false,
        val compromiso: CompromisoEntity? = null,
        val empresa: EmpresaEntity? = null,
        val pago: PagoEntity? = null,
        val reporte: ReporteEntity? = null
    )

    val estado: StateFlow<Estado> = combine(
        repo.getCompromisosByUsuario(idUsuario),
        repo.getPagosByUsuario(idUsuario),
        repo.getReportesByUsuario(idUsuario),
        repo.getAllEmpresas()
    ) { compromisos, pagos, reportes, empresas ->
        val c = compromisos.firstOrNull { it.idCompromiso == idCompromiso }
        Estado(
            cargado = true,
            compromiso = c,
            empresa = empresas.firstOrNull { it.idEmpresa == c?.idEmpresa },
            pago = pagos.firstOrNull { it.idCompromiso == idCompromiso },
            reporte = reportes.filter { it.idCompromiso == idCompromiso }.maxByOrNull { it.fechaCreacion }
        )
    }.comoEstado(Estado())
}

class PagoViewModel(
    private val repo: PagoFlexRepository,
    private val idUsuario: String,
    idCompromiso: String
) : BaseViewModel() {
    data class Estado(
        val compromiso: CompromisoEntity? = null,
        val empresa: EmpresaEntity? = null,
        val procesando: Boolean = false,
        val error: String? = null
    )

    private val _estado = MutableStateFlow(Estado())
    val estado: StateFlow<Estado> = _estado.asStateFlow()

    init {
        viewModelScope.launch {
            val c = repo.getCompromisoById(idCompromiso)
            _estado.value = _estado.value.copy(compromiso = c, empresa = c?.let { repo.getEmpresaById(it.idEmpresa) })
        }
    }

    fun pagar(medioPago: String, onPagado: (String) -> Unit) {
        val c = _estado.value.compromiso ?: return
        if (_estado.value.procesando) return
        _estado.value = _estado.value.copy(procesando = true, error = null)
        viewModelScope.launch {
            repo.realizarPagoSimulado(idUsuario, c, medioPago)
                .onSuccess { pago ->
                    _estado.value = _estado.value.copy(procesando = false)
                    onPagado(pago.numeroComprobante)
                }
                .onFailure { e ->
                    _estado.value = _estado.value.copy(procesando = false, error = e.message ?: "No pudimos procesar el pago.")
                }
        }
    }
}

class ComprobanteViewModel(repo: PagoFlexRepository, idUsuario: String, idPagoOCompromiso: String) : BaseViewModel() {
    data class Estado(
        val cargando: Boolean = true,
        val pago: PagoEntity? = null,
        val compromiso: CompromisoEntity? = null,
        val empresa: EmpresaEntity? = null
    )

    private val _estado = MutableStateFlow(Estado())
    val estado: StateFlow<Estado> = _estado.asStateFlow()

    init {
        viewModelScope.launch {
            val p = repo.getPagoByNumero(idPagoOCompromiso) ?: repo.getPagoByCompromiso(idPagoOCompromiso)
            val c = p?.let { repo.getCompromisoById(it.idCompromiso) }
            if (p != null && c != null && c.idUsuarioFinal == idUsuario) {
                _estado.value = Estado(false, p, c, repo.getEmpresaById(c.idEmpresa))
            } else {
                _estado.value = Estado(cargando = false)
            }
        }
    }
}

class HistorialViewModel(repo: PagoFlexRepository, idUsuario: String) : BaseViewModel() {
    data class Estado(
        val pagos: List<PagoEntity> = emptyList(),
        val compromisos: Map<String, CompromisoEntity> = emptyMap(),
        val empresas: Map<String, EmpresaEntity> = emptyMap()
    )

    val estado: StateFlow<Estado> = combine(
        repo.getPagosByUsuario(idUsuario),
        repo.getCompromisosByUsuario(idUsuario),
        repo.getAllEmpresas()
    ) { pagos, compromisos, empresas ->
        Estado(pagos, compromisos.associateBy { it.idCompromiso }, empresas.associateBy { it.idEmpresa })
    }.comoEstado(Estado())
}

class MisReportesViewModel(private val repo: PagoFlexRepository, private val idUsuario: String) : BaseViewModel() {
    data class Estado(
        val cargado: Boolean = false,
        val reportes: List<ReporteEntity> = emptyList(),
        val compromisos: Map<String, CompromisoEntity> = emptyMap(),
        val empresas: Map<String, EmpresaEntity> = emptyMap()
    )

    val estado: StateFlow<Estado> = combine(
        repo.getReportesByUsuario(idUsuario),
        repo.getCompromisosByUsuario(idUsuario),
        repo.getAllEmpresas()
    ) { reportes, compromisos, empresas ->
        Estado(true, reportes, compromisos.associateBy { it.idCompromiso }, empresas.associateBy { it.idEmpresa })
    }.comoEstado(Estado())

    fun crearReporte(idCompromiso: String, motivo: String, comentario: String, onTerminar: (exito: Boolean) -> Unit) {
        viewModelScope.launch {
            val r = repo.crearReporte(idUsuario, idCompromiso, motivo, comentario)
                .onSuccess { avisar("Recibimos tu reporte ${it.idReporte}. Mientras lo revisamos, este cobro queda En revisión.") }
                .onFailure { avisar(it.message ?: "No pudimos enviar el reporte.") }
            onTerminar(r.isSuccess)
        }
    }
}

class RecomendarViewModel(private val repo: PagoFlexRepository, private val idUsuario: String) : BaseViewModel() {
    data class Estado(
        val recomendaciones: List<RecomendacionEntity> = emptyList(),
        val reconocimientos: List<ReconocimientoEntity> = emptyList()
    )

    val estado: StateFlow<Estado> = combine(
        repo.getRecomendacionesByUsuario(idUsuario),
        repo.getReconocimientosByUsuario(idUsuario)
    ) { recs, recon -> Estado(recs, recon) }.comoEstado(Estado())

    fun recomendar(datos: ReglasRecomendacion.Datos, onExito: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            repo.crearRecomendacion(idUsuario, datos)
                .onSuccess {
                    avisar("¡Gracias! Registramos tu recomendación de ${it.nombreOrganizacion}.")
                    onExito()
                }
                .onFailure { onError(it.message ?: "No pudimos registrar la recomendación.") }
        }
    }
}

class AvisosViewModel(
    private val repo: PagoFlexRepository,
    private val appContext: Context,
    idUsuario: String
) : BaseViewModel() {
    data class Estado(
        val compromisos: List<CompromisoEntity> = emptyList(),
        val nombreEmpresa: Map<String, String> = emptyMap(),
        val pagos: List<PagoEntity> = emptyList(),
        val reportes: List<ReporteEntity> = emptyList()
    )

    val estado: StateFlow<Estado> = combine(
        repo.getCompromisosByUsuario(idUsuario),
        repo.getAllEmpresas().map { e -> e.associate { it.idEmpresa to it.nombreFantasia } },
        repo.getPagosByUsuario(idUsuario),
        repo.getReportesByUsuario(idUsuario)
    ) { c, e, p, r -> Estado(c, e, p, r) }.comoEstado(Estado())

    fun guardar(usuario: UsuarioEntity, activos: Boolean, dias: Int, onGuardado: (UsuarioEntity) -> Unit) {
        viewModelScope.launch {
            val actualizado = usuario.copy(avisosActivos = activos, diasAnticipacion = dias)
            repo.updateUsuario(actualizado)
            onGuardado(actualizado)
            avisar(
                if (activos) "Listo: te avisaremos $dias ${if (dias == 1) "día" else "días"} antes."
                else "Listo: desactivaste los avisos."
            )
        }
    }

    fun enviarAhora(
        usuario: UsuarioEntity,
        activos: Boolean,
        dias: Int,
        onGuardado: (UsuarioEntity) -> Unit,
        explicar: (ResultadoAvisos) -> String
    ) {
        viewModelScope.launch {
            val actualizado = usuario.copy(avisosActivos = activos, diasAnticipacion = dias)
            repo.updateUsuario(actualizado)
            onGuardado(actualizado)
            avisar(explicar(ServicioAvisos.enviarPendientes(appContext)))
        }
    }
}
