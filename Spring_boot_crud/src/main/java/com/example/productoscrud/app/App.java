package com.example.productoscrud.app;

import com.example.productoscrud.application.ports.ProductRepository;
import com.example.productoscrud.application.service.ProductService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@SpringBootApplication(scanBasePackages = "com.example.productoscrud")
public class App {
    public static void main(String[] args) {
        SpringApplication.run(App.class, args);
    }

    @Bean
    @Primary
    public ProductService productServiceSQL(@Qualifier("productRepositoryMySQL") ProductRepository productRepository) {
        return new ProductService(productRepository);
    }

    @Bean("productServiceMongo")
    public ProductService productServiceMongo(@Qualifier("productRepositoryMongo") ProductRepository productRepository) {
        return new ProductService(productRepository);
    }
}

// Origen: la JVM llama a App.main() (com.example.productoscrud.app).
// Destino: App.main() inicia Spring (org.springframework.boot); Spring llama a App.productService()
// y conecta ProductService (application.service) con ProductRepository (application.ports).
