package com.ersamu20.aad_ra2_act1.repository;

import java.util.List;

/**
 * Contrato CRUD genérico para cualquier entidad.
 * T  = tipo de la entidad (Store, Product...)
 * ID = tipo de su clave primaria (Integer)
 * Los tests fijan estas firmas: findById devuelve null (no Optional), update y delete devuelven boolean.
 */
public interface CrudRepository<T, ID> {

    /** Inserta la entidad y la devuelve con el ID que ha generado la base de datos. */
    T create(T entity);

    /** Devuelve la entidad o null si no existe. */
    T findById(ID id);

    List<T> findAll();

    /** true si se actualizó alguna fila. */
    boolean update(T entity);

    /** true si se borró alguna fila. */
    boolean delete(ID id);
}