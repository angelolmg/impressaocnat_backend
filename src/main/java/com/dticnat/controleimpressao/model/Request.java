package com.dticnat.controleimpressao.model;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;

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
public class Request {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Prazo para conclusão da solicitação em segundos.
     * O valor padrão é de 1 hora (3600 segundos).
     * O prazo deve ser no mínimo 1 hora e no máximo 48 horas.
     */
    @NotNull(message = "O prazo da solicitação não pode ser nulo.")
    @Min(value = 3600, message = "O prazo deve ser no mínimo 1 hora (3600 segundos).")
    @Max(value = 172800, message = "O prazo deve ser no máximo 48 horas (172800 segundos).")
    private int term;

    /**
     * Timestamp da data e hora em que a solicitação foi criada (em milissegundos desde a época Unix).
     */
    private long creationDate;

    /**
     * Timestamp da data e hora em que a solicitação foi concluída (em milissegundos desde a época Unix).
     * O valor padrão para solicitações não concluídas é 0.
     */
    @Builder.Default
    private long conclusionDate = 0;

    /**
     * Indica se a solicitação é considerada obsoleta/arquivada.
     * Solicitações obsoletas são aquelas que foram fechadas (possuem `conclusionDate` > 0)
     * há mais tempo do que o definido pela variável de ambiente `FILE_CLEANUP_FR` (em horas).
     * Depois deste tempo, arquivos digitais associados são deletados do servidor
     * e esta flag é definida como 'true'.
     * Por padrão, é definido como `false`.
     */
    @Builder.Default
    private boolean stale = false;

    /**
     * Número total de páginas a serem impressas na solicitação, calculado com base nas cópias.
     */
    private int totalPageCount;

    /**
     * Nome de usuário associado à solicitação.
     * Este campo é obrigatório e não pode ser nulo ou vazio.
     */
    @NotNull(message = "O usuário associado não pode ser nulo.")
    @NotEmpty(message = "O usuário associado não pode ser vazio.")
    private String username;

    /**
     * Matrícula do usuário que criou a solicitação.
     * Este campo é obrigatório e não pode ser nulo.
     */
    @NotNull(message = "O matrícula associada não pode ser nula.")
    private String registration;

    /**
     * Lista de cópias de arquivos anexadas à solicitação.
     * A solicitação deve ter pelo menos uma cópia associada.
     * As cópias são carregadas e persistidas em cascata com a solicitação.
     * A ordem das cópias é mantida pelo ID em ordem ascendente.
     */
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "request_id", referencedColumnName = "id")
    @NotEmpty(message = "Deve haver pelo menos uma cópia na solicitação.")
    @OrderBy("id ASC")
    private List<Copy> copies;
}