package com.dticnat.controleimpressao.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class PhysicalFileException extends RuntimeException {
    public PhysicalFileException(String message) {
        super(message);
    }

    public PhysicalFileException() {
        super();
    }
}
