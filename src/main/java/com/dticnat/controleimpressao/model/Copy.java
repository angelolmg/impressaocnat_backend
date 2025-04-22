package com.dticnat.controleimpressao.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;

/**
 * Entidade que representa uma cópia de um arquivo associado a uma solicitação de impressão.
 *
 * Esta entidade armazena informações sobre o arquivo da cópia, como nome, tipo, número de páginas,
 * configurações de impressão e metadados sobre sua localização e natureza (digital ou física).
 */
@Entity
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Copy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Nome do arquivo da cópia.
     * Este campo é obrigatório e não pode ser nulo ou vazio.
     */
    @NotNull(message = "O nome do arquivo não pode ser nulo.")
    @NotEmpty(message = "O nome do arquivo não pode ser vazio.")
    @Schema(example = "arquivo.pdf")
    private String fileName;

    /**
     * Tipo de arquivo da cópia.
     * Geralmente indica o formato do arquivo, como 'application/pdf'.
     */
    @Schema(example = "application/pdf")
    private String fileType;

    /**
     * Número de páginas do arquivo.
     * Este campo é obrigatório e deve ser um valor positivo.
     */
    @NotNull(message = "O número de páginas não pode ser nulo.")
    @Positive(message = "O número de páginas deve ser positivo.")
    private Integer pageCount;

    /**
     * Configurações de impressão para a cópia.
     * Este campo é obrigatório e contém detalhes sobre as opções de impressão desejadas.
     */
    @Embedded
    @NotNull(message = "As configurações de impressão não podem ser nulas.")
    private PrintConfig printConfig;

    /**
     * Indica se o arquivo digital ainda está armazenado no disco do servidor.
     * Por padrão, é definido como `false`.
     */
    @Builder.Default
    private Boolean fileInDisk = false;

    /**
     * Indica se o arquivo é físico (não foi anexado digitalmente à solicitação).
     * Por padrão, é definido como `false`.
     */
    @Builder.Default
    private Boolean isPhysicalFile = false;

    /**
     * ID da solicitação à qual esta cópia pertence.
     * Esta coluna é usada para relacionar a cópia com a solicitação correspondente.
     */
    @ManyToOne
    @JoinColumn(name="solicitation_id")
    @JsonIgnore
    private Solicitation solicitation;

    /**
     * Observações adicionais sobre a cópia.
     */
    private String notes;
}