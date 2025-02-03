package com.dticnat.controleimpressao.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.GONE)
public class FileGoneException extends RuntimeException {
    public FileGoneException(String message) {
        super(message);
    }
}
