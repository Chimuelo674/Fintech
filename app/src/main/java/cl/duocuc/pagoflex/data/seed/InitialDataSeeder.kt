package cl.duocuc.pagoflex.data.seed

import androidx.room.withTransaction
import cl.duocuc.pagoflex.data.local.PagoFlexDatabase
import cl.duocuc.pagoflex.data.local.entities.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

object InitialDataSeeder {
    private val mutex = Mutex()

    suspend fun seedDatabase(db: PagoFlexDatabase) = mutex.withLock {
        if (db.usuarioDao().getUsuarioById("UF-01") != null) return@withLock
        db.withTransaction { cargar(db) }
    }

    private suspend fun cargar(db: PagoFlexDatabase) {
        cargarCatalogos(db)

        val usuarios = listOf(
            UsuarioEntity("UF-01", "USUARIO_FINAL", "15.834.207-3", "Camila Andrea", "Rojas Peña", "crojas@correo.ejemplo", "+56 9 0000 0101", "Maipú", "2023-03-14", true, 3),
            UsuarioEntity("UF-02", "USUARIO_FINAL", "12.476.318-5", "Luis Alberto", "Muñoz Tapia", "lmunoz@correo.ejemplo", "+56 9 0000 0102", "Puente Alto", "2023-11-02", true, 1),
            UsuarioEntity("UF-03", "USUARIO_FINAL", "6.893.541-5", "Rosa Elena", "Contreras Díaz", "rcontreras@correo.ejemplo", "+56 9 0000 0103", "San Miguel", "2024-06-20", true, 5),
            UsuarioEntity("UF-04", "USUARIO_FINAL", "21.045.873-5", "Matías Ignacio", "Soto Vergara", "msoto@correo.ejemplo", "+56 9 0000 0104", "La Florida", "2025-03-07", false, 3),
            UsuarioEntity("UF-05", "USUARIO_FINAL", "17.562.904-1", "Fernanda Paz", "Godoy Lagos", "fgodoy@correo.ejemplo", "+56 9 0000 0105", "Ñuñoa", "2024-01-15", true, 3),
            UsuarioEntity("UF-06", "USUARIO_FINAL", "10.318.265-4", "Héctor Manuel", "Pizarro Campos", "hpizarro@correo.ejemplo", "+56 9 0000 0106", "Quilicura", "2023-08-28", true, 3),
            UsuarioEntity("UF-07", "USUARIO_FINAL", "19.287.430-0", "Javiera Constanza", "Reyes Molina", "jreyes@correo.ejemplo", "+56 9 0000 0107", "San Bernardo", "2025-05-03", true, 1),
            UsuarioEntity("UF-08", "USUARIO_FINAL", "18.704.396-4", "Benjamín Esteban", "Araya Fuentes", "baraya@correo.ejemplo", "+56 9 0000 0108", "Estación Central", "2026-09-11", true, 3),

            UsuarioEntity("AG-01", "AGENTE", "16.248.317-K", "Andrés Felipe", "Lagos Meza", "alagos@pagoflex.ejemplo", area = "Soporte"),
            UsuarioEntity("AG-02", "AGENTE", "18.352.091-1", "Daniela Sofía", "Ortiz Bravo", "dortiz@pagoflex.ejemplo", area = "Soporte"),
            UsuarioEntity("AG-03", "AGENTE", "13.907.264-2", "Ricardo José", "Salinas Cáceres", "rsalinas@pagoflex.ejemplo", area = "Soporte", activo = false),
            UsuarioEntity("AG-04", "AGENTE", "17.781.435-0", "Valentina Ignacia", "Paredes Silva", "vparedes@pagoflex.ejemplo", area = "Comercial"),
            UsuarioEntity("AG-05", "AGENTE", "19.564.028-9", "Tomás Alejandro", "Vidal Rojas", "tvidal@pagoflex.ejemplo", area = "Comercial"),

            UsuarioEntity("EJ-01", "EJECUTIVO", "14.629.853-2", "Paula Andrea", "Saavedra Núñez", "psaavedra@altosmaipo.ejemplo", "+56 9 0000 0401", idEmpresa = "EC-01", cargo = "Ejecutiva de cobranza"),
            UsuarioEntity("EJ-02", "EJECUTIVO", "11.853.720-3", "Marco Antonio", "Fuentes Riquelme", "mfuentes@bosquenativo.ejemplo", "+56 9 0000 0402", idEmpresa = "EC-02", cargo = "Encargado de finanzas"),
            UsuarioEntity("EJ-03", "EJECUTIVO", "15.370.946-7", "Claudia Beatriz", "Henríquez Soto", "chenriquez@creditoandino.ejemplo", "+56 9 0000 0403", idEmpresa = "EC-03", cargo = "Jefa de cobranza"),
            UsuarioEntity("EJ-04", "EJECUTIVO", "17.095.482-3", "Felipe Ignacio", "Morales Tapia", "fmorales@creditoandino.ejemplo", "+56 9 0000 0404", idEmpresa = "EC-03", cargo = "Ejecutivo de cobranza"),
            UsuarioEntity("EJ-05", "EJECUTIVO", "9.846.217-1", "Gloria Inés", "Carrasco Vega", "gcarrasco@loshalcones.ejemplo", "+56 9 0000 0405", idEmpresa = "EC-06", cargo = "Tesorera"),
            UsuarioEntity("EJ-06", "EJECUTIVO", "18.930.571-0", "Sebastián Andrés", "Núñez Olivares", "snunez@itcordillera.ejemplo", "+56 9 0000 0406", idEmpresa = "EC-07", cargo = "Analista de finanzas"),
            UsuarioEntity("EJ-07", "EJECUTIVO", "12.584.396-4", "Mónica Patricia", "Leiva Castro", "mleiva@plazaoriente.ejemplo", "+56 9 0000 0407", idEmpresa = "EC-08", cargo = "Administradora de cobranza")
        )

        val empresas = listOf(
            EmpresaEntity("EC-01", "Inmobiliaria Altos del Maipo SpA", "Altos del Maipo", "76.845.213-K", "Inmobiliaria", "atencion@altosmaipo.ejemplo", "+56 2 0000 0201", 8500.0),
            EmpresaEntity("EC-02", "Corporación Educacional Bosque Nativo", "Colegio Bosque Nativo", "65.132.874-8", "Educación escolar", "pagos@bosquenativo.ejemplo", "+56 2 0000 0202", 5000.0),
            EmpresaEntity("EC-03", "Crédito Andino S.A.", "Crédito Andino", "96.571.438-3", "Crédito de consumo", "clientes@creditoandino.ejemplo", "+56 2 0000 0203", 12000.0),
            EmpresaEntity("EC-04", "Seguros Horizonte Sur S.A.", "Horizonte Sur", "96.814.205-4", "Seguros", "contacto@horizontesur.ejemplo", "+56 2 0000 0204", 3000.0),
            EmpresaEntity("EC-05", "Fundación Manos Abiertas", "Manos Abiertas", "65.247.190-0", "ONG", "socios@manosabiertas.ejemplo", "+56 2 0000 0205", 0.0),
            EmpresaEntity("EC-06", "Club Deportivo Los Halcones", "Club Los Halcones", "65.398.412-K", "Club deportivo", "tesoreria@loshalcones.ejemplo", "+56 2 0000 0206", 2000.0),
            EmpresaEntity("EC-07", "Instituto Técnico Cordillera SpA", "Instituto Cordillera", "77.214.583-7", "Educación superior", "finanzas@itcordillera.ejemplo", "+56 2 0000 0207", 6000.0),
            EmpresaEntity("EC-08", "Arriendos Plaza Oriente Ltda.", "Plaza Oriente", "76.432.871-K", "Administración de arriendos", "cobranza@plazaoriente.ejemplo", "+56 2 0000 0208", 7500.0)
        )
        asegurarRubrosYComunas(db, rubros = empresas.map { it.rubro })
        db.empresaDao().insertEmpresas(empresas)

        val todosLosUsuarios = usuarios + CobranzaSeptiembreSintetica.usuarios
        asegurarRubrosYComunas(db, comunas = todosLosUsuarios.map { it.comuna })
        db.usuarioDao().insertUsuarios(todosLosUsuarios)

        val compromisos = listOf(
            CompromisoEntity("CP-2026-0101", "CP-2026-0101", "UF-01", "EC-08", "Arriendo departamento octubre", 10, 12, 520000.0, "2026-10-10", "Pendiente", 0.0, "EJ-07", "2026-09-20"),
            CompromisoEntity("CP-2026-0102", "CP-2026-0102", "UF-01", "EC-06", "Cuota social octubre", 10, 12, 15000.0, "2026-10-05", "Vencido", 2000.0, "EJ-05", "2026-09-20"),
            CompromisoEntity("CP-2026-0103", "CP-2026-0103", "UF-01", "EC-05", "Aporte mensual octubre", null, null, 10000.0, "2026-10-15", "Pendiente", 0.0, "PagoFlex", "2026-09-20"),
            CompromisoEntity("CP-2026-0104", "CP-2026-0104", "UF-01", "EC-03", "Crédito de consumo", 7, 24, 86400.0, "2026-09-25", "Pagado", 0.0, "EJ-03", "2026-09-01"),
            CompromisoEntity("CP-2026-0105", "CP-2026-0105", "UF-01", "EC-03", "Crédito de consumo", 8, 24, 86400.0, "2026-10-25", "Pendiente", 0.0, "EJ-03", "2026-09-20"),
            CompromisoEntity("CP-2026-0106", "CP-2026-0106", "UF-02", "EC-01", "Cuota pie departamento", 14, 36, 310000.0, "2026-09-30", "Vencido", 8500.0, "EJ-01", "2026-09-01"),
            CompromisoEntity("CP-2026-0107", "CP-2026-0107", "UF-02", "EC-01", "Cuota pie departamento", 15, 36, 310000.0, "2026-10-30", "Pendiente", 0.0, "EJ-01", "2026-09-20"),
            CompromisoEntity("CP-2026-0108", "CP-2026-0108", "UF-03", "EC-04", "Seguro de hogar octubre", 10, 12, 18900.0, "2026-10-01", "Pagado", 0.0, "PagoFlex", "2026-09-20"),
            CompromisoEntity("CP-2026-0109", "CP-2026-0109", "UF-04", "EC-07", "Arancel octubre", 8, 10, 142000.0, "2026-10-05", "En revisión", 0.0, "EJ-06", "2026-09-20"),
            CompromisoEntity("CP-2026-0110", "CP-2026-0110", "UF-05", "EC-02", "Mensualidad octubre", 8, 10, 185000.0, "2026-10-10", "Pendiente", 0.0, "EJ-02", "2026-09-20"),
            CompromisoEntity("CP-2026-0111", "CP-2026-0111", "UF-06", "EC-03", "Crédito de consumo", 19, 36, 54200.0, "2026-09-20", "Anulado", 0.0, "EJ-04", "2026-09-01", "Repactación", "Anulado por repactación", "EJ-04", "2026-09-22"),
            CompromisoEntity("CP-2026-0112", "CP-2026-0112", "UF-07", "EC-06", "Cuota social octubre", 10, 12, 15000.0, "2026-10-05", "Pagado", 0.0, "EJ-05", "2026-09-20"),

            CompromisoEntity("CP-2026-0072", "CP-2026-0072", "UF-02", "EC-01", "Cuota pie departamento", 13, 36, 310000.0, "2026-08-31", "Pagado", 8500.0, "EJ-01", "2026-08-01"),
            CompromisoEntity("CP-2026-0089", "CP-2026-0089", "UF-01", "EC-08", "Arriendo departamento septiembre", 9, 12, 520000.0, "2026-09-10", "Pagado", 0.0, "EJ-07", "2026-08-20"),
            CompromisoEntity("CP-2026-0090", "CP-2026-0090", "UF-01", "EC-06", "Cuota social septiembre", 9, 12, 15000.0, "2026-09-05", "Pagado", 2000.0, "EJ-05", "2026-08-20"),
            CompromisoEntity("CP-2026-0091", "CP-2026-0091", "UF-01", "EC-05", "Aporte mensual septiembre", null, null, 10000.0, "2026-09-15", "Pagado", 0.0, "PagoFlex", "2026-08-20"),
            CompromisoEntity("CP-2026-0098", "CP-2026-0098", "UF-05", "EC-02", "Mensualidad septiembre", 7, 10, 185000.0, "2026-09-10", "Pagado", 0.0, "EJ-02", "2026-08-20"),

            CompromisoEntity("CP-2026-0095", "CP-2026-0095", "UF-08", "EC-06", "Cuota social septiembre", 9, 12, 15000.0, "2026-09-30", "En revisión", 0.0, "EJ-05", "2026-09-12")
        ) + HistorialSintetico.compromisos + CobranzaSeptiembreSintetica.compromisos
        db.compromisoDao().insertCompromisos(compromisos)

        val pagos = listOf(
            PagoEntity("CPR-000451", "CPR-000451", "CP-2026-0104", "2026-09-24 10:42", 86400.0, "Portal de pagos", "Tarjeta de débito"),
            PagoEntity("CPR-000452", "CPR-000452", "CP-2026-0108", "2026-09-30 19:05", 18900.0, "Agente conversacional", "Transferencia"),
            PagoEntity("CPR-000453", "CPR-000453", "CP-2026-0112", "2026-10-04 08:15", 15000.0, "App", "Tarjeta de débito"),
            PagoEntity("CPR-000377", "CPR-000377", "CP-2026-0072", "2026-09-02 21:14", 318500.0, "Agente conversacional", "Pago simulado"),
            PagoEntity("CPR-000398", "CPR-000398", "CP-2026-0089", "2026-09-09 13:05", 520000.0, "Portal de pagos", "Pago simulado"),
            PagoEntity("CPR-000402", "CPR-000402", "CP-2026-0090", "2026-09-07 18:40", 17000.0, "Portal de pagos", "Pago simulado"),
            PagoEntity("CPR-000405", "CPR-000405", "CP-2026-0091", "2026-09-15 08:22", 10000.0, "Agente conversacional", "Pago simulado"),
            PagoEntity("CPR-000447", "CPR-000447", "CP-2026-0098", "2026-09-10 22:31", 185000.0, "Portal de pagos", "Pago simulado")
        ) + HistorialSintetico.pagos + CobranzaSeptiembreSintetica.pagos
        db.pagoDao().insertPagos(pagos)

        val reportes = listOf(
            ReporteEntity("REP-0011", "CP-2026-0109", "Ya pagué por otro medio", "Pagué por transferencia directa al instituto el 03-10.", "2026-10-03", "En gestión", "AG-02", "2026-10-04"),
            ReporteEntity("REP-0009", "CP-2026-0111", "El monto no corresponde", "Repacté el crédito y me siguen cobrando la cuota antigua.", "2026-09-18", "Resuelto", "AG-01", "2026-09-19", "La empresa anuló la cuota por repactación (22-09-2026).", "2026-09-22"),
            ReporteEntity("REP-0010", "CP-2026-0095", "No reconozco este cobro", "Me llegó un cobro de un club al que no pertenezco.", "2026-09-30", "Abierto")
        )
        db.reporteDao().insertReportes(reportes)

        val recomendaciones = listOf(
            RecomendacionEntity("REC-0001", "UF-01", "Jardín Infantil Pequeños Exploradores", null, "Educación parvularia", "Maipú", "Paula Herrera", "pherrera@pexploradores.ejemplo", null, true, "2026-08-12", "Se unió", idAgente = "AG-04", fechaUltimoCambio = "2026-09-25"),
            RecomendacionEntity("REC-0002", "UF-01", "Comunidad Edificio Parque Los Olmos", null, "Administración de condominio", "Maipú", null, null, null, false, "2026-09-02", "Contactada", idAgente = "AG-05", fechaUltimoCambio = "2026-09-10"),
            RecomendacionEntity("REC-0003", "UF-03", "Gimnasio Fuerza Sur", null, "Deporte", "San Miguel", "Raúl Ibáñez", null, "+56 9 0000 0301", true, "2026-09-15", "En evaluación", idAgente = "AG-04", fechaUltimoCambio = "2026-09-28"),
            RecomendacionEntity("REC-0004", "UF-05", "Academia de Música Clave de Sol", null, "Educación artística", "Ñuñoa", null, null, null, false, "2026-09-20", "Recibida"),
            RecomendacionEntity("REC-0005", "UF-04", "Colegio Bosque Nativo", null, "Educación escolar", "La Florida", null, null, null, false, "2026-09-22", "Descartada", "La organización ya es cliente de PagoFlex", "AG-05", "2026-09-23"),
            RecomendacionEntity("REC-0006", "UF-07", "Corporación Deportiva Cordillera", null, "Club deportivo", "San Bernardo", "Ana Valdés", "avaldes@cdcordillera.ejemplo", null, true, "2026-10-01", "Recibida")
        )
        asegurarRubrosYComunas(db, rubros = recomendaciones.map { it.rubro }, comunas = recomendaciones.map { it.comuna })
        db.recomendacionDao().insertRecomendaciones(recomendaciones)

        val reconocimientos = listOf(
            ReconocimientoEntity(idUsuarioFinal = "UF-01", nombreReconocimiento = "Pago Puntual", fechaObtencion = "2026-09-01", motivo = "Indicador de cumplimiento 91.7%"),
            ReconocimientoEntity(idUsuarioFinal = "UF-03", nombreReconocimiento = "Cliente Ejemplar", fechaObtencion = "2026-09-01", motivo = "Indicador de cumplimiento 100%"),
            ReconocimientoEntity(idUsuarioFinal = "UF-05", nombreReconocimiento = "Cliente Ejemplar", fechaObtencion = "2026-09-01", motivo = "Indicador de cumplimiento 95.0%")
        )
        db.reconocimientoDao().insertReconocimientos(
            reconocimientos + ReconocimientoEntity(
                idUsuarioFinal = "UF-01",
                nombreReconocimiento = "Embajador PagoFlex",
                fechaObtencion = "2026-09-25",
                motivo = "REC-0001 · Jardín Infantil Pequeños Exploradores se unió"
            )
        )

        db.historialRecomendacionDao().insertAll(
            listOf(
                HistorialRecomendacionEntity(idRecomendacion = "REC-0001", estado = "Se unió", idAgente = "AG-04", fecha = "2026-09-25"),
                HistorialRecomendacionEntity(idRecomendacion = "REC-0002", estado = "Contactada", idAgente = "AG-05", fecha = "2026-09-10"),
                HistorialRecomendacionEntity(idRecomendacion = "REC-0003", estado = "En evaluación", idAgente = "AG-04", fecha = "2026-09-28"),
                HistorialRecomendacionEntity(
                    idRecomendacion = "REC-0005", estado = "Descartada", idAgente = "AG-05", fecha = "2026-09-23",
                    motivo = "La organización ya es cliente de PagoFlex"
                )
            )
        )
    }

