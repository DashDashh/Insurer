from domain.models import IncidentRequest, IncidentResult
from payout.payout_calculator import PayoutCalculator
from payout.basic_fraud_detector import BasicFraudDetector


class IncidentService:

    def __init__(self):
        self.payout_calculator = PayoutCalculator()
        self.fraud_detector = BasicFraudDetector()

    def process(self, request: IncidentRequest) -> IncidentResult:
        incident = request.incident

        # 1. Проверка на фрод
        is_fraud = self.fraud_detector.is_fraud(incident)

        if is_fraud:
            return IncidentResult(
                payment_amount=0,
                is_fraud=True,
                message="Подозрение на мошенничество, выплата отклонена"
            )

        # 2. Расчёт выплаты
        payment = self.payout_calculator.calculate(
            request.coverage_amount,
            incident.damage_amount
        )

        return IncidentResult(
            payment_amount=payment,
            is_fraud=False,
            message="Инцидент обработан, произведена выплата"
        )