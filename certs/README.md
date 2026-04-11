# Сертификаты (локально)

Папка `generated/` создаётся скриптом `scripts/tls/gen-chain-and-keystore.ps1` и **в git не попадает** (см. `.gitignore`).

Содержимое:

- `ziovpo-bsuir-trust-anchor.crt` — корневой сертификат (импорт в доверенные корневые ЦС Windows).
- `ziovpo-bsuir-app-endpoint.p12` — keystore для Spring Boot (`SSL_KEY_STORE`).

Не выкладывайте `.p12` и `.key` в репозиторий. Для CI используйте **GitHub Secrets** (см. `docs/tasks/task-2-tls-ci.md`).
