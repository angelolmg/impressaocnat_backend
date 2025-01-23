package com.dticnat.controleimpressao.service;

import com.dticnat.controleimpressao.model.Copy;
import com.dticnat.controleimpressao.model.Request;
import com.dticnat.controleimpressao.repository.CopyRepository;
import com.dticnat.controleimpressao.repository.RequestRepository;
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

    public boolean updateCopyCount(Long copyId, int copyCount) {
        Optional<Copy> copiaOptional = findById(copyId);

        if (copiaOptional.isPresent()) {
            Copy copy = copiaOptional.get();
            copy.setCopyCount(copyCount);
            copyRepository.save(copy);
            return true;
        }
        return false;
    }

    public void instanceCopies(Request request) {
        List<Copy> copies = request.getCopies();
        copies.forEach((copy)-> {
            copy.setRequestId(request.getId());
            copy.setFileInDisk(true);
            create(copy);
        });
    }

    public Request removeOldCopies(Request oldRequest) {
        List<Copy> oldCopies = oldRequest.getCopies();
        oldRequest.getCopies().clear();

        oldCopies.forEach((copy) -> {
            copyRepository.delete(copy);
        });

        return oldRequest;
    }
}