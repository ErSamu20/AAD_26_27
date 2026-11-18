package com.ersamu20.aad_ra2_act1.model;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Cabecera de una venta. Se corresponde con la tabla "venta".
 * Las líneas de detalle van aparte (SaleDetail), igual que en el modelo relacional.
 */
@Builder
public record Sale(
        Integer id,                 // id_venta
        Integer storeId,            // id_tienda (FK)
        LocalDateTime saleDate,     // fecha_venta (si es null, la BD usa CURRENT_TIMESTAMP)
        BigDecimal total            // total de la venta
) {}