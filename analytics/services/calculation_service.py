from domain.models import CalculationRequest, CalculationResult
from risk.risk_strategy import RiskStrategy


class InsuranceCalculatorService:

    def __init__(self, risk_strategy: RiskStrategy):
        self.risk_strategy = risk_strategy
        self.base_rate = 1000

    def _coverage_factor(self, coverage: float) -> float:
        return 1 + coverage / 10_000_000

    def calculate(self, request: CalculationRequest) -> CalculationResult:
        risk = self.risk_strategy.calculate(request)

        price = (
            self.base_rate
            * risk
            * self._coverage_factor(request.coverage_amount)
        )

        return CalculationResult(
            calculated_cost=price,
            risk_score=risk
        )