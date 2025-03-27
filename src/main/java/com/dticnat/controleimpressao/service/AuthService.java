package com.dticnat.controleimpressao.service;

import com.dticnat.controleimpressao.exception.UnauthorizedException;
import com.dticnat.controleimpressao.model.Request;
import com.dticnat.controleimpressao.model.dto.UserData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Arrays;

@Service
public class AuthService {

    @Value("${dticnat.admins.adminRegistrations}")
    private String[] adminRegistrations;

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
    public UserData getUserData(String authToken) {
        RestClient restClient = RestClient.builder()
                .baseUrl("https://suap.ifrn.edu.br")
                .defaultHeader("Authorization", authToken)
                .build();

        UserData userData;

        try {
            userData = restClient.get().uri("/api/rh/meus-dados/").retrieve().body(UserData.class);
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
    public boolean isAdmin(String registration) {
        if (registration == null) {
            return false;
        }
        return Arrays.asList(adminRegistrations).contains(registration);
    }

    /**
     * Verifica se o usuário autenticado tem permissão para acessar a solicitação.
     *
     * Este metodo verifica se o usuário autenticado (representado pelo UserData) é um
     * administrador ou se a solicitação pertence ao próprio usuário. Caso contrário,
     * uma exceção UnauthorizedException é lançada.
     *
     * @param userData O objeto UserData do usuário autenticado.
     * @param request  A solicitação que se deseja acessar.
     * @return A matrícula do proprietário da solicitação.
     * @throws UnauthorizedException Se o usuário não tiver permissão para acessar a solicitação.
     */
    public String validateUserAccessAndGetOwnerRegistration(UserData userData, Request request) throws UnauthorizedException {
        boolean isAdmin = isAdmin(userData.getMatricula());
        String requestOwner = String.valueOf(request.getRegistration());

        if (!isAdmin && !userData.getMatricula().equals(requestOwner)) {
            throw new UnauthorizedException();
        }

        return requestOwner;
    }
}