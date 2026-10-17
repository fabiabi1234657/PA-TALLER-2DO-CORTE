package com.example.productoscrud.domain.model;

import java.math.BigDecimal;

public class Product {
    private Integer id;
    private String name;
    private BigDecimal price;
    private String description = "";

    public Product() {
    }

    public Product(Integer id, String name, BigDecimal price, String description) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.description = description;
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public boolean esValido() {
        return name != null && !name.isBlank() && name.length() <= 100
            && price != null && price.signum() >= 0
            && description != null && description.length() <= 255;
    }
}

// Origen: ProductController (infrastructure.web.controllers) recibe Product desde JSON;
// ProductRepositoryMySQL (infrastructure.persistence.mysql) lo crea al leer MySQL.
// Destino: Product.esValido() comprueba sus propios datos; este modelo no llama a infraestructura.
