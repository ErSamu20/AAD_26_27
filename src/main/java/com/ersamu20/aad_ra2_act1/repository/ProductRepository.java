package com.ersamu20.aad_ra2_act1.repository;

import com.ersamu20.aad_ra2_act1.config.PostgresqlDriver;
import com.ersamu20.aad_ra2_act1.model.Product;
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
 * Acceso a datos de la tabla "producto" con JDBC puro.
 * Mismo patrón que StoreRepository: PreparedStatement + try-with-resources + mapeo de ResultSet.
 * Diferencia: el precio es NUMERIC en la BD, así que se lee y escribe como BigDecimal.
 */
@Repository
@RequiredArgsConstructor
public class ProductRepository implements CrudRepository<Product, Integer> {

    private final PostgresqlDriver postgresqlDriver;

    @Override
    public Product create(Product product) {
        String sql = """
                INSERT INTO producto (sku, nombre, precio)
                VALUES (?, ?, ?)
                """;

        try (Connection conn = postgresqlDriver.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, product.sku());
            ps.setString(2, product.name());
            ps.setBigDecimal(3, product.price());      // BigDecimal -> NUMERIC(10,2)
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return Product.builder()
                            .id(keys.getInt("id_producto"))
                            .sku(product.sku())
                            .name(product.name())
                            .price(product.price())
                            .build();
                }
            }
            throw new RuntimeException("No se obtuvo el ID generado para el producto " + product.sku());

        } catch (SQLException e) {
            throw new RuntimeException("Error insertando el producto " + product.sku(), e);
        }
    }

    @Override
    public Product findById(Integer id) {
        String sql = """
                SELECT id_producto, sku, nombre, precio
                FROM producto
                WHERE id_producto = ?
                """;

        try (Connection conn = postgresqlDriver.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
            return null;   // no existe

        } catch (SQLException e) {
            throw new RuntimeException("Error consultando el producto con id " + id, e);
        }
    }

    @Override
    public List<Product> findAll() {
        String sql = """
                SELECT id_producto, sku, nombre, precio
                FROM producto
                ORDER BY id_producto
                """;

        List<Product> products = new ArrayList<>();

        try (Connection conn = postgresqlDriver.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                products.add(mapRow(rs));
            }
            return products;

        } catch (SQLException e) {
            throw new RuntimeException("Error obteniendo la lista de productos", e);
        }
    }

    @Override
    public boolean update(Product product) {
        String sql = """
                UPDATE producto
                SET sku = ?, nombre = ?, precio = ?
                WHERE id_producto = ?
                """;

        try (Connection conn = postgresqlDriver.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, product.sku());
            ps.setString(2, product.name());
            ps.setBigDecimal(3, product.price());
            ps.setInt(4, product.id());

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            throw new RuntimeException("Error actualizando el producto con id " + product.id(), e);
        }
    }

    /**
     * Borra un producto. Si ya aparece en alguna línea de detalle_venta, la BD lo impide
     * (ON DELETE RESTRICT) y se lanza una excepción: no se puede borrar algo que tiene ventas.
     */
    @Override
    public boolean delete(Integer id) {
        String sql = "DELETE FROM producto WHERE id_producto = ?";

        try (Connection conn = postgresqlDriver.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);
            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            throw new RuntimeException("Error borrando el producto con id " + id, e);
        }
    }

    /** Convierte la fila actual del ResultSet en un Product. */
    private Product mapRow(ResultSet rs) throws SQLException {
        return Product.builder()
                .id(rs.getInt("id_producto"))
                .sku(rs.getString("sku"))
                .name(rs.getString("nombre"))
                .price(rs.getBigDecimal("precio"))     // NUMERIC -> BigDecimal exacto
                .build();
    }
}