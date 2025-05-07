package com.dticnat.controleimpressao.service;

import com.dticnat.controleimpressao.exception.FileGoneException;
import com.dticnat.controleimpressao.exception.ForbiddenException;
import com.dticnat.controleimpressao.exception.PhysicalFileException;
import com.dticnat.controleimpressao.exception.UnauthorizedException;
import com.dticnat.controleimpressao.model.Copy;
import com.dticnat.controleimpressao.model.Event;
import com.dticnat.controleimpressao.model.Solicitation;
import com.dticnat.controleimpressao.model.User;
import com.dticnat.controleimpressao.model.dto.CommentDTO;
import com.dticnat.controleimpressao.model.dto.SolicitationDTO;
import com.dticnat.controleimpressao.model.enums.EventType;
import com.dticnat.controleimpressao.model.enums.Role;
import com.dticnat.controleimpressao.repository.SolicitationRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.criteria.*;
import jakarta.validation.Valid;
import org.apache.coyote.BadRequestException;
import org.apache.tomcat.util.http.fileupload.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;


@Service
public class SolicitationService {

    @Autowired
    private SolicitationRepository solicitationRepository;

    @Autowired
    private CopyService copyService;

    @Autowired
    private AuthService authService;

    @Autowired
    private EventService eventService;

    @Value("${arquivos.base-dir}")
    private String BASE_DIR;

    @Value("${arquivos.cleanup-rate-hours}")
    private Long CLEANUP_RATE_HOURS;

    private static final Logger logger = LoggerFactory.getLogger(SolicitationService.class);

    /**
     * Busca todas as solicitações com opções de filtragem.
     * Este metodo permite buscar todas as solicitações da base de dados,
     * aplicando filtros opcionais por período, termo de pesquisa, status de conclusão
     * e registro do usuário.
     *
     * @param startDate        Timestamp (Unix time) da data de início para filtrar por período (opcional).
     * @param endDate          Timestamp (Unix time) da data de término para filtrar por período (opcional).
     * @param query            Termo de pesquisa para filtrar por texto em campos relevantes (opcional).
     * @param is_concluded     Booleano para filtrar solicitações concluídas (true) ou pendentes (false) (opcional).
     * @param userRegistration Registro do usuário para filtrar solicitações por usuário (opcional).
     * @return Uma lista de solicitações que correspondem aos critérios de filtragem, ordenadas por ID em ordem ascendente.
     */
    public List<Solicitation> findAll(LocalDateTime startDate, LocalDateTime endDate, String query, Boolean is_concluded, String userRegistration) {
        Specification<Solicitation> spec = filterRequests(startDate, endDate, query, is_concluded, userRegistration);
        return solicitationRepository.findAll(spec, Sort.by(Sort.Direction.ASC, "id"));
    }

    public Page<Solicitation> findPage(LocalDateTime startDate, LocalDateTime endDate,
                                       String query, Boolean is_concluded, String userRegistration,
                                       int pageNo, int pageSize,
                                       String sortingColumn, String sortingDirection) {
        Sort sort = Sort.by(Sort.Direction.ASC, "id"); // Default sort

        if (sortingColumn != null && !sortingColumn.isEmpty()) {
            Sort.Direction direction = Sort.Direction.ASC;
            if ("desc".equalsIgnoreCase(sortingDirection)) {
                direction = Sort.Direction.DESC;
            }

            if (sortingColumn.equals("registrationNumber")) {
                sort = Sort.by(direction, "user.registrationNumber");
            } else if (sortingColumn.equals("commonName")) {
                sort = Sort.by(direction, "user.commonName");
            } else {
                sort = Sort.by(direction, sortingColumn); // Sort by a direct field in Solicitation
            }
        }

        Pageable pageable = PageRequest.of(pageNo, pageSize, sort);
        Specification<Solicitation> spec = filterRequests(startDate, endDate, query, is_concluded, userRegistration);
        return solicitationRepository.findAll(spec, pageable);
    }

    /**
     * Busca uma solicitação pelo seu ID.
     * Este metodo busca e retorna uma solicitação da base de dados com o ID especificado.
     *
     * @param id O ID da solicitação a ser buscada.
     * @return Um Optional contendo a solicitação, se encontrada, ou um Optional vazio caso contrário.
     */
    public Optional<Solicitation> findById(Long id) {
        return solicitationRepository.findById(id);
    }

