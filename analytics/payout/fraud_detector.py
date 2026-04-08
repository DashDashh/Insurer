from abc import ABC, abstractmethod
from domain.models import Incident


class FraudDetector(ABC):

    @abstractmethod
    def is_fraud(self, incident: Incident) -> bool:
        pass