from abc import ABC, abstractmethod
from typing import List
from domain.models import IncidentRecord


class KbmStrategy(ABC):

    @abstractmethod
    def calculate(self, current_kbm: float, history: List[IncidentRecord]) -> float:
        pass