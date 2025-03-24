package com.dticnat.controleimpressao.controller;

import com.dticnat.controleimpressao.exception.FileGoneException;
import com.dticnat.controleimpressao.exception.PhysicalFileException;
import com.dticnat.controleimpressao.exception.UnauthorizedException;
import com.dticnat.controleimpressao.model.Request;
import com.dticnat.controleimpressao.model.dto.UserData;
import com.dticnat.controleimpressao.service.AuthService;
import com.dticnat.controleimpressao.service.CopyService;
import com.dticnat.controleimpressao.service.RequestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

@RestController
@RequestMapping("/solicitacoes")
@Tag(name = "Solicitações", description = "Operações relacionadas a solicitações de impressão")
public class RequestController {

    @Autowired
    private RequestService requestService;

    @Autowired
    private CopyService copyService;

    @Autowired
    private AuthService authService;

    /**
     * Lista todas as solicitações, com opções de filtragem por usuário, status, data e pesquisa.
     *
     * @param filtering   Indica se a filtragem por usuário deve ser aplicada (opcional).
     * @param concluded   Indica se as solicitações concluídas devem ser filtradas (opcional).
     * @param startDate   Data de início (unix time) para filtragem por data (opcional).
     * @param endDate     Data de término (unix time) para filtragem por data (opcional).
     * @param query       Termo de pesquisa para filtragem por texto (opcional).
     * @return Lista de solicitações filtradas ou mensagem de erro.
     */
    @Operation(summary = "Lista todas as solicitações")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                            description = "Lista de solicitações retornada com sucesso",
                            content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = Request.class))),
    })
    @GetMapping
    public ResponseEntity<?> getAllRequests(HttpServletRequest httpRequest,
                                            @Parameter(description = "Indica se a filtragem por usuário deve ser aplicada (opcional).") @RequestParam(value = "filtering", required = false) Boolean filtering,
                                            @Parameter(description = "Indica se as solicitações concluídas devem ser filtradas (opcional).") @RequestParam(value = "concluded", required = false) Boolean concluded,
                                            @Parameter(description = "Data de início (unix time) para filtragem por data (opcional).") @RequestParam(value = "startDate", required = false) Long startDate,
                                            @Parameter(description = "Data de término (unix time) para filtragem por data (opcional).") @RequestParam(value = "endDate", required = false) Long endDate,
                                            @Parameter(description = "Termo de pesquisa para filtragem por texto (opcional).") @RequestParam(value = "query", required = false) String query) {
        // Recuperar dados do usuário
        UserData userData = (UserData) httpRequest.getAttribute("userData");
        boolean isAdmin = (boolean) httpRequest.getAttribute("isAdmin");

        // Se for admin retornaremos TODAS as solicitações, não passamos filtro de matrícula
        // Se não for, passamos a matrícula como filtro
        // A não ser que o admin esteja filtrando seus resultados para receber apenas as próprias solicitações
        String userRegistration = (!isAdmin || (filtering != null && filtering)) ? userData.getMatricula() : null;

        List<Request> requests = requestService.findAll(startDate, endDate, query, concluded, userRegistration);
        return ResponseEntity.ok(requests);
    }

    /**
     * Busca uma solicitação pelo ID.
     *
     * @param id ID da solicitação.
     * @return Solicitação encontrada ou mensagem de erro.
     */
    @Operation(summary = "Busca uma solicitação pelo ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Solicitação encontrada.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = Request.class))),
            @ApiResponse(responseCode = "404", description = "Solicitação não encontrada.", content = @Content(mediaType = "text/plain", schema = @Schema(type = "string", example = "Solicitação com ID 123 não encontrada.")))
    })
    @GetMapping("/{id}")
    public ResponseEntity<?> getRequestById(HttpServletRequest httpRequest,
                                            @PathVariable Long id) {
        // Recuperar dados do usuário
        UserData userData = (UserData) httpRequest.getAttribute("userData");
        boolean isAdmin = (boolean) httpRequest.getAttribute("isAdmin");

        Optional<Request> solicitacao = requestService.findById(id);

        return solicitacao.<ResponseEntity<?>>map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).body("Solicitação com ID " + id + " não encontrada."));
    }

    /**
     * Busca e baixa um arquivo associado a uma solicitação.
     *
     * @param id        ID da solicitação.
     * @param fileName  Nome do arquivo.
     * @return Arquivo para download ou mensagem de erro.
     */
    @Operation(summary = "Busca e baixa um arquivo associado a uma solicitação")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Arquivo encontrado.", content = @Content(mediaType = "application/octet-stream")),
            @ApiResponse(responseCode = "401", description = "Não autorizado", content = @Content(mediaType = "text/plain", schema = @Schema(type = "string", example = "Solicitação com ID 123 não encontrada."))),
            @ApiResponse(responseCode = "404", description = "Arquivo não encontrado", content = @Content(mediaType = "text/plain", schema = @Schema(type = "string", example = "Solicitação com ID 123 não encontrada."))),
            @ApiResponse(responseCode = "410", description = "Arquivo removido", content = @Content(mediaType = "text/plain", schema = @Schema(type = "string", example = "Solicitação com ID 123 não encontrada."))),
            @ApiResponse(responseCode = "500", description = "Erro interno ao processar o arquivo", content = @Content(mediaType = "text/plain", schema = @Schema(type = "string", example = "Solicitação com ID 123 não encontrada.")))
    })
    @GetMapping("/{id}/{fileName}")
    public ResponseEntity<?> downloadFile(HttpServletRequest httpRequest,
                                          @PathVariable Long id,
                                          @PathVariable String fileName) {

        // Recuperar dados do usuário
        UserData userData = (UserData) httpRequest.getAttribute("userData");
        boolean isAdmin = (boolean) httpRequest.getAttribute("isAdmin");

        try {
            // Chama o serviço para realizar a lógica de validação e busca do arquivo
            return requestService.getFileResponse(userData, isAdmin, id, fileName);
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).contentType(MediaType.TEXT_PLAIN).body("Solicitação com ID: " + id + " não existe.");
        } catch (PhysicalFileException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).contentType(MediaType.TEXT_PLAIN).body("O arquivo é físico e não pode ser encontrado no sistema.");
        } catch (UnauthorizedException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).contentType(MediaType.TEXT_PLAIN).body("Usuário não autorizado");
        } catch (FileNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).contentType(MediaType.TEXT_PLAIN).body("O arquivo " + fileName + " não foi encontrado no sistema.");
        } catch (FileGoneException e) {
            return ResponseEntity.status(HttpStatus.GONE).contentType(MediaType.TEXT_PLAIN).body("O arquivo " + fileName + " não está mais disponível.");
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).contentType(MediaType.TEXT_PLAIN).body("Erro ao processar o arquivo.".getBytes(StandardCharsets.UTF_8));
        }
    }

    /**
     * Atualiza parcialmente uma solicitação existente, permitindo a modificação de dados e a adição/substituição de arquivos.
     *
     * @param id          ID da solicitação a ser atualizada.
     * @param request     Objeto Request contendo os dados a serem atualizados.
     * @param files       Lista de arquivos a serem adicionados ou substituídos (opcional).
     * @return Solicitação atualizada ou mensagem de erro.
     */
    @Operation(summary = "Atualiza parcialmente uma solicitação existente")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Solicitação atualizada com sucesso", content = @Content(mediaType = "application/json", schema = @Schema(implementation = com.dticnat.controleimpressao.model.Request.class))),
            @ApiResponse(responseCode = "400", description = "Token de acesso não encontrado ou formato incorreto.", content = @Content(mediaType = "text/plain", schema = @Schema(type = "string", example = "Token de acesso não encontrado. Por favor, realizar login."))),
            @ApiResponse(responseCode = "401", description = "Não autorizado. A solicitação não pertence ao usuário ou o usuário não tem permissão para editar.", content = @Content(mediaType = "text/plain", schema = @Schema(type = "string", example = "Solicitação não pertence ao usuário. Não foi possível editar."))),
            @ApiResponse(responseCode = "404", description = "Usuário ou solicitação não encontrada.", content = @Content(mediaType = "text/plain", schema = @Schema(type = "string", example = "'Usuário não encontrado.' ou 'Solicitação com ID 123 não encontrada.'"))),
            @ApiResponse(responseCode = "500", description = "Erro interno ao processar a solicitação ou arquivos.", content = @Content(mediaType = "text/plain", schema = @Schema(type = "string", example = "Erro ao salvar arquivos: disco cheio ou Erro ao processar dados da solicitação.")))
    })
    @PatchMapping(value = "/{id}", consumes = {"multipart/form-data"})
    public ResponseEntity<?> patchRequest(HttpServletRequest httpRequest,
            @Parameter(description = "ID da solicitação") @PathVariable Long id,
            @Parameter(description = "Dados da solicitação a serem atualizados") @RequestPart("solicitacao") @Valid com.dticnat.controleimpressao.model.Request request,
            @Parameter(description = "Lista de arquivos a serem adicionados/substituídos") @RequestPart(value = "arquivos", required = false) List<MultipartFile> files) {

        // Recuperar dados do usuário
        UserData userData = (UserData) httpRequest.getAttribute("userData");
        boolean isAdmin = (boolean) httpRequest.getAttribute("isAdmin");

        // Verificar se solicitação sendo alterada pertence ao usuário tentando editá-la
        if (!isAdmin && !requestService.belongsTo(id, userData.getMatricula())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Solicitação não pertence ao usuário. Não foi possivel editar.");
        }

        if (files == null) files = new ArrayList<>();

        String mensagemErro = requestService.saveFiles(request, files, false);

        if (!mensagemErro.isBlank()) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(mensagemErro);
        }

        Request editedRequest;

        try {
            copyService.instanceCopiesFromRequest(request);
            editedRequest = requestService.patch(id, request);
        } catch (NoSuchElementException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
        }

        return ResponseEntity.ok(editedRequest);
    }

    /**
     * Cria uma nova solicitação com os dados fornecidos e os arquivos anexados.
     *
     * @param request Objeto Request contendo os dados da nova solicitação.
     * @param files   Lista de arquivos a serem anexados à solicitação.
     * @return Nova solicitação criada ou mensagem de erro.
     */
    @Operation(summary = "Cria uma nova solicitação")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Solicitação criada com sucesso", content = @Content(mediaType = "application/json", schema = @Schema(implementation = com.dticnat.controleimpressao.model.Request.class))),
            @ApiResponse(responseCode = "500", description = "Erro interno ao processar a solicitação ou arquivos", content = @Content(mediaType = "text/plain"))
    })
    @PostMapping(consumes = {"multipart/form-data"})
    public ResponseEntity<?> createRequest(HttpServletRequest httpRequest,
                                           @RequestPart("solicitacao") @Valid Request request,
                                           @RequestPart("arquivos") List<MultipartFile> files) {

        // Recuperar dados do usuário
        UserData userData = (UserData) httpRequest.getAttribute("userData");
        boolean isAdmin = (boolean) httpRequest.getAttribute("isAdmin");

        // Instanciar e associar cópias à solicitação
        copyService.instanceCopiesFromRequest(request);

        // Criar nova a solicitação no banco de dados
        Request newRequest = requestService.create(request);

        // Salvar os arquivos em disco
        String mensagemErro = requestService.saveFiles(newRequest, files, true);

        // Lógica para quando há erro de salvamento IO
        // Se um arquivo da solicitação dá erro, os demais salvos anteriormente devem ser excluídos
        if (!mensagemErro.isBlank()) {
            requestService.removeRequest(newRequest.getId());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(mensagemErro);
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(newRequest);
    }

    /**
     * Atualiza o status de conclusão de uma solicitação para concluída ou não concluída.
     *
     * @param id ID da solicitação a ter o status atualizado.
     * @return Mensagem de sucesso ou erro.
     */
    @Operation(summary = "Atualiza o status de conclusão de uma solicitação")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Status da solicitação atualizado com sucesso", content = @Content(mediaType = "application/json", schema = @Schema(type = "object"))),
            @ApiResponse(responseCode = "500", description = "Não foi possível atualizar o status da solicitação", content = @Content(mediaType = "application/json", schema = @Schema(type = "object")))
    })
    @PatchMapping("/{id}/status")
    public ResponseEntity<Map<String, String>> toggleRequestStatus(HttpServletRequest httpRequest,
                                                                   @PathVariable Long id) {

        // Recuperar dados do usuário
        UserData userData = (UserData) httpRequest.getAttribute("userData");
        boolean isAdmin = (boolean) httpRequest.getAttribute("isAdmin");

        return (requestService.toggleConclusionDatebyId(id)) ?
                ResponseEntity.ok(Map.of("message", "Status da solicitação (ID " + id + ") atualizado.")) :
                ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                        Map.of("message", "Não foi possivel atualizar status da solicitação (ID " + id + ")."));
    }

    /**
     * Remove uma solicitação pelo ID.
     *
     * @param id ID da solicitação a ser removida.
     * @return Mensagem de sucesso ou erro.
     */
    @Operation(summary = "Remove uma solicitação pelo ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Solicitação removida com sucesso", content = @Content(mediaType = "application/json", schema = @Schema(type = "object"))),
            @ApiResponse(responseCode = "404", description = "Solicitação não encontrada", content = @Content(mediaType = "application/json", schema = @Schema(type = "object")))
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> removeRequest(HttpServletRequest httpRequest,
                                                             @PathVariable Long id) {
        // Recuperar dados do usuário
        UserData userData = (UserData) httpRequest.getAttribute("userData");
        boolean isAdmin = (boolean) httpRequest.getAttribute("isAdmin");

        return (requestService.removeRequest(id)) ? ResponseEntity.ok(Map.of("message", "Solicitação (ID " + id + ") removida com sucesso.")) : ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "Solicitação (ID " + id + ") não encontrada."));
    }
}