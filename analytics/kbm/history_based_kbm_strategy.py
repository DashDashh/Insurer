from typing import List
from kbm.kbm_strategy import KbmStrategy
from domain.models import IncidentRecord


class HistoryBasedKbmStrategy(KbmStrategy):

    def calculate(self, current_kbm: float, history: List[IncidentRecord]) -> float:
        total_damage = sum(i.damage_amount for i in history)

        # масштабируем влияние ущерба
        factor = 1 + total_damage / 1_000_000

        return current_kbm * factor