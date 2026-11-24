package cl.duocuc.pagoflex.pagos.repository;

import cl.duocuc.pagoflex.pagos.model.Pago;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface PagoRepository extends JpaRepository<Pago, String> {
    List<Pago> findByIdCompromisoIn(Collection<String> idsCompromiso);
    boolean existsByIdCompromiso(String idCompromiso);
}
