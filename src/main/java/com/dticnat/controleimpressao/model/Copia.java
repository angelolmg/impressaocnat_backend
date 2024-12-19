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
public class Copia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "O nome do arquivo não pode ser nulo.")
    @NotEmpty(message = "O nome do arquivo não pode ser vazio.")
    private String nomeArquivo;

    @NotNull(message = "A extensão do arquivo (pdf, docx) não pode ser nulo.")
    @NotEmpty(message = "A extensão do arquivo (pdf, docx) não pode ser vazia.")
    private String extensaoArquivo;

    @NotNull(message = "O número de cópias não pode ser nulo.")
    @Positive(message = "O número de cópias deve ser positivo.")
    private int numeroCopiasRequisitadas;

    private int numeroPaginas;

    private Boolean possuiArquivoSalvo;

    @Column(name="solicitacao_id")
    private Long solicitacaoId;
}