package com.ersamu20.aad_ra2_act1.service;

import com.ersamu20.aad_ra2_act1.config.PostgresqlDriver;
import com.ersamu20.aad_ra2_act1.model.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Suite 2: Evaluación Completa de Lógica de Negocio y Transacciones ACID")
public class AadTexServicesTest {

    @Autowired
    private PostgresqlDriver postgresqlDriver;

    @Autowired
    private StoreService storeService;

    @Autowired
    private ProductService productService;

    private static Store activeStore;
    private static Product activeProduct1;
    private static Product activeProduct2;

    @Test
    @Order(1)
    @DisplayName("01. StoreService: Métodos de Negocio y Gestión de Tiendas")
    void testStoreServiceFullBusinessLogic() {
        String code = "SVC_STORE_" + System.currentTimeMillis();
        Store store = Store.builder()
                .code(code)
                .name("Zara Sol Service")
                .country("España")
                .region("EMEA")
                .build();

        // Crear
        Store created = storeService.createStore(store);
        assertNotNull(created.id());

        // Obtener por ID
        Store fetched = storeService.getStoreById(created.id());
        assertEquals("Zara Sol Service", fetched.name());

        // Listar todas
        List<Store> all = storeService.getAllStores();
        assertTrue(all.stream().anyMatch(s -> s.id().equals(created.id())));

        // Actualizar
        Store updatedData = Store.builder()
                .id(created.id())
                .code(code)
                .name("Zara Sol Service Renovada")
                .country("España")
                .region("EMEA")
                .build();
        boolean updated = storeService.updateStore(updatedData);
        assertTrue(updated);

        activeStore = storeService.getStoreById(created.id());
        assertEquals("Zara Sol Service Renovada", activeStore.name());
    }

    @Test
    @Order(2)
    @DisplayName("02. ProductService: Métodos de Negocio y Catálogo de Productos")
    void testProductServiceFullBusinessLogic() {
        String sku1 = "SKU-SVC-1-" + System.currentTimeMillis();
        String sku2 = "SKU-SVC-2-" + System.currentTimeMillis();

        Product p1 = productService.createProduct(Product.builder()
                .sku(sku1)
                .name("Abrigo Lana")
                .price(new BigDecimal("120.00"))
                .build());

        Product p2 = productService.createProduct(Product.builder()
                .sku(sku2)
                .name("Bufanda Sostenible")
                .price(new BigDecimal("25.00"))
                .build());

        assertNotNull(p1.id());
        assertNotNull(p2.id());

        List<Product> catalog = productService.getAllProducts();
        assertTrue(catalog.size() >= 2);

        activeProduct1 = p1;
        activeProduct2 = p2;
    }
}