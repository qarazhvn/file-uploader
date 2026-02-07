# Async File Uploader

Сервис асинхронной загрузки файлов на Spring Boot.

## Что это

REST API для загрузки файлов в S3-совместимое хранилище (MinIO). Файлы принимаются через HTTP, обрабатываются асинхронно и сохраняются в MinIO. Метаданные хранятся в PostgreSQL.

## Стек

- Java 17, Spring Boot 3
- PostgreSQL 15
- MinIO
- Docker

## Быстрый старт

```bash
git clone https://github.com/qarazhvn/file-uploader.git
cd file-uploader
docker compose up --build
```

После запуска:
- Swagger UI: http://localhost:8080/swagger-ui.html
- MinIO Console: http://localhost:9001 (minioadmin / minioadmin)

## Особенности

- **Асинхронная обработка** — HTTP-запрос не блокируется на время загрузки
- **Идемпотентность** — повторный запрос с тем же ключом не создаёт дубликат
- **Консистентность** — при ошибке загрузки происходит откат

## Документация