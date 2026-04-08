class PayoutCalculator:

    def calculate(self, coverage: float, damage: float) -> float:
        # базовое правило страхования
        return min(coverage, damage)