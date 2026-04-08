from services.kbm_service import KbmService
from domain.models import KbmRequest, IncidentRecord


class KbmHandler:

    def __init__(self):
        self.service = KbmService()

    def handle(self, payload: dict) -> dict:

        manufacturer_history = [
            IncidentRecord(damage_amount=i["damage_amount"])
            for i in payload.get("manufacturer_history", [])
        ]

        operator_history = [
            IncidentRecord(damage_amount=i["damage_amount"])
            for i in payload.get("operator_history", [])
        ]

        request = KbmRequest(
            manufacturer_kbm=payload["manufacturer_kbm"],
            operator_kbm=payload["operator_kbm"],
            manufacturer_history=manufacturer_history,
            operator_history=operator_history
        )

        result = self.service.recalculate(request)

        return {
            "new_manufacturer_kbm": result.new_manufacturer_kbm,
            "new_operator_kbm": result.new_operator_kbm
        }