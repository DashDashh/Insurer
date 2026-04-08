from services.calculation_service import InsuranceCalculatorService
from risk.basic_risk_strategy import BasicRiskStrategy
from domain.models import CalculationRequest


class CalculationHandler:

    def __init__(self):
        self.calculator = InsuranceCalculatorService(
            BasicRiskStrategy()
        )

    def handle(self, payload: dict) -> dict:
        request = CalculationRequest(
            manufacturer_kbm=payload["manufacturer_kbm"],
            operator_kbm=payload["operator_kbm"],
            security_goals=payload["security_goals"],
            required_goals=payload.get("required_goals", []),
            coverage_amount=payload["coverage_amount"]
        )

        result = self.calculator.calculate(request)

        return {
            "calculated_cost": result.calculated_cost,
            "risk_score": result.risk_score
        }