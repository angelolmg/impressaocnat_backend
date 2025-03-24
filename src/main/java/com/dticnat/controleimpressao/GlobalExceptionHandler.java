package com.dticnat.controleimpressao;

import com.dticnat.controleimpressao.exception.AuthorizationException;
import com.dticnat.controleimpressao.model.dto.Payload;
import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@Hidden // Necesário para não dar conflito com a documentação Swagger v3
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AuthorizationException.class)
    public ResponseEntity<Payload<Object>> handleException(AuthorizationException ex) {
        Payload<Object> payload = new Payload<>();
        payload.setStatus(ex.getStatus().value());
        payload.setMensagem(ex.getMessage());
        return ResponseEntity.status(payload.getStatus()).body(payload);
    }
}