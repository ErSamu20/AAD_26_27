package com.ersamu20.aad_ra2_act1.service;

import com.ersamu20.aad_ra2_act1.model.Product;
import com.ersamu20.aad_ra2_act1.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Capa de servicio del catálogo de productos. Delega en ProductRepository.
 */
@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;

    public Product createProduct(Product product) {
        return productRepository.create(product);
    }

    /** Devuelve el producto o null si no existe. */
    public Product getProductById(Long id) {
        return productRepository.findById(id);
    }

    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    public boolean updateProduct(Product product) {
        return productRepository.update(product);
    }

    public boolean deleteProduct(Long id) {
        return productRepository.delete(id);
    }
}