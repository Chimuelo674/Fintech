package cl.duocuc.pagoflex.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

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
        ForeignKey(entity = EmpresaEntity::class, parentColumns = ["idEmpresa"], childColumns = ["idEmpresa"])
    ]
)
data class UsuarioEntity(
    @PrimaryKey val idUsuario: String,
    val idRol: String,
    val rut: String,
    val nombres: String,
    val apellidos: String,
    val correo: String,
    val telefono: String? = null,
    val comuna: String? = null,
    val fechaIncorporacion: String? = null,
    val avisosActivos: Boolean = true,
    val diasAnticipacion: Int = 3,
    val area: String? = null,
    val idEmpresa: String? = null,
    val cargo: String? = null,
    val activo: Boolean = true
) {
    val nombreCompleto: String
        get() = "$nombres $apellidos"
}
