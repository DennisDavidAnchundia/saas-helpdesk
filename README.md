# SaaS Help Desk

Sistema de atención al cliente tipo Zendesk/Freshdesk, construido como SaaS multi-tenant.

## Stack

- **Backend:** Java 21, Spring Boot 4.0.7 (MVC, Data JPA, Security, OAuth2 Client, Validation, WebSocket, Cache, Actuator)
- **Base de datos:** PostgreSQL 16, Redis 7, Flyway
- **Frontend:** React 19, TypeScript, Vite, Tailwind CSS, TanStack Query, i18n
- **Tiempo real:** WebSocket (STOMP)
- **Pagos:** Stripe
- **Infraestructura:** Docker, docker-compose, Kubernetes (Kustomize)
- **Calidad:** JUnit 5, Mockito, H2 (tests)

## Funcionalidades

- Multi-tenant (aislamiento por empresa con discriminator column)
- Autenticación: Email/Password + **Google OAuth 2.0**
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
| GET | `/oauth2/authorization/google` | Login con Google OAuth 2.0 | No |

## Autenticación

### Email/Password
```bash
# Registrar
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"fullName":"Tu Nombre","email":"tu@email.com","password":"password123","tenantName":"Tu Empresa"}'

# Login
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"tu@email.com","password":"password123","tenantSlug":"tu-empresa"}'
```

### Google OAuth 2.0
1. Configurar Google Cloud Console (ver `docs/estado.md`)
2. Abrir `http://localhost:8080/oauth2/authorization/google`
3. Autenticar con Google
4. Redirigir a frontend con JWT

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
- ✅ Fase 1.4: Google OAuth 2.0 Login

### Siguiente
- 1.5: Filtro de autenticación + roles
- 1.6: Tests de auth
- 1.7: Documentación OAuth
