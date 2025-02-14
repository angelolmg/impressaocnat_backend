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

    @NotNull(message = "A extensão do arquivo (pdf, docx) não pode ser nula.")
    @NotEmpty(message = "A extensão do arquivo (pdf, docx) não pode ser vazia.")
    private String fileType;

    @NotNull(message = "O número de cópias não pode ser nulo.")
    @Positive(message = "O número de cópias deve ser positivo.")
    private int copyCount;

    private int pageCount;

    private Boolean fileInDisk = true;

    // Arquivo é físico e não foi anexado digitalmente à solicitação
    private Boolean isPhysicalFile = false;

    @Column(name="request_id")
    private Long requestId;
}