    /**
     * Cria uma especificação para filtrar solicitações com base em vários critérios.
     * Este metodo constrói uma Specification JPA para filtrar solicitações com base em
     * data de início, data de término, termo de pesquisa, status de conclusão e
     * registro do usuário. Os filtros são aplicados de forma combinada (AND).
     *
     * @param startDate        Timestamp (Unix time) da data de início para filtrar por período (opcional).
     * @param endDate          Timestamp (Unix time) da data de término para filtrar por período (opcional).
     * @param userQuery        Termo de pesquisa para filtrar por texto em nome do solicitante, matrícula ou ID da solicitação (opcional).
     * @param isConcluded      Booleano para filtrar solicitações concluídas (true) ou pendentes (false) (opcional).
     * @param userRegistration Registro do usuário para filtrar solicitações por usuário (opcional).
     * @return Uma Specification JPA que pode ser usada para filtrar solicitações.
     */
    public Specification<Solicitation> filterRequests(LocalDateTime startDate, LocalDateTime endDate, String userQuery, Boolean isConcluded, String userRegistration) {
        return (Root<Solicitation> root, CriteriaQuery<?> query, CriteriaBuilder cb) -> {
            Predicate predicate = cb.conjunction();

            // Filtragem por data de criação
            // Neste caso 'creationDate' pode estar entre 'startDate' e ('endDate' + 1 dia)
            // 'startDate' e 'endDate' são o timestamp do **início** dos seus respectivos dias
            if (startDate != null) {
                predicate = cb.and(predicate, cb.greaterThanOrEqualTo(root.get("creationDate"), startDate));
            }

            if (endDate != null) {
                // Adicionamos 1 dia para incluir solicitações deste dia também
                predicate = cb.and(predicate, cb.lessThanOrEqualTo(root.get("creationDate"), endDate.plusDays(1)));
            }

            // Filtragem por status de conclusão
            if (isConcluded != null) {
                // Filtrar entre apenas concluidos ou não concluídos
                // Se 'is_concluded' == null, significa que tudo será retornado
                if (isConcluded) predicate = cb.and(predicate, cb.isNotNull(root.get("conclusionDate")));
                else predicate = cb.and(predicate, cb.isNull(root.get("conclusionDate")));
            }

            // Filtragem por matrícula do solicitante
            if (userRegistration != null) {
                predicate = cb.and(predicate, cb.equal(root.get("user").get("registrationNumber"), userRegistration));
            }

            Predicate queryPredicate = cb.conjunction();

            // Filtragem por texto (nome, matrícula ou ID)
            if (userQuery != null && !userQuery.isEmpty()) {
                String trimmedQuery = userQuery.trim();
                // Testar se query é nome do solicitante
                queryPredicate = cb.like(cb.lower(cb.trim(root.get("user").get("commonName"))), "%" + trimmedQuery.toLowerCase() + "%");

                // Testar se query é matrícula
                queryPredicate = cb.or(queryPredicate, cb.like(cb.trim(root.get("user").get("registrationNumber")), "%" + trimmedQuery + "%"));

                try {
                    // Testar se query é o ID da solicitação
                    long solicitationId = Long.parseLong(userQuery);
                    queryPredicate = cb.or(queryPredicate, cb.equal(root.get("id"), solicitationId));

                    // Testar se query é o prazo da solicitação (em horas)
                    int solicitationDeadline = Integer.parseInt(userQuery);
                    queryPredicate = cb.or(queryPredicate, cb.equal(root.get("deadline"), solicitationDeadline));

                } catch (NumberFormatException ex) {
                    logger.debug("Não foi possível converter query para inteiro: {}", userQuery);
                }
            }

            return cb.and(predicate, queryPredicate);
        };
    }

