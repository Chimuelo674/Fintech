# PagoFlex: versión con estilo de app bancaria

## Cómo probar
- Al abrir la app aparece el **ingreso con RUT y clave**.
- Toca **"Usuarios de prueba"** para elegir una cuenta. La clave de todas es **1234**.
- Si marcas "Recordar mi RUT" y el teléfono tiene una huella registrada (Android 9 o superior), la app ofrece activar el **ingreso con huella**.
- Si te equivocas 3 veces, el ingreso se bloquea 30 segundos.

## Qué cambió
| Área | Cambio |
|---|---|
| Ingreso | `ui/session/LoginScreen.kt`, `LoginViewModel.kt` y `Acceso.kt` reemplazan el selector de rol simulado. |
| Inicio | `UsuarioDashboardScreen.kt` tiene ahora un encabezado con saludo y avatar, una tarjeta tipo cuenta, accesos rápidos en círculos, un banner y la sección "Últimos movimientos". |
| QR y link de pago | `CobroQrScreen.kt` genera un QR (con ZXing) y un link para que otra persona pague un compromiso pendiente. |
| Simulador de costos | `SimuladorCostosScreen.kt` compara el 3% fijo de PagoFlex con un banco tradicional (comisión, IVA y mantención). |
| Comprobante | `ComprobanteScreen.kt` muestra un check animado, el monto destacado, el N° de operación y el detalle en formato ticket. |
| Lógica | `domain/ReglasAcceso.kt`, `SimuladorCostos.kt` y `LinkPago.kt`, con sus tests en `FuncionesBancoTest.kt`. |

## Dependencias nuevas
- `androidx.biometric:biometric:1.1.0`: ingreso con huella.
- `androidx.fragment:fragment-ktx:1.8.5`: la huella necesita que `MainActivity` sea una `FragmentActivity`.
- `com.google.zxing:core:3.5.3`: genera los códigos QR.

## Limitaciones de la versión de prueba
- La clave es la misma para todas las cuentas y se revisa en el teléfono. Una app real la validaría en un servidor.
- El link y el QR muestran cómo funcionaría el pago, pero no hay un servidor que lo reciba.
- Los costos del banco en el simulador son valores referenciales.
