package com.mku.attendance;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@SpringBootApplication
public class MkuAttendanceApplication {
    public static void main(String[] args) {
        SpringApplication.run(MkuAttendanceApplication.class, args);
    }

    // ✅ Allow access to static images
    @Configuration
    public static class WebConfig implements WebMvcConfigurer {
        @Override
        public void addResourceHandlers(ResourceHandlerRegistry registry) {
            registry.addResourceHandler("/images/**")
                    .addResourceLocations("classpath:/static/images/");
        }
    }
}