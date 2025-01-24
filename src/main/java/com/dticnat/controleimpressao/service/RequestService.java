package com.dticnat.controleimpressao.service;


import com.dticnat.controleimpressao.model.Copy;
import com.dticnat.controleimpressao.model.Request;
import com.dticnat.controleimpressao.repository.RequestRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;


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
            tmp.setConclusionDate(System.currentTimeMillis());
            requestRepository.save(tmp);

            return true;
        }
        return false;
    }

    // Cria nova solicitação no banco de dados
    public Request create(Request request) {

        // 3. Configurar data de criação da solicitação em unix time
        request.setCreationDate(System.currentTimeMillis());

        // 4. Data de conclusão na criação é 0
        request.setConclusionDate(0);

        // TODO: 6. Verificar número de páginas total para cópias
        // ...

        // 7. Persistir a solicitação no banco de dados
        return requestRepository.save(request);
    }

    public Request patch(Long id, Request newRequest) {
        Optional<Request> request = findById(id);

        if (request.isPresent()) {
            newRequest.setId(request.get().getId());
            newRequest.setConclusionDate(request.get().getConclusionDate());
            newRequest.setCreationDate(request.get().getCreationDate());
            return requestRepository.save(newRequest);
        }
        throw new NoSuchElementException("Solicitação com ID " + id + " não encontrada");
    }


    public boolean existsById(Long id) {
        return requestRepository.existsById(id);
    }

    private List<Copy> filterNewCopies(Request comingRequest) {
        // Busca a solicitação base no repositório usando o ID da solicitação que está chegando
        return requestRepository.findById(comingRequest.getId())
                .map(baseRequest -> {
                    // Obtém a lista de cópias da solicitação base (existente)
                    List<Copy> existingCopies = baseRequest.getCopies();

                    // Filtra as cópias da nova solicitação
                    return comingRequest.getCopies().stream()
                            .filter(newCopy ->
                                    // Verifica se a cópia não existe na solicitação base
                                    existingCopies.stream()
                                            .noneMatch(existingCopy ->
                                                    // Compara os nomes dos arquivos
                                                    Objects.equals(existingCopy.getFileName(), newCopy.getFileName())
                                            )
                            )
                            .collect(Collectors.toList());
                })
                // Se a solicitação base não for encontrada, retorna as cópias da nova solicitação
                .orElse(comingRequest.getCopies());
    }

    // Retorna uma lista de cópias que é diferença entre a lista de cópias da primeira com a segunda
    // Retorna: set(c) = set(a.copias()) - set(b.copias())
    private List<Copy> filterDiffCopies(Request firstRequest, Request secondRequest) {
        Set<String> secondRequestFileNames = secondRequest.getCopies().stream()
                .map(Copy::getFileName)
                .collect(Collectors.toSet());

        return firstRequest.getCopies().stream()
                .filter(fCopy -> !secondRequestFileNames.contains(fCopy.getFileName()))
                .collect(Collectors.toList());
    }


    public String saveFiles(Request request, List<MultipartFile> files, Boolean isNewRequest) {
        List<Copy> copiesToUpload = request.getCopies();

        // Se não é uma nova solicitação, é edição de uma solicitação existente
        if (!isNewRequest) {
            Request baseRequest = requestRepository.findById(request.getId()).get();

            // Filtrar copias a adicionar à solicitação, caso hajam, caso contrário retorna []
            copiesToUpload = filterDiffCopies(request, baseRequest);
        }

        // Checar se o número de arquivos anexados é igual ao número de objetos de cópia
        if (files.size() != copiesToUpload.size())
            return "O número de arquivos enviados não corresponde ao número de cópias";

        String userPath = baseDir + request.getRegistration();

        try {
            // Cria o diretório do usuário, se necessário
            java.nio.file.Files.createDirectories(java.nio.file.Paths.get(userPath));

            // Itera sobre os arquivos e salva
            for (int i = 0; i < files.size(); i++) {
                MultipartFile file = files.get(i);
                Copy copy = copiesToUpload.get(i);

                // Define o caminho do arquivo
                String filePath = userPath + "/" + copy.getFileName();

                // Salva o arquivo no disco
                file.transferTo(new File(filePath));
            }

            return "";

        } catch (IOException e) {
            return "Erro IO: " + e.getMessage();
        } catch (Exception e) {
            return "Erro inesperado: " + e.getMessage();
        }

    }

    public boolean removeRequest(Long id) {
        Optional<Request> solicitacao = findById(id);
        if (solicitacao.isEmpty()) return false;

        requestRepository.delete(solicitacao.get());
        return true;
    }

    public Request save(Request request) {
        return requestRepository.save(request);
    }
}