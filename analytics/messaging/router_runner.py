from messaging.router import MessageRouter

from messaging.kafka_consumer import (
    KafkaConsumerWrapper
)

from messaging.kafka_producer import (
    KafkaProducerWrapper
)

from messaging.mqtt_client import MQTTClient

from config import Config

from utils.logger import get_logger

from handlers.calculation_handler import (
    CalculationHandler
)

from handlers.incident_handler import (
    IncidentHandler
)

from handlers.kbm_handler import (
    KbmHandler
)

logger = get_logger("Runner")


class MessagingRunner:

    def __init__(self, calculator_service):

        self.router = MessageRouter({

            "CALCULATION":
                CalculationHandler(
                    calculator_service
                ),

            "INCIDENT":
                IncidentHandler(),

            "KBM_UPDATE":
                KbmHandler()
        })

    def start(self):

        logger.info(
            f"Starting messaging runner "
            f"with profile: "
            f"{Config.MESSAGING_PROFILE}"
        )

        if Config.MESSAGING_PROFILE == "mqtt":
            self._start_mqtt()
        else:
            self._start_kafka()

    def _start_mqtt(self):

        logger.info(
            "Starting MQTT consumer..."
        )

        client = MQTTClient(
            self.router
        )

        client.connect()

        client.loop()

    def _start_kafka(self):

        logger.info(
            "Starting Kafka consumer..."
        )

        consumer = KafkaConsumerWrapper()

        producer = KafkaProducerWrapper()

        while True:

            message = consumer.poll()

            if not message:
                continue

            try:

                response = self.router.route(
                    message
                )

                target_topic = (
                    self._resolve_reply_topic(
                        message
                    )
                )

                producer.send(
                    target_topic,
                    response
                )

            except Exception as e:

                logger.error(
                    f"Kafka processing error: {e}"
                )

    def _resolve_reply_topic(self, message):

        reply_to = message.get("reply_to")

        if reply_to:
            return reply_to

        sender = message.get("sender")

        if sender:
            return f"systems.{sender}"

        return "systems.analytics-debug"