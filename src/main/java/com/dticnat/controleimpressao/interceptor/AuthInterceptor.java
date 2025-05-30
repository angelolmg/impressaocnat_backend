package com.dticnat.controleimpressao.interceptor;

import com.dticnat.controleimpressao.exception.AuthorizationException;
import com.dticnat.controleimpressao.exception.UnauthorizedException;
import com.dticnat.controleimpressao.model.User;
import com.dticnat.controleimpressao.service.AuthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.util.Objects;

@Component
public class AuthInterceptor implements HandlerInterceptor {

    @Autowired
    private AuthService authService;

    private static final Logger logger = LoggerFactory.getLogger(AuthInterceptor.class);

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws ResponseStatusException, UnauthorizedException, AuthorizationException {
        // Necessário para buscar uma solicilitação por ID
        if(Objects.equals(request.getMethod(), "OPTIONS")) return true;

        String uri = request.getRequestURI();

        // Permite passar determinadas requisições sem autenticar o usuário
        if (uri.startsWith("/api/auth/") || uri.startsWith("/api/images/")) return true;

        String token = request.getHeader("Authorization");
        if (token == null) throw new UnauthorizedException("Usuário não autenticado.");

        try {
            User userPrincipal = authService.getUserPrincipal(token);
            if (userPrincipal == null) throw new UnauthorizedException("Usuário não encontrado.");

            request.setAttribute("userPrincipal", userPrincipal);
            return true;

        } catch (AuthorizationException e) {
            logger.error("Erro ao obter dados do usuário: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error("Erro no servidor do SUAP: {}", e.getMessage());
            throw new RuntimeException(e);
        }
    }
}