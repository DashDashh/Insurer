# Интеграционные тесты страховщика

В этой папке находятся интеграционные тесты сервиса страховой компании.
Тесты работают с Kafka или MQTT: отправляют запрос в request topic и получают ответ из response topic.

Основной файл с тестами: insurance_integration_test.go.

## Что покрыто

- CALCULATION: успешный расчет стоимости и КБМ.
- PURCHASE + POLICY_TERMINATION: создание полиса и успешное закрытие.
- INCIDENT: успешная обработка инцидента с выплатой и пересчетом КБМ.
- INCIDENT без incident payload: ошибка валидации (FAILED).
- POLICY_TERMINATION для несуществующего заказа: ошибка (FAILED).

## Быстрый запуск

Из корня репозитория:

```bash
make integration-test
```

Эта команда:
- поднимает нужный брокер и insurance-service;
- запускает go test внутри контейнера tests;
- после завершения останавливает окружение.

## Ручной запуск

1. Поднять окружение:

```bash
make docker-up
```

2. Запустить тесты в контейнере:

```bash
docker compose run --build --rm --entrypoint go tests test -race -v ./...
```

3. Остановить окружение:

```bash
make docker-down
```

## Переменные окружения

Тесты можно гибко настроить через env:

- BROKER_TYPE: kafka или mqtt, выбирает профиль запуска.
- KAFKA_BROKERS: список брокеров через запятую.
	Пример: kafka:29092,localhost:9092
- MQTT_SERVER: адрес MQTT брокера.
	Пример: tcp://mosquitto:1883
- MQTT_USERNAME: логин для MQTT (опционально).
- MQTT_PASSWORD: пароль для MQTT (опционально).
- INSURANCE_REQUEST_TOPIC: явное имя request topic.
- INSURANCE_RESPONSE_TOPIC: явное имя response topic.

Если топики не заданы явно, используются дефолты:

- systems.insurer
- systems.tests

## Локальный запуск из папки tests

```bash
go test -race -v ./...
```

Пример запуска конкретного теста:

```bash
go test -race -v -run TestIncidentRequestSuccess ./...
```
