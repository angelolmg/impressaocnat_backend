package com.dticnat.controleimpressao.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Classe embarcável que representa as configurações de impressão para uma cópia de arquivo.
 *
 * Esta classe agrupa as preferências de impressão definidas pelo usuário, como número de cópias,
 * seleção de páginas, layout e impressão frente e verso. Ela é utilizada como um tipo incorporado
 * na entidade {@link Copy}.
 */
@Embeddable
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PrintConfig {

    /**
     * Número de cópias a serem impressas do arquivo.
     * Este campo é obrigatório e deve ser um valor positivo.
     */
    @NotNull(message = "O número de cópias não pode ser nulo.")
    @Positive(message = "O número de cópias deve ser positivo.")
    private Integer copyCount;

    /**
     * Tipo de seleção de páginas para impressão.
     * Pode ser "Todas" para imprimir todas as páginas ou "Personalizado" para imprimir um intervalo específico.
     * Este campo é obrigatório.
     */
    @NotNull(message = "O tipo de seleção de páginas não pode ser nulo.")
    @Pattern(regexp = "^Todas$|^Personalizado$", message = "O tipo de seleção de páginas deve ser 'Todas' ou 'Personalizado'.")
    private String pages;

    /**
     * Intervalo de páginas a serem impressas, caso o tipo de seleção seja "Personalizado".
     * A string segue um formato específico, como "1-11, 18", "22-23", "10".
     */
    @Schema(example = "5-11")
    @Pattern(regexp = "^(\\d+(-\\d+)?)(,\\s*\\d+(-\\d+)?)*$", message = "O intervalo de páginas deve seguir o formato 'n' ou 'n-m' ou 'n-m, k'.")
    private String pageIntervals;

    /**
     * Número de páginas a serem impressas por folha de papel.
     * As opções geralmente são 1, 2 ou 4 páginas por folha.
     * Este campo é obrigatório.
     */
    @NotNull(message = "O número de páginas por folha não pode ser nulo.")
    @Min(value = 1, message = "O número de páginas por folha deve ser no mínimo 1.")
    @Max(value = 4, message = "O número de páginas por folha deve ser no máximo 4.")
    private Integer pagesPerSheet;

    /**
     * Layout de impressão do documento.
     * Pode ser "Retrato" (vertical) ou "Paisagem" (horizontal).
     * Este campo é obrigatório.
     */
    @NotNull(message = "O layout de impressão não pode ser nulo.")
    @Pattern(regexp = "^Retrato$|^Paisagem$", message = "O layout de impressão deve ser 'Retrato' ou 'Paisagem'.")
    private String layout;

    /**
     * Indica se a impressão deve ser realizada em frente e verso do papel.
     * Este campo é obrigatório.
     */
    @NotNull(message = "A opção de impressão frente e verso não pode ser nula.")
    private Boolean frontAndBack;

    /**
     * Número total de folhas de papel necessárias para a cópia, calculado com base nas configurações de impressão.
     * Este campo é calculado e pode não ser definido diretamente pelo usuário.
     */
    private Integer sheetsTotal;
}
