from messaging.router import MessageRouter
from messaging.kafka_consumer import KafkaConsumerWrapper
from messaging.kafka_producer import KafkaProducerWrapper
from messaging.mqtt_client import MQTTClient

from handlers.calculation_handler import CalculationHandler
from handlers.incident_handler import IncidentHandler
from handlers.kbm_handler import KbmHandler

from config import Config
from utils.logger import get_logger

logger = get_logger("Runner")


class MessagingRunner:

    def __init__(self, calculator_service):
        self.router = MessageRouter({
            "CALCULATION": CalculationHandler(calculator_service),
            "INCIDENT": IncidentHandler(),
            "KBM_UPDATE": KbmHandler()
        })

    def start(self):
        logger.info(
            f"Starting messaging runner with profile: "
            f"{Config.MESSAGING_PROFILE}"
        )

        if Config.MESSAGING_PROFILE == "kafka":
            self._start_kafka()
        else:
            self._start_mqtt()

    def _start_kafka(self):
        logger.info("Starting Kafka consumer...")

        consumer = KafkaConsumerWrapper()
        producer = KafkaProducerWrapper()

        while True:
            message = consumer.poll()

            if not message:
                continue

            logger.info(f"Received message: {message}")

            try:
                response = self.router.route(message)

                sender = message.get("sender", "unknown")
                target_topic = f"systems.{sender}"

                logger.info(
                    f"Sending response to topic: {target_topic}"
                )

                producer.send(target_topic, response)

            except Exception as e:
                logger.error(f"Error processing message: {e}")

    def _start_mqtt(self):

        logger.info("Starting MQTT consumer...")

        def callback(message):

            logger.info(f"Received MQTT message: {message}")

            try:
                response = self.router.route(message)

                sender = message.get("sender", "unknown")
                target_topic = f"systems.{sender}"

                logger.info(
                    f"Sending MQTT response to: {target_topic}"
                )

                return response, target_topic

            except Exception as e:
                logger.error(f"Error processing message: {e}")
                return None, None

        client = MQTTClient(callback)

        client.connect()
        client.loop()
