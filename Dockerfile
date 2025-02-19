# Stage 1: Construção da aplicação com Maven
# Utilizamos uma imagem base com Maven, Amazon Corretto 23 e Alpine Linux
FROM maven:3.9.9-amazoncorretto-23-alpine AS build

# Definimos o diretório de trabalho dentro do container
WORKDIR /app

# Copiamos o arquivo pom.xml para o diretório de trabalho
# Este arquivo contém as dependências e configurações do projeto Maven
COPY pom.xml .

# Baixamos as dependências do projeto e armazenamos em cache
# O modo offline garante que o Maven não tente baixar novamente se já estiverem em cache
RUN mvn dependency:go-offline

# Copiamos o código fonte da aplicação para o diretório de trabalho
COPY src ./src

# Construímos a aplicação
# clean remove arquivos gerados em builds anteriores
# package empacota a aplicação em um arquivo JAR
# -DskipTests ignora a execução de testes durante o build
RUN mvn clean package -DskipTests

# Stage 2: Criação da imagem final
# Utilizamos uma imagem base menor, apenas com o Java e o necessário para executar a aplicação
FROM maven:3.9.9-amazoncorretto-23-alpine

# Cria o usuário e grupo da aplicação
# Executa o container com o usuário spring
RUN addgroup -S spring && adduser -S spring -G spring
USER spring

# Definimos o diretório de trabalho na imagem final
WORKDIR /app

# Copiamos o arquivo JAR gerado no stage anterior para a imagem final
# Utilizamos --from=build para especificar que o arquivo deve ser copiado do stage "build"
COPY --from=build /app/target/controleimpressao-0.0.1-SNAPSHOT.jar .

# Muda o dono do arquivo JAR para o usuário spring
#RUN chown spring:spring /app/controleimpressao-0.0.1-SNAPSHOT.jar

# Cria o diretório de arquivos
RUN mkdir -p /app/archives

# Define o volume
VOLUME /app/archives

# Expomos a porta 8080, que é a porta padrão utilizada por aplicações Spring Boot
# Esta porta será mapeada para a porta do host durante a execução do container
EXPOSE 8080

# Define as variáveis de ambiente
# ENV define as variáveis que estarão disponíveis dentro do container
ENV FILE_BASE_DIR=/app/archives/

# Define o healthcheck
HEALTHCHECK --interval=30m --timeout=10s --retries=3 CMD curl -f http://localhost:8080/health || exit 1

# Definimos o comando que será executado quando o container for iniciado
# Neste caso, executamos a aplicação Java
ENTRYPOINT ["java", "-jar", "/app/controleimpressao-0.0.1-SNAPSHOT.jar"]