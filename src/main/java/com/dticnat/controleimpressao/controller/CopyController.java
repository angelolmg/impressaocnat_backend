package com.dticnat.controleimpressao.controller;

import com.dticnat.controleimpressao.model.Copy;
import com.dticnat.controleimpressao.model.dto.UserData;
import com.dticnat.controleimpressao.service.AuthService;
import com.dticnat.controleimpressao.service.CopyService;
import com.dticnat.controleimpressao.service.RequestService;
import jakarta.servlet.http.HttpServletRequest;
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
    public ResponseEntity<?> getCopiesFromRequest(HttpServletRequest httpRequest,
                                                  @RequestParam(value = "query", required = false) String query,
                                                  @PathVariable Long requestID) {
        // Recuperar dados do usuário
        UserData userData = (UserData) httpRequest.getAttribute("userData");
        boolean isAdmin = (boolean) httpRequest.getAttribute("isAdmin");

        // Se não for admin, checar se a solicitação que esta sendo acessada pertence ao usuário
        if(!isAdmin && !requestService.belongsToUserCheck(requestID, userData)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Não autorizado.");
        }

        List<Copy> copies = copyService.findAllByRequestId(requestID, query);
        return ResponseEntity.ok(copies);
    }
}
