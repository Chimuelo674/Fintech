package cl.duocuc.pagoflex.pagos.service;

import cl.duocuc.pagoflex.pagos.exception.ConflictoException;
import cl.duocuc.pagoflex.pagos.exception.NoEncontradoException;
import cl.duocuc.pagoflex.pagos.model.Compromiso;
import cl.duocuc.pagoflex.pagos.model.Reporte;
import cl.duocuc.pagoflex.pagos.repository.CompromisoRepository;
import cl.duocuc.pagoflex.pagos.repository.ReporteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
public class ReporteService {

    public static final Set<String> ESTADOS = Set.of("Abierto", "En gestión", "Resuelto");

    private final ReporteRepository repo;
    private final CompromisoRepository compromisoRepo;

    public ReporteService(ReporteRepository repo, CompromisoRepository compromisoRepo) {
        this.repo = repo;
        this.compromisoRepo = compromisoRepo;
    }

    /** Reportes de un usuario final, por estado, o todos. */
    public List<Reporte> listar(String usuario, String estado) {
        if (usuario != null && !usuario.isBlank()) {
            List<String> ids = compromisoRepo.findByIdUsuarioFinalOrderByFechaVencimientoAsc(usuario).stream()
                    .map(Compromiso::getIdCompromiso)
                    .toList();
            return ids.isEmpty() ? List.of() : repo.findByIdCompromisoIn(ids);
        }
        if (estado != null && !estado.isBlank()) return repo.findByEstado(estado);
        return repo.findAll();
    }

    public Reporte obtener(String id) {
        return repo.findById(id).orElseThrow(() -> new NoEncontradoException("No existe el reporte " + id + "."));
    }

    @Transactional
    public Reporte crear(Reporte r) {
        validarEstado(r.getEstado());
        if (!compromisoRepo.existsById(r.getIdCompromiso())) {
            throw new NoEncontradoException("No existe el compromiso " + r.getIdCompromiso() + ".");
        }
        if (repo.existsById(r.getIdReporte())) {
            throw new ConflictoException("Ya existe el reporte " + r.getIdReporte() + ".");
        }
        return repo.save(r);
    }

    @Transactional
    public int crearLote(List<Reporte> reportes) {
        int insertados = 0;
        for (Reporte r : reportes) {
            if (r.getIdReporte() == null || repo.existsById(r.getIdReporte())) continue;
            repo.save(r);
            insertados++;
        }
        return insertados;
    }

    @Transactional
    public Reporte actualizar(String id, Reporte datos) {
        obtener(id);
        validarEstado(datos.getEstado());
        datos.setIdReporte(id);
        return repo.save(datos);
    }

    @Transactional
    public void eliminar(String id) {
        obtener(id);
        repo.deleteById(id);
    }

    private void validarEstado(String estado) {
        if (!ESTADOS.contains(estado)) {
            throw new ConflictoException("Estado no válido: " + estado + ". Usa uno de " + ESTADOS + ".");
        }
    }
}
