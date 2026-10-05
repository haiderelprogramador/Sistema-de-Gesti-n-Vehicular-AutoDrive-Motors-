# AutoDrive Motors – API REST

Sistema de Gestión Vehicular para AutoDrive Motors: clientes, vehículos, ventas y mantenimientos,
con conversión de precios COP → USD usando una API pública de tasas de cambio.

**Stack:** Java 17 · Spring Boot 3.3 · Spring Data JPA / Hibernate · MySQL · Bean Validation · Lombok · Postman

## Cómo correrlo

### Requisitos
- JDK 17 o superior
- Maven 3.9+ (o abrir el proyecto en IntelliJ / VS Code, que trae Maven)
- MySQL 8 (solo para el perfil normal)

### Opción A: con MySQL
1. Ajusta usuario y contraseña en `src/main/resources/application.properties`
   (por defecto `root` / `root`). La base `autodrive_db` se crea sola al arrancar.
2. Ejecuta:
   ```bash
   mvn spring-boot:run
   ```

### Opción B: sin instalar MySQL (H2 en memoria)
```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```
Consola de la base: http://localhost:8080/h2-console (JDBC URL `jdbc:h2:mem:autodrive_db`, usuario `sa`).

La API queda en **http://localhost:8080**.

### Panel web
Abre **http://localhost:8080** en el navegador para usar el panel visual: patio de vehículos por estado, inventario con precio en dólares, clientes, ventas con cálculo del descuento en vivo y taller. Usa la misma API REST, así que todas las reglas de negocio se aplican igual.



### Pruebas automáticas
```bash
mvn test
```
Verifican las reglas de negocio (descuento, cambios de estado, unicidad, validaciones, conversión a USD).

### Postman
Importa `postman/AutoDrive-Motors.postman_collection.json` y ejecuta la colección completa con el
**Runner**, en orden y con la base vacía. Las peticiones guardan los ids que crean en variables de la
colección, y cada una tiene tests que validan la respuesta. Las que empiezan con `ERROR -` comprueban
que las reglas de negocio rechacen operaciones inválidas.

## Endpoints

| Método | Ruta | Descripción |
|---|---|---|
| GET | `/clientes` | Listar clientes |
| GET | `/clientes/{id}` | Consultar cliente |
| GET | `/clientes/{id}/ventas` | Historial de compras del cliente |
| POST | `/clientes` | Registrar cliente |
| PUT | `/clientes/{id}` | Actualizar cliente |
| DELETE | `/clientes/{id}` | Eliminar cliente (si no tiene ventas) |
| GET | `/vehiculos` | Listar vehículos |
| GET | `/vehiculos/{id}` | Consultar vehículo |
| GET | `/vehiculos/disponibles` | Vehículos disponibles |
| GET | `/vehiculos/marca/{marca}` | Vehículos por marca |
| GET | `/vehiculos/{id}/precio-usd` | Precio convertido a USD (API externa) |
| GET | `/vehiculos/{id}/mantenimientos` | Historial de mantenimientos |
| POST | `/vehiculos` | Registrar vehículo |
| PUT | `/vehiculos/{id}` | Actualizar vehículo |
| DELETE | `/vehiculos/{id}` | Eliminar vehículo |
| POST | `/ventas` | Registrar venta |
| GET | `/ventas` | Listar ventas |
| GET | `/ventas/{id}` | Consultar venta |
| POST | `/mantenimientos` | Registrar mantenimiento |
| GET | `/mantenimientos` | Listar mantenimientos |
| GET | `/mantenimientos/{id}` | Consultar mantenimiento |
| GET | `/mantenimientos/vehiculo/{vehiculoId}` | Historial por vehículo |
| PUT | `/mantenimientos/{id}/finalizar` | Finalizar mantenimiento (vehículo vuelve a DISPONIBLE) |
| GET | `/tasa-cambio` | Tasa COP/USD actual |
| GET | `/reportes/resumen` | Resumen general para gerencia |

## Ejemplos de JSON

**POST /vehiculos**
```json
{ "placa": "ABC123", "marca": "Toyota", "modelo": "Prado", "anio": 2025, "color": "Blanco", "precio": 250000000 }
```

**POST /ventas** (la fecha, el descuento y el total los calcula el sistema)
```json
{ "clienteId": 1, "vehiculoId": 1, "metodoPago": "Transferencia" }
```

**POST /mantenimientos** (`tipo`: PREVENTIVO, CORRECTIVO o REVISION)
```json
{ "vehiculoId": 2, "tipo": "PREVENTIVO", "descripcion": "Cambio de aceite", "costo": 450000 }
```

**Formato de error**
```json
{
  "timestamp": "2026-10-04T16:20:00",
  "status": 409,
  "error": "Conflict",
  "mensaje": "El vehículo con placa ABC123 ya fue vendido",
  "ruta": "/ventas"
}
```

## Estructura

```
src/main/java/com/autodrive
├── controller/     Endpoints REST
├── service/        Interfaces de la lógica de negocio
│   └── impl/       Implementaciones
├── repository/     Repositorios JPA
├── model/entity/   Entidades (Cliente, Vehiculo, Venta, Mantenimiento)
├── model/enums/    EstadoVehiculo, EstadoMantenimiento, TipoMantenimiento
├── dto/request/    DTOs de entrada con validaciones
├── dto/response/   DTOs de salida
├── mapper/         Conversión entidad ↔ DTO
├── client/         Consumo de la API de tasas de cambio
├── exception/      Excepciones propias y manejador global
└── config/         Configuración del cliente HTTP
```
