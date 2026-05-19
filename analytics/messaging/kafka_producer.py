import json
from confluent_kafka import Producer
from config import Config


class KafkaProducerWrapper:

    def __init__(self):
        self.producer = Producer({
            "bootstrap.servers": Config.KAFKA_BOOTSTRAP_SERVERS
        })

    def send(self, topic: str, message: dict):
        self.producer.produce(topic, json.dumps(message))
        self.producer.flush()