    /**
     * Atualiza o status da solicitação para concluída ou pendente, alternando a data de conclusão.
     * Este metodo busca uma solicitação pelo seu ID e alterna o status de conclusão.
     * Se a solicitação já estiver marcada como concluída (possui uma data de conclusão maior que zero),
     * a data de conclusão é definida como zero, marcando-a como pendente. Caso contrário, a data de
     * conclusão é definida como a data atual, marcando-a como concluída.
     *
     * @param solicitation A solicitação a ser atualizada.
     * @throws EntityNotFoundException Se a solicitação com o ID especificado não for encontrada.
     * @throws ForbiddenException      Se a solicitação estiver arquivada (stale), impedindo a alteração do status.
     */
    public void toggleConclusionDate(Solicitation solicitation, User user) throws ForbiddenException {
        // Não atualize o status de solicitações obsoletas/arquivadas
        if (solicitation.isArchived()) throw new ForbiddenException();

        // Se estiver aberto, feche (null). Se estiver fechado (null), abra.
        LocalDateTime newConclusionDate = solicitation.getConclusionDate() == null ? LocalDateTime.now() : null;
        EventType eventType = newConclusionDate != null ? EventType.REQUEST_CLOSING : EventType.REQUEST_OPENING;

        // Adicionar evento de toggle à timeline
        solicitation.setConclusionDate(newConclusionDate);
        solicitation.getTimeline().add(Event.builder()
                .solicitationId(solicitation.getId())
                .user(user)
                .type(eventType)
                .creationDate(LocalDateTime.now())
                .build());

        solicitationRepository.save(solicitation);
    }

    /**
     * Cria uma nova solicitação na base de dados.
     * Este metodo recebe um objeto Request, configura a data de criação e a data de conclusão inicial,
     * e persiste a solicitação na base de dados.
     *
     * @param solicitationDTO O objeto Request contendo os dados da solicitação a ser criada.
     * @return O objeto Request persistido na base de dados, incluindo o ID gerado.
     */
    public Solicitation create(SolicitationDTO solicitationDTO, User user) {
        // Cria base da solicitação
        // TODO: Verificar número de páginas total para cópias
        Solicitation newSolicitation = Solicitation
                .builder()
                .deadline(solicitationDTO.getDeadline())
                .creationDate(LocalDateTime.now())
                .user(user)
                .totalPageCount(solicitationDTO.getTotalPageCount())
                .build();

        // Intancia arquivos e associa à solicitação
        List<Copy> copies = copyService.instanceCopiesFromRequest(newSolicitation, solicitationDTO.getCopies());
        newSolicitation.setCopies(copies);

        // Cria e associa evento inicial de criação à linha do tempo
        Event creationEvent = Event
                .builder()
                .user(user)
                .type(EventType.REQUEST_OPENING)
                .creationDate(LocalDateTime.now())
                .build();
        List<Event> events = List.of(creationEvent);
        newSolicitation.setTimeline(events);

        // Persistir a solicitação no banco de dados
        return solicitationRepository.save(newSolicitation);
    }

    /**
     * Atualiza parcialmente os dados de uma solicitação existente.
     * Este metodo busca uma solicitação pelo ID e, se encontrada, atualiza seus dados
     * com os valores fornecidos no objeto `newRequest`. Campos como ID, matrícula,
     * nome do criador, data de criação e data de conclusão não podem ser alterados
     * por este metodo, mantendo seus valores originais.
     *
     * @param id              O ID da solicitação a ser atualizada.
     * @param newSolicitation O objeto Request contendo os dados a serem atualizados.
     * @return O objeto Request atualizado e persistido na base de dados.
     * @throws EntityNotFoundException Se a solicitação com o ID especificado não for encontrada.
     */
    public Solicitation patch(Long id, Solicitation newSolicitation, User user) throws EntityNotFoundException {
        Solicitation solicitation = findById(id).orElseThrow(EntityNotFoundException::new);

        // Garante que campos imutáveis não sejam alterados
        newSolicitation.setId(solicitation.getId());
        newSolicitation.setArchived(solicitation.isArchived());
        newSolicitation.setUser(newSolicitation.getUser());
        newSolicitation.setCreationDate(solicitation.getCreationDate());
        newSolicitation.setConclusionDate(solicitation.getConclusionDate());

        solicitation.getTimeline().add(
                Event
                        .builder()
                        .solicitationId(newSolicitation.getId())
                        .user(user)
                        .type(EventType.REQUEST_EDITING)
                        .creationDate(LocalDateTime.now())
                        .build()
        );

        newSolicitation.setTimeline(solicitation.getTimeline());

        return solicitationRepository.save(newSolicitation);
    }

