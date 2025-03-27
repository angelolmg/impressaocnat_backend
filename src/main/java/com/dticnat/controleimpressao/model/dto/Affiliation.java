package com.dticnat.controleimpressao.model.dto;

import lombok.Getter;

import java.util.List;

/**
 * Data Transfer Object (DTO) para representar os dados de vínculo de um usuário,
 * tipicamente obtidos de um sistema como o SUAP (Sistema Unificado de Administração Pública).
 *
 * Este DTO encapsula informações detalhadas sobre a afiliação do usuário a uma instituição,
 * incluindo dados de identificação, setor, jornada de trabalho, campus, cargo, funções,
 * informações acadêmicas e contato institucional.
 */
@Getter
public class Affiliation {
    /**
     * Matrícula do usuário na instituição.
     * Identificador único do usuário dentro do sistema.
     */
    private String matricula;

    /**
     * Nome completo do usuário.
     */
    private String nome;

    /**
     * Setor do usuário conforme registrado no SUAP.
     */
    private String setor_suap;

    /**
     * Setor do usuário conforme registrado no SIAPE (Sistema Integrado de Administração de Pessoal).
     */
    private String setor_siape;

    /**
     * Jornada de trabalho do usuário.
     */
    private String jornada_trabalho;

    /**
     * Campus ao qual o usuário está vinculado.
     */
    private String campus;

    /**
     * Cargo do usuário na instituição.
     */
    private String cargo;

    /**
     * Lista de funções exercidas pelo usuário.
     */
    private List<String> funcao;

    /**
     * Disciplina de ingresso do usuário (se aplicável).
     */
    private String disciplina_ingresso;

    /**
     * Categoria do vínculo do usuário (ex: servidor, aluno).
     */
    private String categoria;

    /**
     * Lista de telefones institucionais do usuário.
     */
    private List<String> telefones_institucionais;

    /**
     * URL para a foto do usuário no formato 75x100 pixels.
     */
    private String url_foto_75x100;

    /**
     * URL para o currículo Lattes do usuário (se disponível).
     */
    private String curriculo_lattes;
}