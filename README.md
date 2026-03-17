# Insurer
Сервис страховой компании для обработки инцидентов и рассчёта полисов.

## Стек
- Java 17
- Apache Kafka
- Docker
- H2 db (inmemory база данных, позже переведём на PostgreSQL)

## Запуск
```bash
make docker-up

# Запуск с нужным количеством реплик insurance-service
make docker-up INSURANCE_REPLICAS=<Количество реплик сервиса>
```

## Форматы сообщений для брокера

### Топики
- Формат имени топика: v1.Insurer.<id>.insurer-service.<requests/responses>.
- Пример: v1.Insurer.ad6e.insurer-service.requests.
- В качестве id используется HOSTNAME контейнера.
- На данный момент осуществляется общение через 2 топика "<...>.requests" и "<...>.responses" для запросов и ответов соответственно. Тип запроса определяется с помощью поля requestType, в response указывается также id request'а, которому он соответствует.

### Проверка стоимости, покупка полиса, закрытие полиса. Топик "<...>.requests".
Покупка полиса и его закрытие после выполнения заказа осуществляется аналогичным реквестом, где меняется requestType на PURCHASE и POLICY_TERMINATION соответственно.
```json
{
  "requestId": "req-123-2026",
  "orderId": "order-456-2026",
  "manufacturerId": "manuf-789",
  "operatorId": "oper-012",
  "droneId": "drone-345",
  
  "droneSafetyPurpose": "COMMERCIAL_DELIVERY",
  "requiredSafetyPurpose": "COMMERCIAL_DELIVERY",
  
  "calculationId": "calc-678-2026",
  
  "incident": null  
  "requestType": "CALCULATION"
}
```

### Обработка инцидента. Топик "<...>.requests".
```json
{
  "requestId": "req-123-2026",
  "orderId": "order-456-2026",
  "manufacturerId": "manuf-789",
  "operatorId": "oper-012",
  "droneId": "drone-345",
  
  "droneSafetyPurpose": "COMMERCIAL_DELIVERY",
  "requiredSafetyPurpose": "COMMERCIAL_DELIVERY",
  
  "calculationId": "calc-678-2026",
  
  "incident": {
    "incidentId": "inc-999-2026",
    "orderId": "order-456-2026",
    "policyId": "pol-777-2026",
    "damageAmount": 15000.50,
    "incidentDate": "2026-03-02T18:30:00",
    "status": "REPORTED"
  },  
  "requestType": "INCIDENT"
}
```

### Ответ. Топик "<...>.responses".
Формирование осуществляется на основе модели (поля, не соответствующие типу запроса остаются null):
```java
    private String responseId;
    private String requestId;
    private String orderId;
    private String policyId;
    private ResponseStatus status;
    private String message;

    // Для расчёта
    private BigDecimal calculatedCost;
    private BigDecimal manufacturerKbm;
    private BigDecimal operatorKbm;

    // Для полиса
    private LocalDateTime policyStartDate;
    private LocalDateTime policyEndDate;

    // Для инцидентов
    private BigDecimal coverageAmount;
    private BigDecimal paymentAmount;
    private BigDecimal newManufacturerKbm;
    private BigDecimal newOperatorKbm;

    public enum ResponseStatus {
        SUCCESS,
        FAILED
    }
```

пример для расчёта стоимости полиса:
```json
{
  "responseId": "resp-123-2026",
  "requestId": "req-123-2026",
  "orderId": "order-456-2026",
  "policyId": "pol-777-2026",
  "status": "SUCCESS",
  "message": "Расчет стоимости страхования выполнен успешно",
  "calculatedCost": 15000.50,
  "manufacturerKbm": 0.85,
  "operatorKbm": 1.10,
  "policyStartDate": null,
  "policyEndDate": null,
  "coverageAmount": null,
  "paymentAmount": null,
  "newManufacturerKbm": null,
  "newOperatorKbm": null
}
```
