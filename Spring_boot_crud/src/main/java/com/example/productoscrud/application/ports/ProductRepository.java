package com.example.productoscrud.application.ports;

import com.example.productoscrud.domain.model.Product;
import java.util.List;
import java.util.Optional;

public abstract class ProductRepository {
    public abstract List<Product> listar();
    public abstract Optional<Product> buscarPorId(int id);
    public abstract Product crear(Product product);
    public abstract boolean actualizar(int id, Product product);
    public abstract boolean eliminar(int id);

    // Métodos para consultas avanzadas (sobrescritos en persistencia NoSQL Mongo)
    public List<Product> filtrarPorRangoPrecio(java.math.BigDecimal min, java.math.BigDecimal max) { return List.of(); }
    public List<Product> filtrarPorPalabra(String texto) { return List.of(); }
    public List<Product> listarTopCaros(int limite) { return List.of(); }
    public List<Product> filtrarMayoresAlPromedio() { return List.of(); }
}

// Origen: ProductService (com.example.productoscrud.application.service) llama a este contrato.
// Destino: Java ejecuta los métodos sobrescritos en ProductRepositoryMySQL
// (com.example.productoscrud.infrastructure.persistence.mysql), que extiende esta clase.
