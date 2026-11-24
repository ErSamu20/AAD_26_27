package com.ersamu20.aad_ra2_act1.service;

import com.ersamu20.aad_ra2_act1.config.PostgresqlDriver;
import com.ersamu20.aad_ra2_act1.model.Sale;
import com.ersamu20.aad_ra2_act1.model.SaleDetail;
import com.ersamu20.aad_ra2_act1.repository.SaleRepository;
import com.ersamu20.aad_ra2_act1.repository.StoreRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Lógica de negocio de las ventas. Coordina la transacción: registrar una venta implica varias
 * escrituras (cabecera + líneas) que deben ocurrir TODAS o NINGUNA (atomicidad, la A de ACID).
 */
@Service
@RequiredArgsConstructor
public class SaleService {

    private final PostgresqlDriver postgresqlDriver;
    private final StoreRepository storeRepository;
    private final SaleRepository saleRepository;

    /**
     * Registra una venta completa (cabecera + líneas) en una única transacción.
     * Si algo falla, se deshace todo con rollback y se lanza SaleException.
     */
    public Sale processSale(Long storeId, List<SaleDetail> details) {
        // 1. Validaciones de los datos: se hacen ANTES de abrir la transacción, no cuesta nada fallar pronto
        validateSale(storeId, details);
        BigDecimal total = calculateTotal(details);

        // 2. Iniciar la transacción manual (autocommit desactivado)
        postgresqlDriver.beginTransaction();
        try {
            // 3. Validar que la tienda existe
            if (storeRepository.findById(storeId) == null) {
                throw new SaleException("La tienda " + storeId + " no existe");
            }

            // 4. Registrar la cabecera (aún sin confirmar)
            Sale header = saleRepository.create(Sale.builder()
                    .storeId(storeId)
                    .total(total)
                    .build());

            // 5. Insertar las líneas de detalle en lote; si alguna viola una FK o un CHECK, lanza excepción
            saleRepository.createDetails(header.id(), details);

            // 6. Todo correcto: confirmar
            postgresqlDriver.commit();
            return header;

        } catch (RuntimeException e) {
            // 7. Cualquier fallo: deshacer INMEDIATAMENTE, incluida la cabecera que ya se había insertado
            postgresqlDriver.rollback();
            if (e instanceof SaleException) {
                throw e;                      // ya es una excepción de negocio con mensaje claro
            }
            throw new SaleException("No se pudo registrar la venta; se ha hecho rollback", e);
        }
    }

    /** Total acumulado de ventas de una tienda, calculado por la función PL/pgSQL de la base de datos. */
    public BigDecimal getTotalSalesByStore(Long storeId) {
        return saleRepository.calculateTotalSalesByStore(storeId);
    }

    /** Reglas de validación de la venta. Lanza SaleException con un mensaje claro si algo no cumple. */
    private void validateSale(Long storeId, List<SaleDetail> details) {
        if (storeId == null) {
            throw new SaleException("La venta necesita el id de la tienda");
        }
        if (details == null || details.isEmpty()) {
            throw new SaleException("La venta necesita al menos una línea de detalle");
        }
        for (SaleDetail detail : details) {
            if (detail == null || detail.productId() == null) {
                throw new SaleException("Cada línea de detalle necesita el id del producto");
            }
            if (detail.quantity() == null || detail.quantity() <= 0) {
                throw new SaleException("La cantidad de cada línea debe ser mayor que 0");
            }
            if (detail.unitPrice() == null || detail.unitPrice().compareTo(BigDecimal.ZERO) <= 0) {
                throw new SaleException("El precio unitario de cada línea debe ser mayor que 0");
            }
        }
    }

    /** Suma cantidad × precio unitario de todas las líneas, con 2 decimales (columna NUMERIC(12,2)). */
    private BigDecimal calculateTotal(List<SaleDetail> details) {
        BigDecimal total = BigDecimal.ZERO;
        for (SaleDetail detail : details) {
            total = total.add(detail.unitPrice().multiply(BigDecimal.valueOf(detail.quantity())));
        }
        return total.setScale(2, RoundingMode.HALF_UP);
    }
}