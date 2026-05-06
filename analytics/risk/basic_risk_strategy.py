import math
from domain.models import CalculationRequest
from risk.risk_strategy import RiskStrategy

class BasicRiskStrategy:

    def calculate(self, request: CalculationRequest) -> float:
        security = set(request.security_goals)
        required = set(request.required_goals)

        gap = len(required - security)
        num_required = len(required)

        # КБМ
        r_kbm = math.sqrt(request.manufacturer_kbm * request.operator_kbm)

        # Сложность миссии
        r_mission = 1 + 0.15 * math.log1p(num_required)

        # GAP
        gap_ratio = gap / num_required if num_required > 0 else 0
        r_gap = 1 + 0.5 * (gap_ratio ** 2)

        # Покрытие
        r_coverage = math.log1p(request.coverage_amount / 1_000_000)

        # Стабилизация
        r_stability = 1 + math.tanh(r_kbm - 1)

        return r_kbm * r_mission * r_gap * r_coverage * r_stability