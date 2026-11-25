package com.ersamu20.aad_ra2_act1.model;

import lombok.Builder;

/**
 * Tienda de la multinacional. Se corresponde con la tabla "tienda".
 * Un record es inmutable: no tiene setters y sus atributos son final.
 * Java genera solo el constructor, los accesores (store.code()), equals, hashCode y toString.
 * @Builder (Lombok) añade Store.builder()...build() para construir objetos de forma legible.
 */
@Builder
public record Store(
        Long id,        // id_tienda (null hasta que la BD lo genera)
        String code,       // codigo
        String name,       // nombre
        String country,    // pais
        String region      // region
) {}