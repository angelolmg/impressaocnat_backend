package com.dticnat.controleimpressao.model.enums;

public enum EventType {
    COMMENT, // comentário
    REQUEST_OPENING, // abertura de solicitação
    REQUEST_CLOSING, // fechamento de solicitação
    REQUEST_TOGGLE, // alteração de status da solicitação (análogo com OPENING e CLOSING)
    REQUEST_ARCHIVING, // arquivamento de solicitação
    REQUEST_EDITING, // edição de solicitação
    REQUEST_DELETING,  // deleção da solicitação
    REQUEST_VIEWING  // visualização de solicitação
}