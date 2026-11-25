package com.ersamu20.aad_ra2_act1.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Recoge el bloque "database:" de application.yml y lo guarda en atributos Java.
 * Así evitamos escribir la URL, el usuario y la contraseña directamente en el código.
 */
@Data                                              // Lombok: genera getters, setters, toString, equals y hashCode
@Component                                         // Spring crea una única instancia y permite inyectarla
@ConfigurationProperties(prefix = "database")      // Enlaza cada clave "database.xxx" con el atributo xxx
public class DatabaseProperties {

    private String url;
    private String username;
    private String password;
    private String driverClassName;                // Spring convierte "driver-class-name" (yml) en driverClassName
}