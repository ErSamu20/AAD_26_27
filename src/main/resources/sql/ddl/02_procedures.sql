CREATE OR REPLACE FUNCTION calcular_total_ventas_tienda(p_id_tienda INT)
RETURNS NUMERIC AS $$
DECLARE
v_total NUMERIC(12, 2);
BEGIN
SELECT COALESCE(SUM(total), 0.00) INTO v_total
FROM venta
WHERE id_tienda = p_id_tienda;

RETURN v_total;
END;
$$ LANGUAGE plpgsql;