package com.tms.edi;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableCaching
@EnableAsync
@EnableScheduling
@EnableRetry
@OpenAPIDefinition(
    info = @Info(
        title       = "TMS EDI Integration Platform API",
        version     = "1.0",
        description = "Enterprise EDI Integration Platform for Transport Management System",
        contact     = @Contact(name = "TMS Team", email = "edi-support@tms.com")
    )
)
@SecurityScheme(
    name   = "bearerAuth",
    type   = SecuritySchemeType.HTTP,
    scheme = "bearer",
    bearerFormat = "JWT",
    in     = SecuritySchemeIn.HEADER
)
public class TmsEdiApplication {

    public static void main(String[] args) {
        SpringApplication.run(TmsEdiApplication.class, args);
    }
}
