from typing import List
from kbm.kbm_strategy import KbmStrategy
from domain.models import IncidentRecord


class BasicKbmStrategy(KbmStrategy):

    def calculate(self, current_kbm: float, history: List[IncidentRecord]) -> float:
        incidents_count = len(history)

        # каждый инцидент увеличивает КБМ на 10%
        return current_kbm * (1 + 0.1 * incidents_count)