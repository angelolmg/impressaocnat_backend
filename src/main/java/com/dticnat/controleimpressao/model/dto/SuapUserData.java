package com.dticnat.controleimpressao.model.dto;

import com.dticnat.controleimpressao.model.enums.Role;
import lombok.*;

import java.util.List;

/**
 * Data Transfer Object (DTO) para representar os dados de um usuário,
 * tipicamente obtidos de um sistema como o SUAP (Sistema Unificado de Administração Pública).
 *
 * Este DTO encapsula informações pessoais e de vínculo do usuário, incluindo dados de identificação,
 * contato, informações acadêmicas/profissionais e uma flag customizada para indicar se o usuário
 * possui permissão de administrador no sistema.
 */
@Getter
@Setter
public class SuapUserData {
    /**
     * Identificador único do usuário no sistema.
     */
    private Long id;

    /**
     * Matrícula do usuário na instituição.
     * Identificador único do usuário dentro do sistema institucional.
     */
    private String matricula;

    /**
     * Nome usual do usuário.
     */
    private String nome_usual;

    /**
     * Cadastro de Pessoa Física (CPF) do usuário.
     */
    private String cpf;

    /**
     * Registro Geral (RG) do usuário.
     */
    private String rg;

    /**
     * Lista contendo os nomes da filiação do usuário (pai e mãe).
     */
    private List<String> filiacao;

    /**
     * Data de nascimento do usuário no formato ISO 'yyyy-MM-dd'.
     */
    private String data_nascimento;

    /**
     * Naturalidade do usuário.
     */
    private String naturalidade;

    /**
     * Tipo sanguíneo do usuário.
     */
    private String tipo_sanguineo;

    /**
     * Endereço de e-mail institucional do usuário.
     */
    private String email;

    /**
     * URL para a foto do usuário no formato 75x100 pixels.
     */
    private String url_foto_75x100;

    /**
     * URL para a foto do usuário no formato 150x200 pixels.
     */
    private String url_foto_150x200;

    /**
     * Tipo de vínculo do usuário com a instituição (ex: servidor, aluno).
     */
    private String tipo_vinculo;

    /**
     * Objeto contendo informações detalhadas sobre o vínculo do usuário com a instituição.
     */
    private SuapAffiliation vinculo;

    /**
     * Flag customizada, inicializada no AuthInterceptor.
     */
    private Role role;
}

