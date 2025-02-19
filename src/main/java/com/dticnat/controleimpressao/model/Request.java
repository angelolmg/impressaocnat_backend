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
public class Request {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Prazo em segundos (default de 1 hora)
    @NotNull(message = "O prazo da solicitação não pode ser nulo.")
    @Min(value = 3600, message = "O prazo deve ser no mínimo 1 hora (3600 segundos).")
    @Max(value = 172800, message = "O prazo deve ser no máximo 48 horas (172800 segundos).")
    private int term;

    // Data de solicitação em Unix time
    private long creationDate;

    // Data de conclusão em Unix time
    // Padrão para não concluído = 0
    @Builder.Default
    private long conclusionDate = 0;

    // Número de páginas total
    private int totalPageCount;

    // Usuário associado à solicitação
    @NotNull(message = "O usuário associado não pode ser nulo.")
    @NotEmpty(message = "O usuário associado não pode ser vazio.")
    private String username;

    // A matrícula do usuário solicitante
    @NotNull(message = "O matrícula associada não pode ser nula.")
    private String registration;

    @OneToMany(cascade=CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name="request_id", referencedColumnName="id")
    @NotEmpty(message = "Deve haver pelo menos uma cópia na solicitação.")
    @OrderBy("id ASC")
    private List<Copy> copies;
}