package com.dticnat.controleimpressao.model.dto;

import lombok.*;

import java.util.List;

/**
 * DTO for user data.
 */
@Getter
public class UserData {
    private Long id;
    private String matricula;
    private String nome_usual;
    private String cpf;
    private String rg;
    private List<String> filiacao;
    private String data_nascimento; // ISO format 'yyyy-MM-dd'
    private String naturalidade;
    private String tipo_sanguineo;
    private String email;
    private String url_oto_75x100;
    private String url_foto_150x200;
    private String tipo_vinculo;
    private Vinculo vinculo;
}

