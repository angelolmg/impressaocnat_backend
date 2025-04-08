package com.dticnat.controleimpressao.controller;

import com.dticnat.controleimpressao.exception.FileGoneException;
import com.dticnat.controleimpressao.exception.ForbiddenException;
import com.dticnat.controleimpressao.exception.PhysicalFileException;
import com.dticnat.controleimpressao.exception.UnauthorizedException;
import com.dticnat.controleimpressao.model.Request;
import com.dticnat.controleimpressao.model.dto.Payload;
import com.dticnat.controleimpressao.model.dto.RequestDTO;
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
import jakarta.validation.Valid;
import org.apache.coyote.BadRequestException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.FileNotFoundException;
import java.nio.file.NoSuchFileException;
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
                            array = @ArraySchema(schema = @Schema(implementation = Request.class))))
    })
    @GetMapping
    public ResponseEntity<?> getAllRequests(HttpServletRequest httpRequest,
                                            @Parameter(description = "Indica se a filtragem por usuário deve ser aplicada (opcional).") @RequestParam(value = "filtering", required = false) Boolean filtering,
                                            @Parameter(description = "Indica se as solicitações concluídas devem ser filtradas (opcional).") @RequestParam(value = "concluded", required = false) Boolean concluded,
                                            @Parameter(description = "Data de início (unix time) para filtragem por data (opcional).") @RequestParam(value = "startDate", required = false) Long startDate,
                                            @Parameter(description = "Data de término (unix time) para filtragem por data (opcional).") @RequestParam(value = "endDate", required = false) Long endDate,
                                            @Parameter(description = "Termo de pesquisa para filtragem por texto (opcional).") @RequestParam(value = "query", required = false) String query) {

        // Recuperar dados do usuário autenticado do request http
        UserData userData = (UserData) httpRequest.getAttribute("userData");

        // Se for admin retornaremos TODAS as solicitações, não passamos filtro de matrícula
        // Se não for, passamos a matrícula como filtro
        // A não ser que o admin esteja filtrando seus resultados para receber apenas as próprias solicitações
        String userRegistration = (!userData.isAdmin() || (filtering != null && filtering)) ? userData.getMatricula() : null;

        // Buscar as solicitações filtradas
        List<Request> requests = requestService.findAll(startDate, endDate, query, concluded, userRegistration);
        return ResponseEntity.ok(requests);
    }

    /**
     * Busca uma solicitação pelo ID, com validação de permissão para usuários não administradores.
            *
            * @param requestId ID da solicitação a ser buscada.
            * @return Solicitação encontrada (se o usuário tiver permissão) ou mensagem de erro.
            */
    @Operation(summary = "Busca uma solicitação pelo ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Solicitação encontrada.",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = Request.class))),
            @ApiResponse(responseCode = "401", description = "Não autorizado.",
                    content = @Content(mediaType = "text/plain", schema = @Schema(type = "string", example = "Usuário não autorizado a acessar esta solicitação."))),
            @ApiResponse(responseCode = "404", description = "Solicitação não encontrada.",
                    content = @Content(mediaType = "text/plain", schema = @Schema(type = "string", example = "Solicitação com ID 123 não encontrada.")))
    })
    @GetMapping("/{requestId}")
    public ResponseEntity<?> getRequestById(HttpServletRequest httpRequest,
                                            @Parameter(description = "ID da solicitação a ser buscada.") @PathVariable Long requestId) {

        // Recuperar dados do usuário autenticado do request http
        UserData userData = (UserData) httpRequest.getAttribute("userData");

        // Verificar se solicitação sendo alterada pertence ao usuário tentando buscá-la
        // Se o usuario for admin, ele pode editar mesmo solicitações que não são dele
        try {
            Request userRequest = requestService.canInteract(requestId, userData, false);
            return ResponseEntity.ok(userRequest);

        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .contentType(MediaType.TEXT_PLAIN)
                    .body("Solicitação com ID " + requestId + " não encontrada.");
        } catch (UnauthorizedException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .contentType(MediaType.TEXT_PLAIN)
                    .body("Usuário não autorizado a acessar esta solicitação.");
        }
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
            @ApiResponse(responseCode = "200", description = "Arquivo encontrado.",
                    content = @Content(mediaType = "application/octet-stream")),
            @ApiResponse(responseCode = "401", description = "Não autorizado.",
                    content = @Content(mediaType = "text/plain", schema = @Schema(type = "string", example = "Usuário não autorizado a acessar o arquivo desta solicitação."))),
            @ApiResponse(responseCode = "404", description = "Solicitação ou arquivo não encontrado.",
                    content = @Content(mediaType = "text/plain", schema = @Schema(type = "string", example = "Solicitação com ID 123 não encontrada ou Cópia com nome 'nomeDoArquivo.ext' não encontrada ou Arquivo 'nomeDoArquivo.ext' não encontrado."))),
            @ApiResponse(responseCode = "410", description = "Arquivo removido.",
                    content = @Content(mediaType = "text/plain", schema = @Schema(type = "string", example = "O arquivo 'nomeDoArquivo.ext' não está mais disponível."))),
            @ApiResponse(responseCode = "500", description = "Erro interno ao processar o arquivo (IOException, OutOfMemoryError, SecurityException, etc).",
                    content = @Content(mediaType = "text/plain", schema = @Schema(type = "string", example = "Erro ao ler o arquivo do sistema.")))
    })
    @GetMapping("/{id}/{fileName}")
    public ResponseEntity<?> downloadFile(HttpServletRequest httpRequest,
                                          @Parameter(description = "ID da solicitação.") @PathVariable Long id,
                                          @Parameter(description = "Nome do arquivo a ser baixado.") @PathVariable String fileName) {

        // Recuperar dados do usuário autenticado do request http
        UserData userData = (UserData) httpRequest.getAttribute("userData");

        try {
            // Chama o serviço para realizar a lógica de validação e busca do arquivo
            return requestService.getFileResponse(userData, id, fileName);

            // A operação de buscar um arquivo em disco pode gerar diversas exceções diferentes
            // Aqui foi bastante glanularizado para buscar retornar a mensagem de erro mais apropriada
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .contentType(MediaType.TEXT_PLAIN)
                    .body(String.format("Solicitação com ID: %d não encontrada.", id));
        } catch (FileNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .contentType(MediaType.TEXT_PLAIN)
                    .body(String.format("Cópia com nome: %s não foi encontrada na solicitação.", fileName));
        } catch (UnauthorizedException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .contentType(MediaType.TEXT_PLAIN)
                    .body("Usuário não autorizado a acessar o arquivo desta solicitação.");
        } catch (NoSuchFileException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .contentType(MediaType.TEXT_PLAIN)
                    .body(String.format("O arquivo '%s' não foi encontrado no sistema.", fileName));
        } catch (PhysicalFileException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .contentType(MediaType.TEXT_PLAIN)
                    .body(String.format("O arquivo '%s' é físico e não pode ser encontrado no sistema.", fileName));
        } catch (FileGoneException e) {
            return ResponseEntity.status(HttpStatus.GONE)
                    .contentType(MediaType.TEXT_PLAIN)
                    .body(String.format("O arquivo '%s' não está mais disponível.", fileName));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .contentType(MediaType.TEXT_PLAIN)
                    .body("Erro ao ler o arquivo do sistema.");
        }
    }

    /**
     * Atualiza parcialmente uma solicitação existente, permitindo a modificação de dados e a adição/substituição de arquivos.
     *
     * @param requestId   ID da solicitação a ser atualizada.
     * @param request     Objeto Request contendo os dados a serem atualizados.
     * @param files       Lista de arquivos a serem adicionados ou substituídos (opcional).
     * @return Solicitação atualizada ou mensagem de erro.
     */
    @Operation(summary = "Atualiza parcialmente uma solicitação existente")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Solicitação atualizada com sucesso",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = com.dticnat.controleimpressao.model.Request.class))),
            @ApiResponse(responseCode = "400", description = "Número de arquivos enviados foi insuficiente.",
                    content = @Content(mediaType = "text/plain", schema = @Schema(type = "string", example = "O número de arquivos enviados (2) não corresponde ao número de cópias a carregar (3)."))),
            @ApiResponse(responseCode = "401", description = "Não autorizado. A solicitação não pertence ao usuário ou o usuário não tem permissão para editar.",
                    content = @Content(mediaType = "text/plain", schema = @Schema(type = "string", example = "Usuário não autorizado a acessar esta solicitação."))),
            @ApiResponse(responseCode = "403", description = "Não é possível alterar o status de uma solicitação que foi arquivada.",
                    content = @Content(mediaType = "text/plain", schema = @Schema(type = "string", example = "Não é possível alterar solicitações arquivadas."))),
            @ApiResponse(responseCode = "404", description = "Solicitação não encontrada.",
                    content = @Content(mediaType = "text/plain", schema = @Schema(type = "string", example = "Solicitação (ID 000123) não encontrada."))),
            @ApiResponse(responseCode = "500", description = "Erro interno ao processar a solicitação ou arquivos.",
                    content = @Content(mediaType = "text/plain", schema = @Schema(type = "string", example = "Erro inesperado ao processar solicitação.")))
    })
    @PatchMapping(value = "/{requestId}", consumes = {"multipart/form-data"})
    public ResponseEntity<?> patchRequest(HttpServletRequest httpRequest,
            @Parameter(description = "ID da solicitação") @PathVariable Long requestId,
            @Parameter(description = "Dados da solicitação a serem atualizados")
                                              @RequestPart("solicitacao") @Valid com.dticnat.controleimpressao.model.Request request,
            @Parameter(description = "Lista de arquivos a serem adicionados/substituídos")
                                              @RequestPart(value = "arquivos", required = false) List<MultipartFile> files) {

        // Recuperar dados do usuário autenticado do request http
        UserData userData = (UserData) httpRequest.getAttribute("userData");

        try {
            // Verificar se solicitação sendo alterada pertence ao usuário tentando editá-la
            // Se o usuario for admin, ele pode editar mesmo solicitações que não são dele
            requestService.canInteract(requestId, userData, true);

            // Caso não sejam enviados dados de arquivos digitais, inicializa-se um placeholder vazio
            if (files == null) files = new ArrayList<>();

            // Tenta salvar os arquivos passados pelo usuário
            // 'isNewRequest == false' indica que é edição de uma solicitação já existente
            requestService.saveFiles(request, files, false);

            // Intancia cópias associadas na database antes de editar solicitação
            // copyService.instanceCopiesFromRequest(request);
            Request editedRequest = requestService.patch(requestId, request);

            // Retorna solicitação editada
            return ResponseEntity.ok(editedRequest);

        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .contentType(MediaType.TEXT_PLAIN)
                    .body("Solicitação com ID " + requestId + " não encontrada.");
        } catch (UnauthorizedException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .contentType(MediaType.TEXT_PLAIN)
                    .body("Usuário não autorizado a acessar esta solicitação.");
        } catch (BadRequestException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .contentType(MediaType.TEXT_PLAIN)
                    .body(e.getMessage());
        } catch (ForbiddenException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .contentType(MediaType.TEXT_PLAIN)
                    .body("Não é possível alterar solicitações arquivadas.");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .contentType(MediaType.TEXT_PLAIN)
                    .body("Erro inesperado ao processar solicitação.");
        }
    }

    /**
     * Cria uma nova solicitação com os dados fornecidos e os arquivos anexados.
     *
     * @param requestDTO Objeto RequestDTO contendo os dados da nova solicitação.
     * @param files   Lista de arquivos a serem anexados à solicitação.
     * @return Nova solicitação criada ou mensagem de erro.
     */
    @Operation(summary = "Cria uma nova solicitação")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Solicitação criada com sucesso",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = com.dticnat.controleimpressao.model.Request.class))),
            @ApiResponse(responseCode = "400", description = "Número de arquivos enviados foi insuficiente.",
                    content = @Content(mediaType = "text/plain", schema = @Schema(type = "string", example = "O número de arquivos enviados (2) não corresponde ao número de cópias a carregar (3)."))),
            @ApiResponse(responseCode = "403", description = "Não é possível alterar o status de uma solicitação que foi arquivada.",
                    content = @Content(mediaType = "text/plain", schema = @Schema(type = "string", example = "Não é possível alterar solicitações arquivadas."))),
            @ApiResponse(responseCode = "500", description = "Erro interno ao processar a solicitação ou arquivos",
                    content = @Content(mediaType = "text/plain", schema = @Schema(type = "string", example = "Erro inesperado ao processar solicitação.")))
    })
    @PostMapping(consumes = {"multipart/form-data"})
    public ResponseEntity<?> createRequest(HttpServletRequest httpRequest,
                                           @Parameter(description = "Dados da solicitação a ser criada") @Valid @RequestPart("solicitacao") RequestDTO requestDTO,
                                           @Parameter(description = "Lista de arquivos associados a solicitação") @RequestPart(value = "arquivos", required = false) List<MultipartFile> files) {

        // Recuperar dados do usuário autenticado do request http
        UserData userData = (UserData) httpRequest.getAttribute("userData");

        // Criar nova a solicitação no banco de dados
        Request newRequest = requestService.create(requestDTO, userData);

        // Salvar os arquivos em disco
        // Se um arquivo da solicitação dá erro, os demais salvos anteriormente devem ser excluídos
        try {
            requestService.saveFiles(newRequest, files, true);
            return ResponseEntity.status(HttpStatus.CREATED).body(newRequest);
        } catch (BadRequestException e) {
            requestService.removeRequest(newRequest.getId());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .contentType(MediaType.TEXT_PLAIN)
                    .body(e.getMessage());
        } catch (Exception e) {
            requestService.removeRequest(newRequest.getId());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .contentType(MediaType.TEXT_PLAIN)
                    .body("Erro inesperado ao processar solicitação.");
        }
    }

    /**
     * Altera o status de conclusão de uma solicitação para concluída ou não concluída.
     *
     * @param requestId ID da solicitação a ter o status atualizado.
     * @return Mensagem de sucesso ou erro.
     */
    @Operation(summary = "Altera o status de conclusão de uma solicitação")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Status da solicitação atualizado com sucesso.",
                    content = @Content(mediaType = "text/plain", schema = @Schema(type = "string", example = "Status da solicitação atualizado com sucesso"))),
            @ApiResponse(responseCode = "401", description = "Não autorizado. A solicitação não pertence ao usuário ou o usuário não tem permissão para editar.",
                    content = @Content(mediaType = "text/plain", schema = @Schema(type = "string", example = "Usuário não autorizado a acessar esta solicitação."))),
            @ApiResponse(responseCode = "403", description = "Não é possível alterar o status de uma solicitação que foi arquivada.",
                    content = @Content(mediaType = "text/plain", schema = @Schema(type = "string", example = "Não é possível alterar solicitações arquivadas."))),
            @ApiResponse(responseCode = "404", description = "Solicitação não encontrada.",
                    content = @Content(mediaType = "text/plain", schema = @Schema(type = "string", example = "Solicitação (ID 000123) não encontrada."))),
    })
    @PatchMapping("/{requestId}/status")
    public ResponseEntity<?> toggleRequestStatus(HttpServletRequest httpRequest,
                                                 @Parameter(description = "ID da solicitação") @PathVariable Long requestId) {

        // Recuperar dados do usuário autenticado do request http
        UserData userData = (UserData) httpRequest.getAttribute("userData");

        try {
            // Verificar se solicitação sendo alterada pertence ao usuário tentando editá-la
            // Se o usuario for admin, ele pode editar mesmo solicitações que não são dele
            requestService.canInteract(requestId, userData, true);

            // Alterna status da solicitação
            requestService.toggleConclusionDatebyId(requestId);
            Payload<?> response = Payload
                    .builder()
                    .status(HttpStatus.OK.value())
                    .message("Status da solicitação atualizado com sucesso.")
                    .build();
            return ResponseEntity.status(response.getStatus()).body(response);

        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .contentType(MediaType.TEXT_PLAIN)
                    .body("Solicitação (ID " + String.format("%06d", requestId) + ") não encontrada.");
        } catch (UnauthorizedException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .contentType(MediaType.TEXT_PLAIN)
                    .body("Usuário não autorizado a editar esta solicitação.");
        } catch (ForbiddenException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .contentType(MediaType.TEXT_PLAIN)
                    .body("Não é possível alterar solicitações arquivadas.");
        }
    }

    /**
     * Remove uma solicitação pelo ID.
     *
     * @param requestId ID da solicitação a ser removida.
     * @return Mensagem de sucesso ou erro.
     */
    @Operation(summary = "Remove uma solicitação pelo ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Solicitação removida com sucesso",
                    content = @Content(mediaType = "text/plain", schema = @Schema(type = "string", example = "Solicitação (ID 000123) removida com sucesso."))),
            @ApiResponse(responseCode = "401", description = "Não autorizado. A solicitação não pertence ao usuário ou o usuário não tem permissão para editar.",
                    content = @Content(mediaType = "text/plain", schema = @Schema(type = "string", example = "Usuário não autorizado a remover esta solicitação."))),
            @ApiResponse(responseCode = "403", description = "Não é possível alterar o status de uma solicitação que foi arquivada.",
                    content = @Content(mediaType = "text/plain", schema = @Schema(type = "string", example = "Não é possível alterar solicitações arquivadas."))),
            @ApiResponse(responseCode = "404", description = "Solicitação não encontrada",
                    content = @Content(mediaType = "text/plain", schema = @Schema(type = "string", example = "Solicitação (ID 000123) não encontrada.")))
    })
    @DeleteMapping("/{requestId}")
    public ResponseEntity<?> removeRequest(HttpServletRequest httpRequest,
                                           @Parameter(description = "ID da solicitação") @PathVariable Long requestId) {
        // Recuperar dados do usuário autenticado do request http
        UserData userData = (UserData) httpRequest.getAttribute("userData");

        try {
            // Verificar se solicitação sendo alterada pertence ao usuário tentando editá-la
            // Se o usuario for admin, ele pode remover mesmo solicitações que não são dele
            requestService.canInteract(requestId, userData, true);

            // Remove solicitação
            requestService.removeRequest(requestId);

            Payload<?> response = Payload
                    .builder()
                    .status(HttpStatus.OK.value())
                    .message("Solicitação (ID " + String.format("%06d", requestId) + ") removida com sucesso.")
                    .build();
            return ResponseEntity.status(response.getStatus()).body(response);

        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .contentType(MediaType.TEXT_PLAIN)
                    .body("Solicitação (ID " + String.format("%06d", requestId) + ") não encontrada.");
        } catch (UnauthorizedException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .contentType(MediaType.TEXT_PLAIN)
                    .body("Usuário não autorizado a remover esta solicitação.");
        } catch (ForbiddenException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .contentType(MediaType.TEXT_PLAIN)
                    .body("Não é possível alterar solicitações arquivadas.");
        }
    }
}