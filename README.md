# Sistema CRUD Multi-Persistencia (Spring Boot + React + MySQL + MongoDB)

Proyecto fullstack desarrollado con **Spring Boot (Java 21)** y **React (Vite)** bajo **Arquitectura Hexagonal**, permitiendo alternar la persistencia entre bases de datos relacionales (**MySQL**) y NoSQL (**MongoDB**).

---

## Estructura del Repositorio

- `Spring_boot_crud/`: Backend en Spring Boot 4.1.1 con arquitectura hexagonal desacoplada.
  - `dbs/productoscruddb.sql`: Script SQL para inicializar MySQL.
  - `application/ports/ProductRepository.java`: Puerto / contrato abstracto de persistencia.
  - `infrastructure/persistence/mysql/ProductRepositoryMySQL.java`: Adaptador de persistencia SQL (Spring JDBC).
  - `infrastructure/persistence/mongo/ProductRepositoryMongo.java`: Adaptador de persistencia NoSQL (Spring Data MongoDB) con 4 pipelines de agregación.
  - `infrastructure/web/controllers/`: Controladores REST para SQL (`/productos`) y NoSQL (`/productos/nosql`).
- `react_crud/`: Frontend en React + Vite.
  - Menú de selección entre SQL y NoSQL.
  - Doble interfaz CRUD completa.
  - Filtros avanzados para MongoDB con visualización en tiempo real del código JSON de cada pipeline de agregación.

---

## 1. Puesta en Marcha del Backend (`Spring_boot_crud`)

### Requisitos
- Java 21 JDK
- MySQL 8.x en el puerto `3306` (Base de datos `productoscruddb`, script en `Spring_boot_crud/dbs/productoscruddb.sql`)
- MongoDB 6.x o superior en el puerto `27017`

### Variables de Entorno (Opcionales si usas valores por defecto)
```powershell
$env:DB_USER = "root"
$env:DB_PASSWORD = "123"
$env:MONGO_HOST = "localhost"
$env:MONGO_PORT = "27017"
$env:MONGO_DB = "productoscruddb"
```

### Ejecutar Backend
```powershell
cd Spring_boot_crud
.\mvnw.cmd spring-boot:run
```
El backend estará escuchando en `http://localhost:8081`.

---

## 2. Puesta en Marcha del Frontend (`react_crud`)

```powershell
cd react_crud
npm ci
npm run dev
```
Abre la aplicación en [http://localhost:5173/](http://localhost:5173/).

---

## 3. Pipelines de Agregación MongoDB Implementados

1. **Rango de Precios (`$match` + `$sort`):**
   ```json
   [
     { "$match": { "price": { "$gte": 50000, "$lte": 500000 } } },
     { "$sort": { "price": 1 } }
   ]
   ```

2. **Búsqueda por Texto (`$match` + `$or` + `$regex` + `$sort`):**
   ```json
   [
     {
       "$match": {
         "$or": [
           { "name": { "$regex": "portátil", "$options": "i" } },
           { "description": { "$regex": "portátil", "$options": "i" } }
         ]
       }
     },
     { "$sort": { "name": 1 } }
   ]
   ```

3. **Top N Más Costosos (`$sort` + `$limit`):**
   ```json
   [
     { "$sort": { "price": -1 } },
     { "$limit": 3 }
   ]
   ```

4. **Superiores al Promedio General (`$group` + `$avg` + `$unwind` + `$match` + `$replaceRoot` + `$sort`):**
   ```json
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
   ]
   ```

---

## 4. Publicación en GitHub

Para subir este proyecto a un repositorio de GitHub:

```bash
git init
git add .
git commit -m "Implementación de arquitectura hexagonal multi-persistencia (MySQL y MongoDB) con pipelines de agregación y frontend React"
git branch -M main
git remote add origin https://github.com/TU_USUARIO/TU_REPOSITORIO.git
git push -u origin main
```
