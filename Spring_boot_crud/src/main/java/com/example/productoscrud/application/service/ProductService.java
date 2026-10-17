package com.example.productoscrud.application.service;

import com.example.productoscrud.application.ports.ProductRepository;
import com.example.productoscrud.domain.model.Product;
import java.util.List;
import java.util.Optional;

public class ProductService {
    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public List<Product> listar() {
        return productRepository.listar();
    }

    public Optional<Product> buscarPorId(int id) {
        return productRepository.buscarPorId(id);
    }

    public Product crear(Product product) {
        return productRepository.crear(product);
    }

    public Optional<Product> actualizar(int id, Product product) {
        if (!productRepository.actualizar(id, product)) {
            return Optional.empty();
        }
        return productRepository.buscarPorId(id);
    }

    public boolean eliminar(int id) {
        return productRepository.eliminar(id);
    }

    public List<Product> filtrarPorRangoPrecio(java.math.BigDecimal min, java.math.BigDecimal max) {
        return productRepository.filtrarPorRangoPrecio(min, max);
    }

    public List<Product> filtrarPorPalabra(String texto) {
        return productRepository.filtrarPorPalabra(texto);
    }

    public List<Product> listarTopCaros(int limite) {
        return productRepository.listarTopCaros(limite);
    }

    public List<Product> filtrarMayoresAlPromedio() {
        return productRepository.filtrarMayoresAlPromedio();
    }
}

// Origen: ProductController (com.example.productoscrud.infrastructure.web.controllers)
// llama a los métodos de ProductService (com.example.productoscrud.application.service).
// Destino: el servicio llama a ProductRepository (com.example.productoscrud.application.ports)
// y utiliza Product (com.example.productoscrud.domain.model) sin depender de MySQL.
