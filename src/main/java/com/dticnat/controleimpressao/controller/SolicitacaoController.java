package com.dticnat.controleimpressao.controller;



import com.dticnat.controleimpressao.model.Copia;
import com.dticnat.controleimpressao.model.Solicitacao;
import com.dticnat.controleimpressao.service.CopiaService;
import com.dticnat.controleimpressao.service.SolicitacaoService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/solicitacoes")
public class SolicitacaoController {

    @Autowired
    private SolicitacaoService solicitacaoService;

    @Autowired
    private CopiaService copiaService;

    // 1. Listar todas as solicitações
    @GetMapping
    public ResponseEntity<List<Solicitacao>> getAllSolicitacoes() {
        List<Solicitacao> solicitacoes = solicitacaoService.findAll();
        return ResponseEntity.ok(solicitacoes);
    }

    // 2. Buscar uma solicitação pelo ID
    @GetMapping("/{id}")
    public ResponseEntity<?> getSolicitacaoById(@PathVariable Long id) {
        Optional<Solicitacao> solicitacao = solicitacaoService.findById(id);

        return solicitacao.<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("Solicitação com ID " + id + " não encontrada."));
    }

    // 3. Criar uma nova solicitação
    @PostMapping(consumes = {"multipart/form-data"})
    public ResponseEntity<?> criarSolicitacao(
            @RequestPart("solicitacao") @Valid Solicitacao solicitacao,
            @RequestPart("arquivos") List<MultipartFile> arquivos) {

        // 3.2 Salvar os arquivos em disco
        String mensagemErro = solicitacaoService.salvarArquivos(solicitacao, arquivos);

        // 3.3 Lógica para quando há erro de salvamento IO
        // Se um arquivo da solicitação dá erro, os demais salvos anteriormente devem ser excluídos
        if (!mensagemErro.isBlank()) {
            // TODO: Lógica de remoção de arquivos
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(mensagemErro);
        }

        // 3.4 Associar cópias à solicitação
        List<Copia> copias = solicitacao.getCopias();
        copias.forEach((copia)-> {
            copia.setSolicitacaoId(solicitacao.getId());
            copia.setPossuiArquivoSalvo(true);
            copiaService.create(copia);
        });

        // 3.5 Criar nova a solicitação no banco de dados
        Solicitacao novaSolicitacao = solicitacaoService.create(solicitacao);
        return ResponseEntity.status(HttpStatus.CREATED).body(novaSolicitacao);
    }

    // 4. Atualizar o status da solicitação para 1 (concluída)
    @PatchMapping("/{id}/status")
    public ResponseEntity<?> concludeStatusSolicitacao(@PathVariable Long id) {
        return (solicitacaoService.concludeStatusbyId(id)) ?
                ResponseEntity.ok("Status da solicitação com ID " + id + " atualizado para fechada.") :
                ResponseEntity.status(HttpStatus.NOT_FOUND).body("Solicitação com ID " + id + " não encontrada.");
    }
}