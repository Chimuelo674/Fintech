package cl.duocuc.pagoflex.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "recomendacion",
    indices = [Index("idUsuarioFinal"), Index("estado"), Index("rubro"), Index("comuna")],
    foreignKeys = [
        ForeignKey(entity = UsuarioEntity::class, parentColumns = ["idUsuario"], childColumns = ["idUsuarioFinal"]),
        ForeignKey(entity = EstadoRecomendacionEntity::class, parentColumns = ["nombre"], childColumns = ["estado"]),
        ForeignKey(entity = RubroEntity::class, parentColumns = ["nombre"], childColumns = ["rubro"]),
        ForeignKey(entity = ComunaEntity::class, parentColumns = ["nombre"], childColumns = ["comuna"])
    ]
)
data class RecomendacionEntity(
    @PrimaryKey val idRecomendacion: String,
    val idUsuarioFinal: String,
    val nombreOrganizacion: String,
    val rutOrganizacion: String? = null,
    val rubro: String? = null,
    val comuna: String? = null,
    val nombreContacto: String? = null,
    val correoContacto: String? = null,
    val telefonoContacto: String? = null,
    val autorizaContacto: Boolean = false,
    val fecha: String,
    val estado: String = "Recibida",
    val motivoDescarte: String? = null,
    val idAgente: String? = null,
    val fechaUltimoCambio: String? = null
)
