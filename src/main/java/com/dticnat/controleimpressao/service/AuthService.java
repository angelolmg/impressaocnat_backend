package com.dticnat.controleimpressao.service;

import com.dticnat.controleimpressao.model.dto.UserData;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Arrays;

@Service
public class AuthService {

    @Value("${dticnat.admins.adminRegistrations}")
    public String[] adminRegistrations;

    public UserData getUserData(String authToken) {
        RestClient restClient = RestClient.builder()
                .baseUrl("https://suap.ifrn.edu.br")
                .defaultHeader("Authorization", authToken)
                .build();

        return restClient.get().uri("/api/rh/meus-dados/").retrieve().body(UserData.class);
    }

    public boolean isAdmin(String registration) {
        if(registration == null) return false;
        return Arrays.asList(adminRegistrations).contains(registration);
    }
}
