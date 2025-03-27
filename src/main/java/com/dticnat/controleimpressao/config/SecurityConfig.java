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

//    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
//        http
//                .csrf(csrf -> csrf.disable()) // Desabilita o CSRF (Cross-Site Request Forgery) - considerar habilitar e configurar em produção
//                // .cors(cors -> cors.disable()) // Desabilita o CORS (Cross-Origin Resource Sharing) - considerar habilitar e configurar em produção
//                .authorizeHttpRequests(auth -> auth
//                        // Configuração de acesso por roles (papéis)
//                        // .requestMatchers("/admin/**").hasRole("ADMIN")
//                        // .requestMatchers("/user/**").hasAnyRole("USER", "ADMIN")
//
//                        // Configuração de acesso por permissões (authorities)
//                        // .requestMatchers("/api/requests/**").hasAuthority("READ_REQUEST")
//                        // .requestMatchers(HttpMethod.POST, "/api/requests").hasAuthority("CREATE_REQUEST")
//
//                        // Configuração de acesso por métodos HTTP em rotas específicas
//                        // .requestMatchers(HttpMethod.GET, "/api/copies/**").permitAll()
//                        // .requestMatchers(HttpMethod.POST, "/api/copies").authenticated()
//
//                        // Permitir acesso irrestrito a algumas rotas públicas
//                        .requestMatchers("/public/**", "/auth/**", "/h2-console/**", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
//
//                        // Exemplo de configuração de acesso para o H2 Console (não recomendado para produção)
//                        .requestMatchers("/h2-console/**").permitAll()
//                        .headers(headers -> headers.frameOptions().sameOrigin()) // Necessário para o H2 Console
//
//                        // Configuração padrão: protege todas as outras rotas (requer autenticação)
//                        .anyRequest().authenticated()
//                )
//                // Configuração de diferentes métodos de autenticação
//                // .httpBasic(withDefaults()) // Habilita a autenticação básica HTTP
//                // .formLogin(form -> form
//                //         .loginPage("/login") // Página de login personalizada
//                //         .permitAll()
//                //         .defaultSuccessUrl("/dashboard")
//                //         .failureUrl("/login?error=true")
//                // )
//                // .oauth2Login(oauth2 -> oauth2
//                //         .loginPage("/oauth2/login")
//                //         .userInfoEndpoint(userInfo -> userInfo.userService(customOAuth2UserService))
//                //         .successHandler(oauth2AuthenticationSuccessHandler)
//                //         .failureHandler(oauth2AuthenticationFailureHandler)
//                // )
//                // .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class) // Adicionar um filtro JWT antes do filtro de autenticação padrão
//
//                // Configuração de logout
//                // .logout(logout -> logout
//                //         .logoutRequestMatcher(new AntPathRequestMatcher("/logout"))
//                //         .logoutSuccessUrl("/login?logout")
//                //         .permitAll()
//                //         .deleteCookies("JSESSIONID")
//                //         .invalidateHttpSession(true)
//                // )
//
//                // Configuração de tratamento de erros de autorização
//                // .exceptionHandling(exceptionHandling -> exceptionHandling
//                //         .accessDeniedPage("/access-denied")
//                // )
//
//                // Configuração de CORS (se habilitado)
//                // .cors(cors -> cors
//                //         .configurationSource(request -> {
//                //             CorsConfiguration corsConfiguration = new CorsConfiguration();
//                //             corsConfiguration.setAllowedOrigins(Arrays.asList("http://localhost:3000"));
//                //             corsConfiguration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
//                //             corsConfiguration.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type"));
//                //             return corsConfiguration;
//                //         })
//                // )
//                .httpBasic(basic -> basic.disable())
//                .formLogin(form -> form.disable());
//
//        return http.build();
//    }
}

