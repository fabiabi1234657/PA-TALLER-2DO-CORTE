# Frontend React - CRUD Multi-Persistencia (SQL y NoSQL)

Aplicación React construida con Vite para interactuar con el backend Spring Boot desacoplado.

## Características

1. **Menú de Selección de Persistencia:**
   - Permite alternar instantáneamente entre la persistencia **SQL (MySQL)** y **NoSQL (MongoDB)**.
2. **Doble CRUD Completo:**
   - Cada motor permite listar, crear, editar y eliminar productos en su respectiva base de datos.
3. **Filtros Avanzados con Pipelines de Agregación MongoDB:**
   - En el modo NoSQL se cuenta con una barra de 4 filtros basados en `aggregate`:
     1. Rango de precios (`$match` y `$sort`).
     2. Búsqueda por palabra clave en nombre o descripción (`$regex` y `$or`).
     3. Top N productos de mayor precio (`$sort` y `$limit`).
     4. Productos con precio superior al promedio (`$group`, `$avg`, `$unwind`, `$replaceRoot`).
4. **Visor de Código de Pipelines:**
   - La interfaz imprime en tiempo real el código JSON del pipeline de agregación ejecutado en MongoDB.

## Ejecución

1. Inicia el backend Spring Boot en el puerto `8081`.
2. Desde esta carpeta (`react_crud`):

```powershell
npm ci
npm run dev
```

Abre [http://127.0.0.1:5173/](http://127.0.0.1:5173/) o la URL indicada en la consola.