    private suspend fun cargarCatalogos(db: PagoFlexDatabase) {
        val cat = db.catalogoDao()
        cat.insertRoles(ValoresCatalogo.ROLES)
        cat.insertEstadosCompromiso(ValoresCatalogo.ESTADOS_COMPROMISO.map(::EstadoCompromisoEntity))
        cat.insertEstadosReporte(ValoresCatalogo.ESTADOS_REPORTE.map(::EstadoReporteEntity))
        cat.insertEstadosRecomendacion(ValoresCatalogo.ESTADOS_RECOMENDACION.map { (n, o) -> EstadoRecomendacionEntity(n, o) })
        cat.insertMotivosReporte(ValoresCatalogo.MOTIVOS_REPORTE.map(::MotivoReporteEntity))
        cat.insertMotivosAnulacion(ValoresCatalogo.MOTIVOS_ANULACION.map(::MotivoAnulacionEntity))
        cat.insertCanales(ValoresCatalogo.CANALES.map(::CanalEntity))
        cat.insertMediosPago(ValoresCatalogo.MEDIOS_PAGO.map(::MedioPagoEntity))
        cat.insertAreas(ValoresCatalogo.AREAS.map(::AreaEntity))
        cat.insertReconocimientos(ValoresCatalogo.RECONOCIMIENTOS)
    }

    private suspend fun asegurarRubrosYComunas(
        db: PagoFlexDatabase,
        rubros: List<String?> = emptyList(),
        comunas: List<String?> = emptyList()
    ) {
        db.catalogoDao().insertRubros(rubros.filterNotNull().distinct().map(::RubroEntity))
        db.catalogoDao().insertComunas(comunas.filterNotNull().distinct().map(::ComunaEntity))
    }
}
