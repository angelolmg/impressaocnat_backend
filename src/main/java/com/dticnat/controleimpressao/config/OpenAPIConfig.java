package com.dticnat.controleimpressao.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;

import java.util.ArrayList;
import java.util.List;

@Configuration
public class OpenAPIConfig {

    /**
     *
     * This method is needed to allow sending multipart requests. For example, when an item is
     * created together with an image. If this is not set the request will return an exception with:
     *
     * Resolved [org.springframework.web.HttpMediaTypeNotSupportedException: Content-Type
     * 'application/octet-stream' is not supported]
     *
     * @param converter
     */
    public OpenAPIConfig(MappingJackson2HttpMessageConverter converter) {
        var supportedMediaTypes = new ArrayList<>(converter.getSupportedMediaTypes());
        supportedMediaTypes.add(new MediaType("application", "octet-stream"));
        converter.setSupportedMediaTypes(supportedMediaTypes);
    }

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
                                .description("Token de acesso ao SUAP (Implicit OAUTH2 ou JWT comum). Para obtê-lo, realizar login por meio do endpoint /api/auth/login-suap OU verificar 'suapToken' no localStorage da aplicação front (pós login).")
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")))
                .info(new Info()
                        .title("Controle de Impressão - IFRN CNAT")
                        .description("Documentação da API de solicitações de cópias da gráfica IFRN CNAT")
                        .version("1.0")
//                        .termsOfService("http://example.com/terms")
                        .contact(new Contact().name("Diretoria de Tecnologia da Informação (DTI) IFRN/CNAT").email("dti.cnat@ifrn.edu.br"))
                        .license(new License().name("GPLv3").url("https://www.gnu.org/licenses/gpl-3.0.txt")));
    }
}
