from handlers.calculation_handler import CalculationHandler
from handlers.incident_handler import IncidentHandler
from handlers.kbm_handler import KbmHandler
from domain.enums import ActionType


class MessageRouter:

    def __init__(self):
        self.calculation_handler = CalculationHandler()
        self.incident_handler = IncidentHandler()
        self.kbm_handler = KbmHandler()

    def route(self, message: dict) -> dict:
        action = message.get("action")
        payload = message.get("payload", {})

        if action == ActionType.CALCULATION:
            return self.calculation_handler.handle(payload)

        elif action == ActionType.INCIDENT:
            return self.incident_handler.handle(payload)

        elif action == ActionType.KBM_UPDATE:
            return self.kbm_handler.handle(payload)

        else:
            raise ValueError(f"Unknown action: {action}")