package com.campusos.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String frontendDir = Path.of("frontend").toAbsolutePath().toUri().toString();
        String parentFrontend = Path.of("../frontend").toAbsolutePath().toUri().toString();
        registry.addResourceHandler("/css/**").addResourceLocations(frontendDir + "css/", parentFrontend + "css/", "classpath:/static/css/");
        registry.addResourceHandler("/js/**").addResourceLocations(frontendDir + "js/", parentFrontend + "js/", "classpath:/static/js/");
        registry.addResourceHandler("/assets/**").addResourceLocations(frontendDir + "assets/", parentFrontend + "assets/", "classpath:/static/assets/");
        registry.addResourceHandler("/*.html", "/").addResourceLocations(frontendDir, parentFrontend, "classpath:/static/");
    }
}
