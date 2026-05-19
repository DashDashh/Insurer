import json
from confluent_kafka import Consumer
from config import Config
from utils.logger import get_logger
import time

logger = get_logger("KafkaConsumer")


class KafkaConsumerWrapper:

    def __init__(self, max_retries=15, retry_delay=5):
        self.consumer = None
        self._connect_with_retry(max_retries, retry_delay)

    def _connect_with_retry(self, max_retries, retry_delay):
        for attempt in range(1, max_retries + 1):
            try:
                logger.info(f"Attempt {attempt}/{max_retries} to connect to Kafka...")
                self.consumer = Consumer({
                    "bootstrap.servers": Config.KAFKA_BOOTSTRAP_SERVERS,
                    "group.id": Config.KAFKA_GROUP_ID,
                    "auto.offset.reset": "earliest",
                    "allow.auto.create.topics": "true",
                    "socket.timeout.ms": 10000,
                    "session.timeout.ms": 10000
                })

                # Проверяем подключение, запрашивая метаданные
                self.consumer.list_topics(timeout=5)

                logger.info(f"Successfully connected to Kafka at {Config.KAFKA_BOOTSTRAP_SERVERS}")

                # Подписываемся на топик
                self.consumer.subscribe([Config.REQUEST_TOPIC])
                logger.info(f"Subscribed to topic: {Config.REQUEST_TOPIC}")

                return  # Успех - выходим из метода

            except Exception as e:
                logger.warning(f"Connection failed: {e}")
                if attempt < max_retries:
                    logger.info(f"Retrying in {retry_delay} seconds...")
                    time.sleep(retry_delay)
                else:
                    logger.error(f"Failed to connect to Kafka after {max_retries} attempts")
                    raise

    def poll(self):
        msg = self.consumer.poll(1.0)

        if msg is None:
            return None

        if msg.error():
            logger.error(msg.error())
            return None
        
        raw = msg.value()
        print("RAW MESSAGE:", raw)

        return json.loads(msg.value())