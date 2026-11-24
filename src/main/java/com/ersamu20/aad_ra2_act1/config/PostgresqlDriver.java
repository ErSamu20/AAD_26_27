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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;

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

    // Conexión de la transacción en curso de ESTE hilo (null si no hay transacción).
    // Lombok no la incluye en el constructor porque ya está inicializada.
    private final ThreadLocal<Connection> transactionConnection = new ThreadLocal<>();

    /**
     * Abre una conexión NUEVA cada vez que se llama.
     * Quien la pida debe cerrarla (try-with-resources) para liberar el socket y la sesión en el servidor.
     */
    /**
     * Devuelve una conexión:
     *  - Si este hilo tiene una transacción abierta, la MISMA conexión de la transacción (con close() desactivado).
     *  - Si no, una conexión nueva en modo autocommit; quien la pida debe cerrarla (try-with-resources).
     */
    public Connection getConnection() throws SQLException {
        Connection transaction = transactionConnection.get();
        if (transaction != null) {
            return withoutClose(transaction);
        }
        return openConnection();
    }

    /** Abre siempre una conexión nueva contra PostgreSQL. */
    private Connection openConnection() throws SQLException {
        return DriverManager.getConnection(
                databaseProperties.getUrl(),
                databaseProperties.getUsername(),
                databaseProperties.getPassword()
        );
    }

    /**
     * Envuelve la conexión de la transacción en un Proxy que ignora close().
     * Así los repositorios pueden seguir usando try-with-resources sin cerrar la transacción a mitad.
     * Cualquier otro método (prepareStatement, commit...) se delega en la conexión real.
     */
    private Connection withoutClose(Connection real) {
        return (Connection) Proxy.newProxyInstance(
                Connection.class.getClassLoader(),
                new Class<?>[]{Connection.class},
                (proxy, method, args) -> {
                    if ("close".equals(method.getName())) {
                        return null;                        // ignoramos el cierre
                    }
                    try {
                        return method.invoke(real, args);   // el resto, a la conexión real
                    } catch (InvocationTargetException e) {
                        throw e.getCause();                 // devolvemos la SQLException original, no la envoltura
                    }
                });
    }

    /** Inicia una transacción manual: abre una conexión, desactiva el autocommit y la asocia a este hilo. */
    public void beginTransaction() {
        if (transactionConnection.get() != null) {
            throw new IllegalStateException("Ya hay una transacción en curso en este hilo");
        }
        try {
            Connection conn = openConnection();
            conn.setAutoCommit(false);          // a partir de aquí nada se confirma hasta commit()
            transactionConnection.set(conn);
        } catch (SQLException e) {
            throw new IllegalStateException("No se pudo iniciar la transacción", e);
        }
    }

    /** Confirma la transacción (todo o nada) y libera la conexión. */
    public void commit() {
        Connection conn = transactionConnection.get();
        if (conn == null) {
            throw new IllegalStateException("No hay ninguna transacción en curso");
        }
        try {
            conn.commit();
        } catch (SQLException e) {
            throw new RuntimeException("Error haciendo commit de la transacción", e);
        } finally {
            endTransaction(conn);
        }
    }

    /**
     * Deshace la transacción y libera la conexión. Si no hay transacción (p. ej. ya terminó) no hace nada;
     * si el rollback falla solo se registra, para no tapar la excepción original que lo provocó.
     */
    public void rollback() {
        Connection conn = transactionConnection.get();
        if (conn == null) {
            return;
        }
        try {
            conn.rollback();
        } catch (SQLException e) {
            log.error("Error haciendo rollback de la transacción", e);
        } finally {
            endTransaction(conn);
        }
    }

    /** Fin de la transacción: olvida la conexión de este hilo (evita fugas) y la cierra de verdad. */
    private void endTransaction(Connection conn) {
        transactionConnection.remove();
        try {
            conn.close();
        } catch (SQLException e) {
            log.warn("Error cerrando la conexión de la transacción", e);
        }
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

            try (Connection conn = openConnection();
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