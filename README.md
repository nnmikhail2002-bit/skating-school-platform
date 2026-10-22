# Skating School Platform

Backend для школы фигурного катания на Java и Spring Boot. В приложении можно вести список учеников и тренеров, управлять услугами и расписанием, записывать учеников на занятия.

Сейчас готовы основные REST API, авторизация и тесты. Заказов, абонементов и отдельного сервиса платежей пока нет.

## Возможности

- Регистрация учеников и вход по email и паролю.
- Роли `STUDENT`, `TRAINER`, `ADMIN`. Авторизация через JWT, пароли хранятся в виде BCrypt-хешей.
- Управление учениками, тренерами, услугами и тренировками.
- Назначение услуг тренерам, проверка расписания.
- Поиск, фильтрация, сортировка и пагинация.
- Запись на занятие, отмена записи и повторное бронирование после отмены.
- Контроль свободных мест, в том числе при одновременных запросах на бронирование.
- Swagger UI для работы с API.

## Технологии

| Компонент | Технологии |
|---|---|
| Backend | Java 21, Spring Boot 4.0.8, Spring Web MVC, Bean Validation, OpenAPI |
| Безопасность | Spring Security, OAuth2 Resource Server, JWT (HS256), BCrypt |
| База данных | PostgreSQL 16, Spring Data JPA, Hibernate, Liquibase |
| Сборка | Maven, Maven Wrapper |
| Тесты | JUnit 5, Mockito, MockMvc, Testcontainers |
| Инфраструктура | Docker Compose, GitHub Actions |

## Структура проекта

```text
skating-school-platform/
|-- .github/workflows/backend-ci.yml
|-- backend/
|   `-- skating-platform-backend/
|       |-- pom.xml
|       |-- mvnw
|       |-- mvnw.cmd
|       `-- src/
|           |-- main/
|           `-- test/
|-- compose.yaml
|-- .env.example
`-- README.md
```

Backend разделен на `controller`, `service`, `repository`. Также есть DTO, сущности JPA, мапперы, конфигурация безопасности и обработка ошибок.

## Локальный запуск

Инструкция для Windows и PowerShell. Понадобятся JDK 21, Docker Desktop с запущенным Docker Engine и Git. Maven отдельно устанавливать не нужно.

Команды ниже выполняются из корня репозитория, если не указано другое.

### 1. Настроить переменные окружения

```powershell
Copy-Item .env.example .env
```

В `.env` нужно задать свои `POSTGRES_PASSWORD` и `JWT_SECRET`. Для JWT (HS256) используйте случайный секрет длиной не меньше 32 байт.

Не добавляйте `.env` в Git. В репозитории должен остаться только `.env.example` с тестовыми значениями.

Для создания начального администратора можно заполнить `ADMIN_EMAIL` и `ADMIN_PASSWORD`. Пароль должен содержать не меньше 8 символов. Если поля пустые, администратор автоматически не создается.

### 2. Запустить PostgreSQL

```powershell
docker compose up -d postgres
docker compose ps
```

`compose.yaml` поднимает только PostgreSQL. Подключение к базе: `localhost:5432`.

### 3. Запустить backend

```powershell
cd backend/skating-platform-backend
.\mvnw.cmd spring-boot:run
```

Приложение загружает `.env` по относительному пути из `application.properties`. Поэтому Maven нужно запускать из каталога `backend/skating-platform-backend`. При старте Liquibase применяет миграции, Hibernate проверяет структуру таблиц.

Swagger UI: http://localhost:8080/swagger-ui/index.html

OpenAPI JSON: http://localhost:8080/v3/api-docs

## Пример работы с API

Регистрация ученика:

```http
POST /api/auth/register
Content-Type: application/json

{
  "firstName": "Ivan",
  "lastName": "Petrov",
  "phone": "+79991234567",
  "email": "ivan@example.test",
  "password": "ExamplePass123!"
}
```

Вход:

```http
POST /api/auth/login
Content-Type: application/json

{
  "email": "ivan@example.test",
  "password": "ExamplePass123!"
}
```

В ответ на вход приходит `token`. Для защищенных маршрутов его нужно передавать в заголовке `Authorization: Bearer <token>`.

Бронирование занятия:

```http
POST /api/bookings
Authorization: Bearer <token>
Content-Type: application/json

{
  "trainingSessionId": 1
}
```

Занятие с таким ID должно существовать. Сначала администратор создает услугу, тренера, назначает услугу тренеру и добавляет занятие в расписание.

### Основные маршруты

| Маршрут | Назначение |
|---|---|
| `POST /api/auth/register`, `POST /api/auth/login` | Регистрация и вход |
| `/api/students`, `GET /api/students/me` | Ученики и профиль ученика |
| `/api/trainers`, `GET /api/trainers/me` | Тренеры и профиль тренера |
| `/api/services`, `/api/services/search`, `/api/services/filter` | Услуги и фильтры |
| `/api/training-sessions`, `GET /api/training-sessions/me` | Расписание и занятия тренера |
| `POST /api/bookings`, `GET /api/bookings/me` | Создание и просмотр своих записей |
| `PATCH /api/bookings/cancel` | Отмена записи |
| `GET /api/bookings/trainer/me` | Записи на занятия тренера |

Права доступа зависят от роли. Методы, параметры запросов и ответы можно посмотреть в Swagger UI.

## Тесты и CI

Из каталога `backend/skating-platform-backend`:

```powershell
.\mvnw.cmd -B clean verify
```

Для интеграционных тестов нужен запущенный Docker Engine. Testcontainers сам поднимет временный PostgreSQL, запускать базу через `docker compose` для тестов не требуется.

В последнем полном локальном прогоне прошли 182 теста: 165 unit/controller и 17 интеграционных. Интеграционные тесты проверяют миграции, работу с PostgreSQL, авторизацию через JWT и конкурентное бронирование.

GitHub Actions настроен в `.github/workflows/backend-ci.yml`. При push в `main` или `feature/**`, а также при Pull Request в `main` или `feature/tests` запускается сборка на Java 21 и Maven `clean verify`. Первый запуск CI и проверка PR завершились успешно.

## Конкурентное бронирование

При одновременной записи двух учеников на последнее свободное место есть риск превысить вместимость занятия. Чтобы этого не происходило, `BookingService` внутри транзакции загружает тренировку с `PESSIMISTIC_WRITE` (`SELECT ... FOR UPDATE` в PostgreSQL). Проверка свободных мест выполняется после получения блокировки.

Для этого случая есть отдельный интеграционный тест: два ученика пытаются занять одно место, успешным должен быть только один запрос.

## В планах

- Заказы и абонементы (`Order`, `Subscription`) в основном backend.
- Отдельный `Payment Service` со своей PostgreSQL, имитацией платежного шлюза и Redis.
- Kafka и сервис уведомлений.
- Интеграция существующего Telegram-бота и Web-клиент.

Эти части пока не реализованы.
