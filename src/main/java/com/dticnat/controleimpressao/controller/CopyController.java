package com.dticnat.controleimpressao.controller;

import com.dticnat.controleimpressao.model.Copy;
import com.dticnat.controleimpressao.model.dto.UserData;
import com.dticnat.controleimpressao.service.AuthService;
import com.dticnat.controleimpressao.service.CopyService;
import com.dticnat.controleimpressao.service.RequestService;
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

    @Autowired
    private RequestService requestService;

    @Autowired
    private AuthService authService;

    // 1. Listar todas as copias
    @GetMapping
    public ResponseEntity<List<Copy>> getAllCopies() {
        List<Copy> copies = copyService.findAll();
        return ResponseEntity.ok(copies);
    }

    @GetMapping("/{requestID}")
    public ResponseEntity<?> getCopiesFromRequest(@RequestHeader(name = "Authorization", required = false) String fullToken,
                                                  @RequestParam(value = "query", required = false) String query,
                                                  @PathVariable Long requestID) {
        // Possui um token (está logado)
        if (fullToken == null)
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Token de acesso não encontrado. Por favor, realizar login.");

        // Buscar dados do usuário
        UserData userData = authService.getUserData(fullToken);
        if (userData == null)
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Usuário não encontrado.");

        // Verificar se o mesmo é admin
        boolean isAdmin = authService.isAdmin(userData.getMatricula());

        // Se não for admin, checar se a solicitação que esta sendo acessada pertence ao usuário
        if(!isAdmin && !requestService.belongsToUserCheck(requestID, userData)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Não autorizado.");
        }

        List<Copy> copies = copyService.findAllByRequestId(requestID, query);
        return ResponseEntity.ok(copies);
    }
}
