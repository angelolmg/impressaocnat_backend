package com.dticnat.controleimpressao.controller;

import com.dticnat.controleimpressao.exception.UnauthorizedException;
import com.dticnat.controleimpressao.model.Copy;
import com.dticnat.controleimpressao.model.dto.UserData;
import com.dticnat.controleimpressao.service.AuthService;
import com.dticnat.controleimpressao.service.CopyService;
import com.dticnat.controleimpressao.service.RequestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/copias")
@Tag(name = "Cópias", description = "Operações relacionadas a cópias de arquivos")
public class CopyController {

    @Autowired
    private CopyService copyService;

    @Autowired
    private RequestService requestService;

    @Autowired
    private AuthService authService;

    /**
     * Lista as cópias de arquivos associadas a uma solicitação específica.
     * Retorna uma lista de cópias de arquivos associadas a uma solicitação específica,
     * identificada pelo ID da solicitação (`requestId`). Permite filtrar as cópias por um
     * termo de pesquisa opcional. Apenas administradores ou o usuário que criou a
     * solicitação têm permissão para acessar esta informação.
     *
     * @param httpRequest Dados da requisição HTTP, contendo informações do usuário autenticado.
     * @param query       Termo de pesquisa para filtrar as cópias por nome de arquivo (opcional).
     * @param requestId   ID da solicitação da qual as cópias serão listadas.
     * @return A lista de cópias da solicitação
     */
    @Operation(summary = "Lista as cópias de arquivos de uma solicitação")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de cópias da solicitação retornada com sucesso.",
                    content = @Content(mediaType = "application/json",
                            array = @ArraySchema(schema = @Schema(implementation = Copy.class)))),
            @ApiResponse(responseCode = "401", description = "Não autorizado.",
                    content = @Content(mediaType = "text/plain", schema = @Schema(type = "string", example = "Usuário não autorizado a acessar esta solicitação."))
            ),
            @ApiResponse(responseCode = "404", description = "Solicitação não encontrada.",
                    content = @Content(mediaType = "text/plain", schema = @Schema(type = "string", example = "Solicitação (ID 000123) não encontrada."))
            )
    })
    @GetMapping("/{requestId}")
    public ResponseEntity<?> getCopiesFromRequest(HttpServletRequest httpRequest,
                                                  @Parameter(description = "Termo de pesquisa para filtrar por nome de arquivo (opcional).") @RequestParam(value = "query", required = false) String query,
                                                  @Parameter(description = "ID da solicitação da qual as cópias serão listadas.") @PathVariable Long requestId) {
        // Recuperar dados do usuário
        UserData userData = (UserData) httpRequest.getAttribute("userData");

        try {
            // Verificar se solicitação sendo alterada pertence ao usuário tentando editá-la
            // Se o usuario for admin, ele pode editar mesmo solicitações que não são dele
            requestService.canInteract(requestId, userData, false);
            List<Copy> copies = copyService.findAllByRequestId(requestId, query);
            return ResponseEntity.ok(copies);

        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .contentType(MediaType.TEXT_PLAIN)
                    .body("Solicitação (ID " + requestId + ") não encontrada.");
        } catch (UnauthorizedException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .contentType(MediaType.TEXT_PLAIN)
                    .body("Usuário não autorizado a acessar esta solicitação.");
        }
    }
}
