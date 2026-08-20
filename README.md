# SaaS Help Desk

Sistema de atención al cliente tipo Zendesk/Freshdesk, construido como SaaS multi-tenant.

## Stack

- **Backend:** Java 21, Spring Boot 4.0.7 (MVC, Data JPA, Security, Validation, WebSocket, Cache, Actuator)
- **Base de datos:** PostgreSQL 16, Redis 7, Flyway
- **Frontend:** React 19, TypeScript, Vite, Tailwind CSS, TanStack Query, i18n
- **Tiempo real:** WebSocket (STOMP)
- **Pagos:** Stripe
- **Infraestructura:** Docker, docker-compose, Kubernetes (Kustomize)
- **Calidad:** JUnit 5, Mockito, H2 (tests)

## Funcionalidades

- Multi-tenant (aislamiento por empresa con discriminator column)
- Roles: Admin, Agent, Customer
- Sistema de tickets con estados y SLA
- Chat en tiempo real
- Base de conocimiento
- Dashboard con métricas
- Billing con Stripe

## Cómo correr

```bash
# 1. Levantar base de datos
docker-compose up -d

# 2. Correr backend
cd backend
.\mvnw.cmd spring-boot:run

# 3. Tests
.\mvnw.cmd -B test
```

## API Endpoints

| Método | Ruta | Descripción | Auth |
|--------|------|-------------|------|
| GET | `/api/health` | Health check | No |
| POST | `/api/auth/register` | Registrar usuario + empresa | No |
| POST | `/api/auth/login` | Login y obtener JWT | No |

## Estructura

```
├── backend/          # API REST Spring Boot
├── docker-compose.yml
├── docs/             # Documentación (no commiteada)
└── README.md
```

## Estado

En construcción.

### Completado
- ✅ Fase 0: Repo + Setup + Flyway schema
- ✅ Fase 1.1: Entidades User, Tenant + enums
- ✅ Fase 1.2: Registro de usuario con BCrypt
- ✅ Fase 1.3: Login con JWT

### Siguiente
- 1.4: Filtro de autenticación + roles
- 1.5: Tests de auth
