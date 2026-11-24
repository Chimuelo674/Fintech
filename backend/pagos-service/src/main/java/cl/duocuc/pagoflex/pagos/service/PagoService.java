package cl.duocuc.pagoflex.pagos.service;

import cl.duocuc.pagoflex.pagos.exception.ConflictoException;
import cl.duocuc.pagoflex.pagos.exception.NoEncontradoException;
import cl.duocuc.pagoflex.pagos.model.Compromiso;
import cl.duocuc.pagoflex.pagos.model.Pago;
import cl.duocuc.pagoflex.pagos.repository.CompromisoRepository;
import cl.duocuc.pagoflex.pagos.repository.PagoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class PagoService {

    private final PagoRepository repo;
    private final CompromisoRepository compromisoRepo;

    public PagoService(PagoRepository repo, CompromisoRepository compromisoRepo) {
        this.repo = repo;
        this.compromisoRepo = compromisoRepo;
    }

    /** Pagos de un usuario final (a través de sus compromisos), o todos si no se indica usuario. */
    public List<Pago> listar(String usuario) {
        if (usuario == null || usuario.isBlank()) return repo.findAll();
        List<String> ids = compromisoRepo.findByIdUsuarioFinalOrderByFechaVencimientoAsc(usuario).stream()
                .map(Compromiso::getIdCompromiso)
                .toList();
        return ids.isEmpty() ? List.of() : repo.findByIdCompromisoIn(ids);
    }

    public Pago obtener(String id) {
        return repo.findById(id).orElseThrow(() -> new NoEncontradoException("No existe el pago " + id + "."));
    }

    /**
     * Registra el pago y deja el compromiso como "Pagado" en la misma transacción.
     * Rechaza pagar dos veces, o pagar un compromiso anulado o con un reporte en revisión.
     */
    @Transactional
    public Pago crear(Pago pago) {
        Compromiso compromiso = compromisoRepo.findById(pago.getIdCompromiso())
                .orElseThrow(() -> new NoEncontradoException("No existe el compromiso " + pago.getIdCompromiso() + "."));
        if (repo.existsById(pago.getIdPago())) {
            throw new ConflictoException("Ya existe el pago " + pago.getIdPago() + ".");
        }
        if (repo.existsByIdCompromiso(compromiso.getIdCompromiso())) {
            throw new ConflictoException("El compromiso " + compromiso.getIdCompromiso() + " ya está pagado.");
        }
        if ("Anulado".equals(compromiso.getEstado()) || "En revisión".equals(compromiso.getEstado())) {
            throw new ConflictoException("No se puede pagar un compromiso en estado " + compromiso.getEstado() + ".");
        }
        if (pago.getCanal() == null) pago.setCanal("App");
        Pago guardado = repo.save(pago);
        compromiso.setEstado("Pagado");
        compromisoRepo.save(compromiso);
        return guardado;
    }

    @Transactional
    public int crearLote(List<Pago> pagos) {
        int insertados = 0;
        for (Pago p : pagos) {
            if (p.getIdPago() == null || repo.existsById(p.getIdPago()) || repo.existsByIdCompromiso(p.getIdCompromiso())) continue;
            repo.save(p);
            insertados++;
        }
        return insertados;
    }

    @Transactional
    public Pago actualizar(String id, Pago datos) {
        obtener(id);
        datos.setIdPago(id);
        return repo.save(datos);
    }

    @Transactional
    public void eliminar(String id) {
        obtener(id);
        repo.deleteById(id);
    }
}
