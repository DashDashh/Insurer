from domain.models import CalculationRequest, CalculationResult
from risk.risk_strategy import RiskStrategy


class InsuranceCalculatorService:

    def __init__(self, risk_strategy: RiskStrategy):
        self.risk_strategy = risk_strategy
        self.base_rate = 1000  # можно вынести в конфиг

    def calculate(self, request: CalculationRequest) -> CalculationResult:
        risk = self.risk_strategy.calculate(
            request.security_goals,
            request.required_goals
        )

        price = (
            self.base_rate
            * request.manufacturer_kbm
            * request.operator_kbm
            * risk
            * self._coverage_factor(request.coverage_amount)
        )

        return CalculationResult(
            calculated_cost=price,
            risk_score=risk
        )

    def _coverage_factor(self, coverage: float) -> float:
        # простая нормализация
        return 1 + coverage / 10_000_000