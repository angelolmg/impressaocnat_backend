package com.dticnat.controleimpressao.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

////        String loginUrl = "https://suap.ifrn.edu.br/o/authorize/" +
////                "?response_type=" + "token" +
////                "&grant_type="    + "implicit" +
////                "&client_id="     + "1JHU2ziXvIPIsSgPlDz9ZhBbmRQ6Zz13Cpb2fSlp" +
////                "&scope="  + "identificacao email documentos_pessoais" +
////                "&redirect_uri="  + "http://localhost:8080/";
//
//
////        return http
////                .authorizeHttpRequests( auth ->
////                {
////                    auth.requestMatchers("/").permitAll();
////                    auth.anyRequest().authenticated();
////                })
////                .oauth2Login(oauth2 ->
////                        oauth2.loginPage(loginUrl)
////                )
////                .logout(logout ->
////                        logout.logoutSuccessUrl("/")
////                )
////                .build();
//
        return http
                .csrf( csrf -> csrf.disable())
//                .cors(cors -> cors.disable())
                .authorizeHttpRequests( auth -> auth.anyRequest().permitAll())
                .build();
    }
}

