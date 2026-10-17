# CRUD de productos con Spring Boot (SQL + NoSQL Hexagonal)

Proyecto independiente con arquitectura hexagonal desacoplada. Permite **crear, listar, buscar, actualizar y eliminar** productos con doble soporte de persistencia intercambiable: **MySQL (SQL)** y **MongoDB (NoSQL)**.

Usa Java 21, Spring Boot y Maven 3.9.9. Corre en el puerto **8081** y usa la base **`productoscruddb`** tanto en MySQL como en MongoDB.

## Preparación

### 1. Base de datos SQL (MySQL)
1. Ejecuta [dbs/productoscruddb.sql](dbs/productoscruddb.sql) en MySQL Workbench. Crea la tabla `products` y algunos datos de ejemplo.
2. Si tus credenciales no son `root` y `123`, define las variables en PowerShell:

```powershell
$env:DB_USER = "root"
$env:DB_PASSWORD = "TU_CLAVE"
```

### 2. Base de datos NoSQL (MongoDB)
Inicia tu instancia local de MongoDB (o MongoDB Atlas) en el puerto por defecto 27017:
- Por defecto se conecta a `mongodb://localhost:27017/productoscruddb`.
- Si necesitas credenciales o un host diferente, puedes definir:

```powershell
$env:MONGO_HOST = "localhost"
$env:MONGO_PORT = "27017"
$env:MONGO_DB = "productoscruddb"
```

### 3. Iniciar el Backend
Desde esta carpeta (`Spring_boot_crud`), ejecuta:

```powershell
.\mvnw.cmd spring-boot:run
```

---

## Arquitectura Hexagonal y Persistencia Desacoplada

Gracias al desacoplamiento de la arquitectura hexagonal, el dominio y los casos de uso (`ProductService`) se comunican exclusivamente a través del puerto abstracto (`ProductRepository`):

- **Contrato abstracto:** `application/ports/ProductRepository.java`
- **Adaptador SQL:** `infrastructure/persistence/mysql/ProductRepositoryMySQL.java` (Spring JDBC)
- **Adaptador NoSQL:** `infrastructure/persistence/mongo/ProductRepositoryMongo.java` (Spring Data MongoDB)
- **Configuración e inyección:** `app/App.java` conecta ambas alternativas tecnológicas (`productServiceSQL` y `productServiceMongo`) sin requerir una fábrica abstracta adicional.

---

## Operaciones HTTP

### 1. Endpoints SQL (MySQL)
| Método | Ruta | Resultado |
| --- | --- | --- |
| GET | `/productos` | Lista todos los productos en SQL |
| GET | `/productos/{id}` | Busca uno por ID; responde 404 si no existe |
| POST | `/productos` | Crea un producto en MySQL |
| PUT | `/productos/{id}` | Actualiza un producto existente |
| DELETE | `/productos/{id}` | Elimina un producto por ID |

### 2. Endpoints NoSQL (MongoDB)
| Método | Ruta | Resultado |
| --- | --- | --- |
| GET | `/productos/nosql` | Lista todos los productos en MongoDB |
| GET | `/productos/nosql/{id}` | Busca uno por ID en MongoDB |
| POST | `/productos/nosql` | Crea un producto en MongoDB |
| PUT | `/productos/nosql/{id}` | Actualiza un producto en MongoDB |
| DELETE | `/productos/nosql/{id}` | Elimina un producto en MongoDB |

### 3. Métodos de Filtrado NoSQL con Pipelines de Agregación (`aggregate`)
| Método | Ruta | Pipeline / Filtro |
| --- | --- | --- |
| GET | `/productos/nosql/filtros/rango?min=X&max=Y` | Pipeline con `$match` (`$gte`, `$lte`) y `$sort` por precio |
| GET | `/productos/nosql/filtros/palabra?texto=T` | Pipeline con `$match` (`$or`, `$regex` en nombre/descripción) y `$sort` |
| GET | `/productos/nosql/filtros/top?limite=N` | Pipeline con `$sort` descendente y `$limit` de los N más costosos |
| GET | `/productos/nosql/filtros/promedio` | Pipeline avanzado con `$group` (`$avg`), `$unwind`, `$match` con `$expr`, `$replaceRoot` y `$sort` |

Cada endpoint de filtros devuelve un objeto con:
- `filtro`: Nombre descriptivo del filtro aplicado.
- `pipeline`: Código exacto del pipeline JSON ejecutado en MongoDB.
- `productos`: Lista de productos resultantes.
