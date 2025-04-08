package com.dticnat.controleimpressao.service;

import com.dticnat.controleimpressao.model.Copy;
import com.dticnat.controleimpressao.model.Request;
import com.dticnat.controleimpressao.model.dto.CopyDTO;
import com.dticnat.controleimpressao.repository.CopyRepository;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class CopyService {

    @Autowired
    private CopyRepository copyRepository;

    /**
     * Retorna uma lista com todas as cópias de arquivos registradas no sistema.
     *
     * @return Lista de todos os objetos Copy.
     */
    public List<Copy> findAll() {
        return copyRepository.findAll();
    }

    /**
     * Busca uma cópia de arquivo pelo seu ID.
     *
     * @param id O ID da cópia a ser buscada.
     * @return Um Optional contendo a cópia, se encontrada.
     */
    public Optional<Copy> findById(Long id) {
        return copyRepository.findById(id);
    }

    /**
     * Cria e salva uma nova cópia de arquivo no banco de dados.
     *
     * @param copyDTO O objeto {@link CopyDTO} a ser persistido.
     */
    public Copy create(CopyDTO copyDTO, Long requestId) {

        Copy copy = Copy
                .builder()
                .fileName(copyDTO.getFileName())
                .fileType(copyDTO.getFileType())
                .pageCount(copyDTO.getPageCount())
                .printConfig(copyDTO.getPrintConfig())
                .fileInDisk(!copyDTO.getIsPhysicalFile())
                .isPhysicalFile(copyDTO.getIsPhysicalFile())
                .notes(copyDTO.getNotes())
                .build();

        return copyRepository.save(copy);
    }

    public Copy save(Copy copy) {
        return copyRepository.save(copy);
    }

    /**
     * Verifica se uma cópia de arquivo existe pelo seu ID.
     *
     * @param id O ID da cópia a ser verificada.
     * @return true se a cópia existir, false caso contrário.
     */
    public boolean existsById(Long id) {
        return copyRepository.existsById(id);
    }

    /**
     * Retorna uma lista de cópias de arquivos associadas a um ID de solicitação,
     * permitindo filtrar por um termo de pesquisa no nome do arquivo.
     *
     * @param requestId O ID da solicitação para a qual buscar as cópias.
     * @param query     Termo de pesquisa para filtrar pelo nome do arquivo (opcional).
     * @return Lista de objetos Copy correspondentes aos critérios de busca.
     */
    public List<Copy> findAllByRequestId(Long requestId, String query) {
        Specification<Copy> spec = filterCopies(requestId, query);
        return copyRepository.findAll(spec, Sort.by(Sort.Direction.ASC, "id"));
    }

    /**
     * Cria uma especificação JPA para filtrar cópias por ID de solicitação e nome de arquivo.
     *
     * @param requestId O ID da solicitação para filtrar as cópias.
     * @param userQuery Termo de pesquisa para filtrar pelo nome do arquivo (opcional).
     * @return Uma Specification JPA para a consulta.
     */
    public Specification<Copy> filterCopies(Long requestId, String userQuery) {
        return (Root<Copy> root, CriteriaQuery<?> query, CriteriaBuilder cb) -> {
            Predicate predicate = cb.conjunction();

            // Filtrar por ID de solicitação
            if (requestId != null) {
                predicate = cb.and(predicate, cb.equal(root.get("requestId"), requestId));
            }

            // Filtrar por query de texto no nome do arquivo (case-insensitive e busca parcial)
            if (userQuery != null && !userQuery.isEmpty()) {
                // Testar se query é nome do solicitante
                predicate = cb.and(predicate, cb.like(cb.lower(cb.trim(root.get("fileName"))), "%" + userQuery.trim().toLowerCase() + "%"));
            }

            return predicate;
        };
    }

    /**
     * Cria e salva os objetos de cópia associados a uma dada solicitação.
     *
     * Este metodo itera sobre a lista de cópias de uma solicitação, define o ID da solicitação
     * e o status inicial do arquivo em disco, e persiste cada cópia no banco de dados.
     *
     * @param request A solicitação da qual as cópias serão instanciadas.
     */
    public List<Copy> instanceCopiesFromRequest(Request request, List<CopyDTO> copiesDTO) {
        List<Copy> copies = new ArrayList<>();
        copiesDTO.forEach((copyDTO) -> {
            copies.add(create(copyDTO, request.getId()));
        });

        return copies;
    }

    /**
     * Atualiza o status do arquivo em disco ('fileInDisk') de uma cópia previamente salva.
     *
     * Este metodo busca uma cópia pelo seu ID e atualiza a flag 'fileInDisk' com o novo status fornecido.
     * É utilizado, por exemplo, durante a tarefa de deleção de arquivos de uma solicitação obsoleta.
     *
     * @param copyId O ID da cópia a ser atualizada.
     * @param status O novo status do arquivo em disco (true para disponível, false para não disponível).
     */
    public void updateFileStatus(Long copyId, boolean status) {
        Optional<Copy> optCopy = findById(copyId);

        if(optCopy.isEmpty()) return; // Se a cópia não existe, não há nada a fazer

        Copy copy = optCopy.get();
        copy.setFileInDisk(status);
        copyRepository.save(copy);
    }
}