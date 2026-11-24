package cl.duocuc.pagoflex.pagos.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/** Problema que un usuario reporta sobre un compromiso; lo atiende un agente. */
@Entity
@Table(name = "reporte")
public class Reporte {

    @Id
    @NotBlank
    @Column(length = 20)
    private String idReporte;

    @NotBlank
    @Column(nullable = false, length = 30)
    private String idCompromiso;

    @NotBlank
    @Column(nullable = false, length = 60)
    private String motivo;

    @Column(length = 500)
    private String comentario;

    @NotBlank
    @Column(nullable = false, length = 10)
    private String fechaCreacion;

    /** Abierto, En gestión o Resuelto. */
    @NotBlank
    @Column(nullable = false, length = 20)
    private String estado;

    private String idAgente;

    private String fechaToma;

    @Column(length = 1000)
    private String respuesta;

    private String fechaResolucion;

    public String getIdReporte() { return idReporte; }
    public void setIdReporte(String idReporte) { this.idReporte = idReporte; }
    public String getIdCompromiso() { return idCompromiso; }
    public void setIdCompromiso(String idCompromiso) { this.idCompromiso = idCompromiso; }
    public String getMotivo() { return motivo; }
    public void setMotivo(String motivo) { this.motivo = motivo; }
    public String getComentario() { return comentario; }
    public void setComentario(String comentario) { this.comentario = comentario; }
    public String getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(String fechaCreacion) { this.fechaCreacion = fechaCreacion; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    public String getIdAgente() { return idAgente; }
    public void setIdAgente(String idAgente) { this.idAgente = idAgente; }
    public String getFechaToma() { return fechaToma; }
    public void setFechaToma(String fechaToma) { this.fechaToma = fechaToma; }
    public String getRespuesta() { return respuesta; }
    public void setRespuesta(String respuesta) { this.respuesta = respuesta; }
    public String getFechaResolucion() { return fechaResolucion; }
    public void setFechaResolucion(String fechaResolucion) { this.fechaResolucion = fechaResolucion; }
}
