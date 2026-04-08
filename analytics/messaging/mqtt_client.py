import json
import paho.mqtt.client as mqtt
from config import Config
from utils.logger import get_logger

logger = get_logger("MQTT")


class MQTTClient:

    def __init__(self, on_message_callback):
        self.client = mqtt.Client()
        self.client.on_message = self._on_message

        self.on_message_callback = on_message_callback

    def connect(self):
        self.client.connect(Config.MQTT_HOST, Config.MQTT_PORT)
        self.client.subscribe(Config.REQUEST_TOPIC)

    def _on_message(self, client, userdata, msg):
        data = json.loads(msg.payload)

        response, target_topic = self.on_message_callback(data)

        if response:
            client.publish(target_topic, json.dumps(response))

    def loop(self):
        self.client.loop_forever()