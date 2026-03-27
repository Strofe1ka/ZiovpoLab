# Лабораторная работа №6 — Carsharing API

## ЗиОВПО — сдача по веткам (без Pull Request)

Каждая работа — в **своей ветке**; преподаватель смотрит нужную ветку на GitHub (**branch** → выбрать `lab-1`, `lab-2`, …). Сводная таблица: [`docs/labs.md`](docs/labs.md).

| Лаба | Ветка   | Содержание |
|------|---------|------------|
| 1    | `lab-1` | Подготовка репозитория: JWT access/refresh, роли, HTTPS, PostgreSQL, `.gitlab-ci.yml` (`test` / `build`) |

Серверная часть перенесена из РБПО (Carsharing API).

---

**Предмет:** PO6  
**Тема:** TLS/HTTPS, цепочка сертификатов, CI/CD

## Project Topic

Carsharing REST API — a system for managing car rentals, users, rides, and payments. Users can start rides with available cars, end rides (cost calculated by distance × 10), and pay for completed rides.

## Security (Spring Security)

- **Basic Auth** — все защищённые эндпоинты требуют заголовок `Authorization: Basic <base64(username:password)>`
- **CSRF** — настроен `CookieCsrfTokenRepository` (при Basic Auth токен не требуется)
- **Роли**: `USER`, `ADMIN`
- **Регистрация** — `POST /register` (без авторизации). Первый зарегистрированный пользователь получает роль `ADMIN`
- **Пароль** — минимум 8 символов, заглавная, строчная, цифра, спецсимвол

## Main Entities

| Entity   | Description                                      |
|----------|--------------------------------------------------|
| **Car**  | Vehicle with brand, model, plate number, availability |
| **User** | Customer with first name, last name, email, username, password, role |
| **Ride** | Trip linking a user and a car, with start/end time, distance, cost |
| **Payment** | Payment record for a ride, with amount and paid status |

## Relationships

- **Ride** → **User** (`@ManyToOne`): each ride belongs to one user
- **Ride** → **Car** (`@ManyToOne`): each ride uses one car
- **Payment** → **Ride** (`@ManyToOne`): each payment is for one ride

## Constraints

- `Car.plateNumber` — unique
- `User.email` — unique
- IDs — auto-increment (`GenerationType.IDENTITY`)

## Available Endpoints

### Регистрация (без авторизации)
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/register` | Регистрация нового пользователя (роль USER; первый — ADMIN) |

### Cars (GET — USER/ADMIN; POST/PUT/DELETE — ADMIN)
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/cars` | Create car |
| GET | `/cars` | Get all cars (optional `?available=true`) |
| GET | `/cars/available` | List available cars |
| GET | `/cars/{id}` | Get car by ID |
| PUT | `/cars/{id}` | Update car |
| DELETE | `/cars/{id}` | Delete car |

### Users (только ADMIN)
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/users` | Create user (username, password обязательны) |
| GET | `/users` | Get all users |
| GET | `/users/{id}` | Get user by ID |
| GET | `/users/{id}/income` | Get user income (sum of paid rides) |
| PUT | `/users/{id}` | Update user |
| DELETE | `/users/{id}` | Delete user |

### Rides (USER, ADMIN)
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/rides` | Start ride |
| GET | `/rides` | Get all rides |
| GET | `/rides/{id}` | Get ride by ID |
| PUT | `/rides/{id}/end` | End ride |
| POST | `/rides/{id}/pay` | Pay for ride |
| DELETE | `/rides/{id}` | Delete ride |

### Payments (USER, ADMIN)
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/payments` | Create payment |
| GET | `/payments` | Get all payments |
| GET | `/payments/{id}` | Get payment by ID |
| PUT | `/payments/{id}/pay` | Mark payment as paid |
| DELETE | `/payments/{id}` | Delete payment |

## Business Operations (5)

1. **Start a ride** — Check car availability, create Ride, mark car as occupied
2. **End a ride** — Compute cost (distance × 10), update ride, mark car as available
3. **Pay for a ride** — Create Payment and set paid = true
4. **Get user income** — Sum of all paid rides for a user
5. **List available cars** — Cars where `available = true`

All operations that change multiple entities use `@Transactional`.

## TLS / HTTPS

Сервис работает по **HTTPS** (порт 8443). Для локальной разработки необходимо сгенерировать цепочку сертификатов.

### Генерация сертификатов

1. Установите OpenSSL (входит в Git for Windows или установите отдельно).
2. Выполните скрипт, указав **номер студенческого билета**:

```powershell
# Из корня репозитория:
.\scripts\generate-certificates.ps1 -StudentId "12345678"

