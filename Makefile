SHELL := /bin/bash
.SHELLFLAGS := -o pipefail -c
.ONESHELL:
.PHONY: up det logs

# or drop -u if you want local time
TS  := $(shell date +%Y%m%dT%H%M%SZ)
LOG := ./logs/compose.$(TS).log
$(shell mkdir -p ./logs)

stop:
	- docker compose down
	- docker image rm kc_keycloak
	- docker image rm kc_mockpass

run:
	docker compose up 2>&1 | tee -a "$(LOG)"; exit $${PIPESTATUS[0]}
# 	docker compose --ansi never --progress plain up 2>&1 | tee -a "$(LOG)"; exit $${PIPESTATUS[0]}
# 	docker compose up 2>&1 | awk '{ print strftime("%FT%T%z"), $0 }' | tee -a "$(LOG)"; exit $${PIPESTATUS[0]}
# 	docker compose up 2>&1 | ts '%Y-%m-%dT%H:%M:%S%z' | tee -a "$(LOG)"; exit $${PIPESTATUS[0]}
# 	docker compose up -d && docker compose logs -f 2>&1 | tee -a "$(LOG)"; exit $${PIPESTATUS[0]}

run-win:
	docker compose up 2>&1 | Tee-Object -FilePath .\logs\compose.$(Get-Date -Format "yyyy-MM-dd_HH-mm-ss").log -Append
# 	docker-compose up 2>&1 | Tee-Object -FilePath .\logs\compose.$(Get-Date -Format "yyyy-MM-dd_HH-mm-ss").log -Append
# 	docker compose --ansi never up 2>&1 | Tee-Object -FilePath .\logs\compose.$(Get-Date -Format "yyyy-MM-dd_HH-mm-ss").log -Append
# 	docker compose up 2>&1 | ForEach-Object { "$(Get-Date -Format o) $_" } | Tee-Object -FilePath .\logs\compose.$(Get-Date -Format "yyyy-MM-dd_HH-mm-ss").log -Append
# 	docker compose logs -f 2>&1 | Tee-Object -FilePath .\logs\compose.$(Get-Date -Format "yyyy-MM-dd_HH-mm-ss").log -Append

run-recreate:
	docker compose up -d --build --force-recreate
	
cleanup:
	-docker compose down
	-docker container rm ids_op
	-docker container rm cpds_api
	-docker container rm aceas_api
	-docker container rm kc_agency
	-docker container rm kc_db_agency
	-docker container rm mockpass	
	-docker image rm kc_ids
	-docker image rm kc_cpds_api
	-docker image rm kc_aceas_api
	-docker image rm kc_agency
	-docker image rm mockpass
	-docker volume rm app-sso_ids_data
	-docker volume rm app-sso_kc_pgdata_agency

re-ids:
	docker compose up -d --no-deps --build --force-recreate ids
re-cpds:
	docker compose up -d --no-deps --build --force-recreate cpds-api
re-aceas:
	docker compose up -d --no-deps --build --force-recreate aceas-api
re-web:
	docker compose up -d --no-deps --build --force-recreate web
re-mockpass:
	docker compose up -d --no-deps --build --force-recreate mockpass
re-kc:
	docker compose up -d --no-deps --build --force-recreate keycloak
re-db:
	docker compose up -d --no-deps --build --force-recreate db

log-ids:
	docker logs ids_op -f 
log-cpds:
	docker logs cpds_api -f 
log-aceas:
	docker logs aceas_api -f 
log-web:
	docker logs web -f 
log-mockpass:
	docker logs mockpass -f
log-kc:
	docker logs kc_agency -f
log-kc:
	docker logs kc_db_agency -f