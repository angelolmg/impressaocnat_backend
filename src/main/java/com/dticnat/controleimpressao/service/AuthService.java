package com.dticnat.controleimpressao.service;

import com.dticnat.controleimpressao.exception.UnauthorizedException;
import com.dticnat.controleimpressao.model.Solicitation;
import com.dticnat.controleimpressao.model.dto.SuapUserData;
import com.dticnat.controleimpressao.model.enums.Role;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Arrays;

@Service
public class AuthService {

    @Value("${dticnat.auth.adminRegistrations}")
    private String[] adminRegistrations;

    @Value("${dticnat.auth.managerRegistrations}")
    private String[] managerRegistrations;

    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);

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
    public SuapUserData getUserData(String authToken) {
        RestClient restClient = RestClient.builder()
                .baseUrl("https://suap.ifrn.edu.br")
                .defaultHeader("Authorization", authToken)
                .build();

        SuapUserData userData;

        try {
            userData = restClient.get().uri("/api/rh/meus-dados/").retrieve().body(SuapUserData.class);
            return userData;

        } catch (Exception e) {
            // Logar o erro para diagnóstico
            logger.error("Erro ao obter dados do usuário do SUAP: {}", e.getMessage());
            return null;
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
        if (Arrays.asList(managerRegistrations).contains(registration)) return Role.MANAGER;
        if (Arrays.asList(adminRegistrations).contains(registration)) return Role.ADMIN;
        return Role.USER;
    }

    /**
     * Verifica se o usuário autenticado tem permissão para acessar a solicitação.
     *
     * Este metodo verifica se o usuário autenticado (representado pelo UserData) é um
     * administrador ou se a solicitação pertence ao próprio usuário. Caso contrário,
     * uma exceção UnauthorizedException é lançada.
     *
     * @param userData O objeto UserData do usuário autenticado.
     * @param solicitation  A solicitação que se deseja acessar.
     * @return A matrícula do proprietário da solicitação.
     * @throws UnauthorizedException Se o usuário não tiver permissão para acessar a solicitação.
     */
    public String validateUserAccessAndGetOwnerRegistration(SuapUserData userData, Solicitation solicitation) throws UnauthorizedException {
        boolean isAdminOrManager = getRole(userData.getMatricula()) != Role.USER;
        String requestOwner = String.valueOf(solicitation.getCreatorUser().getRegistrationNumber());

        if (!isAdminOrManager && !userData.getMatricula().equals(requestOwner)) {
            throw new UnauthorizedException();
        }

        return requestOwner;
    }
}