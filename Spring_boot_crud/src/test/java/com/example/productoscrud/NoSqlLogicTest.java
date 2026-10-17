package com.example.productoscrud;

import com.example.productoscrud.application.ports.ProductRepository;
import com.example.productoscrud.application.service.ProductService;
import com.example.productoscrud.domain.model.Product;
import com.example.productoscrud.infrastructure.web.controllers.ProductNoSqlController;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

public class NoSqlLogicTest {

    static class MockNoSqlRepository extends ProductRepository {
        @Override public List<Product> listar() { return List.of(new Product(1, "Teclado", new BigDecimal("100.00"), "Gamer")); }
        @Override public Optional<Product> buscarPorId(int id) { return Optional.empty(); }
        @Override public Product crear(Product product) { return product; }
        @Override public boolean actualizar(int id, Product product) { return true; }
        @Override public boolean eliminar(int id) { return true; }

        @Override public List<Product> filtrarPorRangoPrecio(BigDecimal min, BigDecimal max) { return listar(); }
        @Override public List<Product> filtrarPorPalabra(String texto) { return listar(); }
        @Override public List<Product> listarTopCaros(int limite) { return listar(); }
        @Override public List<Product> filtrarMayoresAlPromedio() { return listar(); }
    }

    @Test
    void testProductValidationAndNullDescription() {
        Product validoSinDesc = new Product(1, "Monitor", new BigDecimal("500.00"), null);
        Assertions.assertEquals("", validoSinDesc.getDescription());
        Assertions.assertTrue(validoSinDesc.esValido());

        validoSinDesc.setDescription(null);
        Assertions.assertEquals("", validoSinDesc.getDescription());
        Assertions.assertTrue(validoSinDesc.esValido());

        Product precioExcesivo = new Product(2, "Servidor", new BigDecimal("99999999999.99"), "11 enteros");
        Assertions.assertFalse(precioExcesivo.esValido());

        Product demasiadosDecimales = new Product(3, "Cable", new BigDecimal("10.999"), "3 decimales");
        Assertions.assertFalse(demasiadosDecimales.esValido());
    }

    @Test
    void testControllerRejectsInvalidProductWith400() {
        ProductService service = new ProductService(new MockNoSqlRepository());
        ProductNoSqlController controller = new ProductNoSqlController(service);

        Product invalido = new Product(null, "", new BigDecimal("-10"), "Inválido");
        Assertions.assertEquals(HttpStatus.BAD_REQUEST, controller.crear(invalido).getStatusCode());
        Assertions.assertEquals(HttpStatus.BAD_REQUEST, controller.actualizar(1, invalido).getStatusCode());
    }

    @Test
    void testNoSqlPipelinesAndController() {
        ProductService service = new ProductService(new MockNoSqlRepository());
        ProductNoSqlController controller = new ProductNoSqlController(service);

        Map<String, Object> rango = controller.filtrarPorRango(new BigDecimal("50"), new BigDecimal("200"));
        Assertions.assertTrue(rango.get("pipeline").toString().contains("$match"));
        Assertions.assertTrue(rango.get("pipeline").toString().contains("$gte"));

        Map<String, Object> palabra = controller.filtrarPorPalabra("Gamer");
        Assertions.assertTrue(palabra.get("pipeline").toString().contains("$regex"));

        Map<String, Object> top = controller.listarTopCaros(3);
        Assertions.assertTrue(top.get("pipeline").toString().contains("$limit"));

        Map<String, Object> promedio = controller.filtrarMayoresAlPromedio();
        Assertions.assertTrue(promedio.get("pipeline").toString().contains("$avg"));
        Assertions.assertTrue(promedio.get("pipeline").toString().contains("$group"));
    }
}
