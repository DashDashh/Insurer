.PHONY: help unit-test integration-test tests docker-up docker-down docker-logs

CORE_SERVICES = zookeeper kafka kafdrop insurance-service
TEST_SERVICE = tests
INSURANCE_REPLICAS ?= 1
INSURANCE_INSTANCE_ID ?=
COMPOSE_FILE ?= docker-compose.yml
TEST_COMPOSE_FILE ?= docker-compose.dev.yml
DOCKER_COMPOSE = docker compose -f $(COMPOSE_FILE)

help:
	@echo "make docker-up         - Запустить систему (по умолчанию 1 реплика insurance-service)"
	@echo "                         Пример: make docker-up INSURANCE_REPLICAS=3"
	@echo "                         Опционально: make docker-up INSURANCE_INSTANCE_ID=1"
	@echo "make docker-down       - Остановить систему"
	@echo "make docker-logs       - Логи"
	@echo "make unit-test         - Unit тесты компонентов"
	@echo "make integration-test  - Интеграционные тесты (docker required)"
	@echo "make tests             - Все тесты"

docker-up:
	@INSURANCE_INSTANCE_ID=$(INSURANCE_INSTANCE_ID) $(DOCKER_COMPOSE) up -d --build --scale insurance-service=$(INSURANCE_REPLICAS) $(CORE_SERVICES)

docker-down:
	@$(DOCKER_COMPOSE) down 2>/dev/null

docker-logs:
	@$(DOCKER_COMPOSE) logs -f

unit-test:
	@mvn test

integration-test:
	@BROKER_TYPE=kafka docker compose -f $(TEST_COMPOSE_FILE) --profile kafka up -d --build insurance-service zookeeper kafka kafdrop
	@BROKER_TYPE=kafka docker compose -f $(TEST_COMPOSE_FILE) run --build --rm --entrypoint go $(TEST_SERVICE) test -race -v ./...
	-@docker compose -f $(TEST_COMPOSE_FILE) down 2>/dev/null
	@BROKER_TYPE=mqtt docker compose -f $(TEST_COMPOSE_FILE) --profile mqtt up -d --build insurance-service mosquitto
	@BROKER_TYPE=mqtt docker compose -f $(TEST_COMPOSE_FILE) run --build --rm --entrypoint go $(TEST_SERVICE) test -race -v ./...
	-@docker compose -f $(TEST_COMPOSE_FILE) down 2>/dev/null

tests: unit-test integration-test