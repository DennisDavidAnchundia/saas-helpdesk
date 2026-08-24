package com.helpdesk;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.TimeZone;

@SpringBootApplication
public class SaasHelpdeskApplication {

    public static void main(String[] args) {
        // El proyecto habla UTC en todas partes: la BD guarda hora de pared UTC
        // (columnas TIMESTAMP sin zona) y el frontend la interpreta con 'Z'.
        // Fijar la JVM evita depender del huso del host (dev UTC-5, prod suele ser UTC).
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        SpringApplication.run(SaasHelpdeskApplication.class, args);
    }
}
