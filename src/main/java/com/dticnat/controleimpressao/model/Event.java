package com.dticnat.controleimpressao.model;

import com.dticnat.controleimpressao.model.enums.EventType;
import com.fasterxml.jackson.annotation.JsonIgnore;
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
    private User user; // Usuário que criou

    private String content; // Conteúdo

    @Enumerated(EnumType.STRING)
    private EventType type; // Tipo de evento (enum)

    private LocalDateTime creationDate; // Data de criação

    @ManyToOne
    @JoinColumn(name="solicitation_id")
    @JsonIgnore
    private Solicitation solicitation;
}