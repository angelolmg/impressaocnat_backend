package com.dticnat.controleimpressao.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;

@Configuration
public class OpenAPIConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Controle de Inpressão - IFRN CNAT")
                        .description("Documentação da API de solicitações de cópias da gráfica IFRN CNAT")
                        .version("1.0")
//                        .termsOfService("http://example.com/terms")
//                        .contact(new io.swagger.v3.oas.models.info.Contact().name("Your Name").email("your.email@example.com"))
                        .license(new io.swagger.v3.oas.models.info.License().name("GPLv3").url("https://www.gnu.org/licenses/gpl-3.0.txt")));
    }
}
