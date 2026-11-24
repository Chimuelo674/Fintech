package cl.duocuc.pagoflex.usuarios.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

/** Empresa u organización que cobra a sus clientes a través de PagoFlex. */
@Entity
@Table(name = "empresa")
public class Empresa {

    @Id
    @NotBlank
    @Column(length = 20)
    private String idEmpresa;

    @NotBlank
    @Column(nullable = false, length = 120)
    private String razonSocial;

    @NotBlank
    @Column(nullable = false, length = 80)
    private String nombreFantasia;

    @NotBlank
    @Column(nullable = false, unique = true, length = 15)
    private String rut;

    @NotBlank
    private String rubro;

    private String correoAtencion;
    private String telefonoAtencion;

    @NotNull
    @PositiveOrZero
    private Double recargoAtraso;

    private Boolean activa = true;

    public String getIdEmpresa() { return idEmpresa; }
    public void setIdEmpresa(String idEmpresa) { this.idEmpresa = idEmpresa; }
    public String getRazonSocial() { return razonSocial; }
    public void setRazonSocial(String razonSocial) { this.razonSocial = razonSocial; }
    public String getNombreFantasia() { return nombreFantasia; }
    public void setNombreFantasia(String nombreFantasia) { this.nombreFantasia = nombreFantasia; }
    public String getRut() { return rut; }
    public void setRut(String rut) { this.rut = rut; }
    public String getRubro() { return rubro; }
    public void setRubro(String rubro) { this.rubro = rubro; }
    public String getCorreoAtencion() { return correoAtencion; }
    public void setCorreoAtencion(String correoAtencion) { this.correoAtencion = correoAtencion; }
    public String getTelefonoAtencion() { return telefonoAtencion; }
    public void setTelefonoAtencion(String telefonoAtencion) { this.telefonoAtencion = telefonoAtencion; }
    public Double getRecargoAtraso() { return recargoAtraso; }
    public void setRecargoAtraso(Double recargoAtraso) { this.recargoAtraso = recargoAtraso; }
    public Boolean getActiva() { return activa; }
    public void setActiva(Boolean activa) { this.activa = activa; }
}
