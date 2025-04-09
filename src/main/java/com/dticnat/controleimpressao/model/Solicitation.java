package com.dticnat.controleimpressao.model;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Entidade que representa uma solicitação de impressão no sistema.
 *
 * Uma solicitação contém informações sobre o prazo para conclusão, datas de criação e conclusão,
 * status de obsolescência, número total de páginas, o usuário associado e a lista de cópias
 * de arquivos a serem impressas.
 */
@Entity
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Solicitation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Prazo para conclusão da solicitação em horas.
     * O valor padrão é de 1 hora.
     * O prazo deve ser no mínimo 1 hora e no máximo 48 horas.
     */
    @NotNull(message = "O prazo da solicitação não pode ser nulo.")
    @Min(value = 1, message = "O prazo deve ser no mínimo 1 hora.")
    @Max(value = 48, message = "O prazo deve ser no máximo 48 horas.")
    private int deadline;

    /**
     * Timestamp da data e hora em que a solicitação foi criada.
     */
    private LocalDateTime creationDate;

    /**
     * Timestamp da data e hora em que a solicitação foi concluída.
     */
    @Builder.Default
    private LocalDateTime conclusionDate = LocalDateTime.MIN;

    /**
     * Indica se a solicitação é considerada obsoleta/arquivada.
     * Solicitações obsoletas são aquelas que foram fechadas (possuem `conclusionDate` > 0)
     * há mais tempo do que o definido pela variável de ambiente `FILE_CLEANUP_FR` (em horas).
     * Depois deste tempo, arquivos digitais associados são deletados do servidor
     * e esta flag é definida como 'true'.
     * Por padrão, é definido como `false`.
     */
    @Builder.Default
    private boolean archived = false;

    @Embedded
    @NotNull(message = "O usuário de criação não pode ser nulo.")
    private User creatorUser; // Usuário que criou

    /**
     * Número total de páginas a serem impressas na solicitação, calculado com base nas cópias.
     */
    @Positive(message = "O número total de páginas deve ser positivo.")
    private int totalPageCount;

    /**
     * Lista de cópias de arquivos anexadas à solicitação.
     * A solicitação deve ter pelo menos uma cópia associada.
     * As cópias são carregadas e persistidas em cascata com a solicitação.
     * A ordem das cópias é mantida pelo ID em ordem ascendente.
     */
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "solicitation_id", referencedColumnName = "id")
    @NotEmpty(message = "Deve haver pelo menos uma cópia na solicitação.")
    @OrderBy("id ASC")
    private List<Copy> copies;

    @OneToMany(mappedBy = "solicitation", cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "solicitation_id", referencedColumnName = "id")
    private List<Event> timeline; // Linha do Tempo Lista<Evento>
}