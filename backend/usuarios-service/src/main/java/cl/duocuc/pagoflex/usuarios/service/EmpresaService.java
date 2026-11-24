package cl.duocuc.pagoflex.usuarios.service;

import cl.duocuc.pagoflex.usuarios.exception.ConflictoException;
import cl.duocuc.pagoflex.usuarios.exception.NoEncontradoException;
import cl.duocuc.pagoflex.usuarios.model.Empresa;
import cl.duocuc.pagoflex.usuarios.repository.EmpresaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class EmpresaService {

    private final EmpresaRepository repo;

    public EmpresaService(EmpresaRepository repo) {
        this.repo = repo;
    }

    public List<Empresa> listar() {
        return repo.findAll();
    }

    public Empresa obtener(String id) {
        return repo.findById(id).orElseThrow(() -> new NoEncontradoException("No existe la empresa " + id + "."));
    }

    @Transactional
    public Empresa crear(Empresa empresa) {
        if (repo.existsById(empresa.getIdEmpresa())) {
            throw new ConflictoException("Ya existe una empresa con el id " + empresa.getIdEmpresa() + ".");
        }
        return repo.save(empresa);
    }

    @Transactional
    public int crearLote(List<Empresa> empresas) {
        int insertadas = 0;
        for (Empresa e : empresas) {
            if (e.getIdEmpresa() == null || repo.existsById(e.getIdEmpresa())) continue;
            repo.save(e);
            insertadas++;
        }
        return insertadas;
    }

    @Transactional
    public Empresa actualizar(String id, Empresa datos) {
        obtener(id);
        datos.setIdEmpresa(id);
        return repo.save(datos);
    }

    @Transactional
    public void eliminar(String id) {
        if (!repo.existsById(id)) {
            throw new NoEncontradoException("No existe la empresa " + id + ".");
        }
        repo.deleteById(id);
    }
}
