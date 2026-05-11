from utils.logger import get_logger

logger = get_logger("Router")


class MessageRouter:

    def __init__(self, handlers):
        self.handlers = handlers

    def route(self, message):

        action = message.get("action")

        if not action:
            raise Exception("Missing action")

        handler = self.handlers.get(action)

        if not handler:
            raise Exception(f"No handler for action: {action}")

        payload = message.get("payload", {})

        logger.info(f"Routing action: {action}")

        return handler.handle(payload)