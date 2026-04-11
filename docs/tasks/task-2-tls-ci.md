# Задание: TLS-цепочка, доверенный корень, CI и секреты

Связано с требованиями: изучить теорию и методичный документ; цепочка сертификатов (≥ 3 звена) с **номером студенческого билета** в сертификатах; **TLS** для сервиса; корень в **доверенные**; **CI**: компиляция, тесты, упаковка, выгрузка артефакта; **keystore + пароль** только в **GitHub Secrets** / GitLab Variables.

## 1. Теория и документ

- **UML / ER** — см. ссылки в корневом [README.md](../../README.md).
- Методичный **документ курса** — пройти по указанию преподавателя (файл/страница в LMS). В отчёте кратко перечислите, какие разделы прочитаны.

## 2. Цепочка из трёх сертификатов (имена ≠ примеру)

Скрипт: [scripts/tls/gen-chain-and-keystore.ps1](../../scripts/tls/gen-chain-and-keystore.ps1).

```powershell
cd scripts\tls
.\gen-chain-and-keystore.ps1 -StudentTicket "ВАШ_НОМЕР" -KeystorePassword "свой-пароль"
```

Файлы (префикс `ziovpo-bsuir-*`, не `root-ca` / `myserver` из типовых гайдов):

| Звено | Файл |
|--------|------|
| Корень | `ziovpo-bsuir-trust-anchor.crt` |
| Промежуточный | `ziovpo-bsuir-policy-intermediate.crt` |
| Сервер (localhost) | `ziovpo-bsuir-app-endpoint.crt` |

В **каждом** сертификате в Subject есть `OU=StudentTicket-<номер>`.

Проверка цепочки:

```powershell
openssl verify -CAfile certs\generated\ziovpo-bsuir-trust-anchor.crt -untrusted certs\generated\ziovpo-bsuir-policy-intermediate.crt certs\generated\ziovpo-bsuir-app-endpoint.crt
```

## 3. Переключение Spring Boot на TLS

В `.env` (после генерации `certs/generated/`):

```env
SERVER_PORT=8443
SSL_ENABLED=true
SSL_KEY_STORE_TYPE=PKCS12
SSL_KEY_STORE=file:C:/полный/путь/к/certs/generated/ziovpo-bsuir-app-endpoint.p12
SSL_KEY_STORE_PASSWORD=тот_же_что_в_скрипте
SSL_KEY_ALIAS=ziovpo.bsuir.app.endpoint
```

Если в пути к `.p12` есть **пробелы**, надёжнее положить keystore в каталог **без пробелов** (например `C:\tls\ziovpo-bsuir-app-endpoint.p12`) и указать его в `SSL_KEY_STORE`.

Запуск:

```powershell
mvn spring-boot:run
```

Открывайте API по **`https://localhost:8443`** при `SERVER_PORT=8443` (см. `.env` выше).

## 4. Доверенный корень для браузера (Windows)

1. Дважды откройте `certs/generated/ziovpo-bsuir-trust-anchor.crt`.
2. **Установить сертификат** → **Локальный компьютер** → поместить в **Доверенные корневые центры сертификации**.
3. Перезапустите браузер.

После этого Chrome/Edge не должны ругаться на `https://localhost:...` для вашего серверного сертификата.

## 5. CI: компиляция, тесты, упаковка, артефакт

Файл: [.github/workflows/ci.yml](../../.github/workflows/ci.yml).

В одном пайплайне шаги: **compile → test → package → upload-artifact** (хранилище артефактов GitHub Actions).

## 6. GitHub Secrets для keystore (не коммитить `.p12`)

В репозитории: **Settings → Secrets and variables → Actions → New repository secret**.

Рекомендуемые имена:

| Secret | Содержимое |
|--------|------------|
| `SSL_KEYSTORE_BASE64` | Файл `ziovpo-bsuir-app-endpoint.p12` в **Base64 одной строкой** (без переносов). |
| `SSL_KEYSTORE_PASSWORD` | Пароль от PKCS12. |

Пример PowerShell для Base64:

```powershell
[Convert]::ToBase64String([IO.File]::ReadAllBytes("C:\путь\к\ziovpo-bsuir-app-endpoint.p12")) | Set-Clipboard
```

Дальше вставьте из буфера в значение секрета.

В репозитории есть второй job **`package-with-keystore-from-secrets`**: он запускается только при **ручном** запуске workflow (**Actions** → **CI** → **Run workflow**). Он декодирует `SSL_KEYSTORE_BASE64` в `certs/ci/ziovpo-bsuir-app-endpoint.p12` (каталог в `.gitignore`, в артефакт **не** попадает), затем выполняет compile → test → package и выгружает JAR. Секреты **`SSL_KEYSTORE_BASE64`** и **`SSL_KEYSTORE_PASSWORD`** должны быть заданы заранее.

При **push** и **pull_request** выполняется только **`build-test-package`**. При **Run workflow** вручную выполняется только **`package-with-keystore-from-secrets`** (нужны оба секрета), без дублирования обычной сборки.

## 7. Соответствие «прошлому проекту» (РБПО)

Серверная часть остаётся Spring Boot + JWT + роли + PostgreSQL, как в подготовительном репозитории. При расхождении с кодом РБПО переносите пакеты/контроллеры вручную и сохраняйте единый стиль с текущим проектом.
