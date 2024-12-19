package com.dticnat.controleimpressao.service;

import com.dticnat.controleimpressao.model.Copia;
import com.dticnat.controleimpressao.repository.CopiaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class CopiaService {

    @Autowired
    private CopiaRepository copiaRepository;

    public List<Copia> findAll() {
        return copiaRepository.findAll();
    }

    public Optional<Copia> findById(Long id) {
        return copiaRepository.findById(id);
    }

    public Copia create(Copia copia) {
        return copiaRepository.save(copia);
    }

    public boolean existsById(Long id) {
        return copiaRepository.existsById(id);
    }

    public boolean updateNumeroCopias(Long copiaId, int numCopias) {
        Optional<Copia> copiaOptional = findById(copiaId);

        if (copiaOptional.isPresent()) {
            Copia copia = copiaOptional.get();
            copia.setNumeroCopiasRequisitadas(numCopias);
            copiaRepository.save(copia);
            return true;
        }
        return false;
    }
}