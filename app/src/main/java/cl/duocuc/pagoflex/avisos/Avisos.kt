package cl.duocuc.pagoflex.avisos

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import cl.duocuc.pagoflex.MainActivity
import cl.duocuc.pagoflex.R
import cl.duocuc.pagoflex.data.local.PagoFlexDatabase
import cl.duocuc.pagoflex.domain.ReglasAvisos
import cl.duocuc.pagoflex.repository.PagoFlexRepository
import cl.duocuc.pagoflex.util.Reloj
import java.util.Calendar
import java.util.concurrent.TimeUnit

object AvisosPrefs {
    private const val PREFS = "pagoflex_avisos"
    private const val KEY_USUARIO = "usuario_sesion"

    private fun prefs(ctx: Context) = ctx.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun guardarUsuario(ctx: Context, idUsuario: String?) {
        prefs(ctx).edit().apply { if (idUsuario == null) remove(KEY_USUARIO) else putString(KEY_USUARIO, idUsuario) }.apply()
    }

    fun usuarioActual(ctx: Context): String? = prefs(ctx).getString(KEY_USUARIO, null)

    private fun clave(idCompromiso: String, fecha: String) = "enviado|$idCompromiso|$fecha"

    fun yaEnviado(ctx: Context, claveAviso: String, fecha: String) = prefs(ctx).getBoolean(clave(claveAviso, fecha), false)

    fun marcarEnviado(ctx: Context, claveAviso: String, fecha: String) {
        prefs(ctx).edit().putBoolean(clave(claveAviso, fecha), true).apply()
    }
}

object Notificador {
    const val CANAL = "vencimientos"

    fun crearCanal(ctx: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val canal = NotificationChannel(CANAL, "Avisos de vencimiento", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "Te recordamos tus compromisos antes de que venzan."
            }
            ctx.getSystemService(NotificationManager::class.java).createNotificationChannel(canal)
        }
    }

    fun tienePermiso(ctx: Context): Boolean =
        (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) &&
            NotificationManagerCompat.from(ctx).areNotificationsEnabled()

    @SuppressLint("MissingPermission")
    fun mostrar(ctx: Context, aviso: ReglasAvisos.Aviso) {
        if (!tienePermiso(ctx)) return
        crearCanal(ctx)
        val abrirApp = PendingIntent.getActivity(
            ctx, 0,
            Intent(ctx, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val notificacion = NotificationCompat.Builder(ctx, CANAL)
            .setSmallIcon(R.drawable.ic_notificacion)
            .setContentTitle(aviso.titulo)
            .setContentText(aviso.mensaje)
            .setStyle(NotificationCompat.BigTextStyle().bigText(aviso.mensaje))
            .setContentIntent(abrirApp)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(ctx).notify(aviso.clave.hashCode(), notificacion)
    }
}

data class ResultadoAvisos(
    val enviados: List<ReglasAvisos.Aviso> = emptyList(),
    val yaEnviadosHoy: Int = 0,
    val fueraDeHorario: Boolean = false,
    val sinPermiso: Boolean = false,
    val sinSesion: Boolean = false
)

object ServicioAvisos {
    suspend fun enviarPendientes(ctx: Context): ResultadoAvisos {
        val idUsuario = AvisosPrefs.usuarioActual(ctx) ?: return ResultadoAvisos(sinSesion = true)
        if (!ReglasAvisos.horarioPermitido(Calendar.getInstance().get(Calendar.HOUR_OF_DAY))) {
            return ResultadoAvisos(fueraDeHorario = true)
        }
        if (!Notificador.tienePermiso(ctx)) return ResultadoAvisos(sinPermiso = true)

        val db = PagoFlexDatabase.getDatabase(ctx)
        PagoFlexRepository(db).sincronizarEstados()
        val usuario = db.usuarioDao().getUsuarioById(idUsuario) ?: return ResultadoAvisos(sinSesion = true)
        val empresas = db.empresaDao().getAllEmpresasOnce().associate { it.idEmpresa to it.nombreFantasia }
        val compromisos = db.compromisoDao().getCompromisosByUsuarioOnce(idUsuario)

        val hoy = Reloj.hoy()
        val nombre: (String) -> String = { empresas[it] ?: it }
        val avisos = ReglasAvisos.avisosDelDia(usuario.avisosActivos, usuario.diasAnticipacion, compromisos, nombre, hoy) +
            ReglasAvisos.avisosDeNovedades(
                usuario.avisosActivos,
                db.pagoDao().getPagosByUsuarioOnce(idUsuario),
                compromisos.associateBy { it.idCompromiso },
                db.reporteDao().getReportesByUsuarioOnce(idUsuario),
                nombre,
                hoy
            )
        val (repetidos, nuevos) = avisos.partition { AvisosPrefs.yaEnviado(ctx, it.clave, hoy) }
        nuevos.forEach {
            Notificador.mostrar(ctx, it)
            AvisosPrefs.marcarEnviado(ctx, it.clave, hoy)
        }
        return ResultadoAvisos(enviados = nuevos, yaEnviadosHoy = repetidos.size)
    }

    fun programar(ctx: Context) {
        Notificador.crearCanal(ctx)
        val tarea = PeriodicWorkRequestBuilder<AvisosWorker>(1, TimeUnit.HOURS).build()
        WorkManager.getInstance(ctx).enqueueUniquePeriodicWork(
            "avisos_vencimiento", ExistingPeriodicWorkPolicy.KEEP, tarea
        )
    }
}

class AvisosWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {
    override suspend fun doWork(): Result {
        ServicioAvisos.enviarPendientes(applicationContext)
        return Result.success()
    }
}
