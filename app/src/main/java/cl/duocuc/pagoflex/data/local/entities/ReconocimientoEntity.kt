package cl.duocuc.pagoflex.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "usuario_reconocimiento")
data class ReconocimientoEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val idUsuarioFinal: String,
    val nombreReconocimiento: String, // "Pago Puntual", "Racha de 3 Meses", "Embajador PagoFlex"
    val descripcion: String,
    val fechaObtencion: String,       // "YYYY-MM-DD"
    val motivo: String
)
