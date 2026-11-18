package com.ersamu20.aad_ra2_act1.model;

import lombok.Builder;

import java.math.BigDecimal;

/**
 * Línea de detalle de una venta. Se corresponde con la tabla "detalle_venta".
 * Su clave primaria es compuesta: (id_venta, id_producto).
 */
@Builder
public record SaleDetail(
        Integer saleId,           // id_venta (FK)
        Integer productId,        // id_producto (FK)
        Integer quantity,         // cantidad (> 0)
        BigDecimal unitPrice      // precio_unitario en el momento de la venta
) {}