    /**
     * Salva os arquivos anexados a uma solicitação.
     * Este metodo recebe uma solicitação e uma lista de arquivos anexados, e os salva no sistema de arquivos.
     * Para solicitações novas, todos os arquivos anexados são salvos. Para edições de solicitações existentes,
     * compara os arquivos existentes com os novos e salva apenas os arquivos novos ou modificados, além de
     * remover os arquivos obsoletos.
     *
     * @param solicitation A solicitação à qual os arquivos estão anexados.
     * @param files        A lista de arquivos anexados (MultipartFile).
     * @param isNewRequest Flag indicando se a solicitação é nova (true) ou uma edição (false).
     * @throws IOException             Se ocorrer um erro ao salvar os arquivos no sistema de arquivos.
     * @throws BadRequestException     Se o número de arquivos enviados não corresponder ao número de cópias a serem carregadas.
     * @throws EntityNotFoundException Se a solicitação existente não for encontrada durante uma edição.
     */
    public void saveFiles(Solicitation solicitation, List<MultipartFile> files, Boolean isNewRequest) throws
            IOException,
            BadRequestException,
            EntityNotFoundException {
        List<Copy> copiesToUpload = solicitation.getCopies();
        List<Copy> copiesToDelete = null;

        // Se não é uma nova solicitação, é edição de uma solicitação existente
        if (!isNewRequest) {
            Solicitation baseSolicitation = solicitationRepository.findById(solicitation.getId())
                    .orElseThrow(EntityNotFoundException::new);

            // Persistir dono da requisição caso admin que esteja editando
            solicitation.setUser(baseSolicitation.getUser());

            // Filtrar copias a adicionar à solicitação, caso hajam, caso contrário retorna []
            Map<String, List<Copy>> result = filterUploadDeleteFiles(solicitation, baseSolicitation);
            copiesToUpload = result.get("toUpload");
            copiesToDelete = result.get("toDelete");
        }

        // Checar se o número de arquivos anexados é igual ao número de objetos de cópia
        // Aqui significa que não foram enviados arquivos anexos suficientes
        if (files.size() < copiesToUpload.size())
            throw new BadRequestException("O número de arquivos enviados (" + files.size() + ") não corresponde ao número de cópias para carregar (" + copiesToUpload.size() + ").");

        // Aqui significa que arquivo(s) anexado(s) de mesmo nome já existe(m) na solicitação
        // Retorne sem sobreescrever
        if (files.size() > copiesToUpload.size()) return;

        String requestPath = BASE_DIR + solicitation.getUser().getRegistrationNumber() + '/' + solicitation.getId();

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
                boolean fileExists = file.getSize() > 0;
                if (fileExists) file.transferTo(new File(filePath));

                // Atualize o status de exsitência do arquivo
                copy.setFileInDisk(fileExists);
                copy.setIsPhysicalFile(!copy.getFileInDisk());
                copyService.save(copy);
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

    /**
     * Remove uma solicitação da base de dados e exclui a pasta de arquivos associada.
     * Este metodo busca uma solicitação pelo ID, e se encontrada, remove a solicitação
     * da base de dados e exclui a pasta contendo os arquivos associados à solicitação
     * no sistema de arquivos.
     *
     * @param id O ID da solicitação a ser removida.
     * @throws EntityNotFoundException Se a solicitação com o ID especificado não for encontrada.
     */
    public void removeRequest(Long id, boolean sendNotification, User triggeringUser) throws EntityNotFoundException {
        // Busca a solicitação pelo ID
        Solicitation solicitation = solicitationRepository.findById(id).orElseThrow(EntityNotFoundException::new);

        // Define o caminho da pasta de arquivos associada à solicitação e a remove
        String solicitationPath = BASE_DIR + solicitation.getUser().getRegistrationNumber() + '/' + solicitation.getId();
        removeFolder(solicitationPath);

        // Remove a solicitação do banco de dados
        solicitationRepository.delete(solicitation);

        // Envia a notificação assincronamente após a exclusão, se a flag estiver true
        if (sendNotification) {
            eventService.sendNotificationForLooseEvent(solicitation, triggeringUser, EventType.REQUEST_DELETING);
        }
    }

    /**
     * Busca os dados do arquivo associado a uma solicitação e constrói a resposta para download.
     * Este metodo busca uma solicitação pelo ID, verifica se o usuário tem permissão para acessar o arquivo,
     * localiza a cópia do arquivo dentro da solicitação e, em seguida, utiliza um serviço para construir
     * a resposta HTTP contendo o arquivo para download.
     *
     * @param user           Dados do usuário autenticado.
     * @param solicitationId ID da solicitação.
     * @param fileName       Nome do arquivo a ser baixado.
     * @return ResponseEntity contendo o arquivo para download.
     * @throws EntityNotFoundException Se a solicitação com o ID especificado não for encontrada.
     * @throws UnauthorizedException   Se o usuário não tiver permissão para acessar o arquivo da solicitação.
     * @throws FileNotFoundException   Se o arquivo especificado não for encontrado dentro da solicitação.
     * @throws PhysicalFileException   Se o arquivo for físico e não puder ser encontrado no sistema.
     * @throws FileGoneException       Se o arquivo não estiver mais disponível.
     * @throws NoSuchFileException     Se o arquivo não for encontrado no sistema de arquivos.
     * @throws IOException             Se ocorrer um erro ao ler o arquivo do sistema de arquivos.
     */
    public ResponseEntity<?> getFileResponse(User user, Long solicitationId, String fileName) throws
            PhysicalFileException,
            FileGoneException,
            IOException,
            EntityNotFoundException,
            FileNotFoundException,
            UnauthorizedException,
            NoSuchFileException {

        // Buscar a solicitação no banco
        Solicitation solicitation = findById(solicitationId).orElseThrow(EntityNotFoundException::new);

        // Verificar se o usuário tem permissão para acessar o arquivo
        // Lança UnauthorizedException caso usuário não tenha permissão necessária
        String solicitationOwnerRegistration = solicitation.getUser().getRegistrationNumber();
        if (!user.isAdminOrManager() && !user.getRegistrationNumber().equals(solicitationOwnerRegistration))
            throw new UnauthorizedException();

        // Buscar se o arquivo existe dentro da solicitação
        Copy copy = solicitation.getCopies()
                .stream()
                .filter(c -> Objects.equals(c.getFileName(), fileName))
                .findFirst()
                .orElseThrow(FileNotFoundException::new);

        // Criar resposta com o arquivo
        return buildFileResponse(solicitationId, solicitationOwnerRegistration, copy);
    }

    /**
     * Verifica se o usuário tem permissão para interagir com a solicitação.
     * <p>
     * Este metodo busca uma solicitação pelo ID e verifica se o usuário autenticado
     * tem permissão para interagir com ela. A permissão é concedida se a solicitação
     * pertencer ao usuário ou se o usuário for um administrador. Para operações de
     * modificação (PATCH/DELETE), a interação é proibida se a solicitação estiver arquivada.
     *
     * @param solicitationId ID da solicitação a ser verificada.
     * @param user           Usuário autenticado.
     * @param eventType  Flag indicando tipo de evento (EDIÇÃO)
     * @return A solicitação em questão, se o usuário tiver permissão.
     * @throws EntityNotFoundException Caso a solicitação com o ID especificado não for encontrada.
     * @throws UnauthorizedException   Caso o usuário não tenha permissão para interagir com a solicitação.
     * @throws ForbiddenException      Caso o usuário tente modificar uma solicitação arquivada.
     */
    public Solicitation canInteract(Long solicitationId, User user, EventType eventType) throws
            EntityNotFoundException,
            UnauthorizedException,
            ForbiddenException {
        // Busca a solicitação
        Solicitation solicitation = solicitationRepository.findById(solicitationId).orElseThrow(EntityNotFoundException::new);

        // Proíbe iterações do tipo modificação (PATCH/DELETE) se a solicitação estiver arquivada
        if ((eventType == EventType.REQUEST_EDITING || eventType == EventType.REQUEST_DELETING) && solicitation.isArchived())
            throw new ForbiddenException();

        // Proíba iterações se a solicitação não pertencer ao usuário e o mesmo não for admin
        if (!solicitation.getUser().getRegistrationNumber().equals(user.getRegistrationNumber()) &&
                !user.isAdminOrManager())
            throw new UnauthorizedException();

        return solicitation;
    }

    /**
     * Escaneia todas as solicitações fechadas que já passaram do período de obsolescência.
     * Este é um metodo agendado em ScheduledTasks, cronometrada pela variavel de ambiente CLEANUP_RATE_FR.
     * Ele itera por todas as solicitações, verifica se estão fechadas e se o tempo decorrido desde a
     * conclusão ultrapassou o período de obsolescência. Para as solicitações obsoletas identificadas,
     * os arquivos associados são removidos do disco, a pasta da solicitação é excluída e o
     * status da solicitação é atualizado para 'stale = true' (obsoleta/arquivada).
     *
     * @return O número total de arquivos removidos durante a execução da tarefa.
     */
    public int removeStaleFiles() {
        List<Solicitation> solicitations = solicitationRepository.findAll();
        LocalDateTime now = LocalDateTime.now();
        AtomicInteger deletedTotal = new AtomicInteger();

        solicitations.forEach((solicitation) -> {
            // Se a solicitação já é obsoleta, ignora.
            // Se a solicitação está fechada (conclusionDate != MIN) e o período de remoção já passou...
            if (!solicitation.isArchived() &&
                    solicitation.getConclusionDate() != null &&
                    solicitation.getConclusionDate().plusHours(CLEANUP_RATE_HOURS).isBefore(now)) {

                // Busca todas as cópias associadas à solicitação
                List<Copy> copies = copyService.findAllBySolicitationId(solicitation.getId(), "");
                // Define o caminho da pasta da solicitação
                String requestPath = BASE_DIR + solicitation.getUser().getRegistrationNumber() + '/' + solicitation.getId();

                // Remover/atualizar arquivos e remover pasta da requisição
                int numDeleted = deleteFiles(copies, requestPath);
                removeFolder(requestPath);

                // Atualizar status de obsolência da solicitação
                solicitation.setArchived(true);

                // Adicionar evento de arquivamento à timeline
                solicitation.getTimeline().add(Event.builder()
                        .solicitationId(solicitation.getId())
                        .user(User.builder()
                                .role(Role.SYSTEM)
                                .build())
                        .type(EventType.REQUEST_ARCHIVING)
                        .creationDate(LocalDateTime.now())
                        .build());

                solicitationRepository.save(solicitation);

                deletedTotal.addAndGet(numDeleted);
            }
        });

        return deletedTotal.get();
    }

    public void addNewComment(@Valid CommentDTO comment, Solicitation solicitation, User user) {
        // Adicionar evento de comentario à timeline
        solicitation.getTimeline().add(Event.builder()
                .solicitationId(solicitation.getId())
                .user(user)
                .type(EventType.COMMENT)
                .content(comment.getMessage())
                .creationDate(LocalDateTime.now())
                .build());

        solicitationRepository.save(solicitation);
    }

// ============================================================= //
//  Métodos auxiliares
// ============================================================= //

    /**
     * Tenta remover o diretório especificado pelo caminho.
     * Este metodo utiliza a biblioteca Apache Commons IO para remover o diretório e
     * todo o seu conteúdo. Se ocorrer um erro durante a remoção, o erro é logado
     * como informação, mas a exceção não é propagada.
     *
     * @param path O caminho do diretório a ser removido.
     */
    private void removeFolder(String path) {
        try {
            FileUtils.deleteDirectory(new File(path));
            logger.info("Diretório removido: {}", path);
        } catch (IOException e) {
            logger.info("Erro ao deletar diretório: {}", String.valueOf(e));
        }
    }

    /**
     * Verifica a disponibilidade do arquivo e constrói a resposta HTTP para download.
     * Este método verifica se o arquivo associado à cópia está disponível para download.
     * Se o arquivo for físico ou não estiver no disco, lança a exceção apropriada.
     * Caso contrário, lê o arquivo do sistema de arquivos e constrói um ResponseEntity
     * com o arquivo para download.
     *
     * @param solicitationId           O ID da solicitação associada ao arquivo.
     * @param requestOwnerRegistration O registro do proprietário da solicitação.
     * @param copy                     O objeto Copy que representa o arquivo.
     * @return ResponseEntity contendo o arquivo para download.
     * @throws PhysicalFileException Se o arquivo for apenas uma referência física e não estiver disponível para download direto.
     * @throws FileGoneException     Se o arquivo foi removido do disco.
     * @throws NoSuchFileException   Se o arquivo não for encontrado no sistema de arquivos.
     * @throws IOException           Se ocorrer um erro ao ler o arquivo do sistema de arquivos.
     */
    private ResponseEntity<?> buildFileResponse(Long solicitationId, String requestOwnerRegistration, Copy copy) throws
            PhysicalFileException,
            FileGoneException,
            NoSuchFileException,
            IOException {

        // Verifica se o arquivo é apenas uma referência física (não armazenado para download direto)
        if (copy.getIsPhysicalFile()) throw new PhysicalFileException();
        // Verifica se o arquivo foi removido do disco
        if (!copy.getFileInDisk()) throw new FileGoneException();

        // Obtém o arquivo do sistema de arquivos
        File downloadFile = getFile(solicitationId, requestOwnerRegistration, copy);

        // Lê o conteúdo do arquivo para um ByteArrayInputStream
        // Define os headers da resposta HTTP
        ByteArrayInputStream in = new ByteArrayInputStream(Files.readAllBytes(downloadFile.toPath()));
        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Disposition", "attachment; filename=" + copy.getFileName());
        headers.add("Content-Type", copy.getFileType());

        return ResponseEntity.ok().headers(headers).body(new InputStreamResource(in));
    }

    /**
     * Obtém o arquivo do sistema de arquivos com base nos dados da solicitação e da cópia.
     * Este metodo constrói o caminho completo para o arquivo com base no ID da solicitação,
     * registro do proprietário e nome do arquivo da cópia. Em seguida, verifica se o
     * arquivo existe no sistema de arquivos.
     *
     * @param solicitationId           O ID da solicitação associada ao arquivo.
     * @param requestOwnerRegistration O registro do proprietário da solicitação.
     * @param copy                     O objeto Copy que representa o arquivo.
     * @return O objeto File representando o arquivo no sistema de arquivos.
     * @throws NoSuchFileException Se o arquivo não for encontrado no sistema de arquivos.
     */
    private File getFile(Long solicitationId, String requestOwnerRegistration, Copy copy) throws NoSuchFileException {
        String fileLocation = BASE_DIR + requestOwnerRegistration + "/" + solicitationId + "/" + copy.getFileName();
        File downloadFile = new File(fileLocation);

        // Verifica se o arquivo existe no sistema de arquivos
        if (!downloadFile.exists()) throw new NoSuchFileException(null);

        return downloadFile;
    }

    /**
     * Filtra as cópias de arquivos para upload e deleção com base em duas solicitações.
     * Este metodo compara as cópias de arquivos de duas solicitações (normalmente a solicitação
     * atualizada e a versão original) e determina quais arquivos precisam ser adicionados
     * (presentes na solicitação atualizada, mas não na original) e quais precisam ser removidos
     * (presentes na solicitação original, mas não na atualizada).
     *
     * @param updatedSolicitation  A solicitação atualizada.
     * @param originalSolicitation A versão original da solicitação.
     * @return Um mapa contendo duas listas: "toUpload" (cópias a serem adicionadas) e "toDelete" (cópias a serem removidas).
     */
    private Map<String, List<Copy>> filterUploadDeleteFiles(Solicitation updatedSolicitation, Solicitation originalSolicitation) {
        // Obtém os nomes dos arquivos da solicitação atualizada
        Set<String> updatedFileNames = getFileNamesFromRequest(updatedSolicitation);
        // Obtém os nomes dos arquivos da solicitação original
        Set<String> originalFileNames = getFileNamesFromRequest(originalSolicitation);

        // Identifica as cópias a serem adicionadas (presentes na atualizada, ausentes na original)
        List<Copy> toUpload = updatedSolicitation.getCopies().stream()
                .filter(copy -> !originalFileNames.contains(copy.getFileName()))
                .toList();

        // Identifica as cópias a serem removidas (ausentes na atualizada, presentes na original)
        List<Copy> toDelete = originalSolicitation.getCopies().stream()
                .filter(copy -> !updatedFileNames.contains(copy.getFileName()))
                .toList();

        // Retorna um mapa contendo as listas de arquivos para upload e deleção
        return Map.of("toUpload", toUpload, "toDelete", toDelete);
    }

    /**
     * Extrai os nomes dos arquivos de uma solicitação e retorna um conjunto.
     * Este metodo recebe um objeto Request e retorna um conjunto contendo os nomes
     * de todos os arquivos associados às cópias da solicitação.
     *
     * @param solicitation A solicitação da qual os nomes dos arquivos serão extraídos.
     * @return Um conjunto de strings contendo os nomes dos arquivos.
     */
    private Set<String> getFileNamesFromRequest(Solicitation solicitation) {
        return solicitation.getCopies().stream().map(Copy::getFileName).collect(Collectors.toSet());
    }

    /**
     * Remove os arquivos associados às cópias, se existirem no caminho base especificado.
     * Este metodo itera sobre a lista de cópias fornecida e tenta deletar o arquivo do sistema de arquivos.
     * O status 'fileInDisk' da cópia é atualizado para 'false'.
     *
     * @param copies   A lista de objetos Copy representando os arquivos a serem removidos.
     * @param basePath O caminho base onde os arquivos estão localizados.
     * @return O número de arquivos que foram removidos com sucesso.
     */
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
}