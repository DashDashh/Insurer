import uuid
import time

from domain.models import CalculationRequest


class CalculationHandler:

    def __init__(self, calculator_service):
        self.calculator_service = calculator_service

    def handle(self, message):

        payload = message["payload"]

        request = CalculationRequest(
            manufacturer_kbm=payload["manufacturer_kbm"],
            operator_kbm=payload["operator_kbm"],
            security_goals=payload["security_goals"],
            required_goals=payload["required_goals"],
            coverage_amount=payload["coverage_amount"]
        )

        result = self.calculator_service.calculate(request)

        return {
            "message_id": str(uuid.uuid4()),
            "action": "CALCULATION_RESULT",
            "sender": "analytics",
            "correlation_id": message.get("correlation_id"),
            "timestamp": int(time.time() * 1000),
            "message_type": "response",
            "success": True,

            "payload": {

                # идентификаторы
                "order_id": payload.get("order_id"),
                "operator_id": payload.get("operator_id"),
                "manufacturer_id": payload.get("manufacturer_id"),
                "drone_id": payload.get("drone_id"),

                # бизнес-данные
                "calculated_cost": result.calculated_cost,
                "risk_score": result.risk_score,

                # доп. поля
                "coverage_amount": payload.get("coverage_amount"),
                "security_goals": payload.get("security_goals"),
                "required_goals": payload.get("required_goals")
            }
        }