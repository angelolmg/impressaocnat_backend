package com.dticnat.controleimpressao.model;

import com.dticnat.controleimpressao.model.enums.Role;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Embeddable
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class User {
    private String commonName; // Nome usual
    private String registrationNumber; // Matrícula
    private String email;
    private String phoneNumbers; // Telefones
    private String sector; // Setor suap
    private String photoUrl; // url Foto

    @JsonIgnore
    @Enumerated(EnumType.STRING)
    private Role role; // Papel

    @JsonIgnore
    public boolean isAdminOrManager() {
        return role.equals(Role.ADMIN) || role.equals(Role.MANAGER);
    }

    @JsonIgnore
    public boolean isAdmin() {
        return role.equals(Role.ADMIN);
    }
}