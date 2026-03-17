.PHONY: help unit-test integration-test tests docker-up docker-down docker-logs

CORE_SERVICES = zookeeper kafka kafdrop insurance-service
TEST_SERVICE = tests

help:
	@echo "make docker-up         - Запустить систему (docker compose up)"
	@echo "make docker-down       - Остановить систему"
	@echo "make docker-logs       - Логи"
	@echo "make unit-test         - Unit тесты компонентов"
	@echo "make integration-test  - Интеграционные тесты (docker required)"
	@echo "make tests             - Все тесты"

docker-up:
	@docker compose up -d --build $(CORE_SERVICES)

docker-down:
	@docker compose down 2>/dev/null

docker-logs:
	@docker compose logs -f

unit-test:
	@mvn test

integration-test: docker-up
	@docker compose run --build --rm --entrypoint go $(TEST_SERVICE) test -race -v ./...
	-$(MAKE) docker-down

tests: unit-test integration-test