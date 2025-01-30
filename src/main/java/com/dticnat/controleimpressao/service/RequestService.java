package com.dticnat.controleimpressao.service;

import com.dticnat.controleimpressao.model.Copy;
import com.dticnat.controleimpressao.model.Request;
import com.dticnat.controleimpressao.repository.RequestRepository;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.jpa.domain.Specification;
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

    public List<Request> findAll(Long startDate, Long endDate, String query, String userRegistration) {
        Specification<Request> spec = filterRequests(startDate, endDate, query, userRegistration);
        return requestRepository.findAll(spec);
    }

    public Optional<Request> findById(Long id) {
        return requestRepository.findById(id);
    }

    public Specification<Request> filterRequests(Long startDate, Long endDate, String userQuery, String userRegistration) {
        return (Root<Request> root, CriteriaQuery<?> query, CriteriaBuilder cb) -> {
            Predicate predicate = cb.conjunction();

            if (startDate != null && endDate != null) {
                predicate = cb.and(predicate, cb.greaterThanOrEqualTo(root.get("creationDate"), startDate));
                predicate = cb.and(predicate, cb.lessThanOrEqualTo(root.get("creationDate"), endDate));
            }

            if (userQuery != null && !userQuery.isEmpty()) {
            // Testar se query é o prazo da solicitação
                try {
                    int userTerm = Integer.parseInt(userQuery) * 60 * 60;
                    predicate = cb.and(predicate, cb.equal(root.get("term"), userTerm));
                } catch (NumberFormatException ex) {
                    // Error handling
                }
            }

            if(userRegistration != null) {
                predicate = cb.and(predicate, cb.equal(root.get("registration"), userRegistration));
            }

            return predicate;
        };
    }

    // Atualiza o status da solicitação para concluída
    public boolean toogleConclusionDatebyId(Long id) {
        Optional<Request> solicitacao = findById(id);

        if (solicitacao.isEmpty()) return false;

        Request tmp = solicitacao.get();
        if (tmp.getConclusionDate() > 0) {
            tmp.setConclusionDate(0);
        } else {
            tmp.setConclusionDate(System.currentTimeMillis());
        }
        requestRepository.save(tmp);

        return true;
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

        if (request.isEmpty()) throw new NoSuchElementException("Solicitação com ID " + id + " não encontrada");

        newRequest.setId(request.get().getId());
        newRequest.setConclusionDate(request.get().getConclusionDate());
        newRequest.setCreationDate(request.get().getCreationDate());

        return requestRepository.save(newRequest);
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
        // Aqui significa que não foram enviados arquivos anexos suficientes
        if (files.size() < copiesToUpload.size())
            return "O número de arquivos enviados não corresponde ao número de cópias";

        // Aqui significa que arquivo(s) anexado(s) de mesmo nome já existe(m) na solicitação, não sobreescrever
        if (files.size() > copiesToUpload.size())
            return "";

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
}