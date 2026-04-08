from typing import List
from risk.risk_strategy import RiskStrategy


class BasicRiskStrategy(RiskStrategy):

    def calculate(self, security_goals: List[str], required_goals: List[str]) -> float:
        missing = len(set(required_goals) - set(security_goals))
        return 1 + missing * 0.2