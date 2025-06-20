package com.programacao_web.rpg_market.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {
    
    private static final Logger logger = LoggerFactory.getLogger(WebMvcConfig.class);
    
    @Autowired
    private FileStorageProperties fileStorageProperties;    @Override
    public void addResourceHandlers(@NonNull ResourceHandlerRegistry registry) {
        try {
            // Handle dynamic uploaded images first
            String uploadDir = fileStorageProperties.getUploadDir();
            logger.info("Upload directory from properties: {}", uploadDir);
            
            if (uploadDir != null && !uploadDir.isEmpty()) {
                Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
                logger.info("Configured resource handler for path: {}", uploadPath);
                
                // This will handle uploaded images with different pattern
                registry.addResourceHandler("/uploads/**")
                        .addResourceLocations("file:" + uploadPath.toString() + "/")
                        .setCachePeriod(3600)
                        .resourceChain(true);
            } else {
                logger.error("Upload directory is null or empty! Check your application.properties");
            }
            
            // Handle static images from resources/static/images (default Spring Boot behavior)
            // This should work automatically, but let's add it explicitly
            registry.addResourceHandler("/images/**")
                    .addResourceLocations("classpath:/static/images/")
                    .setCachePeriod(3600);
                    
        } catch (Exception e) {
            logger.error("Error configuring resource handler: {}", e.getMessage(), e);
        }
    }
}