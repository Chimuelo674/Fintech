package cl.duocuc.pagoflex.usuarios.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/** Persona que usa PagoFlex: usuario final, agente o ejecutivo de empresa. */
@Entity
@Table(name = "usuario")
public class Usuario {

    @Id
    @NotBlank
    @Column(length = 20)
    private String idUsuario;

    /** USUARIO_FINAL, AGENTE o EJECUTIVO. */
    @NotBlank
    @Column(nullable = false, length = 20)
    private String idRol;

    @NotBlank
    @Column(nullable = false, unique = true, length = 15)
    private String rut;

    @NotBlank
    @Column(nullable = false, length = 80)
    private String nombres;

    @NotBlank
    @Column(nullable = false, length = 80)
    private String apellidos;

    @NotBlank
    @Email
    @Column(nullable = false, unique = true, length = 120)
    private String correo;

    private String telefono;
    private String comuna;
    private String fechaIncorporacion;
    private Boolean avisosActivos = true;
    private Integer diasAnticipacion = 3;
    private String area;
    private String idEmpresa;
    private String cargo;
    private Boolean activo = true;

    /** Hash SHA-256 de la clave de 4 dígitos. Nunca se envía en las respuestas. */
    @JsonIgnore
    @Column(length = 64)
    private String claveHash;

    public String getIdUsuario() { return idUsuario; }
    public void setIdUsuario(String idUsuario) { this.idUsuario = idUsuario; }
    public String getIdRol() { return idRol; }
    public void setIdRol(String idRol) { this.idRol = idRol; }
    public String getRut() { return rut; }
    public void setRut(String rut) { this.rut = rut; }
    public String getNombres() { return nombres; }
    public void setNombres(String nombres) { this.nombres = nombres; }
    public String getApellidos() { return apellidos; }
    public void setApellidos(String apellidos) { this.apellidos = apellidos; }
    public String getCorreo() { return correo; }
    public void setCorreo(String correo) { this.correo = correo; }
    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }
    public String getComuna() { return comuna; }
    public void setComuna(String comuna) { this.comuna = comuna; }
    public String getFechaIncorporacion() { return fechaIncorporacion; }
    public void setFechaIncorporacion(String fechaIncorporacion) { this.fechaIncorporacion = fechaIncorporacion; }
    public Boolean getAvisosActivos() { return avisosActivos; }
    public void setAvisosActivos(Boolean avisosActivos) { this.avisosActivos = avisosActivos; }
    public Integer getDiasAnticipacion() { return diasAnticipacion; }
    public void setDiasAnticipacion(Integer diasAnticipacion) { this.diasAnticipacion = diasAnticipacion; }
    public String getArea() { return area; }
    public void setArea(String area) { this.area = area; }
    public String getIdEmpresa() { return idEmpresa; }
    public void setIdEmpresa(String idEmpresa) { this.idEmpresa = idEmpresa; }
    public String getCargo() { return cargo; }
    public void setCargo(String cargo) { this.cargo = cargo; }
    public Boolean getActivo() { return activo; }
    public void setActivo(Boolean activo) { this.activo = activo; }
    public String getClaveHash() { return claveHash; }
    public void setClaveHash(String claveHash) { this.claveHash = claveHash; }
}
