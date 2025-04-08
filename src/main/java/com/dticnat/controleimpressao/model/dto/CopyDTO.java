package com.dticnat.controleimpressao.model.dto;

import com.dticnat.controleimpressao.model.PrintConfig;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CopyDTO {

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
    private Boolean isPhysicalFile = false;

    private String notes;
}
