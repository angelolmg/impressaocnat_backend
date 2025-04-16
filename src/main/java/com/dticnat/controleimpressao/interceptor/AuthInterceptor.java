package com.dticnat.controleimpressao.interceptor;

import com.dticnat.controleimpressao.exception.AuthorizationException;
import com.dticnat.controleimpressao.model.User;
import com.dticnat.controleimpressao.service.AuthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
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
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws ResponseStatusException, AuthorizationException {
        // Necessário para buscar uma solicilitação por ID
        if(Objects.equals(request.getMethod(), "OPTIONS")) return true;

        // Permite passar requisições para /api/auth/ sem autenticar o usuário
        if(request.getRequestURI().matches("/api/auth/.*")) return true;


        String token = request.getHeader("Authorization");
        if (token == null) throw new AuthorizationException("Usuário não autenticado.", HttpStatus.UNAUTHORIZED);

        try {
            User userPrincipal = authService.getUserPrincipal(token);
            if (userPrincipal == null) throw new AuthorizationException("Usuário não encontrado.", HttpStatus.NOT_FOUND);

            request.setAttribute("userPrincipal", userPrincipal);
            return true;

        } catch (AuthorizationException e) {
            logger.error("Erro ao obter dados do usuário: {}", e.getMessage());
            throw e;
        }
    }
}