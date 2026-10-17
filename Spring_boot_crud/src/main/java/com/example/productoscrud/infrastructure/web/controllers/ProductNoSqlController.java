package com.example.productoscrud.infrastructure.web.controllers;

import com.example.productoscrud.application.service.ProductService;
import com.example.productoscrud.domain.model.Product;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/productos/nosql")
@CrossOrigin(origins = "*")
public class ProductNoSqlController {
    private final ProductService productService;

    public ProductNoSqlController(@Qualifier("productServiceMongo") ProductService productService) {
        this.productService = productService;
    }

    @GetMapping({"", "/"})
    public List<Product> listar() {
        return productService.listar();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Product> buscarPorId(@PathVariable int id) {
        return productService.buscarPorId(id)
            .map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping({"", "/"})
    public ResponseEntity<?> crear(@RequestBody Product product) {
        if (!product.esValido()) {
            return ResponseEntity.badRequest().body("Nombre, precio o descripción inválidos");
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(productService.crear(product));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> actualizar(@PathVariable int id, @RequestBody Product product) {
        if (!product.esValido()) {
            return ResponseEntity.badRequest().body("Nombre, precio o descripción inválidos");
        }
        return productService.actualizar(id, product)
            .map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable int id) {
        return productService.eliminar(id)
            ? ResponseEntity.noContent().build()
            : ResponseEntity.notFound().build();
    }

    // 1. Pipeline Rango de Precios
    @GetMapping("/filtros/rango")
    public Map<String, Object> filtrarPorRango(
            @RequestParam(defaultValue = "0") BigDecimal min,
            @RequestParam(defaultValue = "10000000") BigDecimal max) {
        String pipeline = """
            [
              { "$match": { "price": { "$gte": %s, "$lte": %s } } },
              { "$sort": { "price": 1 } }
            ]""".formatted(min, max);
        return Map.of(
            "filtro", "Rango de Precios (" + min + " - " + max + ")",
            "pipeline", pipeline.trim(),
            "productos", productService.filtrarPorRangoPrecio(min, max)
        );
    }

    // 2. Pipeline Búsqueda por Texto
    @GetMapping("/filtros/palabra")
    public Map<String, Object> filtrarPorPalabra(@RequestParam(defaultValue = "") String texto) {
        String pipeline = """
            [
              {
                "$match": {
                  "$or": [
                    { "name": { "$regex": "%s", "$options": "i" } },
                    { "description": { "$regex": "%s", "$options": "i" } }
                  ]
                }
              },
              { "$sort": { "name": 1 } }
            ]""".formatted(texto, texto);
        return Map.of(
            "filtro", "Búsqueda por texto: '" + texto + "'",
            "pipeline", pipeline.trim(),
            "productos", productService.filtrarPorPalabra(texto)
        );
    }

    // 3. Pipeline Top N Productos Más Caros
    @GetMapping("/filtros/top")
    public Map<String, Object> listarTopCaros(@RequestParam(defaultValue = "3") int limite) {
        String pipeline = """
            [
              { "$sort": { "price": -1 } },
              { "$limit": %d }
            ]""".formatted(limite);
        return Map.of(
            "filtro", "Top " + limite + " productos más caros",
            "pipeline", pipeline.trim(),
            "productos", productService.listarTopCaros(limite)
        );
    }

    // 4. Pipeline Precios Mayores al Promedio
    @GetMapping("/filtros/promedio")
    public Map<String, Object> filtrarMayoresAlPromedio() {
        String pipeline = """
            [
              {
                "$group": {
                  "_id": null,
                  "avgPrice": { "$avg": "$price" },
                  "docs": { "$push": "$$ROOT" }
                }
              },
              { "$unwind": "$docs" },
              {
                "$match": {
                  "$expr": { "$gte": ["$docs.price", "$avgPrice"] }
                }
              },
              { "$replaceRoot": { "newRoot": "$docs" } },
              { "$sort": { "price": -1 } }
            ]""";
        return Map.of(
            "filtro", "Productos con precio superior al promedio general",
            "pipeline", pipeline.trim(),
            "productos", productService.filtrarMayoresAlPromedio()
        );
    }
}
