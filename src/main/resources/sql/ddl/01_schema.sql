CREATE TABLE IF NOT EXISTS tienda (
    id_tienda SERIAL PRIMARY KEY,
    codigo VARCHAR(50) UNIQUE NOT NULL,
    nombre VARCHAR(100) NOT NULL,
    pais VARCHAR(50) NOT NULL,
    region VARCHAR(50) NOT NULL
    );

CREATE TABLE IF NOT EXISTS producto (
    id_producto SERIAL PRIMARY KEY,
    sku VARCHAR(40) UNIQUE NOT NULL,
    nombre VARCHAR(100) NOT NULL,
    precio NUMERIC(10, 2) NOT NULL CHECK (precio > 0)
    );

CREATE TABLE IF NOT EXISTS venta (
    id_venta SERIAL PRIMARY KEY,
    id_tienda INT NOT NULL REFERENCES tienda(id_tienda) ON DELETE CASCADE,
    fecha_venta TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    total NUMERIC(12, 2) NOT NULL CHECK (total >= 0)
    );

CREATE TABLE IF NOT EXISTS detalle_venta (
    id_venta INT NOT NULL REFERENCES venta(id_venta) ON DELETE CASCADE,
    id_producto INT NOT NULL REFERENCES producto(id_producto) ON DELETE RESTRICT,
    cantidad INT NOT NULL CHECK (cantidad > 0),
    precio_unitario NUMERIC(10, 2) NOT NULL CHECK (precio_unitario > 0),
    PRIMARY KEY (id_venta, id_producto)
    );