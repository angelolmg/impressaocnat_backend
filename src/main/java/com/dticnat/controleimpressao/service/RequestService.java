package com.dticnat.controleimpressao.service;

import com.dticnat.controleimpressao.exception.FileGoneException;
import com.dticnat.controleimpressao.exception.UnauthorizedException;
import com.dticnat.controleimpressao.model.Copy;
import com.dticnat.controleimpressao.model.Request;
import com.dticnat.controleimpressao.model.dto.UserData;
import com.dticnat.controleimpressao.repository.RequestRepository;
import jakarta.persistence.criteria.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import java.util.stream.Collectors;


@Service
public class RequestService {

    @Autowired
    private RequestRepository requestRepository;

    @Autowired
    private CopyService copyService;

    @Autowired
    private AuthService authService;

    @Value("${arquivos.base-dir}")
    private String BASE_DIR;

    public List<Request> findAll(Long startDate, Long endDate, String query, String userRegistration) {
        Specification<Request> spec = filterRequests(startDate, endDate, query, userRegistration);
        return requestRepository.findAll(spec, Sort.by(Sort.Direction.ASC, "id"));
    }

    public Optional<Request> findById(Long id) {
        return requestRepository.findById(id);
    }

    public Specification<Request> filterRequests(Long startDate, Long endDate, String userQuery, String userRegistration) {
        return (Root<Request> root, CriteriaQuery<?> query, CriteriaBuilder cb) -> {
            Predicate predicate = cb.conjunction();

            // TODO: converter para lógica utilizando LocalDateTime
            // Checando query de data
            // Converter as datas salvas para o início do dia, então comparar
            if (startDate != null) {
                predicate = cb.and(predicate, cb.greaterThanOrEqualTo(root.get("creationDate"), startDate));
            }

            if (endDate != null) {
                // 'endDate' deve ser o timestamp do inicio do último dia
                // Adicionamos 86400000 (unixtime em ms para 1 dia) para incluir solicitações deste dia também
                predicate = cb.and(predicate, cb.lessThanOrEqualTo(root.get("creationDate"), endDate + 86400000));
            }

            // Filtragem de solicitação por matrícula
            if (userRegistration != null) {
                predicate = cb.and(predicate, cb.equal(root.get("registration"), userRegistration));
            }

            Predicate queryPredicate = cb.conjunction();

            // Checando query de texto
            if (userQuery != null && !userQuery.isEmpty()) {
                // Testar se query é nome do solicitante
                queryPredicate = cb.and(queryPredicate, cb.like(cb.lower(cb.trim(root.get("username"))), "%" + userQuery.trim().toLowerCase() + "%"));

                // Testar se query é matrícula
                queryPredicate = cb.or(queryPredicate, cb.like(cb.trim(root.get("registration")), "%" + userQuery.trim() + "%"));

                // Testar se query é o prazo da solicitação
                try {
                    int userTerm = Integer.parseInt(userQuery) * 60 * 60;
                    queryPredicate = cb.or(queryPredicate, cb.equal(root.get("term"), userTerm));

                } catch (NumberFormatException ex) {
                    // Error handling
                    System.err.print("[RequestService] Não foi possivel converter query para inteiro.");
                }
            }

            return cb.and(predicate, queryPredicate);
        };
    }

    public boolean belongsToUserCheck(Long requestId, UserData userData) {
        Optional<Request> request = findById(requestId);
        if (request.isEmpty()) return false;

        Request tmp = request.get();
        return tmp.getRegistration().equals(userData.getMatricula());
    }

