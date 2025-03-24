package com.dticnat.controleimpressao.controller;

import com.dticnat.controleimpressao.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/usuario")
public class UserController {

    @Autowired
    private AuthService authService;

    // 1. Verificar se usuario com matricula 'registration' é admin
    @GetMapping("/admin")
    public ResponseEntity<Boolean> isAdmin(@RequestParam("registration") String registration) {
        Boolean isAdmin = authService.isAdmin(registration);
        return ResponseEntity.ok(isAdmin);
    }
}
