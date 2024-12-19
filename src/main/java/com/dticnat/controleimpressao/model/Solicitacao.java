package com.dticnat.controleimpressao.model;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;

import java.util.List;

@Entity
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Solicitacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private int numeroSolicitacao;

    // Prazo em segundos (default de 1 hora)
    @NotNull(message = "O prazo da solicitação não pode ser nulo.")
    @Min(value = 3600, message = "O prazo deve ser no mínimo 1 hora (3600 segundos).")
    @Max(value = 172800, message = "O prazo deve ser no máximo 48 horas (172800 segundos).")
    private int prazoSolicitacao;

    // Status da solicitação (0 = em aberto, 1 = concluída)
    private int statusSolicitacao = 0;

    // Data de solicitação em Unix time
    private long dataSolicitacao;

    // Data de conclusão em Unix time
    private long dataConclusao = 0;

    // Número de páginas total
    private int numeroPaginasTotal;

    // Usuário associado à solicitação
    @NotNull(message = "O usuário associado não pode ser nulo.")
    @NotEmpty(message = "O usuário associado não pode ser vazio.")
    private String usuarioAssociado;

    // A matrícula do usuário solicitante
    @NotNull(message = "O matrícula associada não pode ser nula.")
    private long matriculaUsuario;

//    @NotEmpty(message = "Deve haver pelo menos uma cópia na solicitação.")
    @OneToMany
    @JoinColumn(name="solicitacao_id", referencedColumnName="id")
    private List<Copia> copias;
}