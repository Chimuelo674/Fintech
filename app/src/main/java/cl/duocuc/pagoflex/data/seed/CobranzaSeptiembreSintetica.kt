package cl.duocuc.pagoflex.data.seed

import cl.duocuc.pagoflex.data.local.entities.CompromisoEntity
import cl.duocuc.pagoflex.data.local.entities.PagoEntity
import cl.duocuc.pagoflex.data.local.entities.UsuarioEntity
import cl.duocuc.pagoflex.util.Rut

object CobranzaSeptiembreSintetica {
    private data class Cartera(
        val idEmpresa: String,
        val total: Int,
        val anulados: Int,
        val pagados: Int,
        val vencidos: Int,
        val recaudado: Long,
        val concepto: String,
        val idEjecutivo: String,
        val recargo: Double
    )

    private val carteras = listOf(
        Cartera("EC-01", 119, 2, 104, 13, 32_476_000, "Cuota pie departamento", "EJ-01", 8500.0),
        Cartera("EC-02", 309, 1, 295, 13, 54_650_000, "Mensualidad septiembre", "EJ-02", 5000.0),
        Cartera("EC-03", 843, 5, 701, 137, 61_452_000, "Crédito de consumo", "EJ-03", 12000.0),
        Cartera("EC-06", 93, 0, 87, 6, 1_317_000, "Cuota social septiembre", "EJ-05", 2000.0),
        Cartera("EC-07", 409, 3, 360, 46, 51_250_000, "Arancel septiembre", "EJ-06", 6000.0),
        Cartera("EC-08", 63, 0, 60, 3, 31_325_000, "Arriendo septiembre", "EJ-07", 7500.0)
    )

    private val maxPersonas = carteras.maxOf { it.total }

    val usuarios: List<UsuarioEntity> by lazy {
        (1..maxPersonas).map { i ->
            val n = "%04d".format(i)
            val cuerpo = (20_000_000 + i).toString()
            UsuarioEntity(
                idUsuario = "UF-S$n",
                idRol = "USUARIO_FINAL",
                rut = Rut.formatear(cuerpo + Rut.dvCalculado(cuerpo)),
                nombres = "Persona",
                apellidos = "de prueba $n",
                correo = "prueba$n@correo.ejemplo",
                fechaIncorporacion = "2026-01-01",
                activo = false
            )
        }
    }

    private val generado: Pair<List<CompromisoEntity>, List<PagoEntity>> by lazy {
        val compromisos = mutableListOf<CompromisoEntity>()
        val pagos = mutableListOf<PagoEntity>()
        for (c in carteras) {
            val codigo = c.idEmpresa.replace("-", "")
            val base = c.recaudado / c.pagados
            val resto = c.recaudado - base * c.pagados
            for (i in 1..c.total) {
                val n = "%04d".format(i)
                val folio = "CP-S-$codigo-$n"
                val dia = 1 + (i - 1) % 28
                val venc = "2026-09-%02d".format(dia)
                val esPagado = i <= c.pagados
                val esAnulado = !esPagado && i <= c.pagados + c.anulados
                val monto = (if (esPagado && i == c.pagados) base + resto else base).toDouble()
                val estado = when {
                    esPagado -> "Pagado"
                    esAnulado -> "Anulado"
                    else -> "Vencido"
                }
                compromisos += CompromisoEntity(
                    idCompromiso = folio,
                    folio = folio,
                    idUsuarioFinal = "UF-S$n",
                    idEmpresa = c.idEmpresa,
                    concepto = c.concepto,
                    monto = monto,
                    fechaVencimiento = venc,
                    estado = estado,
                    recargoAplicado = if (estado == "Vencido") c.recargo else 0.0,
                    registradoPor = c.idEjecutivo,
                    fechaRegistro = "2026-08-25",
                    motivoAnulacion = if (esAnulado) "Repactación" else null,
                    ejecutivoAnulacion = if (esAnulado) c.idEjecutivo else null,
                    fechaAnulacion = if (esAnulado) "2026-09-0${1 + i % 9}" else null
                )
                if (esPagado) {
                    val num = "CPS-$codigo-$n"
                    pagos += PagoEntity(
                        idPago = num,
                        numeroComprobante = num,
                        idCompromiso = folio,
                        fechaHora = "$venc 10:00",
                        montoPagado = monto,
                        canal = if (i % 2 == 0) "Portal de pagos" else "Agente conversacional",
                        medioPago = "Pago simulado"
                    )
                }
            }
        }
        compromisos to pagos
    }

    val compromisos: List<CompromisoEntity> get() = generado.first
    val pagos: List<PagoEntity> get() = generado.second
}
