import os


class Config:
    # broker
    MESSAGING_PROFILE = os.getenv("MESSAGING_PROFILE", "kafka")  # kafka | mqtt

    # kafka
    KAFKA_BOOTSTRAP_SERVERS = os.getenv("KAFKA_BOOTSTRAP_SERVERS", "127.0.0.1:9092")
    KAFKA_GROUP_ID = "insurance-analytics-v2"

    # mqtt
    MQTT_HOST = os.getenv("MQTT_HOST", "mosquitto")
    MQTT_PORT = int(os.getenv("MQTT_PORT", 1883))

    # topics
    REQUEST_TOPIC = "component.agregator_analytics"

    # business config
    BASE_RATE = float(os.getenv("BASE_RATE", 1000))