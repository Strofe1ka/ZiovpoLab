# Задание 1 — подготовка репозитория

## Цель

Подготовить Git-репозиторий серверной части: базовая аутентификация (JWT access/refresh), авторизация по ролям, настройка HTTPS и PostgreSQL, шаблон переменных окружения, CI (test + build), материалы по UML и ER.

## Что сделано в этом репозитории

- Spring Boot backend (`ru.ziovpo.backend`): JWT, роли, REST API авторизации.
- Конфигурация: `src/main/resources/application.yml`, пример секретов — `.env.example`.
- Тесты: `src/test/`, контекст Spring поднимается на H2.
- CI:
  - GitLab: `.gitlab-ci.yml` (стадии `test`, `build`).
  - GitHub: `.github/workflows/ci.yml` (аналогично для репозитория на GitHub).

## Как проверить локально

```bash
mvn test
mvn -DskipTests package
```

Для запуска приложения нужны переменные из `.env.example` (в т.ч. JWT-секреты в Base64).

## Ветка Git

Работа по заданию 1 ведётся в ветке **`lab-1`** (можно смотреть diff относительно `main` после появления следующих заданий).

## Теория (UML и ER)

Ссылки собраны в корневом [README.md](../../../README.md).
