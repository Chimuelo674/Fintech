package cl.duocuc.pagoflex.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

// MER: rut y correo únicos (RN-18)
@Entity(
    tableName = "usuario",
    indices = [
        Index(value = ["rut"], unique = true), Index(value = ["correo"], unique = true),
        Index("idRol"), Index("comuna"), Index("area"), Index("idEmpresa")
    ],
    foreignKeys = [
        ForeignKey(entity = RolEntity::class, parentColumns = ["codigo"], childColumns = ["idRol"]),
        ForeignKey(entity = ComunaEntity::class, parentColumns = ["nombre"], childColumns = ["comuna"]),
        ForeignKey(entity = AreaEntity::class, parentColumns = ["nombre"], childColumns = ["area"]),
        // S-10: un ejecutivo pertenece a una sola empresa cliente
        ForeignKey(entity = EmpresaEntity::class, parentColumns = ["idEmpresa"], childColumns = ["idEmpresa"])
    ]
)
data class UsuarioEntity(
    @PrimaryKey val idUsuario: String, // e.g. "UF-01", "AG-01", "EJ-01"
    val idRol: String,                 // "USUARIO_FINAL", "AGENTE", "EJECUTIVO"
    val rut: String,
    val nombres: String,
    val apellidos: String,
    val correo: String,
    val telefono: String? = null,
    val comuna: String? = null,
    val fechaIncorporacion: String? = null,
    val avisosActivos: Boolean = true,
    val diasAnticipacion: Int = 3,
    val area: String? = null,           // "Soporte", "Comercial" (Agente)
    val idEmpresa: String? = null,      // "EC-01", etc. (Ejecutivo)
    val cargo: String? = null,          // (Ejecutivo)
    val activo: Boolean = true
) {
    val nombreCompleto: String
        get() = "$nombres $apellidos"
}
