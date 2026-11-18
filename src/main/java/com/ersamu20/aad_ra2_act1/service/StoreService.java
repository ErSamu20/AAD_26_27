package com.ersamu20.aad_ra2_act1.service;

import com.ersamu20.aad_ra2_act1.model.Store;
import com.ersamu20.aad_ra2_act1.repository.StoreRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Capa de servicio de tiendas. Es el punto de entrada para la lógica de negocio sobre tiendas:
 * la aplicación habla con el servicio y el servicio habla con el repositorio.
 * Por ahora delega en StoreRepository; si hubiese reglas de negocio, irían aquí.
 */
@Service                     // Spring la registra como bean de la capa de servicio
@RequiredArgsConstructor     // Inyecta StoreRepository por constructor
public class StoreService {

    private final StoreRepository storeRepository;

    public Store createStore(Store store) {
        return storeRepository.create(store);
    }

    /** Devuelve la tienda o null si no existe. */
    public Store getStoreById(Integer id) {
        return storeRepository.findById(id);
    }

    public List<Store> getAllStores() {
        return storeRepository.findAll();
    }

    public boolean updateStore(Store store) {
        return storeRepository.update(store);
    }

    public boolean deleteStore(Integer id) {
        return storeRepository.delete(id);
    }
}