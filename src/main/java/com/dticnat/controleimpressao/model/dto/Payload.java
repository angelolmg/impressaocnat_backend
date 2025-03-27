package com.dticnat.controleimpressao.model.dto;

import lombok.Data;

/**
 * Data Transfer Object (DTO) para padronizar a estrutura de respostas da API.
 * Este DTO encapsula informações sobre o status da requisição HTTP, os dados retornados
 * (ou enviados) e uma mensagem descritiva. Ele facilita a comunicação entre o backend
 * e o frontend, garantindo um formato consistente para as respostas da API.
 *
 * @param <T> O tipo dos dados contidos no payload.
 */
@Data
public class Payload<T> {
    /**
     * Código de status HTTP da resposta.
     * Utiliza os códigos definidos em {@link org.springframework.http.HttpStatus}.
     */
    private int status;

    /**
     * Dados enviados ou recebidos na resposta.
     * Este campo contém o objeto ou coleção de objetos que são o resultado da requisição
     * ou os dados que foram enviados para o servidor. O tipo é genérico (`T`) para
     * permitir o uso com diferentes tipos de dados.
     */
    private T dados;

    /**
     * Mensagem descritiva sobre o resultado da requisição.
     * Esta mensagem pode fornecer informações adicionais sobre o sucesso ou a falha
     * da operação, bem como detalhes relevantes para o consumidor da API.
     */
    private String mensagem;
}