from services.incident_service import IncidentService
from domain.models import IncidentRequest, Incident


class IncidentHandler:

    def __init__(self):
        self.service = IncidentService()

    def handle(self, payload: dict) -> dict:
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
            "payment_amount": result.payment_amount,
            "message": result.message,
            "is_fraud": result.is_fraud
        }