package cl.duocuc.pagoflex.pagos.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/** Pago de un compromiso, con su número de comprobante. */
@Entity
@Table(name = "pago")
public class Pago {

    @Id
    @NotBlank
    @Column(length = 30)
    private String idPago;

    @NotBlank
    @Column(nullable = false, unique = true, length = 30)
    private String numeroComprobante;

    /** Un compromiso se paga una sola vez. */
    @NotBlank
    @Column(nullable = false, unique = true, length = 30)
    private String idCompromiso;

    @NotBlank
    @Column(nullable = false, length = 20)
    private String fechaHora;

    @NotNull
    @Positive
    private Double montoPagado;

    private String canal;

    private String medioPago;

    public String getIdPago() { return idPago; }
    public void setIdPago(String idPago) { this.idPago = idPago; }
    public String getNumeroComprobante() { return numeroComprobante; }
    public void setNumeroComprobante(String numeroComprobante) { this.numeroComprobante = numeroComprobante; }
    public String getIdCompromiso() { return idCompromiso; }
    public void setIdCompromiso(String idCompromiso) { this.idCompromiso = idCompromiso; }
    public String getFechaHora() { return fechaHora; }
    public void setFechaHora(String fechaHora) { this.fechaHora = fechaHora; }
    public Double getMontoPagado() { return montoPagado; }
    public void setMontoPagado(Double montoPagado) { this.montoPagado = montoPagado; }
    public String getCanal() { return canal; }
    public void setCanal(String canal) { this.canal = canal; }
    public String getMedioPago() { return medioPago; }
    public void setMedioPago(String medioPago) { this.medioPago = medioPago; }
}
