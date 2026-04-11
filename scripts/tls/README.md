# TLS: цепочка из трёх сертификатов + PKCS12

## Требования

- Windows: **OpenSSL** (удобнее всего вместе с [Git for Windows](https://git-scm.com/download/win)).

## Команда

В PowerShell из каталога `scripts/tls`.

Если появляется ошибка про **выполнение сценариев отключено** (`ExecutionPolicy`), запустите так (обход только для этой команды):

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File ".\gen-chain-and-keystore.ps1" -StudentTicket "ВАШ_НОМЕР_СТУДЕНЧЕСКОГО" -KeystorePassword "надёжный-пароль-локально"
```

Или один раз для профиля пользователя: `Set-ExecutionPolicy -ExecutionPolicy RemoteSigned -Scope CurrentUser`, затем обычный вызов:

```powershell
.\gen-chain-and-keystore.ps1 -StudentTicket "ВАШ_НОМЕР_СТУДЕНЧЕСКОГО" -KeystorePassword "надёжный-пароль-локально"
```

Имена файлов **не** совпадают с типичными примерами (`root-ca`, `server` и т.п.): используются префиксы `ziovpo-bsuir-*`.

Во всех трёх сертификатах в Subject присутствует `OU=StudentTicket-<номер>`.

Номер может содержать **кириллицу** (например `1БИБ23311`): конфиги для OpenSSL пишутся в **UTF-8 без BOM**.

Результат копируется в `certs/generated/`. Дальше значения для `.env` скрипт выводит в консоль.

## Проверка цепочки

```powershell
openssl verify -CAfile ..\..\certs\generated\ziovpo-bsuir-trust-anchor.crt -untrusted ..\..\certs\generated\ziovpo-bsuir-policy-intermediate.crt ..\..\certs\generated\ziovpo-bsuir-app-endpoint.crt
```

Ожидается: `ziovpo-bsuir-app-endpoint.crt: OK`
