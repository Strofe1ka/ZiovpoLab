# Задание 2 — текстовое описание схемы БД (PostgreSQL)

Схема соответствует ER-диаграмме сущностей «управление лицензиями». Все первичные ключи — `UUID`. Физическая реализация задаётся миграцией Flyway `V1__init_schema.sql` и согласована с JPA-сущностями.

## Таблица `users`

Учётные записи. Поля: уникальный логин `name`, `password_hash`, `email`, строковая роль `role` (в приложении — `ROLE_USER` / `ROLE_ADMIN`), флаги состояния учётной записи (`is_account_expired`, `is_account_locked`, `is_credentials_expired`, `is_disabled`). Связи: один ко многим с `device`, `license` (как держатель и как владелец), `license_history`.

## Таблица `refresh_tokens`

Служебная таблица задания 1: refresh-токены JWT. `user_id` ссылается на `users(id)` с каскадным удалением.

## Таблица `device`

Устройство пользователя: `name`, `mac_address`, `user_id` → `users`. Один пользователь — много устройств.

## Таблица `license_type`

Тип лицензии: `name`, `default_duration_in_days` (срок по умолчанию в днях при первой активации, если дата окончания не задана), `description`. Связь один ко многим с `license`.

## Таблица `product`

Продукт: `name`, `is_blocked`. Связь один ко многим с `license`.

## Таблица `license`

Выданная лицензия: уникальный `code`; `user_id` — держатель (кто активирует на своих устройствах); `owner_id` — владелец (например, покупатель, может продлевать); `product_id`, `type_id`; `first_activation_date` и `ending_date` (могут быть `NULL` до первой активации / явного задания); `blocked`; лимит устройств `device_count`; `description`.

## Таблица `device_license`

Связь «лицензия активирована на устройстве»: `license_id`, `device_id`, `activation_date`. Пара (`license_id`, `device_id`) уникальна.

## Таблица `license_history`

Журнал операций по лицензии: `license_id`, `user_id` (кто выполнил действие), `status` (например `CREATED`, `ACTIVATED`, `VERIFIED`, `RENEWED`), `change_date`, `description`.

## Целостность и индексы

Внешние ключи задают иерархию пользователь → устройство, продукт/тип → лицензия, лицензия → история и активации на устройствах. Дополнительно созданы индексы по `refresh_tokens.user_id`, `device.user_id`, `license.user_id`, `license.owner_id`, `license_history.license_id` для типовых выборок.

## Примечание по развёртыванию

При переходе с прежней схемы (числовой `id` у `users`) требуется пересоздать БД или выполнить отдельную миграцию данных: текущая `V1` рассчитана на чистую установку PostgreSQL.

В `application.yml` включены `flyway.baseline-on-migrate: true` и `baseline-version: 0`, чтобы Flyway мог стартовать, если в `public` уже есть какие-то объекты, но ещё нет таблицы `flyway_schema_history`. Если после этого миграция `V1` падает с ошибкой «отношение уже существует», очистите схему или создайте новую пустую базу и запустите приложение снова.
