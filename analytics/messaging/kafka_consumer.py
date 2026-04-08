import json
from confluent_kafka import Consumer
from config import Config
from utils.logger import get_logger

logger = get_logger("KafkaConsumer")


class KafkaConsumerWrapper:

    def __init__(self):
        self.consumer = Consumer({
            "bootstrap.servers": Config.KAFKA_BOOTSTRAP_SERVERS,
            "group.id": Config.KAFKA_GROUP_ID,
            "auto.offset.reset": "earliest"
        })

        self.consumer.subscribe([Config.REQUEST_TOPIC])

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