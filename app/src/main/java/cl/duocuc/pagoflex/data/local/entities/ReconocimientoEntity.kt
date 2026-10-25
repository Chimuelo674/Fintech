package cl.duocuc.pagoflex.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "usuario_reconocimiento",
    indices = [Index("idUsuarioFinal"), Index("nombreReconocimiento")],
    foreignKeys = [
        ForeignKey(entity = UsuarioEntity::class, parentColumns = ["idUsuario"], childColumns = ["idUsuarioFinal"]),
        // La descripción vive en el catálogo "reconocimiento" (no se repite por usuario, 3FN)
        ForeignKey(entity = ReconocimientoCatalogoEntity::class, parentColumns = ["nombre"], childColumns = ["nombreReconocimiento"])
    ]
)
data class ReconocimientoEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val idUsuarioFinal: String,
    val nombreReconocimiento: String, // "Pago Puntual", "Racha de 3 Meses", "Embajador PagoFlex"
    val fechaObtencion: String,       // "YYYY-MM-DD"
    val motivo: String
)
