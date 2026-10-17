package com.example.productoscrud.infrastructure.web.controllers;

import static com.example.productoscrud.infrastructure.persistence.mongo.ProductRepositoryMongo.*;

import com.example.productoscrud.application.service.ProductService;
import java.math.BigDecimal;
import java.util.Map;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/productos/nosql")
@CrossOrigin(origins = "*")
public class ProductNoSqlController extends ProductController {

    public ProductNoSqlController(@Qualifier("productServiceMongo") ProductService productService) {
        super(productService);
    }

    @GetMapping("/filtros/rango")
    public Map<String, Object> filtrarPorRango(
            @RequestParam(defaultValue = "0") BigDecimal min,
            @RequestParam(defaultValue = "10000000") BigDecimal max) {
        return Map.of(
            "filtro", "Rango de Precios (" + min + " - " + max + ")",
            "pipeline", formatearPipeline(pipelineRango(min, max)),
            "productos", productService.filtrarPorRangoPrecio(min, max)
        );
    }

    @GetMapping("/filtros/palabra")
    public Map<String, Object> filtrarPorPalabra(@RequestParam(defaultValue = "") String texto) {
        return Map.of(
            "filtro", "Búsqueda por texto: '" + texto + "'",
            "pipeline", formatearPipeline(pipelinePalabra(texto)),
            "productos", productService.filtrarPorPalabra(texto)
        );
    }

    @GetMapping("/filtros/top")
    public Map<String, Object> listarTopCaros(@RequestParam(defaultValue = "3") int limite) {
        return Map.of(
            "filtro", "Top " + limite + " productos más caros",
            "pipeline", formatearPipeline(pipelineTop(limite)),
            "productos", productService.listarTopCaros(limite)
        );
    }

    @GetMapping("/filtros/promedio")
    public Map<String, Object> filtrarMayoresAlPromedio() {
        return Map.of(
            "filtro", "Productos con precio superior al promedio general",
            "pipeline", formatearPipeline(pipelinePromedio()),
            "productos", productService.filtrarMayoresAlPromedio()
        );
    }
}
