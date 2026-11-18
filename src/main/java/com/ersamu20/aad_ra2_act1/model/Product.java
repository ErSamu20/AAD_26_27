package com.ersamu20.aad_ra2_act1.model;

import lombok.Builder;

import java.math.BigDecimal;

/**
 * Producto del catálogo. Se corresponde con la tabla "producto".
 * El precio es BigDecimal (no double) porque trabajamos con dinero y necesitamos decimales exactos.
 */
@Builder
public record Product(
        Integer id,          // id_producto
        String sku,          // sku (único)
        String name,         // nombre
        BigDecimal price     // precio (> 0)
) {}