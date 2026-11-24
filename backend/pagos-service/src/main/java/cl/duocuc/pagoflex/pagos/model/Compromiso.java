package cl.duocuc.pagoflex.pagos.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/** Cuota o cobro que una empresa registra para un usuario final. */
@Entity
@Table(name = "compromiso")
public class Compromiso {

    @Id
    @NotBlank
    @Column(length = 30)
    private String idCompromiso;

    @NotBlank
    @Column(nullable = false, unique = true, length = 30)
    private String folio;

    @NotBlank
    @Column(nullable = false, length = 20)
    private String idUsuarioFinal;

    @NotBlank
    @Column(nullable = false, length = 20)
    private String idEmpresa;

    @NotBlank
    @Column(nullable = false, length = 150)
    private String concepto;

    private Integer numeroCuota;

    private Integer totalCuotas;

    @NotNull
    @Positive
    private Double monto;

    @NotBlank
    @Column(nullable = false, length = 10)
    private String fechaVencimiento;

    /** Pendiente, Vencido, Pagado, En revisión o Anulado. */
    @NotBlank
    @Column(nullable = false, length = 20)
    private String estado;

    private Double recargoAplicado;

    private String registradoPor;

    private String fechaRegistro;

    private String motivoAnulacion;

    @Column(length = 500)
    private String comentarioAnulacion;

    private String ejecutivoAnulacion;

    private String fechaAnulacion;

    public String getIdCompromiso() { return idCompromiso; }
    public void setIdCompromiso(String idCompromiso) { this.idCompromiso = idCompromiso; }
    public String getFolio() { return folio; }
    public void setFolio(String folio) { this.folio = folio; }
    public String getIdUsuarioFinal() { return idUsuarioFinal; }
    public void setIdUsuarioFinal(String idUsuarioFinal) { this.idUsuarioFinal = idUsuarioFinal; }
    public String getIdEmpresa() { return idEmpresa; }
    public void setIdEmpresa(String idEmpresa) { this.idEmpresa = idEmpresa; }
    public String getConcepto() { return concepto; }
    public void setConcepto(String concepto) { this.concepto = concepto; }
    public Integer getNumeroCuota() { return numeroCuota; }
    public void setNumeroCuota(Integer numeroCuota) { this.numeroCuota = numeroCuota; }
    public Integer getTotalCuotas() { return totalCuotas; }
    public void setTotalCuotas(Integer totalCuotas) { this.totalCuotas = totalCuotas; }
    public Double getMonto() { return monto; }
    public void setMonto(Double monto) { this.monto = monto; }
    public String getFechaVencimiento() { return fechaVencimiento; }
    public void setFechaVencimiento(String fechaVencimiento) { this.fechaVencimiento = fechaVencimiento; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    public Double getRecargoAplicado() { return recargoAplicado; }
    public void setRecargoAplicado(Double recargoAplicado) { this.recargoAplicado = recargoAplicado; }
    public String getRegistradoPor() { return registradoPor; }
    public void setRegistradoPor(String registradoPor) { this.registradoPor = registradoPor; }
    public String getFechaRegistro() { return fechaRegistro; }
    public void setFechaRegistro(String fechaRegistro) { this.fechaRegistro = fechaRegistro; }
    public String getMotivoAnulacion() { return motivoAnulacion; }
    public void setMotivoAnulacion(String motivoAnulacion) { this.motivoAnulacion = motivoAnulacion; }
    public String getComentarioAnulacion() { return comentarioAnulacion; }
    public void setComentarioAnulacion(String comentarioAnulacion) { this.comentarioAnulacion = comentarioAnulacion; }
    public String getEjecutivoAnulacion() { return ejecutivoAnulacion; }
    public void setEjecutivoAnulacion(String ejecutivoAnulacion) { this.ejecutivoAnulacion = ejecutivoAnulacion; }
    public String getFechaAnulacion() { return fechaAnulacion; }
    public void setFechaAnulacion(String fechaAnulacion) { this.fechaAnulacion = fechaAnulacion; }
}
