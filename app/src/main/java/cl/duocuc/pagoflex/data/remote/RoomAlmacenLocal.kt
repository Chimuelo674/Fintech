package cl.duocuc.pagoflex.data.remote

import androidx.room.withTransaction
import cl.duocuc.pagoflex.data.local.PagoFlexDatabase

/** Implementación de [AlmacenLocal] con la base de datos Room del teléfono. */
class RoomAlmacenLocal(private val db: PagoFlexDatabase) : AlmacenLocal {

    override suspend fun usuarios() = db.usuarioDao().getAllUsuariosOnce()
    override suspend fun empresas() = db.empresaDao().getAllEmpresasOnce()
    override suspend fun compromisos() = db.compromisoDao().getAllCompromisosOnce()
    override suspend fun pagos() = db.pagoDao().getAllPagosOnce()
    override suspend fun reportes() = db.reporteDao().getAllReportesOnce()

    override suspend fun guardarDatosServidor(datos: DatosServidor) {
        db.withTransaction {
            // El orden importa por las llaves foráneas: primero empresas y usuarios, después lo que depende de ellos.
            guardarSeguro(datos.empresas, { db.empresaDao().upsertEmpresas(it) })
            guardarSeguro(datos.usuarios, { db.usuarioDao().upsertUsuarios(it) })
            guardarSeguro(datos.compromisos, { db.compromisoDao().upsertCompromisos(it) })
            guardarSeguro(datos.pagos, { db.pagoDao().upsertPagos(it) })
            guardarSeguro(datos.reportes, { db.reporteDao().upsertReportes(it) })
            // Reportes eliminados en el servidor (por ejemplo, desde otro teléfono).
            db.reporteDao().deleteReportesExcepto(datos.reportes.map { it.idReporte })
        }
    }

    /**
     * Guarda la lista completa; si un registro choca con una regla de la base local
     * (por ejemplo, una llave foránea), guarda los demás uno por uno y omite ese.
     */
    private suspend fun <T> guardarSeguro(items: List<T>, guardar: suspend (List<T>) -> Unit) {
        if (items.isEmpty()) return
        try {
            guardar(items)
        } catch (e: Exception) {
            items.forEach { item ->
                try {
                    guardar(listOf(item))
                } catch (_: Exception) {
                    // Registro incompatible con la base local: se omite.
                }
            }
        }
    }
}
