package cl.duocuc.pagoflex.pagos.repository;

import cl.duocuc.pagoflex.pagos.model.Reporte;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface ReporteRepository extends JpaRepository<Reporte, String> {
    List<Reporte> findByIdCompromisoIn(Collection<String> idsCompromiso);
    List<Reporte> findByEstado(String estado);
}
