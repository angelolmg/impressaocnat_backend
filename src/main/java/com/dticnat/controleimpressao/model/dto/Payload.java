package com.dticnat.controleimpressao.model.dto;

import lombok.Data;

@Data
public class Payload<T> {
    private int status;
    private T dados;
    private String mensagem;
}