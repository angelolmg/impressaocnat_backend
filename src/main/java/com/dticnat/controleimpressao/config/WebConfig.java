package com.dticnat.controleimpressao.config;

import com.dticnat.controleimpressao.interceptor.AuthInterceptor;
import org.springframework.beans.factory.annotation.Value;




import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.context.annotation.Configuration;


@Configuration
@EnableWebMvc
public class WebConfig implements WebMvcConfigurer {

    @Value("${FRONTEND_URL}")
    private String frontendUrl;

    private final AuthInterceptor authInterceptor;

    public WebConfig(AuthInterceptor authInterceptor) {
        this.authInterceptor = authInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor).addPathPatterns("/**").excludePathPatterns("/docs/**", "/v3/api-docs/**");
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins(frontendUrl)
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS", "HEAD", "TRACE", "CONNECT");
    }

}