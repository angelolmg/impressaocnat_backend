package com.dticnat.controleimpressao.service;

import com.dticnat.controleimpressao.model.Copy;
import com.dticnat.controleimpressao.model.Request;
import com.dticnat.controleimpressao.repository.CopyRepository;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class CopyService {

    @Autowired
    private CopyRepository copyRepository;

    public List<Copy> findAll() {
        return copyRepository.findAll();
    }

    public Optional<Copy> findById(Long id) {
        return copyRepository.findById(id);
    }

    public Copy create(Copy copy) {
        return copyRepository.save(copy);
    }

    public boolean existsById(Long id) {
        return copyRepository.existsById(id);
    }

    public List<Copy> findAllByRequestId(Long requestId, String query) {
        Specification<Copy> spec = filterCopies(requestId, query);
        return copyRepository.findAll(spec, Sort.by(Sort.Direction.ASC, "id"));
    }

    public Specification<Copy> filterCopies(Long requestId, String userQuery) {
        return (Root<Copy> root, CriteriaQuery<?> query, CriteriaBuilder cb) -> {
            Predicate predicate = cb.conjunction();

            // Filtrar por ID de solicitação
            if (requestId != null) {
                predicate = cb.and(predicate, cb.equal(root.get("requestId"), requestId));
            }

            // Filtrar por query de texto
            if (userQuery != null && !userQuery.isEmpty()) {
                // Testar se query é nome do solicitante
                predicate = cb.and(predicate, cb.like(cb.lower(cb.trim(root.get("fileName"))), "%" + userQuery.trim().toLowerCase() + "%"));
            }

            return predicate;
        };
    }

    // Cria objetos de cópias de uma dada solicitação
    public void instanceCopies(Request request) {
        List<Copy> copies = request.getCopies();
        copies.forEach((copy) -> {
            copy.setRequestId(request.getId());
            copy.setFileInDisk(true);
            create(copy);
        });
    }
}