package com.dticnat.controleimpressao.model.dto;

import lombok.Getter;

import java.util.List;

/**
 * DTO for vinculo details.
 */

@Getter
public class Vinculo {
    private String matricula;
    private String nome;
    private String setor_suap;
    private String setor_siape;
    private String jornada_trabalho;
    private String campus;
    private String cargo;
    private List<String> funcao;
    private String disciplina_ingresso;
    private String categoria;
    private List<String> telefones_institucionais;
    private String url_foto_75x100;
    private String curriculo_lattes;
}
