package com.bdea.grp2.lambda.configuration;


import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**") // Allow requests for all endpoints
                .allowedOrigins("http://localhost:4200") // Allow requests from the specified origin
                .allowedMethods("GET", "POST", "PUT", "DELETE", "HEAD", "OPTIONS") // Allow specified HTTP methods
                .allowCredentials(true) // Allow cookies
                .allowedHeaders("*") // Allow all headers
                .maxAge(3600); // Cache the preflight response for 1 hour
    }
}
