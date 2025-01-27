package com.dticnat.controleimpressao.controller;

import com.dticnat.controleimpressao.model.Copy;
import com.dticnat.controleimpressao.service.CopyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/copias")
public class CopyController {

    @Autowired
    private CopyService copyService;

    // 1. Listar todas as copias
    @GetMapping
    public ResponseEntity<List<Copy>> getAllCopies() {
        List<Copy> copies = copyService.findAll();
        return ResponseEntity.ok(copies);
    }

    @GetMapping("/{requestID}")
    public ResponseEntity<?> getCopiesFromRequest(@PathVariable Long requestID) {
        List<Copy> copies = copyService.findAllByRequestId(requestID);
        return ResponseEntity.ok(copies);
    }
}
