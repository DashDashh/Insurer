from messaging.router import MessageRouter
from messaging.kafka_consumer import KafkaConsumerWrapper
from messaging.kafka_producer import KafkaProducerWrapper
from messaging.mqtt_client import MQTTClient
from config import Config
from utils.logger import get_logger

logger = get_logger("Runner")


class MessagingRunner:

    def __init__(self):
        self.router = MessageRouter()

    def start(self):
        if Config.MESSAGING_PROFILE == "kafka":
            self._start_kafka()
        else:
            self._start_mqtt()

    def _start_kafka(self):
        consumer = KafkaConsumerWrapper()
        producer = KafkaProducerWrapper()

        while True:
            message = consumer.poll()

            if not message:
                continue

            try:
                response = self.router.route(message)

                target_topic = f"systems.{message['sender']}"

                producer.send(target_topic, response)

            except Exception as e:
                logger.error(f"Error processing message: {e}")

    def _start_mqtt(self):

        def callback(message):
            try:
                response = self.router.route(message)
                target_topic = f"systems.{message['sender']}"
                return response, target_topic

            except Exception as e:
                logger.error(f"Error processing message: {e}")
                return None, None

        client = MQTTClient(callback)
        client.connect()
        client.loop()