    // Atualiza o status da solicitação para concluída
    public boolean toogleConclusionDatebyId(Long id) {
        Optional<Request> request = findById(id);

        if (request.isEmpty()) return false;

        Request tmp = request.get();
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
        if (files.size() > copiesToUpload.size()) return "";

        String requestPath = BASE_DIR + request.getRegistration() + '/' + request.getId();

        try {
            // Cria o diretório do usuário, se necessário
            java.nio.file.Files.createDirectories(java.nio.file.Paths.get(requestPath));

            // Itera sobre os arquivos e salva
            for (int i = 0; i < files.size(); i++) {
                MultipartFile file = files.get(i);
                Copy copy = copiesToUpload.get(i);

                // Define o caminho do arquivo
                String filePath = requestPath + "/" + copy.getFileName();

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

    public ResponseEntity<?> getFileResponse(String fullToken, Long requestID, String fileName) throws IOException {

        // 1. Validar token e obter usuário
        UserData userData = getAuthenticatedUser(fullToken);

        // 2. Buscar a solicitação no banco
        Request request = findById(requestID).orElseThrow(() -> new FileNotFoundException("Solicitação com ID: " + requestID + " não existe."));

        // 3. Verificar se o usuário tem permissão para acessar o arquivo
        validateUserAccess(userData, request);

        // 4. Buscar se o arquivo existe dentro da solicitação
        Copy copy = request.getCopies()
                            .stream()
                            .filter(c -> Objects.equals(c.getFileName(), fileName))
                                                .findFirst()
                                                .orElseThrow(() -> new FileNotFoundException("O arquivo " + fileName + " não existe."));

        // 5. Criar resposta com o arquivo
        return buildFileResponse(userData, requestID, copy);
    }

// ============================================================= //
// 🔹 Métodos auxiliares
// ============================================================= //

    // ✅ Valida o token e obtém os dados do usuário autenticado
    private UserData getAuthenticatedUser(String fullToken) {
        UserData userData = authService.getUserData(fullToken);
        if (userData == null) {
            throw new UnauthorizedException("Usuário não encontrado.");
        }
        return userData;
    }

    // ✅ Converte e valida o ID da solicitação
    private long parseRequestID(String requestID) {
        try {
            return Long.parseLong(requestID);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("ID da solicitação inválido.");
        }
    }

    // ✅ Verifica se o usuário tem permissão para acessar a solicitação
    private void validateUserAccess(UserData userData, Request request) {
        boolean isAdmin = authService.isAdmin(userData.getMatricula());
        if (!isAdmin) {
            String requestOwner = String.valueOf(request.getRegistration());
            if (!userData.getMatricula().equals(requestOwner)) {
                throw new UnauthorizedException("Usuário não autorizado.");
            }
        }
    }

    // ✅ Verifica se o arquivo está disponível e lança exceções apropriadas
    // ✅ Constrói a resposta HTTP com o arquivo
    private ResponseEntity<?> buildFileResponse(UserData userData, Long requestID, Copy copy) throws IOException {

        if (!copy.getFileInDisk()) {
            throw new FileGoneException("O arquivo " + copy.getFileName() + " não está mais disponível.");
        }

        String fileLocation = BASE_DIR + "/" + userData.getMatricula() + "/" + requestID + "/" + copy.getFileName();
        File downloadFile = new File(fileLocation);

        if (!downloadFile.exists()) {
            throw new FileNotFoundException("O arquivo " + copy.getFileName() + " não foi encontrado no sistema.");
        }

        ByteArrayInputStream in = new ByteArrayInputStream(Files.readAllBytes(downloadFile.toPath()));
        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Disposition", "attachment; filename=" + copy.getFileName());
        headers.add("Content-Type", copy.getFileType());

        return ResponseEntity.ok().headers(headers).body(new InputStreamResource(in));
    }

    // Retorna uma lista de cópias que é diferença entre a lista de cópias da primeira com a segunda
    // Retorna: set(c) = set(a.copias()) - set(b.copias())
    private List<Copy> filterDiffCopies(Request firstRequest, Request secondRequest) {
        Set<String> secondRequestFileNames = secondRequest.getCopies().stream().map(Copy::getFileName).collect(Collectors.toSet());

        return firstRequest.getCopies().stream().filter(fCopy -> !secondRequestFileNames.contains(fCopy.getFileName())).collect(Collectors.toList());
    }
}