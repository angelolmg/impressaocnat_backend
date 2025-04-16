package com.dticnat.controleimpressao.controller;

import com.dticnat.controleimpressao.model.dto.SuapLoginDTO;
import com.dticnat.controleimpressao.model.dto.SuapLoginResponseDTO;
import com.dticnat.controleimpressao.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@Tag(name = "Autorização", description = "Operações de autorização via SUAP")
public class AuthController {

    @Autowired
    private AuthService authService;

    /**
     * Autentica um usuário no SUAP com as credenciais fornecidas e retorna os tokens JWT.
     * Este endpoint realiza a autenticação do usuário no sistema SUAP utilizando matrícula e senha.
     * Caso as credenciais estejam corretas, um par de tokens (access e refresh) será retornado.
     *
     * @param credentials Objeto contendo a matrícula (username) e a senha do usuário.
     * @return ResponseEntity contendo os tokens JWT (access e refresh), ou um erro em caso de falha na autenticação.
     */
    @Operation(summary = "Autentica um usuário no SUAP e retorna os tokens JWT",
            description = "Realiza a autenticação com o SUAP utilizando matrícula e senha. Retorna os tokens de acesso e atualização.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Autenticação realizada com sucesso.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = SuapLoginResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "Credenciais inválidas ou falha na autenticação.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(type = "string", example = "Usuário não encontrado ou senha incorreta."))),
            @ApiResponse(responseCode = "500", description = "Erro interno ao processar a autenticação.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(type = "string", example = "Erro ao autenticar com o SUAP.")))
    })
    @PostMapping("/login-suap")
    public ResponseEntity<SuapLoginResponseDTO> loginSUAP(
            @Parameter(description = "Dados de login do usuário.", required = true)
            @RequestBody SuapLoginDTO credentials) {

            return ResponseEntity.ok()
                    .body(authService.getSuapTokenFromCredentials(credentials));
    }
}
