package com.dticnat.controleimpressao.model;

import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Embeddable
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PrintConfig {

    @NotNull(message = "O número de cópias não pode ser nulo.")
    @Positive(message = "O número de cópias deve ser positivo.")
    private Integer copyCount;

    @NotNull(message = "O tipo de seleção de páginas não pode ser nulo.")
    private String pages; // "Todas" ou "Personalizado"

    private String pageIntervals;

    @NotNull(message = "O número de páginas por folha não pode ser nulo.")
    private Integer pagesPerSheet; // 1, 2 ou 4

    @NotNull(message = "O layout de impressão não pode ser nulo.")
    private String layout; // "Retrato" ou "Paisagem"

    @NotNull(message = "A opção de impressão frente e verso não pode ser nula.")
    private Boolean frontAndBack;

    private Integer sheetsTotal;
}
