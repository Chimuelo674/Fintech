package cl.duocuc.pagoflex.data.remote

import cl.duocuc.pagoflex.data.local.entities.CompromisoEntity
import cl.duocuc.pagoflex.data.local.entities.EmpresaEntity
import cl.duocuc.pagoflex.data.local.entities.PagoEntity
import cl.duocuc.pagoflex.data.local.entities.ReporteEntity
import cl.duocuc.pagoflex.data.local.entities.UsuarioEntity

/*
 * Objetos que viajan en JSON entre la app y los microservicios.
 * Los campos opcionales son nullables porque Gson no respeta los valores
 * por defecto de Kotlin; los valores por defecto se ponen al convertir a entidad.
 */

data class LoginRequest(val rut: String, val clave: String)

data class LoteRespuesta(val insertados: Int?)

data class UsuarioDto(
    val idUsuario: String,
    val idRol: String,
    val rut: String,
    val nombres: String,
    val apellidos: String,
    val correo: String,
    val telefono: String?,
    val comuna: String?,
    val fechaIncorporacion: String?,
    val avisosActivos: Boolean?,
    val diasAnticipacion: Int?,
    val area: String?,
    val idEmpresa: String?,
    val cargo: String?,
    val activo: Boolean?
)

data class EmpresaDto(
    val idEmpresa: String,
    val razonSocial: String,
    val nombreFantasia: String,
    val rut: String,
    val rubro: String,
    val correoAtencion: String?,
    val telefonoAtencion: String?,
    val recargoAtraso: Double?,
    val activa: Boolean?
)

data class CompromisoDto(
    val idCompromiso: String,
    val folio: String,
    val idUsuarioFinal: String,
    val idEmpresa: String,
    val concepto: String,
    val numeroCuota: Int?,
    val totalCuotas: Int?,
    val monto: Double,
    val fechaVencimiento: String,
    val estado: String,
    val recargoAplicado: Double?,
    val registradoPor: String?,
    val fechaRegistro: String?,
    val motivoAnulacion: String?,
    val comentarioAnulacion: String?,
    val ejecutivoAnulacion: String?,
    val fechaAnulacion: String?
)

data class PagoDto(
    val idPago: String,
    val numeroComprobante: String,
    val idCompromiso: String,
    val fechaHora: String,
    val montoPagado: Double,
    val canal: String?,
    val medioPago: String?
)

data class ReporteDto(
    val idReporte: String,
    val idCompromiso: String,
    val motivo: String,
    val comentario: String?,
    val fechaCreacion: String,
    val estado: String,
    val idAgente: String?,
    val fechaToma: String?,
    val respuesta: String?,
    val fechaResolucion: String?
)

// ---------- Conversión entidad (Room) <-> DTO (JSON) ----------

fun UsuarioEntity.aDto() = UsuarioDto(
    idUsuario, idRol, rut, nombres, apellidos, correo, telefono, comuna, fechaIncorporacion,
    avisosActivos, diasAnticipacion, area, idEmpresa, cargo, activo
)

fun UsuarioDto.aEntidad() = UsuarioEntity(
    idUsuario = idUsuario,
    idRol = idRol,
    rut = rut,
    nombres = nombres,
    apellidos = apellidos,
    correo = correo,
    telefono = telefono,
    comuna = comuna,
    fechaIncorporacion = fechaIncorporacion,
    avisosActivos = avisosActivos ?: true,
    diasAnticipacion = diasAnticipacion ?: 3,
    area = area,
    idEmpresa = idEmpresa,
    cargo = cargo,
    activo = activo ?: true
)

fun EmpresaEntity.aDto() = EmpresaDto(
    idEmpresa, razonSocial, nombreFantasia, rut, rubro, correoAtencion, telefonoAtencion, recargoAtraso, activa
)

fun EmpresaDto.aEntidad() = EmpresaEntity(
    idEmpresa = idEmpresa,
    razonSocial = razonSocial,
    nombreFantasia = nombreFantasia,
    rut = rut,
    rubro = rubro,
    correoAtencion = correoAtencion.orEmpty(),
    telefonoAtencion = telefonoAtencion.orEmpty(),
    recargoAtraso = recargoAtraso ?: 0.0,
    activa = activa ?: true
)

fun CompromisoEntity.aDto() = CompromisoDto(
    idCompromiso, folio, idUsuarioFinal, idEmpresa, concepto, numeroCuota, totalCuotas, monto,
    fechaVencimiento, estado, recargoAplicado, registradoPor, fechaRegistro, motivoAnulacion,
    comentarioAnulacion, ejecutivoAnulacion, fechaAnulacion
)

fun CompromisoDto.aEntidad() = CompromisoEntity(
    idCompromiso = idCompromiso,
    folio = folio,
    idUsuarioFinal = idUsuarioFinal,
    idEmpresa = idEmpresa,
    concepto = concepto,
    numeroCuota = numeroCuota,
    totalCuotas = totalCuotas,
    monto = monto,
    fechaVencimiento = fechaVencimiento,
    estado = estado,
    recargoAplicado = recargoAplicado ?: 0.0,
    registradoPor = registradoPor ?: "PagoFlex",
    fechaRegistro = fechaRegistro ?: fechaVencimiento,
    motivoAnulacion = motivoAnulacion,
    comentarioAnulacion = comentarioAnulacion,
    ejecutivoAnulacion = ejecutivoAnulacion,
    fechaAnulacion = fechaAnulacion
)

fun PagoEntity.aDto() = PagoDto(idPago, numeroComprobante, idCompromiso, fechaHora, montoPagado, canal, medioPago)

fun PagoDto.aEntidad() = PagoEntity(
    idPago = idPago,
    numeroComprobante = numeroComprobante,
    idCompromiso = idCompromiso,
    fechaHora = fechaHora,
    montoPagado = montoPagado,
    canal = canal ?: "App",
    medioPago = medioPago ?: "Pago simulado"
)

fun ReporteEntity.aDto() = ReporteDto(
    idReporte, idCompromiso, motivo, comentario, fechaCreacion, estado, idAgente, fechaToma, respuesta, fechaResolucion
)

fun ReporteDto.aEntidad() = ReporteEntity(
    idReporte = idReporte,
    idCompromiso = idCompromiso,
    motivo = motivo,
    comentario = comentario,
    fechaCreacion = fechaCreacion,
    estado = estado,
    idAgente = idAgente,
    fechaToma = fechaToma,
    respuesta = respuesta,
    fechaResolucion = fechaResolucion
)
