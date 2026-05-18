import json
import paho.mqtt.client as mqtt

from config import Config
from utils.logger import get_logger

logger = get_logger("MQTT")


class MQTTClient:

    def __init__(self, router):

        self.router = router

        self.client = mqtt.Client()

        self.client.on_connect = self.on_connect
        self.client.on_message = self.on_message

    def connect(self):

        logger.info(
            f"Connecting to MQTT broker "
            f"{Config.MQTT_HOST}:{Config.MQTT_PORT}"
        )

        self.client.connect(
            Config.MQTT_HOST,
            Config.MQTT_PORT,
            60
        )

    def loop(self):

        self.client.loop_forever()

    def on_connect(self, client, userdata, flags, rc):

        if rc == 0:
            logger.info("Connected to MQTT broker")

            client.subscribe(
                Config.REQUEST_TOPIC
            )

            logger.info(
                f"Subscribed to topic: "
                f"{Config.REQUEST_TOPIC}"
            )

        else:
            logger.error(
                f"MQTT connection failed: {rc}"
            )

    def on_message(self, client, userdata, msg):

        try:

            raw = msg.payload.decode()

            logger.info(f"Received MQTT: {raw}")

            message = json.loads(raw)

            response = self.router.route(message)

            target_topic = self._resolve_reply_topic(
                message
            )

            self.publish(
                target_topic,
                response
            )

        except Exception as e:

            logger.error(
                f"MQTT processing error: {e}"
            )

    def publish(self, topic, message):

        payload = json.dumps(message)

        self.client.publish(
            topic,
            payload
        )

        logger.info(
            f"Published response to {topic}"
        )

    def _resolve_reply_topic(self, message):

        reply_to = message.get("reply_to")

        if reply_to:
            return reply_to

        sender = message.get("sender")

        if sender:
            return f"systems/{sender}"

        return "systems/analytics-debug"