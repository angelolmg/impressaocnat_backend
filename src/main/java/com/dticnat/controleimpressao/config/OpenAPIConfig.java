package com.dticnat.controleimpressao.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;

import java.util.List;

@Configuration
public class OpenAPIConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        final String securitySchemeName = "Token SUAP";
        return new OpenAPI()
                .servers(List.of(
                        new Server().url("http://localhost:8080/api").description("Servidor de Desenvolvimento"),
                        new Server().url("http://10.26.1.9:8080/api").description("Servidor de Produção")
                ))
                .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
                .components(new Components()
                        .addSecuritySchemes(securitySchemeName, new SecurityScheme()
                                .name(securitySchemeName)
                                .description("Implicit token obtido após fazer login OAUTH2 no SUAP. Verificar 'suapToken' no localStorage da aplicação front.")
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")))
                .info(new Info()
                        .title("Controle de Impressão - IFRN CNAT")
                        .description("Documentação da API de solicitações de cópias da gráfica IFRN CNAT")
                        .version("1.0")
//                        .termsOfService("http://example.com/terms")
                        .contact(new io.swagger.v3.oas.models.info.Contact().name("Diretoria de Tecnologia da Informação (DTI) IFRN/CNAT").email("dti.cnat@ifrn.edu.br"))
                        .license(new io.swagger.v3.oas.models.info.License().name("GPLv3").url("https://www.gnu.org/licenses/gpl-3.0.txt")));
    }
}
