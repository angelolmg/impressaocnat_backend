package com.dticnat.controleimpressao.interceptor;

import com.dticnat.controleimpressao.exception.AuthorizationException;
import com.dticnat.controleimpressao.model.dto.UserData;
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
        if(Objects.equals(request.getMethod(), "OPTIONS")) return true; // Necessário para buscar uma solicilitação por ID

        String token = request.getHeader("Authorization");

        if (token == null) {
            throw new AuthorizationException("Usuário não autenticado.", HttpStatus.UNAUTHORIZED);
        }

        UserData userData = authService.getUserData(token);
        if (userData == null) {
            throw new AuthorizationException("Usuário não encontrado.", HttpStatus.NOT_FOUND);
        }

        request.setAttribute("userData", userData);
        request.setAttribute("isAdmin", authService.isAdmin(userData.getMatricula()));

        return true;
    }
}