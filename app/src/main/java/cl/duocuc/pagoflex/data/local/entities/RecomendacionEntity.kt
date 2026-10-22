package cl.duocuc.pagoflex.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "recomendacion")
data class RecomendacionEntity(
    @PrimaryKey val idRecomendacion: String, // e.g. "REC-0001"
    val idUsuarioFinal: String,
    val nombreOrganizacion: String,
    val rutOrganizacion: String? = null,
    val rubro: String? = null,
    val comuna: String? = null,
    val nombreContacto: String? = null,
    val correoContacto: String? = null,
    val telefonoContacto: String? = null,
    val autorizaContacto: Boolean = false,
    val fecha: String,                      // "YYYY-MM-DD"
    val estado: String = "Recibida",        // "Recibida", "En evaluación", "Contactada", "Se unió", "Descartada"
    val motivoDescarte: String? = null,
    val idAgente: String? = null,
    val fechaUltimoCambio: String? = null
)
