package cl.duocuc.pagoflex.pagos.repository;

import cl.duocuc.pagoflex.pagos.model.Compromiso;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CompromisoRepository extends JpaRepository<Compromiso, String> {
    List<Compromiso> findByIdUsuarioFinalOrderByFechaVencimientoAsc(String idUsuarioFinal);
    List<Compromiso> findByIdEmpresaOrderByFechaVencimientoAsc(String idEmpresa);
}
