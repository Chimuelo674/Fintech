package cl.duocuc.pagoflex.ui.session

import android.content.Context
import android.content.ContextWrapper
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

/** Preferencias del ingreso: RUT recordado y si el ingreso con huella está activo. */
object AccesoPrefs {
    private const val PREFS = "pagoflex_acceso"
    private const val KEY_RUT = "rut_recordado"
    private const val KEY_HUELLA = "huella_activa"

    private fun prefs(ctx: Context) = ctx.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun rutRecordado(ctx: Context): String? = prefs(ctx).getString(KEY_RUT, null)

    /** Guarda el RUT, o lo borra (junto con la huella) si se pasa null. */
    fun recordarRut(ctx: Context, rut: String?) {
        val editor = prefs(ctx).edit()
        if (rut == null) {
            editor.remove(KEY_RUT).putBoolean(KEY_HUELLA, false)
        } else {
            editor.putString(KEY_RUT, rut)
        }
        editor.apply()
    }

    fun huellaActiva(ctx: Context): Boolean = prefs(ctx).getBoolean(KEY_HUELLA, false)

    fun activarHuella(ctx: Context, activa: Boolean) {
        prefs(ctx).edit().putBoolean(KEY_HUELLA, activa).apply()
    }
}

/** Ingreso con huella usando androidx.biometric. */
object Biometria {
    /**
     * Solo desde Android 9: en versiones anteriores la librería muestra un diálogo
     * que exige un tema AppCompat, y esta app usa un tema Material sin AppCompat.
     */
    fun disponible(ctx: Context): Boolean =
        android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P &&
            BiometricManager.from(ctx).canAuthenticate(BIOMETRIC_WEAK) == BiometricManager.BIOMETRIC_SUCCESS

    fun autenticar(
        activity: FragmentActivity,
        onExito: () -> Unit,
        onError: (String) -> Unit
    ) {
        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                onExito()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                // Si la persona eligió "Usar mi clave" o canceló, no es un error que mostrar.
                if (errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON &&
                    errorCode != BiometricPrompt.ERROR_USER_CANCELED
                ) {
                    onError(errString.toString())
                }
            }
        }
        val prompt = BiometricPrompt(activity, ContextCompat.getMainExecutor(activity), callback)
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Ingresa a PagoFlex")
            .setSubtitle("Usa tu huella para entrar a tu cuenta")
            .setNegativeButtonText("Usar mi clave")
            .setAllowedAuthenticators(BIOMETRIC_WEAK)
            .build()
        prompt.authenticate(info)
    }
}

fun Context.buscarFragmentActivity(): FragmentActivity? {
    var actual: Context = this
    while (actual is ContextWrapper) {
        if (actual is FragmentActivity) return actual
        actual = actual.baseContext
    }
    return null
}
