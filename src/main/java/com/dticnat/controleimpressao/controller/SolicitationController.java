package com.dticnat.controleimpressao.controller;

import com.dticnat.controleimpressao.exception.FileGoneException;
import com.dticnat.controleimpressao.exception.ForbiddenException;
import com.dticnat.controleimpressao.exception.PhysicalFileException;
import com.dticnat.controleimpressao.model.Solicitation;
import com.dticnat.controleimpressao.model.User;
import com.dticnat.controleimpressao.model.dto.CommentDTO;
import com.dticnat.controleimpressao.model.dto.SolicitationDTO;
import com.dticnat.controleimpressao.model.enums.EventType;
import com.dticnat.controleimpressao.service.AuthService;
import com.dticnat.controleimpressao.service.CopyService;
import com.dticnat.controleimpressao.service.SolicitationService;
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
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.FileNotFoundException;
import java.nio.file.NoSuchFileException;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/solicitacoes")
@Tag(name = "Solicitações", description = "Operações relacionadas a solicitações de impressão")
public class SolicitationController {

    @Autowired
    private SolicitationService solicitationService;

    @Autowired
    private CopyService copyService;

    @Autowired
    private AuthService authService;

    /**
     * Lista todas as solicitações, com opções de filtragem por usuário, status, data e pesquisa.
     *
     * @param filtering Indica se a filtragem por usuário deve ser aplicada (opcional).
     * @param concluded Indica se as solicitações concluídas devem ser filtradas (opcional).
     * @param startDate Data de início (unix time) para filtragem por data (opcional).
     * @param endDate   Data de término (unix time) para filtragem por data (opcional).
     * @param query     Termo de pesquisa para filtragem por texto (opcional).
     * @return Lista de solicitações filtradas ou mensagem de erro.
     */
    @Operation(summary = "Lista todas as solicitações")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Lista de solicitações retornada com sucesso.",
                    content = @Content(mediaType = "application/json",
                            array = @ArraySchema(schema = @Schema(implementation = Solicitation.class))))
    })
    @GetMapping
    public ResponseEntity<?> getAllSolicitations(HttpServletRequest httpRequest,
                                                 @Parameter(description = "Indica se a filtragem por usuário deve ser aplicada (opcional).") @RequestParam(value = "filtering", required = false) Boolean filtering,
                                                 @Parameter(description = "Indica se as solicitações concluídas devem ser filtradas (opcional).") @RequestParam(value = "concluded", required = false) Boolean concluded,
                                                 @Parameter(description = "Data de início para filtragem por data (opcional).")
                                                 @RequestParam(value = "startDate", required = false)
                                                 @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
                                                 @Parameter(description = "Data de término para filtragem por data (opcional).")
                                                 @RequestParam(value = "endDate", required = false)
                                                 @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
                                                 @Parameter(description = "Termo de pesquisa para filtragem por texto (opcional).") @RequestParam(value = "query", required = false) String query) {

        // Recuperar dados do usuário autenticado do request http
        User user = (User) httpRequest.getAttribute("userPrincipal");

        // Se for admin retornaremos TODAS as solicitações, não passamos filtro de matrícula
        // Se não for, passamos a matrícula como filtro
        // A não ser que o admin esteja filtrando seus resultados para receber apenas as próprias solicitações
        String userRegistration = (!user.isAdminOrManager() || (filtering != null && filtering)) ? user.getRegistrationNumber() : null;

        // Buscar as solicitações filtradas
        List<Solicitation> solicitations = solicitationService.findAll(startDate, endDate, query, concluded, userRegistration);
        return ResponseEntity.ok(solicitations);
    }


    /**
     * Busca uma página de solicitações com suporte a filtros e ordenação.
     *
     * @param httpRequest          Requisição HTTP com o usuário autenticado.
     * @param filtering            Indica se a filtragem por matrícula deve ser aplicada.
     * @param concluded            Filtro para solicitações concluídas.
     * @param startDate            Data de início para filtro por data.
     * @param endDate              Data de término para filtro por data.
     * @param query                Termo de pesquisa textual.
     * @param sortingColumn        Coluna para ordenação.
     * @param sortingDirection     Direção da ordenação (asc/desc).
     * @param pageNo               Número da página.
     * @param pageSize             Tamanho da página.
     * @return Página de solicitações conforme os filtros aplicados.
     */
    @Operation(summary = "Busca uma página de solicitações")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Página de solicitações retornada com sucesso.",
                    content = @Content(mediaType = "application/json",
                            array = @ArraySchema(schema = @Schema(implementation = Solicitation.class))))
    })
    @GetMapping("/pagina")
    public ResponseEntity<Page<Solicitation>> getPageSolicitations(HttpServletRequest httpRequest,
                                                                   @Parameter(description = "Indica se a filtragem por usuário deve ser aplicada (opcional).") @RequestParam(value = "filtering", required = false) Boolean filtering,
                                                                   @Parameter(description = "Indica se as solicitações concluídas devem ser filtradas (opcional).") @RequestParam(value = "concluded", required = false) Boolean concluded,
                                                                   @Parameter(description = "Data de início para filtragem por data (opcional).")
                                                                   @RequestParam(value = "startDate", required = false)
                                                                   @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
                                                                   @Parameter(description = "Data de término para filtragem por data (opcional).")
                                                                   @RequestParam(value = "endDate", required = false)
                                                                   @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
                                                                   @Parameter(description = "Termo de pesquisa para filtragem por texto (opcional).") @RequestParam(value = "query", required = false) String query,
                                                                   @Parameter(description = "Coluna de ordenação (opcional).") @RequestParam(value = "sortingColumn", required = false) String sortingColumn,
                                                                   @Parameter(description = "direção da ordenação (opcional).") @RequestParam(value = "sortingDirection", required = false) String sortingDirection,
                                                                   @RequestParam(defaultValue = "0") int pageNo,
                                                                   @RequestParam(defaultValue = "10") int pageSize) {

        // Recuperar dados do usuário autenticado do request http
        User user = (User) httpRequest.getAttribute("userPrincipal");

        // Se for admin retornaremos TODAS as solicitações, não passamos filtro de matrícula
        // Se não for, passamos a matrícula como filtro
        // A não ser que o admin esteja filtrando seus resultados para receber apenas as próprias solicitações
        String userRegistration = (!user.isAdminOrManager() || (filtering != null && filtering)) ? user.getRegistrationNumber() : null;

        // Buscar as solicitações filtradas
        Page<Solicitation> solicitations = solicitationService.findPage(startDate, endDate, query, concluded, userRegistration, pageNo, pageSize, sortingColumn, sortingDirection);
        return ResponseEntity.ok(solicitations);
    }

    /**
     * Busca uma solicitação pelo ID, com validação de permissão para usuários não administradores.
     *
     * @param solicitationId ID da solicitação a ser buscada.
     * @return Solicitação encontrada (se o usuário tiver permissão) ou mensagem de erro.
     */
    @Operation(summary = "Busca uma solicitação pelo ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Solicitação encontrada.",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = Solicitation.class))),
            @ApiResponse(responseCode = "403", description = "Proibido.",
                    content = @Content(mediaType = "text/plain", schema = @Schema(type = "string", example = "Usuário não autorizado a acessar esta solicitação."))),
            @ApiResponse(responseCode = "404", description = "Solicitação não encontrada.",
                    content = @Content(mediaType = "text/plain", schema = @Schema(type = "string", example = "Solicitação com ID 123 não encontrada."))),
            @ApiResponse(responseCode = "500", description = "Erro interno.",
                    content = @Content(mediaType = "text/plain"))
    })
    @GetMapping("/{solicitationId}")
    public ResponseEntity<?> getSolicitationById(HttpServletRequest httpRequest,
                                                 @Parameter(description = "ID da solicitação a ser buscada.") @PathVariable Long solicitationId) {

        // Recuperar dados do usuário autenticado do request http
        User user = (User) httpRequest.getAttribute("userPrincipal");

        // Verificar se solicitação sendo alterada pertence ao usuário tentando buscá-la
        // Se o usuario for admin, ele pode editar mesmo solicitações que não são dele
        try {
            Solicitation userSolicitation = solicitationService.canInteract(solicitationId, user, EventType.REQUEST_VIEWING);
            return ResponseEntity.ok(userSolicitation);

        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .contentType(MediaType.TEXT_PLAIN)
                    .body("Solicitação (ID " + String.format("%06d", solicitationId) + ") não encontrada.");
        } catch (ForbiddenException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .contentType(MediaType.TEXT_PLAIN)
                    .body("Usuário não está autorizado a acessar este recurso.");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .contentType(MediaType.TEXT_PLAIN)
                    .body(e.getMessage());
        }
    }

    /**
     * Busca e baixa um arquivo associado a uma solicitação.
     *
     * @param solicitationId ID da solicitação.
     * @param fileName       Nome do arquivo.
     * @return Arquivo para download ou mensagem de erro.
     */
    @Operation(summary = "Busca e baixa um arquivo associado a uma solicitação")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Arquivo encontrado.",
                    content = @Content(mediaType = "application/octet-stream")),
            @ApiResponse(responseCode = "403", description = "Proibido.",
                    content = @Content(mediaType = "text/plain", schema = @Schema(type = "string", example = "Usuário não está autorizado a acessar este recurso."))),
            @ApiResponse(responseCode = "404", description = "Solicitação ou arquivo não encontrado.",
                    content = @Content(mediaType = "text/plain", schema = @Schema(type = "string", example = "Solicitação com ID 123 não encontrada ou Cópia com nome 'nomeDoArquivo.ext' não encontrada ou Arquivo 'nomeDoArquivo.ext' não encontrado."))),
            @ApiResponse(responseCode = "410", description = "Arquivo removido.",
                    content = @Content(mediaType = "text/plain", schema = @Schema(type = "string", example = "O arquivo 'nomeDoArquivo.ext' não está mais disponível."))),
            @ApiResponse(responseCode = "500", description = "Erro interno.",
                    content = @Content(mediaType = "text/plain"))
    })
    @GetMapping("/{solicitationId}/{fileName}")
    public ResponseEntity<?> downloadFile(HttpServletRequest httpRequest,
                                          @Parameter(description = "ID da solicitação.") @PathVariable Long solicitationId,
                                          @Parameter(description = "Nome do arquivo a ser baixado.") @PathVariable String fileName) {

        // Recuperar dados do usuário autenticado do request http
        User user = (User) httpRequest.getAttribute("userPrincipal");

        try {
            // Chama o serviço para realizar a lógica de validação e busca do arquivo
            return solicitationService.getFileResponse(user, solicitationId, fileName);

            // A operação de buscar um arquivo em disco pode gerar diversas exceções diferentes
            // Aqui foi bastante glanularizado para buscar retornar a mensagem de erro mais apropriada
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .contentType(MediaType.TEXT_PLAIN)
                    .body("Solicitação (ID " + String.format("%06d", solicitationId) + ") não encontrada.");
        } catch (FileNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .contentType(MediaType.TEXT_PLAIN)
                    .body(String.format("Cópia com nome: %s não foi encontrada na solicitação.", fileName));
        } catch (ForbiddenException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .contentType(MediaType.TEXT_PLAIN)
                    .body("Usuário não está autorizado a acessar este recurso.");
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
                    .body(e.getMessage());
        }
    }

    /**
     * Cria uma nova solicitação com os dados fornecidos e os arquivos anexados.
     *
     * @param solicitationDTO Objeto RequestDTO contendo os dados da nova solicitação.
     * @param files           Lista de arquivos a serem anexados à solicitação.
     * @return Nova solicitação criada ou mensagem de erro.
     */
    @Operation(summary = "Cria uma nova solicitação")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Solicitação criada com sucesso",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = Solicitation.class))),
            @ApiResponse(responseCode = "400", description = "Número de arquivos enviados foi insuficiente ou arquivo corrompido/encriptado.",
                    content = @Content(mediaType = "text/plain", schema = @Schema(type = "string", example = "O número de arquivos enviados (2) não corresponde ao número de cópias a carregar (3) OU O arquivo enviado 'corrupted.pdf' está encriptado ou corrompido."))),
            @ApiResponse(responseCode = "403", description = "Não é possível alterar o status de uma solicitação que foi arquivada.",
                    content = @Content(mediaType = "text/plain", schema = @Schema(type = "string", example = "Não é possível alterar solicitações arquivadas."))),
            @ApiResponse(responseCode = "500", description = "Erro interno.",
                    content = @Content(mediaType = "text/plain"))
    })
    @PostMapping(consumes = {"multipart/form-data"})
    public ResponseEntity<?> createRequest(HttpServletRequest httpRequest,
                                           @Parameter(description = "Dados da solicitação a ser criada") @Valid @RequestPart("solicitacao") SolicitationDTO solicitationDTO,
                                           @Parameter(description = "Lista de arquivos associados a solicitação. Obs.: para anexar 'Arquivo Físico' é necessário anexar arquivo vazio (size == 0).")
                                               @RequestPart(value = "arquivos", required = false) List<MultipartFile> files) {

        // Recuperar dados do usuário autenticado do request http
        User user = (User) httpRequest.getAttribute("userPrincipal");

        // Criar nova a solicitação no banco de dados
        Solicitation newSolicitation = solicitationService.create(solicitationDTO, user);

        // Salvar os arquivos em disco
        // Se um arquivo da solicitação dá erro, os demais salvos anteriormente devem ser excluídos
        try {
            solicitationService.saveFiles(newSolicitation, files, true);
            return ResponseEntity.status(HttpStatus.CREATED).body(newSolicitation);
        } catch (BadRequestException e) {
            solicitationService.removeRequest(newSolicitation.getId(), false, user);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .contentType(MediaType.TEXT_PLAIN)
                    .body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .contentType(MediaType.TEXT_PLAIN)
                    .body(e.getMessage());
        }
    }

    /**
     * Atualiza parcialmente uma solicitação existente, permitindo a modificação de dados e a adição/substituição de arquivos.
     *
     * @param solicitationId ID da solicitação a ser atualizada.
     * @param solicitation   Objeto Request contendo os dados a serem atualizados.
     * @param files          Lista de arquivos a serem adicionados ou substituídos (opcional).
     * @return Solicitação atualizada ou mensagem de erro.
     */
    @Operation(summary = "Atualiza parcialmente uma solicitação existente")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Solicitação atualizada com sucesso",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = Solicitation.class))),
            @ApiResponse(responseCode = "400", description = "Número de arquivos enviados foi insuficiente.",
                    content = @Content(mediaType = "text/plain", schema = @Schema(type = "string", example = "O número de arquivos enviados (2) não corresponde ao número de cópias a carregar (3)."))),
            @ApiResponse(responseCode = "403", description = "Não é possível alterar o status de uma solicitação que foi arquivada.",
                    content = @Content(mediaType = "text/plain", schema = @Schema(type = "string", example = "Não é possível alterar solicitações arquivadas."))),
            @ApiResponse(responseCode = "404", description = "Solicitação não encontrada.",
                    content = @Content(mediaType = "text/plain", schema = @Schema(type = "string", example = "Solicitação (ID 000123) não encontrada."))),
            @ApiResponse(responseCode = "500", description = "Erro interno.",
                    content = @Content(mediaType = "text/plain"))
    })
    @PatchMapping(value = "/{solicitationId}", consumes = {"multipart/form-data"})
    public ResponseEntity<?> patchSolicitation(HttpServletRequest httpRequest,
                                               @Parameter(description = "ID da solicitação") @PathVariable Long solicitationId,
                                               @Parameter(description = "Dados da solicitação a serem atualizados")
                                               @RequestPart("solicitacao") @Valid Solicitation solicitation,
                                               @Parameter(description = "Lista de arquivos a serem adicionados/substituídos")
                                               @RequestPart(value = "arquivos", required = false) List<MultipartFile> files) {

        // Recuperar dados do usuário autenticado do request http
        User user = (User) httpRequest.getAttribute("userPrincipal");

        try {
            // ID e solicitação precisam corresponder
            if (!Objects.equals(solicitationId, solicitation.getId()))
                throw new BadRequestException("ID enviado não corresponde à solicitação.");

            // Verificar se solicitação sendo alterada pertence ao usuário tentando editá-la
            // Se o usuario for admin, ele pode editar mesmo solicitações que não são dele
            solicitationService.canInteract(solicitationId, user, EventType.REQUEST_EDITING);

            // Caso não sejam enviados dados de arquivos digitais, inicializa-se um placeholder vazio
            if (files == null) files = new ArrayList<>();

            // Tenta salvar os arquivos passados pelo usuário
            // 'isNewRequest == false' indica que é edição de uma solicitação já existente
            solicitationService.saveFiles(solicitation, files, false);

            // Intancia cópias associadas na database antes de editar solicitação
            // copyService.instanceCopiesFromRequest(request);
            Solicitation editedSolicitation = solicitationService.patch(solicitationId, solicitation, user);

            // Retorna solicitação editada
            return ResponseEntity.ok(editedSolicitation);

        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .contentType(MediaType.TEXT_PLAIN)
                    .body("Solicitação (ID " + String.format("%06d", solicitationId) + ") não encontrada.");
        } catch (BadRequestException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .contentType(MediaType.TEXT_PLAIN)
                    .body(e.getMessage());
        } catch (ForbiddenException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .contentType(MediaType.TEXT_PLAIN)
                    .body("Usuário não está autorizado a acessar este recurso.");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .contentType(MediaType.TEXT_PLAIN)
                    .body(e.getMessage());
        }
    }

    /**
     * Altera o status de conclusão de uma solicitação para concluída ou não concluída.
     *
     * @param solicitationId ID da solicitação a ter o status atualizado.
     * @return Mensagem de sucesso ou erro.
     */
    @Operation(summary = "Altera o status de conclusão de uma solicitação")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Status da solicitação atualizado com sucesso.",
                    content = @Content(mediaType = "text/plain", schema = @Schema(type = "string", example = "Status da solicitação atualizado com sucesso."))),
            @ApiResponse(responseCode = "403", description = "Usuário não está autorizado a acessar este recurso.",
                    content = @Content(mediaType = "text/plain", schema = @Schema(type = "string", example = "Não é possível alterar solicitações arquivadas."))),
            @ApiResponse(responseCode = "404", description = "Solicitação não encontrada.",
                    content = @Content(mediaType = "text/plain", schema = @Schema(type = "string", example = "Solicitação (ID 000123) não encontrada."))),
            @ApiResponse(responseCode = "500", description = "Erro interno.",
                    content = @Content(mediaType = "text/plain"))
    })
    @PatchMapping("/{solicitationId}/status")
    public ResponseEntity<?> toggleRequestStatus(HttpServletRequest httpRequest,
                                                 @Parameter(description = "ID da solicitação") @PathVariable Long solicitationId,
                                                 @Parameter(description = "Indica se usuários interessados devem ser notificados após deleção bem sucedida.")
                                                 @RequestParam(value = "sendNotification", required = false) Boolean sendNotification) {

        // Recuperar dados do usuário autenticado do request http
        User user = (User) httpRequest.getAttribute("userPrincipal");

        try {
            // Verificar se solicitação sendo alterada pertence ao usuário tentando editá-la
            // Se o usuario for admin, ele pode editar mesmo solicitações que não são dele
            Solicitation solicitation = solicitationService.canInteract(solicitationId, user, EventType.REQUEST_TOGGLE);

            // Alterna status da solicitação
            solicitationService.toggleConclusionDate(solicitation, sendNotification, user);
            return ResponseEntity.ok("Status da solicitação atualizado com sucesso.");

        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .contentType(MediaType.TEXT_PLAIN)
                    .body("Solicitação (ID " + String.format("%06d", solicitationId) + ") não encontrada.");
        } catch (ForbiddenException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .contentType(MediaType.TEXT_PLAIN)
                    .body("Usuário não está autorizado a acessar este recurso.");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .contentType(MediaType.TEXT_PLAIN)
                    .body(e.getMessage());
        }
    }

    /**
     * Adiciona um novo comentário a uma solicitação.
     *
     * @param comment Objeto contendo as informações do comentário a ser adicionado.
     * @return Mensagem de sucesso ou erro.
     */
    @Operation(summary = "Adiciona um novo comentário a uma solicitação")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Comentário adicionado com sucesso.",
                    content = @Content(mediaType = "text/plain", schema = @Schema(type = "string", example = "Comentário adicionado com sucesso."))),
            @ApiResponse(responseCode = "403", description = "Não é possível adicionar comentários a uma solicitação que foi arquivada.",
                    content = @Content(mediaType = "text/plain", schema = @Schema(type = "string", example = "Usuário não está autorizado a acessar este recurso."))),
            @ApiResponse(responseCode = "404", description = "Solicitação não encontrada.",
                    content = @Content(mediaType = "text/plain", schema = @Schema(type = "string", example = "Solicitação (ID 000123) não encontrada."))),
            @ApiResponse(responseCode = "500", description = "Erro interno.",
                    content = @Content(mediaType = "text/plain"))
    })
    @PatchMapping("/{solicitationId}/comentario")
    public ResponseEntity<?> toggleRequestStatus(HttpServletRequest httpRequest,
                                                 @Parameter(description = "ID da solicitação.") @PathVariable Long solicitationId,
                                                 @Parameter(description = "Objeto de comentário da solicitação.") @RequestBody @Valid CommentDTO comment) {

        // Recuperar dados do usuário autenticado do request http
        User user = (User) httpRequest.getAttribute("userPrincipal");

        try {
            // Verificar se solicitação sendo alterada pertence ao usuário tentando editá-la
            // Se o usuario for admin, ele pode editar mesmo solicitações que não são dele
            Solicitation solicitation = solicitationService.canInteract(solicitationId, user, EventType.REQUEST_EDITING);

            // Adiciona novo comentário à solicitação
            solicitationService.addNewComment(comment, solicitation, user);
            return ResponseEntity.ok("Comentário adicionado com sucesso.");

        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .contentType(MediaType.TEXT_PLAIN)
                    .body("Solicitação (ID " + String.format("%06d", solicitationId) + ") não encontrada.");
        } catch (ForbiddenException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .contentType(MediaType.TEXT_PLAIN)
                    .body("Usuário não está autorizado a acessar este recurso.");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .contentType(MediaType.TEXT_PLAIN)
                    .body(e.getMessage());
        }
    }

    /**
     * Remove uma solicitação pelo ID.
     *
     * @param solicitationId ID da solicitação a ser removida.
     * @return Mensagem de sucesso ou erro.
     */
    @Operation(summary = "Remove uma solicitação pelo ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Solicitação removida com sucesso.",
                    content = @Content(mediaType = "text/plain", schema = @Schema(type = "string", example = "Solicitação (ID 000123) removida com sucesso."))),
            @ApiResponse(responseCode = "403", description = "Proibido.",
                    content = @Content(mediaType = "text/plain", schema = @Schema(type = "string", example = "Usuário não está autorizado a acessar este recurso."))),
            @ApiResponse(responseCode = "404", description = "Solicitação não encontrada.",
                    content = @Content(mediaType = "text/plain", schema = @Schema(type = "string", example = "Solicitação (ID 000123) não encontrada."))),
            @ApiResponse(responseCode = "500", description = "Erro interno.",
                    content = @Content(mediaType = "text/plain"))
    })
    @DeleteMapping("/{solicitationId}")
    public ResponseEntity<?> removeRequest(HttpServletRequest httpRequest,
                                           @Parameter(description = "ID da solicitação") @PathVariable Long solicitationId,
                                           @Parameter(description = "Indica se usuários interessados devem ser notificados após deleção bem sucedida.")
                                           @RequestParam(value = "sendNotification", required = false) Boolean sendNotification) {
        // Recuperar dados do usuário autenticado do request http
        User user = (User) httpRequest.getAttribute("userPrincipal");

        try {
            // Verificar se solicitação sendo alterada pertence ao usuário tentando editá-la
            // Se o usuario for admin, ele pode remover mesmo solicitações que não são dele
            solicitationService.canInteract(solicitationId, user, EventType.REQUEST_DELETING);

            // Remove solicitação
            // Adicionalmente, envia notificação de deleção à usuarios interessados
            solicitationService.removeRequest(solicitationId, sendNotification, user);

            return ResponseEntity.ok("Solicitação (ID " + String.format("%06d", solicitationId) + ") removida com sucesso.");

        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .contentType(MediaType.TEXT_PLAIN)
                    .body("Solicitação (ID " + String.format("%06d", solicitationId) + ") não encontrada.");
        } catch (ForbiddenException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .contentType(MediaType.TEXT_PLAIN)
                    .body("Usuário não está autorizado a acessar este recurso.");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .contentType(MediaType.TEXT_PLAIN)
                    .body(e.getMessage());
        }
    }
}