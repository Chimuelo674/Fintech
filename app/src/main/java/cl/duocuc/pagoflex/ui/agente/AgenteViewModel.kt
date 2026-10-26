package cl.duocuc.pagoflex.ui.agente

import cl.duocuc.pagoflex.data.local.entities.*
import cl.duocuc.pagoflex.repository.PagoFlexRepository
import cl.duocuc.pagoflex.ui.common.BaseViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine

class AgenteViewModel(private val repo: PagoFlexRepository, private val idAgente: String) : BaseViewModel() {
    data class Estado(
        val reportes: List<ReporteEntity> = emptyList(),
        val recomendaciones: List<RecomendacionEntity> = emptyList(),
        val historialPorRec: Map<String, List<HistorialRecomendacionEntity>> = emptyMap(),
        val compromisos: Map<String, CompromisoEntity> = emptyMap(),
        val usuarios: Map<String, UsuarioEntity> = emptyMap(),
        val empresas: Map<String, EmpresaEntity> = emptyMap()
    )

    private data class Casos(
        val reportes: List<ReporteEntity>,
        val recomendaciones: List<RecomendacionEntity>,
        val historial: List<HistorialRecomendacionEntity>
    )

    private val casos = combine(
        repo.getAllReportes(), repo.getAllRecomendaciones(), repo.getHistorialRecomendaciones()
    ) { r, rec, h -> Casos(r, rec, h) }

    val estado: StateFlow<Estado> = combine(
        casos, repo.getAllCompromisos(), repo.getAllUsuarios(), repo.getAllEmpresas()
    ) { k, compromisos, usuarios, empresas ->
        Estado(
            reportes = k.reportes,
            recomendaciones = k.recomendaciones,
            historialPorRec = k.historial.groupBy { it.idRecomendacion },
            compromisos = compromisos.associateBy { it.idCompromiso },
            usuarios = usuarios.associateBy { it.idUsuario },
            empresas = empresas.associateBy { it.idEmpresa }
        )
    }.comoEstado(Estado())

    fun tomarReporte(idReporte: String) = ejecutar(
        accion = { repo.tomarReporte(idReporte, idAgente) },
        ok = { "Tomaste el reporte $idReporte." }
    )

    fun resolverReporte(idReporte: String, respuesta: String, onExito: () -> Unit) = ejecutar(
        accion = { repo.resolverReporte(idReporte, idAgente, respuesta) },
        ok = { "Reporte $idReporte resuelto. El usuario ya puede ver tu respuesta." },
        onExito = { onExito() }
    )

    fun cambiarEstadoRecomendacion(rec: RecomendacionEntity, nuevoEstado: String, motivo: String? = null, onExito: () -> Unit = {}) = ejecutar(
        accion = { repo.cambiarEstadoRecomendacion(rec.idRecomendacion, idAgente, nuevoEstado, motivo) },
        ok = { "${rec.nombreOrganizacion}: ahora está \"$nuevoEstado\"." },
        onExito = { onExito() }
    )
}
