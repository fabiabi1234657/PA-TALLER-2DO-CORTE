-- Ejecutar TODO este archivo en MySQL Workbench, conectado a MySQL 8.0.16 o superior.
-- No borra productos existentes. Los ejemplos solo se insertan si la tabla está vacía.
CREATE DATABASE IF NOT EXISTS productoscruddb CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE productoscruddb;

CREATE TABLE IF NOT EXISTS products (
    id INT NOT NULL AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    price DECIMAL(12,2) NOT NULL,
    description VARCHAR(255) NOT NULL DEFAULT '',
    PRIMARY KEY (id),
    CONSTRAINT precio_no_negativo CHECK (price >= 0)
) ENGINE=InnoDB;

START TRANSACTION;
SET @tabla_vacia = (SELECT COUNT(*) = 0 FROM products);
INSERT INTO products (name, price, description)
SELECT 'Portátil', 2500000.00, 'Equipo de ejemplo para programación' WHERE @tabla_vacia;
INSERT INTO products (name, price, description)
SELECT 'Mouse', 45000.00, 'Mouse USB' WHERE @tabla_vacia;
INSERT INTO products (name, price, description)
SELECT 'Teclado', 85000.00, 'Teclado para el laboratorio' WHERE @tabla_vacia;
COMMIT;

SELECT * FROM products ORDER BY id;
-- Ruta: dbs/productoscruddb.sql. Se ejecuta manualmente completo desde MySQL Workbench.
