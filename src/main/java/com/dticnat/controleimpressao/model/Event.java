package com.dticnat.controleimpressao.model;

import com.dticnat.controleimpressao.model.enums.EventType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Embedded
    private User creatorUser; // Usuário que criou

    private String content; // Conteúdo

    @Enumerated(EnumType.STRING)
    private EventType type; // Tipo de evento (enum)

    private LocalDateTime creationDate; // Data de criação

    @Column(name="solicitation_id")
    private Long solicitationId;
}