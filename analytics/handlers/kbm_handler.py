import uuid
import time

from services.kbm_service import KbmService

from domain.models import (
    KbmRequest,
    IncidentRecord
)


class KbmHandler:

    def __init__(self):
        self.service = KbmService()

    def handle(self, message: dict) -> dict:

        payload = message["payload"]

        manufacturer_history = [
            IncidentRecord(
                damage_amount=i["damage_amount"]
            )
            for i in payload.get("manufacturer_history", [])
        ]

        operator_history = [
            IncidentRecord(
                damage_amount=i["damage_amount"]
            )
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
            "message_id": str(uuid.uuid4()),

            "action": "KBM_UPDATE_RESULT",

            "sender": "analytics",

            "correlation_id": message.get("correlation_id"),

            "timestamp": int(time.time() * 1000),

            "message_type": "response",

            "success": True,

            "payload": {

                # идентификаторы
                "operator_id": payload.get("operator_id"),
                "manufacturer_id": payload.get("manufacturer_id"),
                "drone_id": payload.get("drone_id"),
                "order_id": payload.get("order_id"),

                # старые коэффициенты
                "old_manufacturer_kbm": payload["manufacturer_kbm"],
                "old_operator_kbm": payload["operator_kbm"],

                # новые коэффициенты
                "new_manufacturer_kbm": result.new_manufacturer_kbm,
                "new_operator_kbm": result.new_operator_kbm,

                # статистика
                "manufacturer_incidents_count": len(manufacturer_history),
                "operator_incidents_count": len(operator_history)
            }
        }