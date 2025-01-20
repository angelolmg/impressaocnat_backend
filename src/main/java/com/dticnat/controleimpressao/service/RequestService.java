package com.dticnat.controleimpressao.service;


import com.dticnat.controleimpressao.model.Copy;
import com.dticnat.controleimpressao.model.Request;
import com.dticnat.controleimpressao.repository.RequestRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Optional;


@Service
public class RequestService {

    @Autowired
    private RequestRepository requestRepository;

    @Autowired
    private CopyService copyService;

    @Value("${arquivos.base-dir}")
    private String baseDir;

    public List<Request> findAll() {
        return requestRepository.findAll();
    }

    public Optional<Request> findById(Long id) {
        return requestRepository.findById(id);
    }

    // Atualiza o status da solicitação para '1' (concluída)
    public boolean concludeStatusbyId(Long id) {
        Optional<Request> solicitacao = findById(id);

        if (solicitacao.isPresent()) {
            Request tmp = solicitacao.get();
//            tmp.setStatusSolicitacao(1);
            tmp.setConclusionDate(System.currentTimeMillis());
            requestRepository.save(tmp);

            return true;
        }
        return false;
    }


    // Cria nova solicitação no banco de dados
    public Request create(Request request) {

//        // 1. Gerar número da solicitação
//        solicitacao.setNumeroSolicitacao(findAll().size());

        // 2. Garantir que o status seja sempre 0 (em aberto) quando a solicitação for criada
//        solicitacao.setStatusSolicitacao(0);

        // 3. Configurar data de criação da solicitação em unix time
        request.setCreationDate(System.currentTimeMillis());

        // 4. Data de conclusão na criação é 0
        request.setConclusionDate(0);

        // 6. Verificar número de páginas total para cópias
        // ...

        // 7. Persistir a solicitação no banco de dados
        return requestRepository.save(request);
    }

    public boolean existsById(Long id) {
        return requestRepository.existsById(id);
    }

    public String saveFiles(Request request, List<MultipartFile> arquivos) {
        // Valida se o número de arquivos corresponde às cópias
        if (arquivos.size() != request.getCopies().size()) {
            return "O número de arquivos enviados não corresponde ao número de cópias.";
        }

        String userPath = baseDir + request.getRegistration();

        try {
            // Cria o diretório do usuário, se necessário
            java.nio.file.Files.createDirectories(java.nio.file.Paths.get(userPath));

            // Itera sobre os arquivos e salva
            for (int i = 0; i < arquivos.size(); i++) {
                MultipartFile file = arquivos.get(i);
                Copy copy = request.getCopies().get(i);

                // Define o caminho do arquivo
                String filePath = userPath + "/" + copy.getFileName() + "." + copy.getFileType();

                // Salva o arquivo no disco
                file.transferTo(new File(filePath));
            }

        } catch (IOException e) {
            return "Erro IO: " + e.getMessage();
        } catch (Exception e) {
            return "Erro inesperado: " + e.getMessage();
        }

        return "";
    }
}