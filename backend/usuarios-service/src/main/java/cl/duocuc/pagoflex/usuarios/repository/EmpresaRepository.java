package cl.duocuc.pagoflex.usuarios.repository;

import cl.duocuc.pagoflex.usuarios.model.Empresa;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmpresaRepository extends JpaRepository<Empresa, String> {
}
