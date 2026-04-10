# Ziovpo — серверная часть (Spring Boot)

Структура по заданиям: каталог [docs/tasks](docs/tasks/README.md). **Задание 1** — ветка `lab-1`, описание: [docs/tasks/task-1](docs/tasks/task-1/README.md).

Репозиторий на GitHub: [https://github.com/Strofe1ka/ZiovpoLab](https://github.com/Strofe1ka/ZiovpoLab)

## Содержание задания 1 (кратко)

This repository is prepared as a foundation for the server side of the project.

## What is already included

- JWT authentication with access/refresh token flow:
  - `POST /api/auth/register`
  - `POST /api/auth/login`
  - `POST /api/auth/refresh`
- Role-based authorization:
  - default user role: `ROLE_USER`
  - admin access example: `GET /api/admin/ping`
- HTTPS configuration through environment variables in `application.yml`.
- PostgreSQL database integration via Spring Data JPA.
- CI pipeline (`.gitlab-ci.yml`) with stages:
  - `test`
  - `build`
- Example environment template in `.env.example`.

## Migration checklist from previous project

1. Copy existing production-ready auth/authorization modules from the previous repository if they differ from this starter.
2. Move real secrets/variables to CI/CD variables and local `.env` (never commit secrets).
3. Replace demo endpoints and entities with domain entities from the previous project.
4. Reuse old tests for auth and access rules.
5. Verify HTTPS certificate settings and keystore paths for deployment environment.

## UML theory materials

- [UML overview (Visual Paradigm)](https://www.visual-paradigm.com/guide/uml-unified-modeling-language/what-is-uml/)
- [UML diagram types (Lucidchart)](https://www.lucidchart.com/pages/uml-diagram)

## ER theory materials

- [What is an ER diagram (Lucidchart)](https://www.lucidchart.com/pages/er-diagrams)
- [ER diagram tutorial (Visual Paradigm)](https://www.visual-paradigm.com/guide/data-modeling/what-is-entity-relationship-diagram/)

## Quick start

1. Copy `.env.example` to `.env` and fill values.
2. Ensure PostgreSQL is running and credentials are valid.
3. Run:
   - `mvn test`
   - `mvn spring-boot:run`
