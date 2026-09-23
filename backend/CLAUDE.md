# Backend — LoanApp (Spring Boot 3.2 / Java 21)

## Estructura de paquetes
```
com.capacitacion.loanapp
├── api/
│   ├── controller/     ← @RestController, solo HTTP mapping
│   └── dto/            ← Request/Response DTOs
├── domain/
│   ├── model/          ← Entidades de dominio (POJO puro)
│   ├── service/        ← Lógica de negocio
│   ├── validation/     ← Validators de reglas de negocio
│   └── exception/      ← Excepciones tipadas
└── infrastructure/
    ├── persistence/    ← Repositorios JPA
    └── mail/           ← Notificaciones
```

## Reglas de arquitectura
- Los controladores NO contienen lógica de negocio
- Los servicios NO dependen de clases de infraestructura directamente (usa interfaces)
- Toda dependencia se inyecta por constructor
- Las excepciones de negocio extienden `BusinessRuleException`

## Comandos útiles
```bash
mvn spring-boot:run          # Levantar en dev (H2 en memoria)
mvn test                     # Ejecutar todos los tests
mvn verify -Pcoverage        # Tests + reporte de cobertura JaCoCo
mvn checkstyle:check         # Análisis estático
```

## Variables de entorno necesarias (prod)
```
DB_URL, DB_USER, DB_PASS
JWT_SECRET, JWT_EXPIRY_MS
MAIL_HOST, MAIL_PORT, MAIL_USER, MAIL_PASS
```
