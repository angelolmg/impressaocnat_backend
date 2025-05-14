package com.dticnat.controleimpressao.service;

import com.dticnat.controleimpressao.exception.UnauthorizedException;
import com.dticnat.controleimpressao.model.User;
import com.dticnat.controleimpressao.model.dto.SuapLoginDTO;
import com.dticnat.controleimpressao.model.dto.SuapLoginResponseDTO;
import com.dticnat.controleimpressao.model.dto.SuapUserData;
import com.dticnat.controleimpressao.model.enums.Role;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Arrays;

@Service
public class AuthService {

    @Value("${dticnat.auth.adminRegistrations}")
    private String[] adminRegistrations;

    @Value("${dticnat.auth.managerRegistrations}")
    private String[] managerRegistrations;

    /**
     * Recupera os dados do usuário autenticado a partir do token de autenticação.
     *
     * Este método utiliza um token de autenticação para realizar uma requisição à API do SUAP
     * e obter os dados do usuário correspondente. Em caso de falha na comunicação ou
     * na obtenção dos dados, retorna null.
     *
     * @param authToken O token de autenticação do usuário.
     * @return Um objeto UserData contendo os dados do usuário, ou null se a recuperação falhar.
     */
    public User getUserPrincipal(String authToken) throws UnauthorizedException {
        RestClient restClient = RestClient.builder()
                .baseUrl("https://suap.ifrn.edu.br")
                .defaultHeader("Authorization", authToken)
                .build();

        SuapUserData suapUserData = restClient.get().uri("/api/rh/meus-dados/").retrieve().body(SuapUserData.class);

        // Credenciais inválidas
        if(suapUserData == null || suapUserData.getMatricula() == null)
            throw new UnauthorizedException("Usuário não encontrado.");

        return User
                .builder()
                .commonName(suapUserData.getNome_usual())
                .registrationNumber(suapUserData.getMatricula())
                .email(suapUserData.getEmail())
                .phoneNumbers(String.join(", ", suapUserData.getVinculo().getTelefones_institucionais()))
                .sector(suapUserData.getVinculo().getSetor_suap())
                .photoUrl(suapUserData.getUrl_foto_150x200())
                .role(getRole(suapUserData.getMatricula()))
                .build();
    }


    public SuapLoginResponseDTO getSuapTokenFromCredentials(SuapLoginDTO credentials) throws UnauthorizedException {
        RestClient restClient = RestClient.builder()
                .baseUrl("https://suap.ifrn.edu.br")
                .defaultHeader("Accept", MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .build();

        try {
            SuapLoginResponseDTO tokenObject = restClient.post()
                    .uri("/api/token/pair")
                    .contentType(MediaType.APPLICATION_JSON) // explicitly set content type
                    .body(credentials)
                    .retrieve()
                    .body(SuapLoginResponseDTO.class);

            if (tokenObject == null || tokenObject.getAccess() == null) {
                throw new UnauthorizedException("Usuário não encontrado.");
            }

            return tokenObject;

        } catch (RestClientException e) {
            throw new UnauthorizedException("Erro ao autenticar com o SUAP: " + e.getMessage());
        }
    }

    /**
     * Verifica se um usuário com a matrícula especificada possui permissão de administrador.
     *
     * Este metodo verifica se a matrícula fornecida está presente na lista de matrículas
     * configuradas como administradoras.
     *
     * @param registration A matrícula do usuário a ser verificada.
     * @return true se o usuário for administrador, false caso contrário.
     */
    public Role getRole(String registration) {
        if (Arrays.asList(adminRegistrations).contains(registration)) return Role.ADMIN;
        if (Arrays.asList(managerRegistrations).contains(registration)) return Role.MANAGER;
        return Role.USER;
    }


}