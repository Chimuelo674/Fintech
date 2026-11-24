package cl.duocuc.pagoflex.data.remote

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

/** Microservicio usuarios-service (puerto 8081): usuarios, empresas e inicio de sesión. */
interface UsuariosApi {
    @GET("api/salud")
    suspend fun salud(): Map<String, Any>

    /** 200 con el usuario, o 401 con {"mensaje": "..."} si el RUT o la clave no son correctos. */
    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<UsuarioDto>

    @GET("api/usuarios")
    suspend fun listarUsuarios(): List<UsuarioDto>

    @POST("api/usuarios")
    suspend fun crearUsuario(@Body usuario: UsuarioDto): UsuarioDto

    @POST("api/usuarios/lote")
    suspend fun crearUsuariosLote(@Body usuarios: List<UsuarioDto>): LoteRespuesta

    @PUT("api/usuarios/{id}")
    suspend fun actualizarUsuario(@Path("id") id: String, @Body usuario: UsuarioDto): UsuarioDto

    @GET("api/empresas")
    suspend fun listarEmpresas(): List<EmpresaDto>

    @POST("api/empresas/lote")
    suspend fun crearEmpresasLote(@Body empresas: List<EmpresaDto>): LoteRespuesta
}

/** Microservicio pagos-service (puerto 8082): compromisos, pagos y reportes. */
interface PagosApi {
    @GET("api/salud")
    suspend fun salud(): Map<String, Any>

    // ---------- Compromisos ----------
    @GET("api/compromisos")
    suspend fun listarCompromisos(): List<CompromisoDto>

    @POST("api/compromisos")
    suspend fun crearCompromiso(@Body compromiso: CompromisoDto): CompromisoDto

    @POST("api/compromisos/lote")
    suspend fun crearCompromisosLote(@Body compromisos: List<CompromisoDto>): LoteRespuesta

    @PUT("api/compromisos/{id}")
    suspend fun actualizarCompromiso(@Path("id") id: String, @Body compromiso: CompromisoDto): CompromisoDto

    // ---------- Pagos ----------
    @GET("api/pagos")
    suspend fun listarPagos(): List<PagoDto>

    @GET("api/pagos")
    suspend fun listarPagosDeUsuario(@Query("usuario") idUsuario: String): List<PagoDto>

    /** El servidor deja el compromiso como "Pagado" al registrar el pago. */
    @POST("api/pagos")
    suspend fun crearPago(@Body pago: PagoDto): PagoDto

    @POST("api/pagos/lote")
    suspend fun crearPagosLote(@Body pagos: List<PagoDto>): LoteRespuesta

    // ---------- Reportes ----------
    @GET("api/reportes")
    suspend fun listarReportes(): List<ReporteDto>

    @POST("api/reportes")
    suspend fun crearReporte(@Body reporte: ReporteDto): ReporteDto

    @POST("api/reportes/lote")
    suspend fun crearReportesLote(@Body reportes: List<ReporteDto>): LoteRespuesta

    @PUT("api/reportes/{id}")
    suspend fun actualizarReporte(@Path("id") id: String, @Body reporte: ReporteDto): ReporteDto

    @DELETE("api/reportes/{id}")
    suspend fun eliminarReporte(@Path("id") id: String): Response<Unit>
}
