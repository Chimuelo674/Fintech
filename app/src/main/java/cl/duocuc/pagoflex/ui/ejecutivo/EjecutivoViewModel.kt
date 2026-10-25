package cl.duocuc.pagoflex.ui.ejecutivo

import androidx.lifecycle.viewModelScope
import cl.duocuc.pagoflex.data.local.entities.*
import cl.duocuc.pagoflex.repository.PagoFlexRepository
import cl.duocuc.pagoflex.ui.common.BaseViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/**
 * ViewModel del panel del ejecutivo (RF-17 a RF-20). Solo expone datos de SU empresa
 * (RN-22): compromisos, pagos y los reportes de esos compromisos.
 */
class EjecutivoViewModel(
    private val repo: PagoFlexRepository,
    private val idEjecutivo: String,
    private val idEmpresa: String
) : BaseViewModel() {

    data class Estado(
        val empresa: EmpresaEntity? = null,
        val compromisos: List<CompromisoEntity> = emptyList(),
        val pagos: List<PagoEntity> = emptyList(),
        val reportesPorCompromiso: Map<String, List<ReporteEntity>> = emptyMap(),
        val usuarios: Map<String, UsuarioEntity> = emptyMap()
    )

    private val empresa = MutableStateFlow<EmpresaEntity?>(null)

    val estado: StateFlow<Estado> = combine(
        empresa,
        repo.getCompromisosByEmpresa(idEmpresa),
        repo.getPagosByEmpresa(idEmpresa),
        repo.getAllReportes(),
        repo.getAllUsuarios()
    ) { emp, compromisos, pagos, reportes, usuarios ->
        val ids = compromisos.map { it.idCompromiso }.toSet()
        Estado(
            empresa = emp,
            compromisos = compromisos,
            pagos = pagos,
            reportesPorCompromiso = reportes.filter { it.idCompromiso in ids }.groupBy { it.idCompromiso },
            usuarios = usuarios.associateBy { it.idUsuario }
        )
    }.comoEstado(Estado())

    init {
        viewModelScope.launch {
            repo.sincronizarEstados()
            empresa.value = repo.getEmpresaById(idEmpresa)
        }
    }

    suspend fun buscarPersona(rut: String): UsuarioEntity? = repo.buscarUsuarioFinalPorRut(rut)

    fun anular(comp: CompromisoEntity, motivo: String, comentario: String, onExito: () -> Unit) = ejecutar(
        accion = { repo.anularCompromiso(comp.idCompromiso, idEjecutivo, idEmpresa, motivo, comentario) },
        ok = { "Compromiso ${comp.folio} anulado." },
        onExito = { onExito() }
    )

    /** [onError] muestra el problema dentro del formulario, sin cerrarlo. */
    fun registrar(
        rut: String,
        concepto: String,
        cuota: Int?,
        totalCuotas: Int?,
        monto: Long?,
        fechaVencimiento: String?,
        personaNueva: PagoFlexRepository.PersonaNueva?,
        onExito: (CompromisoEntity) -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            repo.registrarCompromiso(idEjecutivo, idEmpresa, rut, concepto, cuota, totalCuotas, monto, fechaVencimiento, personaNueva)
                .onSuccess {
                    avisar("Compromiso ${it.folio} registrado como Pendiente.")
                    onExito(it)
                }
                .onFailure { onError(it.message ?: "No pudimos registrar el compromiso.") }
        }
    }
}
