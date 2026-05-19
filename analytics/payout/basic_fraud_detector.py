from payout.fraud_detector import FraudDetector
from domain.models import Incident


class BasicFraudDetector(FraudDetector):

    def is_fraud(self, incident: Incident) -> bool:
        # простая логика (можно заменить на ML)
        if incident.damage_amount > 1_000_000:
            return True
        return False