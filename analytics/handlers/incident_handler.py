import uuid
import time

from services.incident_service import IncidentService

from domain.models import (
    IncidentRequest,
    Incident
)


class IncidentHandler:

    def __init__(self):
        self.service = IncidentService()

    def handle(self, message: dict) -> dict:

        payload = message["payload"]

        incident_data = payload["incident"]

        incident = Incident(
            incident_id=incident_data["incident_id"],
            order_id=incident_data["order_id"],
            policy_id=incident_data["policy_id"],
            damage_amount=incident_data["damage_amount"]
        )

        request = IncidentRequest(
            incident=incident,
            coverage_amount=payload["coverage_amount"]
        )

        result = self.service.process(request)

        return {
            "message_id": str(uuid.uuid4()),

            "action": "INCIDENT_RESULT",

            "sender": "analytics",

            "correlation_id": message.get("correlation_id"),

            "timestamp": int(time.time() * 1000),

            "message_type": "response",

            "success": True,

            "payload": {

                # идентификаторы
                "order_id": incident.order_id,
                "policy_id": incident.policy_id,
                "incident_id": incident.incident_id,

                "operator_id": payload.get("operator_id"),
                "manufacturer_id": payload.get("manufacturer_id"),
                "drone_id": payload.get("drone_id"),

                # результаты обработки
                "payment_amount": result.payment_amount,
                "message": result.message,
                "is_fraud": result.is_fraud,

                # исходные данные
                "damage_amount": incident.damage_amount,
                "coverage_amount": payload.get("coverage_amount")
            }
        }