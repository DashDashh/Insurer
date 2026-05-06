@echo off
echo ==============================
echo   INSURANCE ANALYTICS TESTS
echo ==============================

echo.
echo [1] CALCULATION TEST...

echo { "action": "CALCULATION", "sender": "test-client", "payload": { "manufacturer_kbm": 1.0, "operator_kbm": 1.0, "security_goals": ["ЦБ1"], "required_goals": ["ЦБ1", "ЦБ2"], "coverage_amount": 5000000 } } > calc.json

docker exec -i kafka /opt/kafka/bin/kafka-console-producer.sh --topic systems.insurance_system --broker-list localhost:9092 < calc.json

timeout /t 2 > nul


echo.
echo [2] INCIDENT TEST...

echo { "action": "INCIDENT", "sender": "test-client", "payload": { "coverage_amount": 5000000, "incident": { "incident_id": "inc-001", "order_id": "order-123", "policy_id": "policy-123", "damage_amount": 150000 } } } > incident.json

docker exec -i kafka /opt/kafka/bin/kafka-console-producer.sh --topic systems.insurance_system --broker-list localhost:9092 < incident.json

timeout /t 2 > nul


echo.
echo [3] KBM UPDATE TEST...

echo { "action": "KBM_UPDATE", "sender": "test-client", "payload": { "manufacturer_kbm": 1.0, "operator_kbm": 1.0, "manufacturer_history": [ { "damage_amount": 100000 }, { "damage_amount": 200000 } ], "operator_history": [ { "damage_amount": 300000 } ] } } > kbm.json

docker exec -i kafka /opt/kafka/bin/kafka-console-producer.sh --topic systems.insurance_system --broker-list localhost:9092 < kbm.json

timeout /t 2 > nul


echo.
echo ==============================
echo   ALL TESTS SENT
echo ==============================
echo.

pause