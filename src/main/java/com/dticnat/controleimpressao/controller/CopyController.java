package com.dticnat.controleimpressao.controller;

import com.dticnat.controleimpressao.model.Copy;
import com.dticnat.controleimpressao.service.CopyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/copias")
public class CopyController {

    @Autowired
    private CopyService copyService;

    // 1. Atualizar somente o atributo "numeroCopiasRequisitadas" de uma cópia existente
    // TODO validar se o número é positivo e não nulo
    @PatchMapping("/{id}/{numeroCopias}")
    public ResponseEntity<?> updateNumeroCopiasRequisitadas(@PathVariable Long id, @PathVariable int numeroCopias) {
        return (copyService.updateCopyCount(id, numeroCopias)) ?
                ResponseEntity.ok("Cópia com ID " + id + " atualizada para " + numeroCopias + " cópias.") :
                ResponseEntity.status(HttpStatus.NOT_FOUND).body("Cópia com ID " + id + " não encontrada.");
    }

    @GetMapping("/{requestID}")
    public ResponseEntity<?> getCopiesFromRequest(@PathVariable Long requestID) {
        List<Copy> copies = copyService.findAllByRequestId(requestID);
        return ResponseEntity.ok(copies);
    }
}
