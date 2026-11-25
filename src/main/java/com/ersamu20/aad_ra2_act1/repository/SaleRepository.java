package com.ersamu20.aad_ra2_act1.repository;

import com.ersamu20.aad_ra2_act1.config.PostgresqlDriver;
import com.ersamu20.aad_ra2_act1.model.Sale;
import com.ersamu20.aad_ra2_act1.model.SaleDetail;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Acceso a datos de las tablas "venta" (cabecera) y "detalle_venta" (líneas), más la
 * invocación de la función almacenada calcular_total_ventas_tienda.
 * Implementa el CRUD genérico sobre la cabecera; las líneas de detalle se insertan en lote con createDetails.
 */
@Repository
@RequiredArgsConstructor
public class SaleRepository implements CrudRepository<Sale, Long> {

    private final PostgresqlDriver postgresqlDriver;

    /**
     * Inserta la cabecera de la venta y devuelve la venta con el ID generado.
     * Si la venta no trae fecha, se pone la actual (equivale al DEFAULT de la columna).
     */
    @Override
    public Sale create(Sale sale) {
        // La cabecera necesita la tienda a la que pertenece (FK id_tienda)
        if (sale == null || sale.storeId() == null) {
            throw new IllegalArgumentException("La venta a crear necesita el id de la tienda");
        }

        String sql = """
                INSERT INTO venta (id_tienda, fecha_venta, total)
                VALUES (?, ?, ?)
                """;

        LocalDateTime saleDate = sale.saleDate() != null ? sale.saleDate() : LocalDateTime.now();

        try (Connection conn = postgresqlDriver.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setLong(1, sale.storeId());
            ps.setTimestamp(2, Timestamp.valueOf(saleDate));   // LocalDateTime -> Timestamp (JDBC)
            ps.setBigDecimal(3, sale.total());
            ps.executeUpdate();

            // Recuperamos el id_venta que ha generado la BD
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return Sale.builder()
                            .id(keys.getLong("id_venta"))
                            .storeId(sale.storeId())
                            .saleDate(saleDate)
                            .total(sale.total())
                            .build();
                }
            }
            throw new RuntimeException("No se obtuvo el ID generado para la venta de la tienda " + sale.storeId());

        } catch (SQLException e) {
            throw new RuntimeException("Error insertando la cabecera de la venta de la tienda " + sale.storeId(), e);
        }
    }

    /**
     * Inserta varias líneas de detalle en LOTE (batch): se acumulan con addBatch() y se envían
     * juntas con executeBatch(), lo que reduce los viajes a la base de datos.
     * El id de la venta se toma del parámetro saleId (no de cada SaleDetail).
     */
    public void createDetails(Long saleId, List<SaleDetail> details) {
        // Validamos TODO antes de abrir la conexión, para fallar pronto y con un mensaje claro
        if (saleId == null) {
            throw new IllegalArgumentException("Las líneas de detalle necesitan el id de la venta");
        }
        if (details == null) {
            throw new IllegalArgumentException("La lista de líneas de detalle no puede ser null");
        }
        for (SaleDetail detail : details) {
            if (detail == null || detail.productId() == null || detail.quantity() == null) {
                throw new IllegalArgumentException("Cada línea de detalle necesita producto y cantidad");
            }
        }

        String sql = """
                INSERT INTO detalle_venta (id_venta, id_producto, cantidad, precio_unitario)
                VALUES (?, ?, ?, ?)
                """;

        try (Connection conn = postgresqlDriver.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            for (SaleDetail detail : details) {
                ps.setLong(1, saleId);
                ps.setLong(2, detail.productId());
                ps.setInt(3, detail.quantity());
                ps.setBigDecimal(4, detail.unitPrice());
                ps.addBatch();                              // acumula esta línea en el lote
            }
            ps.executeBatch();                              // envía todo el lote; falla si alguna fila viola una restricción

        } catch (SQLException e) {
            throw new RuntimeException("Error insertando las líneas de detalle de la venta " + saleId, e);
        }
    }

    /** Devuelve la venta (solo cabecera) o null si no existe. */
    @Override
    public Sale findById(Long id) {
        // Sin id no puede existir ninguna venta (y ps.setInt(1, id) fallaría con un Integer null)
        if (id == null) {
            return null;
        }

        String sql = """
                SELECT id_venta, id_tienda, fecha_venta, total
                FROM venta
                WHERE id_venta = ?
                """;

        try (Connection conn = postgresqlDriver.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, id);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
            return null;

        } catch (SQLException e) {
            throw new RuntimeException("Error consultando la venta con id " + id, e);
        }
    }

    @Override
    public List<Sale> findAll() {
        String sql = """
                SELECT id_venta, id_tienda, fecha_venta, total
                FROM venta
                ORDER BY id_venta
                """;

        List<Sale> sales = new ArrayList<>();

        try (Connection conn = postgresqlDriver.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                sales.add(mapRow(rs));
            }
            return sales;

        } catch (SQLException e) {
            throw new RuntimeException("Error obteniendo la lista de ventas", e);
        }
    }

    /**
     * Actualiza id_tienda y total de una venta. No toca fecha_venta: la fecha de una venta no cambia
     * al corregir sus datos (y el test construye la Sale sin fecha).
     * Devuelve true si se actualizó alguna fila.
     */
    @Override
    public boolean update(Sale sale) {
        // Hace falta la venta, su id (WHERE) y la tienda (columna NOT NULL)
        if (sale == null || sale.id() == null || sale.storeId() == null) {
            throw new IllegalArgumentException("Para actualizar una venta hacen falta su id y el id de la tienda");
        }

        String sql = """
                UPDATE venta
                SET id_tienda = ?, total = ?
                WHERE id_venta = ?
                """;

        try (Connection conn = postgresqlDriver.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, sale.storeId());
            ps.setBigDecimal(2, sale.total());
            ps.setLong(3, sale.id());

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            throw new RuntimeException("Error actualizando la venta con id " + sale.id(), e);
        }
    }

    /**
     * Borra una venta. Sus líneas de detalle se borran solas (ON DELETE CASCADE en detalle_venta).
     * Devuelve false si la venta no existe (o el id es null).
     */
    @Override
    public boolean delete(Long id) {
        if (id == null) {
            return false;
        }

        String sql = "DELETE FROM venta WHERE id_venta = ?";

        try (Connection conn = postgresqlDriver.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, id);
            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            throw new RuntimeException("Error borrando la venta con id " + id, e);
        }
    }

    /**
     * Invoca la función PL/pgSQL calcular_total_ventas_tienda con un CallableStatement.
     * Sintaxis JDBC de función: { ? = call nombre(?) }
     *   - el primer ? es el valor de RETORNO (parámetro de salida, índice 1)
     *   - el segundo ? es el parámetro de entrada p_id_tienda (índice 2)
     */
    public BigDecimal calculateTotalSalesByStore(Long storeId) {
        // Sin tienda no hay ventas: 0, igual que devuelve la función para una tienda sin ventas
        if (storeId == null) {
            return BigDecimal.ZERO;
        }

        String sql = "{ ? = call calcular_total_ventas_tienda(CAST(? AS INTEGER)) }";

        try (Connection conn = postgresqlDriver.getConnection();
             CallableStatement cs = conn.prepareCall(sql)) {

            cs.registerOutParameter(1, Types.NUMERIC);     // declaramos el tipo del valor devuelto
            cs.setLong(2, storeId);
            cs.execute();

            return cs.getBigDecimal(1);                    // leemos el parámetro de salida

        } catch (SQLException e) {
            throw new RuntimeException("Error invocando calcular_total_ventas_tienda para la tienda " + storeId, e);
        }
    }

    /** Convierte la fila actual del ResultSet en una Sale (fecha_venta puede ser NULL en la BD). */
    private Sale mapRow(ResultSet rs) throws SQLException {
        Timestamp timestamp = rs.getTimestamp("fecha_venta");
        return Sale.builder()
                .id(rs.getLong("id_venta"))
                .storeId(rs.getLong("id_tienda"))
                .saleDate(timestamp != null ? timestamp.toLocalDateTime() : null)
                .total(rs.getBigDecimal("total"))
                .build();
    }
}