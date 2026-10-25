package cl.duocuc.pagoflex.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "empresa_cliente",
    indices = [Index(value = ["rut"], unique = true), Index("rubro")],
    foreignKeys = [ForeignKey(entity = RubroEntity::class, parentColumns = ["nombre"], childColumns = ["rubro"])]
)
data class EmpresaEntity(
    @PrimaryKey val idEmpresa: String, // e.g. "EC-01"
    val razonSocial: String,
    val nombreFantasia: String,
    val rut: String,
    val rubro: String,
    val correoAtencion: String,
    val telefonoAtencion: String,
    val recargoAtraso: Double,
    val activa: Boolean = true
)
