package com.dticnat.controleimpressao.interceptor;

import com.dticnat.controleimpressao.exception.AuthorizationException;
import com.dticnat.controleimpressao.model.dto.SuapUserData;
import com.dticnat.controleimpressao.service.AuthService;
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

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws ResponseStatusException {
        // Necessário para buscar uma solicilitação por ID
        if(Objects.equals(request.getMethod(), "OPTIONS")) return true;

        String token = request.getHeader("Authorization");

        if (token == null) {
            throw new AuthorizationException("Usuário não autenticado.", HttpStatus.UNAUTHORIZED);
        }

        SuapUserData userData = authService.getUserData(token);
        if (userData == null || userData.getMatricula() == null) {
            throw new AuthorizationException("Usuário não encontrado.", HttpStatus.NOT_FOUND);
        }

        userData.setRole(authService.getRole(userData.getMatricula()));
        request.setAttribute("userData", userData);

        return true;
    }
}