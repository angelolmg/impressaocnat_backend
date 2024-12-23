package com.dticnat.controleimpressao.service;


import com.dticnat.controleimpressao.model.Copia;
import com.dticnat.controleimpressao.model.Solicitacao;
import com.dticnat.controleimpressao.repository.SolicitacaoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Optional;


@Service
public class SolicitacaoService {

    @Autowired
    private SolicitacaoRepository solicitacaoRepository;

    @Autowired
    private CopiaService copiaService;

    @Value("${arquivos.base-dir}")
    private String baseDir;

    public List<Solicitacao> findAll() {
        return solicitacaoRepository.findAll();
    }

    public Optional<Solicitacao> findById(Long id) {
        return solicitacaoRepository.findById(id);
    }

    // Atualiza o status da solicitação para '1' (concluída)
    public boolean concludeStatusbyId(Long id) {
        Optional<Solicitacao> solicitacao = findById(id);

        if (solicitacao.isPresent()) {
            Solicitacao tmp = solicitacao.get();
            tmp.setStatusSolicitacao(1);
            tmp.setDataConclusao(System.currentTimeMillis());
            solicitacaoRepository.save(tmp);

            return true;
        }
        return false;
    }


    // Cria nova solicitação no banco de dados
    public Solicitacao create(Solicitacao solicitacao) {

        // 1. Gerar número da solicitação
        solicitacao.setNumeroSolicitacao(findAll().size());

        // 2. Garantir que o status seja sempre 0 (em aberto) quando a solicitação for criada
        solicitacao.setStatusSolicitacao(0);

        // 3. Configurar data de criação da solicitação em unix time
        solicitacao.setDataSolicitacao(System.currentTimeMillis());

        // 4. Data de conclusão na criação é 0
        solicitacao.setDataConclusao(0);

        // 6. Verificar número de páginas total para cópias
        // ...

        // 7. Persistir a solicitação no banco de dados
        return solicitacaoRepository.save(solicitacao);
    }

    public boolean existsById(Long id) {
        return solicitacaoRepository.existsById(id);
    }

    public String salvarArquivos(Solicitacao solicitacao, List<MultipartFile> arquivos) {
        // Valida se o número de arquivos corresponde às cópias
        if (arquivos.size() != solicitacao.getCopias().size()) {
            return "O número de arquivos enviados não corresponde ao número de cópias.";
        }

        String diretorioUsuario = baseDir + solicitacao.getMatriculaUsuario();

        try {
            // Cria o diretório do usuário, se necessário
            java.nio.file.Files.createDirectories(java.nio.file.Paths.get(diretorioUsuario));

            // Itera sobre os arquivos e salva
            for (int i = 0; i < arquivos.size(); i++) {
                MultipartFile arquivo = arquivos.get(i);
                Copia copia = solicitacao.getCopias().get(i);

                // Define o caminho do arquivo
                String caminhoArquivo = diretorioUsuario + "/" + copia.getNomeArquivo() + "." + copia.getExtensaoArquivo();

                // Salva o arquivo no disco
                arquivo.transferTo(new File(caminhoArquivo));
            }

        } catch (IOException e) {
            return "Erro IO: " + e.getMessage();
        } catch (Exception e) {
            return "Erro inesperado: " + e.getMessage();
        }

        return "";
    }
}