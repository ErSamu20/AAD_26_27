package com.ersamu20.aad_ra2_act1.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Gestor manual de conexiones JDBC contra PostgreSQL.
 * Responsabilidades (por ahora): abrir conexiones y ejecutar los scripts SQL al arrancar.
 */
@Slf4j                      // Lombok: crea el objeto "log" para escribir trazas
@Component
@RequiredArgsConstructor    // Lombok: constructor con los atributos final -> Spring inyecta DatabaseProperties
public class PostgresqlDriver {

    // Rutas dentro de src/main/resources. El orden importa: primero las tablas, después la función.
    private static final String[] SCRIPTS = {
            "sql/ddl/01_schema.sql",
            "sql/ddl/02_procedures.sql"
    };

    private final DatabaseProperties databaseProperties;

    /**
     * Abre una conexión NUEVA cada vez que se llama.
     * Quien la pida debe cerrarla (try-with-resources) para liberar el socket y la sesión en el servidor.
     */
    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(
                databaseProperties.getUrl(),
                databaseProperties.getUsername(),
                databaseProperties.getPassword()
        );
    }

    /**
     * @PostConstruct: Spring ejecuta este método automáticamente justo después de crear el bean
     * e inyectar sus dependencias. Así el esquema existe antes de que arranque la aplicación o los tests.
     */
    @PostConstruct
    public void initDatabase() {
        for (String script : SCRIPTS) {
            executeScript(script);
        }
        log.info("Base de datos inicializada con {} scripts", SCRIPTS.length);
    }

    /**
     * Lee el fichero COMPLETO y lo ejecuta de una sola vez.
     * No se puede partir por ';' porque la función PL/pgSQL tiene ';' dentro del bloque $$ ... $$.
     * El driver de PostgreSQL sabe separar las sentencias correctamente.
     */
    private void executeScript(String path) {
        try (InputStream in = new ClassPathResource(path).getInputStream()) {
            String sql = new String(in.readAllBytes(), StandardCharsets.UTF_8);

            try (Connection conn = getConnection();
                 Statement st = conn.createStatement()) {
                st.execute(sql);
            }
            log.info("Script ejecutado: {}", path);

        } catch (IOException | SQLException e) {
            // Fail fast: si la base no está lista, la aplicación no debe arrancar a medias
            throw new IllegalStateException("Error ejecutando el script SQL: " + path, e);
        }
    }
}