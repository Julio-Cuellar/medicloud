package com.jclinical.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Clase principal de inicio para la aplicación Medicloud.
 * <p>
 * Configura la inicialización de Spring Boot, escaneo de componentes,
 * repositorios JPA y entidades bajo el paquete base {@code com.jclinical}.
 * </p>
 */
@SpringBootApplication(scanBasePackages = "com.jclinical")
@EnableJpaRepositories(basePackages = "com.jclinical")
@EntityScan(basePackages = "com.jclinical")
public class MedicloudApplication {

    /**
     * Punto de entrada principal de la aplicación.
     *
     * @param args Argumentos de la línea de comandos pasados a la aplicación.
     */
    public static void main(String[] args) {
        SpringApplication.run(MedicloudApplication.class, args);
    }
}
