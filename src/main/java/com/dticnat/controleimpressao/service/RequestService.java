package com.dticnat.controleimpressao.service;

import com.dticnat.controleimpressao.exception.FileGoneException;
import com.dticnat.controleimpressao.exception.ForbiddenException;
import com.dticnat.controleimpressao.exception.PhysicalFileException;
import com.dticnat.controleimpressao.exception.UnauthorizedException;
import com.dticnat.controleimpressao.model.Copy;
import com.dticnat.controleimpressao.model.Request;
import com.dticnat.controleimpressao.model.dto.UserData;
import com.dticnat.controleimpressao.repository.RequestRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.criteria.*;
import org.apache.coyote.BadRequestException;
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
import java.nio.file.NoSuchFileException;
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
    public void toggleConclusionDatebyId(Long id) throws EntityNotFoundException, ForbiddenException {

        // Verifica se solicitação existe
        Optional<Request> requestOpt = findById(id);
        if (requestOpt.isEmpty()) throw new EntityNotFoundException();

        Request request = requestOpt.get();

        // Não atualize o status de solicitações obsoletas/arquivadas
        if (request.isStale()) throw new ForbiddenException();

        // data de conclusao > 0 -> 0
        // data de conclusao <= 0 -> Data atual
        request.setConclusionDate(
                request.getConclusionDate() > 0 ?
                        0 :
                        System.currentTimeMillis()
        );

        requestRepository.save(request);
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

    public Request patch(Long id, Request newRequest) throws EntityNotFoundException {
        Optional<Request> request = findById(id);

        if (request.isEmpty()) throw new EntityNotFoundException();

        // Para qualquer solicitação, não deve ser possivel alterar:
        // ID, matrícula, nome do criador, data de criação e data de conclusão
        newRequest.setId(request.get().getId());
        newRequest.setRegistration(request.get().getRegistration());
        newRequest.setUsername(request.get().getUsername());
        newRequest.setCreationDate(request.get().getCreationDate());
        newRequest.setConclusionDate(request.get().getConclusionDate());

        return requestRepository.save(newRequest);
    }

    public void saveFiles(Request request, List<MultipartFile> files, Boolean isNewRequest) throws
            IOException,
            BadRequestException,
            EntityNotFoundException {
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
            } else throw new EntityNotFoundException();
        }

        // Checar se o número de arquivos anexados é igual ao número de objetos de cópia
        // Aqui significa que não foram enviados arquivos anexos suficientes
        if (files.size() < copiesToUpload.size())
            throw new BadRequestException("O número de arquivos enviados (" + files.size() + ") não corresponde ao número de cópias para carregar (" + copiesToUpload.size() + ").");

        // Aqui significa que arquivo(s) anexado(s) de mesmo nome já existe(m) na solicitação
        // Retorne sem sobreescrever
        if (files.size() > copiesToUpload.size()) return;

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

                // Salva o arquivo no disco, caso o arquivo não seja nulo
                if (file.getSize() > 0) file.transferTo(new File(filePath));
            }

        } catch (Exception e) {
            // Se salvar um arquivo da solicitação dá erro, aborte operação e delete os salvos anteriormente 'copiesToUpload'
            deleteFiles(copiesToUpload, requestPath);
            throw e;
        } finally {
            // Após salvar arquivo(s), caso seja operação de edição (patch), remover arquivos obsoletos 'copiesToDelete'
            if (!isNewRequest) deleteFiles(copiesToDelete, requestPath);
        }
    }

    public void removeRequest(Long id) throws EntityNotFoundException {
        Optional<Request> requestOpt = findById(id);
        if (requestOpt.isEmpty()) throw new EntityNotFoundException();

        Request request = requestOpt.get();
        String requestPath = BASE_DIR + request.getRegistration() + '/' + request.getId();

        removeFolder(requestPath);
        requestRepository.delete(request);
    }

    public ResponseEntity<?> getFileResponse(UserData userData, Long requestID, String fileName) throws
            PhysicalFileException,
            FileGoneException,
            IOException,
            EntityNotFoundException,
            FileNotFoundException,
            UnauthorizedException,
            NoSuchFileException {

        // Buscar a solicitação no banco
        Request request = findById(requestID).orElseThrow(EntityNotFoundException::new);

        // Verificar se o usuário tem permissão para acessar o arquivo
        // Lança UnauthorizedException caso usuário não tenha permissão necessária
        String requestOwnerRegistration = authService.validateUserAccessAndGetOwnerRegistration(userData, request);

        // Buscar se o arquivo existe dentro da solicitação
        Copy copy = request.getCopies()
                .stream()
                .filter(c -> Objects.equals(c.getFileName(), fileName))
                .findFirst()
                .orElseThrow(FileNotFoundException::new);

        // 5. Criar resposta com o arquivo
        return buildFileResponse(requestID, requestOwnerRegistration, copy);
    }

    /**
     * Verifica se usuário pode interagir com a solicitação
     * Retorna a solicitação caso ela exista e pertença ao usuário (ou se o usuário for admin)
     * Do contrário retorna erro
     *
     * @param requestId     ID da solicitação.
     * @param userData      Dados do usuário.
     * @return A solicitação em questão.
     * @throws EntityNotFoundException  Caso a solicitação com 'requestId' não seja encontrada
     * @throws UnauthorizedException    Caso o usuário não tenha permissão de iteragir com a solicitação
     * @throws ForbiddenException       Caso o usuário tente modificar uma solicitação arquivada
    **/
    public Request canInteract(Long requestId, UserData userData, boolean patchOrDelete) throws
            EntityNotFoundException,
            UnauthorizedException,
            ForbiddenException {
        Optional<Request> requestOpt = requestRepository.findById(requestId);
        if (requestOpt.isEmpty()) throw new EntityNotFoundException();

        Request request = requestOpt.get();

        // Proíba iterações do tipo modificação (PATCH/DELETE) se solicitação estiver arquivada
        if (patchOrDelete && request.isStale()) throw new ForbiddenException();

        // Proíba iterações se a solicitação não pertencer ao usuário e o mesmo não for admin
        if (!userData.isAdmin() && !request.getRegistration().equals(userData.getMatricula())) throw new UnauthorizedException();

        return request;
    }

