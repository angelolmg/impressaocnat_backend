package com.dticnat.controleimpressao.controller;

import com.dticnat.controleimpressao.model.User;
import com.dticnat.controleimpressao.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/usuario")
@Tag(name = "Usuários", description = "Operações relacionadas a autenticação de usuários")
public class UserController {

    @Autowired
    private AuthService authService;

    /**
     * Verifica se um usuário com a matrícula especificada possui permissão de administrador.
     * Permite verificar se um usuário, identificado pela matrícula, possui o papel de administrador.
     * A verificação só é permitida se o usuário autenticado for ele mesmo ou um administrador.
     *
     * @param registration A matrícula do usuário a ser verificada.
     * @return ResponseEntity contendo um booleano indicando se o usuário é administrador (true) ou não (false),
     * ou um status 403 (Forbidden) se o usuário autenticado não tiver permissão para verificar.
     */
    @Operation(summary = "Verifica se um usuário é administrador")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Verificação realizada com sucesso.",
                    content = @Content(mediaType = "application/json", schema = @Schema(type = "boolean", example = "true"))),
            @ApiResponse(responseCode = "403", description = "Não autorizado.",
                    content = @Content(mediaType = "text/plain", schema = @Schema(type = "string", example = "Você não tem permissão para verificar o status de administrador de outros usuários.")))
    })
    @GetMapping("/admin")
    public ResponseEntity<?> isAdmin(
            HttpServletRequest httpRequest,
            @Parameter(description = "Matrícula do usuário a ser verificada.", required = true, example = "123456")
            @RequestParam("registration") String registration) {

        // Recuperar dados do usuário autenticado do request http
        User user = (User) httpRequest.getAttribute("userPrincipal");

        // Usuário só pode conferir o próprio status de admin
        if(registration.equals(user.getRegistrationNumber())) {
            return ResponseEntity.ok(user.isAdmin());
        // OU o status de outros usuários caso ele mesmo seja admin
        } else if (user.isAdmin()) {
            return ResponseEntity.ok(user.isAdmin());
        } else {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .contentType(MediaType.TEXT_PLAIN)
                    .body("Você não tem permissão para verificar o status de administrador de outros usuários.");
        }
    }
}
