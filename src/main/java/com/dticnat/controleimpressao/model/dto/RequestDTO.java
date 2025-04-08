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
public class RequestDTO {

    @NotNull(message = "O prazo da solicitação não pode ser nulo.")
    @Min(value = 3600, message = "O prazo deve ser no mínimo 1 hora (3600 segundos).")
    @Max(value = 172800, message = "O prazo deve ser no máximo 48 horas (172800 segundos).")
    private int term;

    @Positive(message = "O número total de páginas deve ser positivo.")
    private int totalPageCount;

    @NotEmpty(message = "Deve haver pelo menos uma cópia na solicitação.")
    private List<CopyDTO> copies;
}