// ============================================================= //
//  Métodos auxiliares
// ============================================================= //

    // Tenta remover diretório especificado por 'path'
    private void removeFolder(String path) {
        try {
            FileUtils.deleteDirectory(new File(path));
            logger.info("Diretório removido: {}", path);
        } catch (IOException e) {
            logger.info("Erro ao deletar diretório: {}", String.valueOf(e));
        }
    }

    // Verifica se o arquivo está disponível e lança exceções apropriadas
    // Constrói a resposta HTTP com o arquivo
    private ResponseEntity<?> buildFileResponse(Long requestID, String requestOwnerRegistration, Copy copy) throws
            PhysicalFileException,
            FileGoneException,
            NoSuchFileException,
            IOException {

        if (copy.getIsPhysicalFile()) throw new PhysicalFileException();
        if (!copy.getFileInDisk()) throw new FileGoneException();

        File downloadFile = getFile(requestID, requestOwnerRegistration, copy);

        ByteArrayInputStream in = new ByteArrayInputStream(Files.readAllBytes(downloadFile.toPath()));
        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Disposition", "attachment; filename=" + copy.getFileName());
        headers.add("Content-Type", copy.getFileType());

        return ResponseEntity.ok().headers(headers).body(new InputStreamResource(in));
    }

    private File getFile(Long requestID, String requestOwnerRegistration, Copy copy) throws NoSuchFileException {
        String fileLocation = BASE_DIR + requestOwnerRegistration + "/" + requestID + "/" + copy.getFileName();
        File downloadFile = new File(fileLocation);

        if (!downloadFile.exists()) throw new NoSuchFileException(null);
        return downloadFile;
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