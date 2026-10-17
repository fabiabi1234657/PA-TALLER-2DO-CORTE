package com.example.productoscrud.infrastructure.persistence.mysql;

import com.example.productoscrud.application.ports.ProductRepository;
import com.example.productoscrud.domain.model.Product;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.stereotype.Repository;

@Repository("productRepositoryMySQL")
public class ProductRepositoryMySQL extends ProductRepository {
    private final JdbcTemplate jdbcTemplate;
    private final SimpleJdbcInsert insert;

    private final RowMapper<Product> mapear = (fila, numero) -> new Product(
        fila.getInt("id"),
        fila.getString("name"),
        fila.getBigDecimal("price"),
        fila.getString("description")
    );

    public ProductRepositoryMySQL(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.insert = new SimpleJdbcInsert(jdbcTemplate)
            .withTableName("products")
            .usingGeneratedKeyColumns("id");
    }

    @Override
    public List<Product> listar() {
        return jdbcTemplate.query(
            "SELECT id, name, price, description FROM products ORDER BY id", mapear);
    }

    @Override
    public Optional<Product> buscarPorId(int id) {
        return jdbcTemplate.query(
            "SELECT id, name, price, description FROM products WHERE id = ?", mapear, id)
            .stream().findFirst();
    }

    @Override
    public Product crear(Product product) {
        var datos = new MapSqlParameterSource()
            .addValue("name", product.getName())
            .addValue("price", product.getPrice())
            .addValue("description", product.getDescription());
        int id = insert.executeAndReturnKey(datos).intValue();
        return new Product(id, product.getName(), product.getPrice(), product.getDescription());
    }

    @Override
    public boolean actualizar(int id, Product product) {
        return jdbcTemplate.update(
            "UPDATE products SET name = ?, price = ?, description = ? WHERE id = ?",
            product.getName(), product.getPrice(), product.getDescription(), id) > 0;
    }

    @Override
    public boolean eliminar(int id) {
        return jdbcTemplate.update("DELETE FROM products WHERE id = ?", id) > 0;
    }
}

// Origen: ProductService (com.example.productoscrud.application.service) llama a estos
// métodos mediante ProductRepository (com.example.productoscrud.application.ports).
// Destino: ProductRepositoryMySQL (com.example.productoscrud.infrastructure.persistence.mysql)
// usa JdbcTemplate y SimpleJdbcInsert (org.springframework.jdbc.core) para consultar MySQL.
