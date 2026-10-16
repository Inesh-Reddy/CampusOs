package com.campusos.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI campusOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("CampusOS API")
                        .version("1.0.0")
                        .description("Issue reporting, resource booking, and operational analytics for CampusOS. "
                                + "Authentication uses a server-side session cookie (JSESSIONID) issued by POST /api/v1/auth/login. "
                                + "Mutating requests (except login) must also send the CSRF token from the XSRF-TOKEN cookie "
                                + "as the X-XSRF-TOKEN header. There is no bearer-token flow."))
                .servers(List.of(
                        new Server().url("http://127.0.0.1:8080").description("Local (Docker or jar)"),
                        new Server().url("https://campus.example.edu").description("Production (placeholder — update before deployment)")))
                .components(new Components()
                        .addSecuritySchemes("sessionCookie", new SecurityScheme()
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.COOKIE)
                                .name("JSESSIONID")
                                .description("Server-side session cookie returned by POST /api/v1/auth/login."))
                        .addSecuritySchemes("csrfToken", new SecurityScheme()
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.HEADER)
                                .name("X-XSRF-TOKEN")
                                .description("Value of the XSRF-TOKEN cookie set on responses. Required on all mutating requests except login.")))
                .addSecurityItem(new SecurityRequirement()
                        .addList("sessionCookie")
                        .addList("csrfToken"));
    }
}
