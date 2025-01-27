package com.dticnat.controleimpressao.service;

import com.dticnat.controleimpressao.model.Copy;
import com.dticnat.controleimpressao.model.Request;
import com.dticnat.controleimpressao.repository.CopyRepository;
import org.springframework.beans.factory.annotation.Autowired;
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

    public List<Copy> findAllByRequestId(Long id) {
        return copyRepository.findAllByRequestId(id);
    }

    // Cria objetos de cópias de uma dada solicitação
    public void instanceCopies(Request request) {
        List<Copy> copies = request.getCopies();
        copies.forEach((copy)-> {
            copy.setRequestId(request.getId());
            copy.setFileInDisk(true);
            create(copy);
        });
    }
}