# Или из папки scripts:
cd scripts
.\generate-certificates.ps1 -StudentId "12345678"
```

3. Пароль keystore сохранится в `certs\.keystore-password`. Добавьте в `application-local.properties` (или в переменные окружения):

```properties
SSL_KEY_STORE_PASSWORD=<пароль из certs\.keystore-password>
```

### Добавление Root CA в доверенные (браузер)

Чтобы браузер не показывал предупреждение о самоподписанном сертификате:

**Windows:**
1. Дважды щёлкните `certs/root/carsharing-root-ca.crt`
2. «Установить сертификат» → «Текущий пользователь» или «Локальный компьютер»
3. «Поместить все сертификаты в следующее хранилище» → «Доверенные корневые центры сертификации»
4. Завершите мастер

**Chrome/Edge:** используют хранилище Windows, после установки Root CA перезапустите браузер.

### Переменные TLS

| Variable | Description |
|----------|-------------|
| `SSL_KEY_STORE_PATH` | Путь к keystore (по умолчанию `file:./certs/carsharing-keystore.p12`) |
| `SSL_KEY_STORE_PASSWORD` | Пароль keystore (**не коммитить!**) |

---

## CI (GitLab)

Pipeline `.gitlab-ci.yml` (в корне репозитория) выполняет:
- компиляцию
- тестирование (с PostgreSQL)
- упаковку JAR
- загрузку артефакта (`target/*.jar`)

### GitLab CI Variables

Для CI задайте переменные в **Settings -> CI/CD -> Variables**:

| Variable | Описание |
|--------|----------|
| `KEYSTORE_BASE64` | Base64 содержимого `certs/carsharing-keystore.p12` |
| `KEYSTORE_PASSWORD` | Пароль keystore |

**Получение base64 (PowerShell):**
```powershell
[Convert]::ToBase64String([IO.File]::ReadAllBytes("$PWD\\certs\\carsharing-keystore.p12"))
```

⚠️ **Не допускайте утечки** паролей, ключей, keystore и сертификатов в репозиторий!

---

## Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `DB_HOST` | PostgreSQL host | `localhost` |
| `DB_PORT` | PostgreSQL port | `5432` |
| `DB_NAME` | Database name | `carsharing` |
| `DB_USER` | Database user | `postgres` |
| `DB_PASSWORD` | Database password | `postgres` |

## Prerequisites

- Java 17+
- PostgreSQL
- Maven

## Database Setup

1. Start PostgreSQL.
2. Create the database:
```sql
CREATE DATABASE carsharing;
```

3. (Optional) Set environment variables:
```bash
export DB_HOST=localhost
export DB_PORT=5432
export DB_NAME=carsharing
export DB_USER=postgres
export DB_PASSWORD=postgres
```

On Windows (PowerShell):
```powershell
$env:DB_HOST="localhost"
$env:DB_PORT="5432"
$env:DB_NAME="carsharing"
$env:DB_USER="postgres"
$env:DB_PASSWORD="postgres"
```

## Running the Project

**Требуется:** сгенерированные сертификаты и `SSL_KEY_STORE_PASSWORD` в `application-local.properties` или в переменных окружения.

```bash
mvn spring-boot:run
```

Приложение будет доступно по **https://localhost:8443**

Или сборка и запуск JAR:
```bash
mvn clean package
java -jar target/demo-0.0.1-SNAPSHOT.jar
```

## Первый запуск

1. Запустите приложение
2. Вызовите `POST /register` с телом:
```json
{
  "firstName": "Админ",
  "lastName": "Системы",
  "email": "admin@carsharing.ru",
  "username": "admin",
  "password": "<ваш_пароль>"
}
```
3. Первый пользователь получит роль ADMIN. Используйте `admin` / ваш пароль для Basic Auth в Postman.

## Postman

Import `postman/Carsharing_API.postman_collection.json`:

- **Регистрация и аутентификация** — регистрация без авторизации
- **Full Scenario** — Create entities → Start ride → End ride → Pay → Verify (требуется Basic Auth)
- **Cars, Users, Rides, Payments** — CRUD с Basic Auth. Создайте Postman Environment с переменными `authUsername`, `authPassword`, `userPassword`.
