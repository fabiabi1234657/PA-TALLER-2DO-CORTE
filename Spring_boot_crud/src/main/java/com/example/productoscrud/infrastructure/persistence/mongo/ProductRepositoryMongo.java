package com.example.productoscrud.infrastructure.persistence.mongo;

import com.example.productoscrud.application.ports.ProductRepository;
import com.example.productoscrud.domain.model.Product;
import jakarta.annotation.PostConstruct;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import org.bson.Document;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

@Repository("productRepositoryMongo")
public class ProductRepositoryMongo extends ProductRepository {
    private static final String COLLECTION = "products";
    private final MongoTemplate mongoTemplate;

    public ProductRepositoryMongo(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @PostConstruct
    public void inicializarSiVacio() {
        // shortcut: si Mongo no responde o está apagado durante el arranque, no abortar Spring
        try {
            if (mongoTemplate.collectionExists(COLLECTION) && mongoTemplate.getCollection(COLLECTION).countDocuments() > 0) {
                return;
            }
            crear(new Product(null, "Portátil Gamer NoSQL", new BigDecimal("3500000.00"), "Equipo de alto rendimiento"));
            crear(new Product(null, "Mouse Ergonómico NoSQL", new BigDecimal("65000.00"), "Mouse inalámbrico óptico"));
            crear(new Product(null, "Teclado Mecánico NoSQL", new BigDecimal("120000.00"), "Teclado switch blue"));
            crear(new Product(null, "Monitor 4K NoSQL", new BigDecimal("1800000.00"), "Monitor 27 pulgadas"));
        } catch (Exception e) {
            // MongoDB no está conectado en el inicio
        }
    }

    private Product mapear(Document doc) {
        if (doc == null) return null;
        Number idNum = (Number) doc.get("id");
        if (idNum == null) idNum = (Number) doc.get("_id");
        int id = idNum != null ? idNum.intValue() : 0;
        String name = doc.getString("name");
        Object priceObj = doc.get("price");
        BigDecimal price = priceObj instanceof Number
            ? BigDecimal.valueOf(((Number) priceObj).doubleValue())
            : BigDecimal.ZERO;
        String description = doc.getString("description");
        return new Product(id, name, price, description != null ? description : "");
    }

    private synchronized int siguienteId() {
        Query query = new Query().with(Sort.by(Sort.Direction.DESC, "id")).limit(1);
        Document ultimo = mongoTemplate.findOne(query, Document.class, COLLECTION);
        if (ultimo == null) return 1;
        Number idNum = (Number) ultimo.get("id");
        return (idNum != null ? idNum.intValue() : 0) + 1;
    }

    @Override
    public List<Product> listar() {
        List<Document> pipeline = List.of(
            new Document("$sort", new Document("id", 1))
        );
        return ejecutarPipeline(pipeline);
    }

    @Override
    public Optional<Product> buscarPorId(int id) {
        Query query = new Query(Criteria.where("id").is(id));
        Document doc = mongoTemplate.findOne(query, Document.class, COLLECTION);
        return Optional.ofNullable(mapear(doc));
    }

    @Override
    public Product crear(Product product) {
        int id = product.getId() != null && product.getId() > 0 ? product.getId() : siguienteId();
        Document doc = new Document("_id", id)
            .append("id", id)
            .append("name", product.getName())
            .append("price", product.getPrice() != null ? product.getPrice().doubleValue() : 0.0)
            .append("description", product.getDescription() != null ? product.getDescription() : "");
        mongoTemplate.save(doc, COLLECTION);
        return new Product(id, product.getName(), product.getPrice(), product.getDescription());
    }

    @Override
    public boolean actualizar(int id, Product product) {
        Query query = new Query(Criteria.where("id").is(id));
        Update update = new Update()
            .set("name", product.getName())
            .set("price", product.getPrice() != null ? product.getPrice().doubleValue() : 0.0)
            .set("description", product.getDescription() != null ? product.getDescription() : "");
        return mongoTemplate.updateFirst(query, update, COLLECTION).getModifiedCount() > 0;
    }

    @Override
    public boolean eliminar(int id) {
        Query query = new Query(Criteria.where("id").is(id));
        return mongoTemplate.remove(query, COLLECTION).getDeletedCount() > 0;
    }

    // 1. Pipeline Rango de Precios ($match + $sort)
    @Override
    public List<Product> filtrarPorRangoPrecio(BigDecimal min, BigDecimal max) {
        double dMin = min != null ? min.doubleValue() : 0.0;
        double dMax = max != null ? max.doubleValue() : Double.MAX_VALUE;
        List<Document> pipeline = List.of(
            new Document("$match", new Document("price", new Document("$gte", dMin).append("$lte", dMax))),
            new Document("$sort", new Document("price", 1))
        );
        return ejecutarPipeline(pipeline);
    }

    // 2. Pipeline Coincidencia de Texto en Nombre o Descripción ($match + $or + $regex + $sort)
    @Override
    public List<Product> filtrarPorPalabra(String texto) {
        String patron = texto != null ? Pattern.quote(texto) : "";
        List<Document> pipeline = List.of(
            new Document("$match", new Document("$or", List.of(
                new Document("name", new Document("$regex", patron).append("$options", "i")),
                new Document("description", new Document("$regex", patron).append("$options", "i"))
            ))),
            new Document("$sort", new Document("name", 1))
        );
        return ejecutarPipeline(pipeline);
    }

    // 3. Pipeline Top N Productos Más Caros ($sort + $limit)
    @Override
    public List<Product> listarTopCaros(int limite) {
        int lim = limite > 0 ? limite : 5;
        List<Document> pipeline = List.of(
            new Document("$sort", new Document("price", -1)),
            new Document("$limit", lim)
        );
        return ejecutarPipeline(pipeline);
    }

    // 4. Pipeline Productos con Precio Superior al Promedio ($group + $avg + $unwind + $match + $replaceRoot + $sort)
    @Override
    public List<Product> filtrarMayoresAlPromedio() {
        List<Document> pipeline = List.of(
            new Document("$group", new Document("_id", null)
                .append("avgPrice", new Document("$avg", "$price"))
                .append("docs", new Document("$push", "$$ROOT"))),
            new Document("$unwind", "$docs"),
            new Document("$match", new Document("$expr",
                new Document("$gte", List.of("$docs.price", "$avgPrice")))),
            new Document("$replaceRoot", new Document("newRoot", "$docs")),
            new Document("$sort", new Document("price", -1))
        );
        return ejecutarPipeline(pipeline);
    }

    private List<Product> ejecutarPipeline(List<Document> pipeline) {
        List<Product> productos = new ArrayList<>();
        mongoTemplate.getDb()
            .getCollection(COLLECTION)
            .aggregate(pipeline)
            .forEach(doc -> {
                Product p = mapear(doc);
                if (p != null) productos.add(p);
            });
        return productos;
    }
}
