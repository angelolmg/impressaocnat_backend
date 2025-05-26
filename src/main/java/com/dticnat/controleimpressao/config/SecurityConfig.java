package com.dticnat.controleimpressao.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Value("${FRONTEND_URL}")
    private String frontendUrl;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource())) // Use this to integrate CORS
                .authorizeHttpRequests(auth -> auth
                        // Explicitly permit OPTIONS requests for all paths for CORS preflight.
                        // This should be handled by the CorsFilter registered by http.cors() above,
                        // but being explicit can sometimes help ensure it's not blocked prematurely.
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .anyRequest().permitAll()
                );
        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        // This log will now confirm execution when Spring Security requests the CORS config
        System.out.println("🚀 Creating CorsConfigurationSource with frontendUrl: " + frontendUrl);

        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of(frontendUrl));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS", "HEAD"));
        configuration.setAllowedHeaders(List.of(
                "Authorization",
                "Content-Type",
                "X-Requested-With",
                "Accept",
                "Origin", // Essential for CORS
                "Access-Control-Request-Method", // Essential for preflight
                "Access-Control-Request-Headers" // Essential for preflight
        ));
        configuration.setExposedHeaders(List.of( // Headers the browser is allowed to access
                "Access-Control-Allow-Origin",
                "Access-Control-Allow-Credentials"
        ));
        configuration.setAllowCredentials(true); // VERY IMPORTANT: Set this to true if your frontend ever sends credentials (cookies, Authorization header)
        // or if you plan to. Many SPAs do.
        configuration.setMaxAge(3600L); // How long the results of a preflight request can be cached (in seconds)

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration); // Apply this configuration to all paths

        System.out.println("✅ CORS Configuration created for origins: " + configuration.getAllowedOrigins() +
                " with credentials: " + configuration.getAllowCredentials());
        return source;
    }
}

