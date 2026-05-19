import os


class Config:

    # =========================
    # Messaging
    # =========================

    MESSAGING_PROFILE = os.getenv(
        "MESSAGING_PROFILE",
        "kafka"
    )

    # =========================
    # Kafka
    # =========================

    KAFKA_BOOTSTRAP_SERVERS = os.getenv(
        "KAFKA_BOOTSTRAP_SERVERS",
        "kafka:29092"
    )
    

    KAFKA_GROUP_ID = os.getenv(
        "KAFKA_GROUP_ID",
        "analytics-group"
    )

    # =========================
    # MQTT
    # =========================

    MQTT_HOST = os.getenv(
        "MQTT_HOST",
        "mosquitto"
    )

    MQTT_PORT = int(
        os.getenv(
            "MQTT_PORT",
            1883
        )
    )

    # =========================
    # Topics
    # =========================

    REQUEST_TOPIC = os.getenv(
        "REQUEST_TOPIC",
        "component.insurer_analytics"
    )

    RESPONSE_TOPIC = os.getenv(
        "RESPONSE_TOPIC",
        "systems.analytics"
    )