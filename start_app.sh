#!/bin/bash

# Tarefas possíveis
HOT_RELOAD="${1:-false}"  # stop + up
REBUILD="${2:-false}" # stop + rm + up (--build)
UPDATE="${3:-false}" # pull + REBUILD

# Nome do projeto
PROJECT_NAME="controleimpressao"

# Verifica qual argumento foi fornecido
if [ "$HOT_RELOAD" == "true" ]; then
    docker-compose -p "$PROJECT_NAME" stop 2>/dev/null # Ignora erros se eles não existirem
    docker-compose -p "$PROJECT_NAME" up -d

elif [ "$REBUILD" == "true" ]; then
    docker-compose -p "$PROJECT_NAME" stop 2>/dev/null
    docker-compose -p "$PROJECT_NAME" rm -f 2>/dev/null
    docker-compose -p "$PROJECT_NAME" up -d --build

elif [ "$UPDATE" == "true" ]; then
    docker-compose pull
    docker-compose -p "$PROJECT_NAME" stop 2>/dev/null
    docker-compose -p "$PROJECT_NAME" rm -f 2>/dev/null
    docker-compose -p "$PROJECT_NAME" up -d --build
fi

echo "Containers estão em execução."

# Aguarda pressionar uma tecla para fechar o shell
read -n 1 -s -p "Pressione qualquer tecla para sair..."
echo "" # Adiciona uma nova linha após a tecla pressionada