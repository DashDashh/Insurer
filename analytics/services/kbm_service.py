from domain.models import KbmRequest, KbmResult
from kbm.basic_kbm_strategy import BasicKbmStrategy


class KbmService:

    def __init__(self):
        # можно потом внедрять разные стратегии
        self.manufacturer_strategy = BasicKbmStrategy()
        self.operator_strategy = BasicKbmStrategy()

    def recalculate(self, request: KbmRequest) -> KbmResult:

        new_manufacturer_kbm = self.manufacturer_strategy.calculate(
            request.manufacturer_kbm,
            request.manufacturer_history
        )

        new_operator_kbm = self.operator_strategy.calculate(
            request.operator_kbm,
            request.operator_history
        )

        return KbmResult(
            new_manufacturer_kbm=new_manufacturer_kbm,
            new_operator_kbm=new_operator_kbm
        )