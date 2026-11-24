package cl.duocuc.pagoflex.pagos.service;

import cl.duocuc.pagoflex.pagos.exception.ConflictoException;
import cl.duocuc.pagoflex.pagos.exception.NoEncontradoException;
import cl.duocuc.pagoflex.pagos.model.Compromiso;
import cl.duocuc.pagoflex.pagos.repository.CompromisoRepository;
import cl.duocuc.pagoflex.pagos.repository.PagoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
public class CompromisoService {

    public static final Set<String> ESTADOS = Set.of("Pendiente", "Vencido", "Pagado", "En revisión", "Anulado");

    private final CompromisoRepository repo;
    private final PagoRepository pagoRepo;

    public CompromisoService(CompromisoRepository repo, PagoRepository pagoRepo) {
        this.repo = repo;
        this.pagoRepo = pagoRepo;
    }

    /** Filtra por usuario final o por empresa; sin filtros devuelve todos. */
    public List<Compromiso> listar(String usuario, String empresa) {
        if (usuario != null && !usuario.isBlank()) return repo.findByIdUsuarioFinalOrderByFechaVencimientoAsc(usuario);
        if (empresa != null && !empresa.isBlank()) return repo.findByIdEmpresaOrderByFechaVencimientoAsc(empresa);
        return repo.findAll();
    }

    public Compromiso obtener(String id) {
        return repo.findById(id).orElseThrow(() -> new NoEncontradoException("No existe el compromiso " + id + "."));
    }

    @Transactional
    public Compromiso crear(Compromiso c) {
        validarEstado(c.getEstado());
        if (repo.existsById(c.getIdCompromiso())) {
            throw new ConflictoException("Ya existe el compromiso " + c.getIdCompromiso() + ".");
        }
        if (c.getRecargoAplicado() == null) c.setRecargoAplicado(0.0);
        return repo.save(c);
    }

    @Transactional
    public int crearLote(List<Compromiso> compromisos) {
        int insertados = 0;
        for (Compromiso c : compromisos) {
            if (c.getIdCompromiso() == null || repo.existsById(c.getIdCompromiso())) continue;
            if (c.getRecargoAplicado() == null) c.setRecargoAplicado(0.0);
            repo.save(c);
            insertados++;
        }
        return insertados;
    }

    @Transactional
    public Compromiso actualizar(String id, Compromiso datos) {
        obtener(id);
        validarEstado(datos.getEstado());
        datos.setIdCompromiso(id);
        if (datos.getRecargoAplicado() == null) datos.setRecargoAplicado(0.0);
        return repo.save(datos);
    }

    /** Solo se puede eliminar un compromiso que todavía no tiene un pago asociado. */
    @Transactional
    public void eliminar(String id) {
        obtener(id);
        if (pagoRepo.existsByIdCompromiso(id)) {
            throw new ConflictoException("El compromiso " + id + " ya tiene un pago y no se puede eliminar. Puedes anularlo.");
        }
        repo.deleteById(id);
    }

    private void validarEstado(String estado) {
        if (!ESTADOS.contains(estado)) {
            throw new ConflictoException("Estado no válido: " + estado + ". Usa uno de " + ESTADOS + ".");
        }
    }
}
