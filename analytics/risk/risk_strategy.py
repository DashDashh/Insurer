from abc import ABC, abstractmethod
from typing import List


class RiskStrategy(ABC):

    @abstractmethod
    def calculate(self, security_goals: List[str], required_goals: List[str]) -> float:
        pass