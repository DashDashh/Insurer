# Insurer
Сервис страховой компании для обработки инцидентов и рассчёта полисов.

## Стек
- Java 17
- Apache Kafka
- Mosquitto
- Docker
- H2 db (inmemory база данных, позже переведём на PostgreSQL)

## Запуск
```bash
make docker-up

# Запуск с нужным количеством реплик insurance-service
make docker-up INSURANCE_REPLICAS=<Количество реплик сервиса>
```

## Системные переменные для переключения между брокерами
В файле .env нужно установить необходимый профиль kafka/mqtt для соответствующего брокера
Для Mosquitto:
```
MESSAGING_PROFILE=mqtt
COMPOSE_PROFILES=mqtt
```
Для Kafka:
```
MESSAGING_PROFILE=kafka
COMPOSE_PROFILES=kafka
```


## Форматы сообщений для брокера

### Топики
- Формат имени топика: v1.Insurer.<replication_id>.insurer-service.<requests/responses>.
- Пример: v1.Insurer.ad6e.insurer-service.requests.
- В качестве id используется HOSTNAME контейнера.
- На данный момент осуществляется общение через 2 топика "<...>.requests" и "<...>.responses" для запросов и ответов соответственно. Тип запроса определяется с помощью поля requestType, в response указывается также id request'а, которому он соответствует.

### Проверка стоимости, закрытие полиса. Топик "<...>.requests".
Покупка полиса и его закрытие после выполнения заказа осуществляется аналогичным реквестом, где меняется requestType на PURCHASE и POLICY_TERMINATION соответственно.
```json
{
  "request_id": "req-20260319-001",
  "order_id": "order-12345",
  "manufacturer_id": "manuf-001",
  "operator_id": "oper-001",
  "drone_id": "drone-789",
  "security_goals": [
    "ЦБ1",
    "ЦБ2"
  ],
  "coverage_amount": 5000000.00,
  "calculation_id": "calc-678-2026",
  "incident": null,
  "request_type": "CALCULATION"
}
```

### Покупка полиса. Топик "<...>.requests".
```json
{
  "request_id": "req-20260319-001",
  "order_id": "order-12345",
  "manufacturer_id": "manuf-001",
  "operator_id": "oper-001",
  "drone_id": "drone-789",
  "security_goals": [
    "ЦБ1",
    "ЦБ2"
  ],
  "coverage_amount": 5000000.00,
  "calculation_id": "calc-678-2026",
  "incident": null,
  "request_type": "PURCHASE"
}
```
### Обработка инцидента. Топик "<...>.requests".
```json
{
  "request_id": "req-20260320-001",
  "order_id": "order-12345",
  "manufacturer_id": "manuf-001",
  "operator_id": "oper-001",
  "drone_id": "drone-789",
  "security_goals": ["ЦБ1", "ЦБ2"],
  "coverage_amount": 5000000.00,
  "calculation_id": null,
  "incident": {
    "id": null,
    "incident_id": "inc-20260320-001",
    "order_id": "order-12345",
    "policy_id": "401479e6-9021-477f-83f9-50efd1e64da3",
    "damage_amount": 150000.00,
    "incident_date": "2026-03-20T14:30:00",
    "status": "REPORTED"
  },
  "request_type": "INCIDENT"
}
```

### Прекращение полиса. Топик "<...>.requests".
```json
{
  "request_id": "req-20260320-003",
  "order_id": "order-12345",
  "manufacturer_id": "manuf-001",
  "operator_id": "oper-001",
  "drone_id": "drone-789",
  "security_goals": ["ЦБ1", "ЦБ2"],
  "coverage_amount": 5000000.00,
  "calculation_id": null,
  "incident": null,
  "request_type": "POLICY_TERMINATION"
}
```

### Ответ. Топик "<...>.responses".
Поля, не соответствующие типу запроса остаются null

### Пример для расчёта стоимости полиса:
```json
{
  "calculated_cost": 1000.00,
  "coverage_amount": 5000000.00,
  "manufacturer_kbm": 1.0,
  "message": "Расчёт выполнен успешно",
  "new_manufacturer_kbm": null,
  "new_operator_kbm": null,
  "operator_kbm": 1.0,
  "order_id": "order-12345",
  "payment_amount": null,
  "policy_end_date": null,
  "policy_id": null,
  "policy_start_date": null,
  "request_id": "req-20260319-001",
  "response_id": "447e2e4a-8c1f-44d0-af1d-47c4381852d1",
  "status": "SUCCESS"
}
```

### Пример для покупки полиса:
```json
{
  "calculated_cost": 1000,
  "coverage_amount": 5000000.00,
  "manufacturer_kbm": null,
  "message": "Полис успешно оформлен",
  "new_manufacturer_kbm": null,
  "new_operator_kbm": null,
  "operator_kbm": null,
  "order_id": "order-12345",
  "payment_amount": null,
  "policy_end_date": "2026-04-19T18:08:04.101425209",
  "policy_id": "401479e6-9021-477f-83f9-50efd1e64da3",
  "policy_start_date": "2026-03-20T18:08:04.101357411",
  "request_id": "req-20260319-001",
  "response_id": "daab02dc-a533-4c42-9d68-f60181716a22",
  "status": "SUCCESS"
}
```
### Пример для обработки инцидента:
```json
{
  "calculated_cost": null,
  "coverage_amount": 150000.00,
  "manufacturer_kbm": null,
  "message": "Инцидент обработан, произведена выплата",
  "new_manufacturer_kbm": 1.10,
  "new_operator_kbm": 1.10,
  "operator_kbm": null,
  "order_id": "order-12345",
  "payment_amount": 150000.00,
  "policy_end_date": null,
  "policy_id": null,
  "policy_start_date": null,
  "request_id": "req-20260320-001",
  "response_id": "be4bfac6-a38f-4803-af40-55b9e0752560",
  "status": "SUCCESS"
}
```
### Пример для прекращения полиса:
```json
{
  "calculated_cost": null,
  "coverage_amount": null,
  "manufacturer_kbm": null,
  "message": "Полис успешно прекращён",
  "new_manufacturer_kbm": null,
  "new_operator_kbm": null,
  "operator_kbm": null,
  "order_id": "order-12345",
  "payment_amount": null,
  "policy_end_date": null,
  "policy_id": null,
  "policy_start_date": null,
  "request_id": "req-20260320-003",
  "response_id": "43eddff1-bfec-42c0-8fbb-b4ccc7b90d02",
  "status": "SUCCESS"
}
```
