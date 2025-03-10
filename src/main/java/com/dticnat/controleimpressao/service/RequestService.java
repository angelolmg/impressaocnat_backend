package com.dticnat.controleimpressao.service;

import com.dticnat.controleimpressao.exception.FileGoneException;
import com.dticnat.controleimpressao.exception.UnauthorizedException;
import com.dticnat.controleimpressao.model.Copy;
import com.dticnat.controleimpressao.model.Request;
import com.dticnat.controleimpressao.model.dto.UserData;
import com.dticnat.controleimpressao.repository.RequestRepository;
import jakarta.persistence.criteria.*;
import org.apache.tomcat.util.http.fileupload.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
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

    @Value("${arquivos.cleanup-rate-hours}")
    private Long CLEANUP_RATE_HOURS;

    private static final Logger logger = LoggerFactory.getLogger(RequestService.class);

    public List<Request> findAll(Long startDate, Long endDate, String query, Boolean is_concluded, String userRegistration) {
        Specification<Request> spec = filterRequests(startDate, endDate, query, is_concluded, userRegistration);
        return requestRepository.findAll(spec, Sort.by(Sort.Direction.ASC, "id"));
    }

    public Optional<Request> findById(Long id) {
        return requestRepository.findById(id);
    }

    public Specification<Request> filterRequests(Long startDate, Long endDate, String userQuery, Boolean is_concluded, String userRegistration) {
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

            if (is_concluded != null) {
                // Filtrar entre apenas concluidos ou não concluídos
                // Se 'is_concluded' == null, significa que tudo será retornado
                if (is_concluded) predicate = cb.and(predicate, cb.greaterThan(root.get("conclusionDate"), 0));
                else predicate = cb.and(predicate, cb.equal(root.get("conclusionDate"), 0));
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

                try {
                    // Testar se query é o ID da solicitação
                    long userId = Long.parseLong(userQuery);
                    queryPredicate = cb.or(queryPredicate, cb.equal(root.get("id"), userId));

                    // Testar se query é o prazo da solicitação
                    int userTerm = Integer.parseInt(userQuery) * 60 * 60;
                    queryPredicate = cb.or(queryPredicate, cb.equal(root.get("term"), userTerm));

                } catch (NumberFormatException ex) {
                    logger.error("Não foi possivel converter query para inteiro");
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

        // Não atualize o status de solicitações obsoletas/arquivadas
        if (!tmp.isStale()) {
            if (tmp.getConclusionDate() > 0) {
                tmp.setConclusionDate(0);
            } else {
                tmp.setConclusionDate(System.currentTimeMillis());
            }
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

        if (request.isEmpty()) throw new NoSuchElementException("Solicitação com ID " + id + " não encontrada");

        // Para qualquer solicitação, não deve ser possivel alterar:
        // ID, matrícula, nome do criador, data de criação e data de conclusão
        newRequest.setId(request.get().getId());
        newRequest.setRegistration(request.get().getRegistration());
        newRequest.setUsername(request.get().getUsername());
        newRequest.setCreationDate(request.get().getCreationDate());
        newRequest.setConclusionDate(request.get().getConclusionDate());

        return requestRepository.save(newRequest);
    }

    public String saveFiles(Request request, List<MultipartFile> files, Boolean isNewRequest) {
        List<Copy> copiesToUpload = request.getCopies();
        List<Copy> copiesToDelete = null;

        // Se não é uma nova solicitação, é edição de uma solicitação existente
        if (!isNewRequest) {
            Optional<Request> baseRequestOpt = requestRepository.findById(request.getId());

            if (baseRequestOpt.isPresent()) {
                Request baseRequest = baseRequestOpt.get();

                // Persistir dono da requisição caso admin que esteja editando
                request.setUsername(baseRequest.getUsername());
                request.setRegistration(baseRequest.getRegistration());

                // Filtrar copias a adicionar à solicitação, caso hajam, caso contrário retorna []
                Map<String, List<Copy>> result = filterUploadDeleteFiles(request, baseRequest);
                copiesToUpload = result.get("toUpload");
                copiesToDelete = result.get("toDelete");
            } else return "Solicitação não encontrada";
        }

        // Checar se o número de arquivos anexados é igual ao número de objetos de cópia
        // Aqui significa que não foram enviados arquivos anexos suficientes
        if (files.size() < copiesToUpload.size())
            return "O número de arquivos enviados não corresponde ao número de cópias";

        // Aqui significa que arquivo(s) anexado(s) de mesmo nome já existe(m) na solicitação, não sobreescrever
        if (files.size() > copiesToUpload.size()) return "";

        String requestPath = BASE_DIR + request.getRegistration() + '/' + request.getId();
        String msg = "";

        try {
            // Cria o diretório do usuário, se necessário
            java.nio.file.Files.createDirectories(java.nio.file.Paths.get(requestPath));

            // Itera sobre os arquivos e salva
            for (int i = 0; i < files.size(); i++) {
                MultipartFile file = files.get(i);
                Copy copy = copiesToUpload.get(i);

                // Define o caminho do arquivo
                String filePath = requestPath + "/" + copy.getFileName();

                // Salva o arquivo no disco, caso o arquivo não seja nulo
                if (file.getSize() > 0) file.transferTo(new File(filePath));
            }

        } catch (SecurityException e) {
            msg = "Erro de segurança: " + e.getMessage();
        } catch (IOException e) {
            msg = "Erro IO: " + e.getMessage();
        } catch (IllegalStateException e) {
            msg = "Erro de estado ilegal: " + e.getMessage();
        } catch (Exception e) {
            msg = "Erro inesperado: " + e.getMessage();
        } finally {
            // Se salvar um arquivo da solicitação dá erro, aborte operação e delete os salvos anteriormente 'copiesToUpload'
            if (!msg.isEmpty()) {
                System.err.println(msg);
                deleteFiles(copiesToUpload, requestPath);
            }

            // Após salvar arquivo(s), caso seja operação de edição (patch), remover arquivos obsoletos 'copiesToDelete'
            if (!isNewRequest) deleteFiles(copiesToDelete, requestPath);
        }

        return msg;
    }

    public boolean removeRequest(Long id) {
        Optional<Request> request = findById(id);
        if (request.isEmpty()) return false;

        String requestPath = BASE_DIR + request.get().getRegistration() + '/' + request.get().getId();
        removeFolder(requestPath);

        requestRepository.delete(request.get());
        return true;
    }

    public ResponseEntity<?> getFileResponse(String fullToken, Long requestID, String fileName) throws IOException {

        // 1. Validar token e obter usuário
        UserData userData = getAuthenticatedUser(fullToken);

        // 2. Buscar a solicitação no banco
        Request request = findById(requestID).orElseThrow(() -> new FileNotFoundException("Solicitação com ID: " + requestID + " não existe."));

        // 3. Verificar se o usuário tem permissão para acessar o arquivo
        String requestOwnerRegistration = validateUserAccess(userData, request);

        // 4. Buscar se o arquivo existe dentro da solicitação
        Copy copy = request.getCopies()
                .stream()
                .filter(c -> Objects.equals(c.getFileName(), fileName))
                .findFirst()
                .orElseThrow(() -> new FileNotFoundException("O arquivo " + fileName + " não existe."));

        // 5. Criar resposta com o arquivo
        return buildFileResponse(requestID, requestOwnerRegistration, copy);
    }

    // Verifica se solicitação pertence ao usuário
    public boolean belongsTo(Long id, String userRegistration) {
        Optional<Request> baseRequest = requestRepository.findById(id);
        if (baseRequest.isEmpty()) return false;

        String requestRegistration = baseRequest.get().getRegistration();
        return requestRegistration.equals(userRegistration);
    }

// ============================================================= //
//  Métodos auxiliares
// ============================================================= //

    // Tenta remover diretório especificado por 'path'
    private void removeFolder(String path) {
        try {
            FileUtils.deleteDirectory(new File(path));
            logger.info("Diretório removido: {}", path);
        } catch (Exception e) {
            logger.info("Erro ao deletar diretório: {}", String.valueOf(e));
        }
    }

    // Valida o token e obtém os dados do usuário autenticado
    private UserData getAuthenticatedUser(String fullToken) {
        UserData userData = authService.getUserData(fullToken);
        if (userData == null) {
            throw new UnauthorizedException("Usuário não encontrado.");
        }
        return userData;
    }

    // Converte e valida o ID da solicitação
    private long parseRequestID(String requestID) {
        try {
            return Long.parseLong(requestID);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("ID da solicitação inválido.");
        }
    }

    // Verifica se o usuário tem permissão para acessar a solicitação
    private String validateUserAccess(UserData userData, Request request) {
        boolean isAdmin = authService.isAdmin(userData.getMatricula());
        String requestOwner = String.valueOf(request.getRegistration());

        if (!isAdmin && !userData.getMatricula().equals(requestOwner)) {
            throw new UnauthorizedException("Usuário não autorizado.");
        }

        return requestOwner;
    }

    // Verifica se o arquivo está disponível e lança exceções apropriadas
    // Constrói a resposta HTTP com o arquivo
    private ResponseEntity<?> buildFileResponse(Long requestID, String requestOwnerRegistration, Copy copy) throws IOException {

        if (copy.getIsPhysicalFile()) {
            throw new FileNotFoundException("O arquivo é físico e não pode ser encontrado no sistema.");
        }

        if (!copy.getFileInDisk()) {
            throw new FileGoneException("O arquivo " + copy.getFileName() + " não está mais disponível.");
        }

        String fileLocation = BASE_DIR + requestOwnerRegistration + "/" + requestID + "/" + copy.getFileName();
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

    private Map<String, List<Copy>> filterUploadDeleteFiles(Request firstRequest, Request secondRequest) {
        Set<String> firstFileNames = getFileNamesFromRequest(firstRequest);
        Set<String> secondFileNames = getFileNamesFromRequest(secondRequest);

        // (A - B)
        List<Copy> toUpload = firstRequest.getCopies().stream().filter(copy -> !secondFileNames.contains(copy.getFileName())).toList();

        // (B - A)
        List<Copy> toDelete = secondRequest.getCopies().stream().filter(copy -> !firstFileNames.contains(copy.getFileName())).toList();

        return Map.of("toUpload", toUpload, "toDelete", toDelete);
    }

    private Set<String> getFileNamesFromRequest(Request request) {
        return request.getCopies().stream().map(Copy::getFileName).collect(Collectors.toSet());
    }

    // Remove arquivos associados a copias, caso existam em 'basePath'
    // Retorna número de arquivos que foram removidos com sucesso
    private int deleteFiles(List<Copy> copies, String basePath) {
        int deleted = 0;
        if (copies != null && !copies.isEmpty()) {
            for (Copy copy : copies) {
                String filePath = basePath + "/" + copy.getFileName();
                File fileToDelete = new File(filePath);

                if (fileToDelete.exists()) {
                    if (fileToDelete.delete()) {
                        // Arquivo associado deletado com sucesso
                        logger.info("Arquivo removido: {}", filePath);
                        deleted++;

                    } else {
                        logger.error("Falha ao remover arquivo: {}", filePath);
                    }
                } else {
                    logger.info("Arquivo não encontrado: {}", filePath);
                }

                // Se cópia ainda existir (solicitação não foi deletada), atualizar status da cópia
                // Neste caso, como arquivo foi removido, flag 'fileInDisk' será setada para 'false'
                copyService.updateFileStatus(copy.getId(), false);
            }
        }
        return deleted;
    }

    // Escaneia todas as solicitações por fechadas e que já passaram do periodo de obsolência
    // Solicitações obsoletas devem ter seus arquivos removidos do disco
    // Esta é uma tarefa cronometrada pela variavel de ambiente CLENUP_RATE_FR
    // Que dita a cada quantas horas esta função é executada
    public int removeStaleFiles() {
        List<Request> requests = requestRepository.findAll();
        long now = new Date().getTime();
        AtomicInteger deletedTotal = new AtomicInteger();

        requests.forEach((request) -> {
            // Se requisição é obsoleta 'stale', significa que ela já passou por esse processo, pular
            // Se conclusionDate != 0 significa que a solicitação está fechada
            // Se ela está fechada, verificar se o período de remoção de arquivos já passou
            // Para então remover os arquivos obsoletos do disco
            if (!request.isStale() && request.getConclusionDate() != 0 && now >= request.getConclusionDate() + 1000 * CLEANUP_RATE_HOURS) {

                List<Copy> copies = copyService.findAllByRequestId(request.getId(), "");
                String requestPath = BASE_DIR + request.getRegistration() + '/' + request.getId();

                // Remover/atualizar arquivos e remover pasta da requisição
                int numDeleted = deleteFiles(copies, requestPath);
                removeFolder(requestPath);

                // Atualizar status de obsolência da solicitação
                request.setStale(true);
                requestRepository.save(request);

                deletedTotal.addAndGet(numDeleted);
            }
        });

        return deletedTotal.get();
    }
}