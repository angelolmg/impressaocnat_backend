package com.dticnat.controleimpressao.model;

import com.dticnat.controleimpressao.model.enums.Role;
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

    @ElementCollection
    private List<String> phoneNumbers; // Telefones Lista<String>
    private String sector; // Setor suap
    private String photoUrl; // url Foto

    @Enumerated(EnumType.STRING)
    private Role role; // Papel (enum)

    public boolean isAdminOrManager() {
        return role.equals(Role.ADMIN) || role.equals(Role.MANAGER);
    }

    public boolean isAdmin() {
        return role.equals(Role.ADMIN);
    }
}