package com.ersamu20.aad_ra2_act1.repository;

import com.ersamu20.aad_ra2_act1.config.PostgresqlDriver;
import com.ersamu20.aad_ra2_act1.model.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Suite 1: Evaluación Completa de Repositorios JDBC (CRUD + SQL)")
public class AadTexRepositoriesTest {

    @Autowired
    private PostgresqlDriver postgresqlDriver;

    @Autowired
    private StoreRepository storeRepository;

    private static Store testStore;

    @Test
    @Order(1)
    @DisplayName("01. StoreRepository: CREATE y FIND_BY_ID")
    void testStoreCreateAndFindById() {
        String uniqueCode = "STORE_REPO_" + System.currentTimeMillis();
        Store store = Store.builder()
                .code(uniqueCode)
                .name("Tienda Repo Test")
                .country("España")
                .region("EMEA")
                .build();

        Store created = storeRepository.create(store);
        assertNotNull(created.id(), "La tienda creada debe devolver su ID primario autonumérico");
        assertEquals(uniqueCode, created.code());

        Store fetched = storeRepository.findById(created.id());
        assertNotNull(fetched, "Debe encontrar la tienda por su ID");
        assertEquals("Tienda Repo Test", fetched.name());

        testStore = created;
    }

    @Test
    @Order(2)
    @DisplayName("02. StoreRepository: FIND_ALL, UPDATE y DELETE")
    void testStoreFindAllUpdateAndDelete() {
        // FIND_ALL
        List<Store> stores = storeRepository.findAll();
        assertFalse(stores.isEmpty(), "La lista de tiendas no debe estar vacía");

        // UPDATE
        Store toUpdate = Store.builder()
                .id(testStore.id())
                .code(testStore.code())
                .name("Tienda Repo Actualizada")
                .country("Francia")
                .region("EMEA")
                .build();

        boolean updated = storeRepository.update(toUpdate);
        assertTrue(updated, "El update de la tienda debe devolver true");

        Store fetchedAfterUpdate = storeRepository.findById(testStore.id());
        assertEquals("Tienda Repo Actualizada", fetchedAfterUpdate.name());
        assertEquals("Francia", fetchedAfterUpdate.country());

        // DELETE
        Store dummyStore = storeRepository.create(Store.builder()
                .code("DUMMY_" + System.currentTimeMillis())
                .name("Tienda Borrado")
                .country("España")
                .region("EMEA")
                .build());

        boolean deleted = storeRepository.delete(dummyStore.id());
        assertTrue(deleted, "El borrado de la tienda dummy debe ser exitoso");
        assertNull(storeRepository.findById(dummyStore.id()), "La tienda borrada ya no debe existir en BD");
    }
}