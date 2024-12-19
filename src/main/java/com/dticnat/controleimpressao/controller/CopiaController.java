package com.dticnat.controleimpressao.controller;

import com.dticnat.controleimpressao.service.CopiaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/copias")
public class CopiaController {

    @Autowired
    private CopiaService copiaService;

    // 1. Atualizar somente o atributo "numeroCopiasRequisitadas" de uma cópia existente
    // TODO validar se o número é positivo e não nulo
    @PatchMapping("/{id}/{numeroCopias}")
    public ResponseEntity<?> updateNumeroCopiasRequisitadas(@PathVariable Long id, @PathVariable int numeroCopias) {
        return (copiaService.updateNumeroCopias(id, numeroCopias)) ?
                ResponseEntity.ok("Cópia com ID " + id + " atualizada para " + numeroCopias + " cópias.") :
                ResponseEntity.status(HttpStatus.NOT_FOUND).body("Cópia com ID " + id + " não encontrada.");
    }
}
