package com.dticnat.controleimpressao.controller;

import com.dticnat.controleimpressao.exception.FileGoneException;
import com.dticnat.controleimpressao.exception.UnauthorizedException;
import com.dticnat.controleimpressao.model.Request;
import com.dticnat.controleimpressao.model.dto.UserData;
import com.dticnat.controleimpressao.service.AuthService;
import com.dticnat.controleimpressao.service.CopyService;
import com.dticnat.controleimpressao.service.RequestService;
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
public class RequestController {

    @Autowired
    private RequestService requestService;

    @Autowired
    private CopyService copyService;

    @Autowired
    private AuthService authService;

    // 1. Listar todas as solicitações
    @GetMapping
    public ResponseEntity<?> getAllSolicitacoes(@RequestHeader(name = "Authorization", required = false) String fullToken, @RequestParam(value = "filtering", required = false) Boolean filtering, @RequestParam(value = "concluded", required = false) Boolean is_concluded, @RequestParam(value = "startDate", required = false) Long startDate, @RequestParam(value = "endDate", required = false) Long endDate, @RequestParam(value = "query", required = false) String query) {

        // Possui um token (está logado)
        if (fullToken == null)
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Token de acesso não encontrado. Por favor, realizar login.");

        // Buscar dados do usuário
        UserData userData = authService.getUserData(fullToken);
        if (userData == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Usuário não encontrado.");

        // Verificar se o mesmo é admin
        boolean isAdmin = authService.isAdmin(userData.getMatricula());

        // Se for admin retornaremos TODAS as solicitações, não passamos filtro de matrícula
        // Se não for, passamos a matrícula como filtro
        // A não ser que o admin esteja filtrando seus resultados para receber apenas as próprias solicitações
        String userRegistration = (!isAdmin || (filtering != null && filtering)) ? userData.getMatricula() : null;

        List<Request> solicitacoes = requestService.findAll(startDate, endDate, query, is_concluded, userRegistration);
        return ResponseEntity.ok(solicitacoes);
    }

    // 2. Buscar uma solicitação pelo ID
    @GetMapping("/{id}")
    public ResponseEntity<?> getSolicitacaoById(@PathVariable Long id) {
        Optional<Request> solicitacao = requestService.findById(id);

        return solicitacao.<ResponseEntity<?>>map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).body("Solicitação com ID " + id + " não encontrada."));
    }

    // Buscar e baixar arquivo em disco
    @GetMapping("/{id}/{fileName}")
    public ResponseEntity<?> downloadFile(@RequestHeader(name = "Authorization", required = false) String fullToken, @PathVariable Long id, @PathVariable String fileName) {

        if (fullToken == null)
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).contentType(MediaType.TEXT_PLAIN).body("Token de acesso não encontrado. Por favor, realizar login.".getBytes(StandardCharsets.UTF_8));

        try {
            // Chama o serviço para realizar a lógica de validação e busca do arquivo
            return requestService.getFileResponse(fullToken, id, fileName);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).contentType(MediaType.TEXT_PLAIN).body(e.getMessage().getBytes(StandardCharsets.UTF_8));
        } catch (UnauthorizedException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).contentType(MediaType.TEXT_PLAIN).body(e.getMessage().getBytes(StandardCharsets.UTF_8));
        } catch (FileNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).contentType(MediaType.TEXT_PLAIN).body(e.getMessage().getBytes(StandardCharsets.UTF_8));
        } catch (FileGoneException e) {
            return ResponseEntity.status(HttpStatus.GONE).contentType(MediaType.TEXT_PLAIN).body(e.getMessage().getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).contentType(MediaType.TEXT_PLAIN).body("Erro ao processar o arquivo.".getBytes(StandardCharsets.UTF_8));
        }
    }

    @PatchMapping(value = "/{id}", consumes = {"multipart/form-data"})
    public ResponseEntity<?> patchRequest(@RequestHeader(name = "Authorization", required = false) String fullToken,
                                          @PathVariable Long id,
                                          @RequestPart("solicitacao") @Valid Request request,
                                          @RequestPart(value = "arquivos", required = false) List<MultipartFile> files) {

        // Possui um token (está logado)
        if (fullToken == null)
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Token de acesso não encontrado. Por favor, realizar login.");

        // Buscar dados do usuário
        UserData userData = authService.getUserData(fullToken);
        if (userData == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Usuário não encontrado.");

        // Verificar se o mesmo é admin
        boolean isAdmin = authService.isAdmin(userData.getMatricula());

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

    // 3. Criar uma nova solicitação
    @PostMapping(consumes = {"multipart/form-data"})
    public ResponseEntity<?> criarSolicitacao(@RequestPart("solicitacao") @Valid Request request, @RequestPart("arquivos") List<MultipartFile> files) {

        // 3.4 Instanciar e associar cópias à solicitação
        copyService.instanceCopiesFromRequest(request);

        // 3.5 Criar nova a solicitação no banco de dados
        Request newRequest = requestService.create(request);

        // 3.2 Salvar os arquivos em disco
        String mensagemErro = requestService.saveFiles(newRequest, files, true);

        // 3.3 Lógica para quando há erro de salvamento IO
        // Se um arquivo da solicitação dá erro, os demais salvos anteriormente devem ser excluídos
        if (!mensagemErro.isBlank()) {
            requestService.removeRequest(newRequest.getId());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(mensagemErro);
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(newRequest);
    }

    // 4. Atualizar o status da solicitação para 1 (concluída)
    @PatchMapping("/{id}/status")
    public ResponseEntity<Map<String, String>> concludeStatusSolicitacao(@PathVariable Long id) {
        return (requestService.toogleConclusionDatebyId(id)) ? ResponseEntity.ok(Map.of("message", "Status da solicitação (ID " + id + ") atualizado.")) : ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", "Não foi possivel atualizar status da solicitação (ID " + id + ")."));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> removeRequest(@PathVariable Long id) {
        return (requestService.removeRequest(id)) ? ResponseEntity.ok(Map.of("message", "Solicitação (ID " + id + ") removida com sucesso.")) : ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "Solicitação (ID " + id + ") não encontrada."));
    }
}