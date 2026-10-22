package cl.duocuc.pagoflex.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "empresa_cliente")
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
