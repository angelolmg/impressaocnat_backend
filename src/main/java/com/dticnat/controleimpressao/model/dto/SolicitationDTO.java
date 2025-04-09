package com.dticnat.controleimpressao.model.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SolicitationDTO {

    @NotNull(message = "O prazo da solicitação não pode ser nulo.")
    @Min(value = 1, message = "O prazo deve ser no mínimo 1 hora.")
    @Max(value = 48, message = "O prazo deve ser no máximo 48 horas.")
    private int deadline;

    @Positive(message = "O número total de páginas deve ser positivo.")
    private int totalPageCount;

    @NotEmpty(message = "Deve haver pelo menos uma cópia na solicitação.")
    private List<CopyDTO> copies;
}