from domain.models import CalculationRequest


class CalculationHandler:

    def __init__(self, calculator_service):
        self.calculator_service = calculator_service

    def handle(self, payload):

        request = CalculationRequest(
            manufacturer_kbm=payload["manufacturer_kbm"],
            operator_kbm=payload["operator_kbm"],
            security_goals=payload["security_goals"],
            required_goals=payload["required_goals"],
            coverage_amount=payload["coverage_amount"]
        )

        result = self.calculator_service.calculate(request)

        return {
            "status": "SUCCESS",
            "action": "CALCULATION_RESULT",
            "payload": {
                "calculated_cost": result.calculated_cost,
                "risk_score": result.risk_score
            }
        }