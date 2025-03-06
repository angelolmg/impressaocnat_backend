package com.dticnat.controleimpressao.model;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;

@Entity
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Copy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "O nome do arquivo não pode ser nulo.")
    @NotEmpty(message = "O nome do arquivo não pode ser vazio.")
    private String fileName;

    private String fileType;

    @NotNull(message = "O número de páginas não pode ser nulo.")
    @Positive(message = "O número de páginas deve ser positivo.")
    private Integer pageCount;

    @Embedded
    @NotNull(message = "As configurações de impressão não podem ser nulas.")
    private PrintConfig printConfig;

    @Builder.Default
    private Boolean fileInDisk = false;

    // Arquivo é físico e não foi anexado digitalmente à solicitação
    @Builder.Default
    private Boolean isPhysicalFile = false;

    @Column(name="request_id")
    private Long requestId;

    private String notes;
}