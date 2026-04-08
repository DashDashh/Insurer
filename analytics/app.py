from messaging.router_runner import MessagingRunner
from utils.logger import get_logger

logger = get_logger("App")


def main():
    logger.info("Starting analytics service...")

    runner = MessagingRunner()
    runner.start()


if __name__ == "__main__":
    main()