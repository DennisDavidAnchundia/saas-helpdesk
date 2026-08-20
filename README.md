# SaaS Help Desk

Sistema de atención al cliente tipo Zendesk/Freshdesk, construido como SaaS multi-tenant.

## Stack

- **Backend:** Java 21, Spring Boot 4 (MVC, Data JPA, Security, Validation, WebSocket, Cache, Actuator)
- **Base de datos:** PostgreSQL 16, Redis 7, Flyway
- **Frontend:** React 19, TypeScript, Vite, Tailwind CSS, TanStack Query, i18n
- **Tiempo real:** WebSocket (STOMP)
- **Pagos:** Stripe
- **Infraestructura:** Docker, docker-compose, Kubernetes (Kustomize)

## Funcionalidades

- Multi-tenant (aislamiento por empresa)
- Roles: Admin, Agent, Customer
- Sistema de tickets con estados y SLA
- Chat en tiempo real
- Base de conocimiento
- Dashboard con métricas
- Billing con Stripe

## Estado

En construcción.
