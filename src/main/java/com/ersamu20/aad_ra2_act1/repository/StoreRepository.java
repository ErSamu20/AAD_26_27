package com.ersamu20.aad_ra2_act1.repository;

import com.ersamu20.aad_ra2_act1.config.PostgresqlDriver;
import com.ersamu20.aad_ra2_act1.model.Store;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Acceso a datos de la tabla "tienda" con JDBC puro.
 * Todas las consultas usan PreparedStatement: los valores viajan como parámetros (?),
 * nunca concatenados en el SQL, y por eso no es posible la inyección SQL.
 */
@Repository                 // Spring la registra como bean para poder inyectarla
@RequiredArgsConstructor    // Constructor con PostgresqlDriver (atributo final)
public class StoreRepository implements CrudRepository<Store, Long> {

    private final PostgresqlDriver postgresqlDriver;

    @Override
    public Store create(Store store) {
        // Validación previa: sin tienda no hay nada que insertar (evita un NullPointerException al leer sus campos)
        if (store == null) {
            throw new IllegalArgumentException("La tienda a crear no puede ser null");
        }

        // Text block ("""): SQL multilínea sin concatenar cadenas. No incluimos id_tienda: es SERIAL.
        String sql = """
                INSERT INTO tienda (codigo, nombre, pais, region)
                VALUES (?, ?, ?, ?)
                """;

        // try-with-resources: cierra automáticamente la conexión y el PreparedStatement
        try (Connection conn = postgresqlDriver.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, store.code());     // los índices de los ? empiezan en 1
            ps.setString(2, store.name());
            ps.setString(3, store.country());
            ps.setString(4, store.region());
            ps.executeUpdate();

            // RETURN_GENERATED_KEYS: pedimos a la BD el id que acaba de generar
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return Store.builder()
                            .id(keys.getLong("id_tienda"))
                            .code(store.code())
                            .name(store.name())
                            .country(store.country())
                            .region(store.region())
                            .build();
                }
            }
            throw new RuntimeException("No se obtuvo el ID generado para la tienda " + store.code());

        } catch (SQLException e) {
            throw new RuntimeException("Error insertando la tienda " + store.code(), e);
        }
    }

    @Override
    public Store findById(Long id) {
        // Sin id no puede existir ninguna tienda. Además, ps.setLong(1, id) lanzaría NullPointerException
        // al desempaquetar un Integer null.
        if (id == null) {
            return null;
        }

        String sql = """
                SELECT id_tienda, codigo, nombre, pais, region
                FROM tienda
                WHERE id_tienda = ?
                """;

        try (Connection conn = postgresqlDriver.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, id);

            try (ResultSet rs = ps.executeQuery()) {
                // El cursor empieza ANTES de la primera fila: rs.next() avanza y dice si hay fila
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
            return null;   // no existe

        } catch (SQLException e) {
            throw new RuntimeException("Error consultando la tienda con id " + id, e);
        }
    }

    @Override
    public List<Store> findAll() {
        String sql = """
                SELECT id_tienda, codigo, nombre, pais, region
                FROM tienda
                ORDER BY id_tienda
                """;

        List<Store> stores = new ArrayList<>();

        try (Connection conn = postgresqlDriver.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {            // una vuelta por cada fila del resultado
                stores.add(mapRow(rs));
            }
            return stores;

        } catch (SQLException e) {
            throw new RuntimeException("Error obteniendo la lista de tiendas", e);
        }
    }

    @Override
    public boolean update(Store store) {
        // Para actualizar hace falta la tienda y su id (se usa en el WHERE)
        if (store == null || store.id() == null) {
            throw new IllegalArgumentException("Para actualizar una tienda hace falta la tienda y su id");
        }

        String sql = """
                UPDATE tienda
                SET codigo = ?, nombre = ?, pais = ?, region = ?
                WHERE id_tienda = ?
                """;

        try (Connection conn = postgresqlDriver.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, store.code());
            ps.setString(2, store.name());
            ps.setString(3, store.country());
            ps.setString(4, store.region());
            ps.setLong(5, store.id());

            // executeUpdate devuelve el número de filas afectadas
            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            throw new RuntimeException("Error actualizando la tienda con id " + store.id(), e);
        }
    }

    @Override
    public boolean delete(Long id) {
        // Sin id no hay nada que borrar: se indica con false en vez de lanzar un NullPointerException
        if (id == null) {
            return false;
        }

        String sql = "DELETE FROM tienda WHERE id_tienda = ?";

        try (Connection conn = postgresqlDriver.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, id);
            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            throw new RuntimeException("Error borrando la tienda con id " + id, e);
        }
    }

    /** Convierte la fila actual del ResultSet en un Store. Evita repetir código en findById y findAll. */
    private Store mapRow(ResultSet rs) throws SQLException {
        return Store.builder()
                .id(rs.getLong("id_tienda"))
                .code(rs.getString("codigo"))
                .name(rs.getString("nombre"))
                .country(rs.getString("pais"))
                .region(rs.getString("region"))
                .build();
    }
}