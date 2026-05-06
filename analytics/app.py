from messaging.router_runner import MessagingRunner
from utils.logger import get_logger

from risk.basic_risk_strategy import BasicRiskStrategy
from services.calculation_service import InsuranceCalculatorService

logger = get_logger("App")


def main():
    logger.info("Starting analytics service...")

    # выбираем стратегию
    risk_strategy = BasicRiskStrategy()

    calculator = InsuranceCalculatorService(risk_strategy)

    runner = MessagingRunner(calculator)
    runner.start()


if __name__ == "__main__":
    main()