package com.dticnat.controleimpressao.controller;

import com.dticnat.controleimpressao.model.Request;
import com.dticnat.controleimpressao.service.CopyService;
import com.dticnat.controleimpressao.service.RequestService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;

@RestController
@RequestMapping("/solicitacoes")
public class RequestController {

    @Autowired
    private RequestService requestService;

    @Autowired
    private CopyService copyService;

    // 1. Listar todas as solicitações
    @GetMapping
    public ResponseEntity<List<Request>> getAllSolicitacoes() {
        List<Request> solicitacoes = requestService.findAll();
        return ResponseEntity.ok(solicitacoes);
    }

    // 2. Buscar uma solicitação pelo ID
    @GetMapping("/{id}")
    public ResponseEntity<?> getSolicitacaoById(@PathVariable Long id) {
        Optional<Request> solicitacao = requestService.findById(id);

        return solicitacao.<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("Solicitação com ID " + id + " não encontrada."));
    }

    @PatchMapping(value = "/{id}", consumes = {"multipart/form-data"})
    public ResponseEntity<?> patchRequest(@PathVariable Long id,
                                          @RequestPart("solicitacao") @Valid Request request,
                                          @RequestPart(value = "arquivos", required = false) List<MultipartFile> files) {

        if (files == null) files = new ArrayList<>();

        String mensagemErro = requestService.saveFiles(request, files, false);

        if (!mensagemErro.isBlank()) {
            // TODO: Lógica de remoção de arquivos
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(mensagemErro);
        }

        Request editedRequest;

        try {
            editedRequest = requestService.patch(id, request);
        } catch (NoSuchElementException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ex.getMessage());
        }

        return ResponseEntity.ok(editedRequest);
    }

    // 3. Criar uma nova solicitação
    @PostMapping(consumes = {"multipart/form-data"})
    public ResponseEntity<?> criarSolicitacao(
            @RequestPart("solicitacao") @Valid Request request,
            @RequestPart("arquivos") List<MultipartFile> files) {

        // 3.2 Salvar os arquivos em disco
        String mensagemErro = requestService.saveFiles(request, files, true);

        // 3.3 Lógica para quando há erro de salvamento IO
        // Se um arquivo da solicitação dá erro, os demais salvos anteriormente devem ser excluídos
        if (!mensagemErro.isBlank()) {
            // TODO: Lógica de remoção de arquivos
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(mensagemErro);
        }

        // 3.4 Instanciar e associar cópias à solicitação
        copyService.instanceCopies(request);

        // 3.5 Criar nova a solicitação no banco de dados
        Request newRequest = requestService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(newRequest);
    }

    // 4. Atualizar o status da solicitação para 1 (concluída)
    @PatchMapping("/{id}/status")
    public ResponseEntity<Map<String, String>> concludeStatusSolicitacao(@PathVariable Long id) {
        return (requestService.toogleConclusionDatebyId(id)) ?
                ResponseEntity.ok(Map.of("message", "Status da solicitação (ID " + id + ") atualizado.")) :
                ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "Solicitação (ID " + id + ") não encontrada."));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> removeRequest(@PathVariable Long id) {
        return (requestService.removeRequest(id)) ?
                ResponseEntity.ok(Map.of("message", "Solicitação (ID " + id + ") removida com sucesso.")) :
                ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "Solicitação (ID " + id + ") não encontrada."));
    }
}