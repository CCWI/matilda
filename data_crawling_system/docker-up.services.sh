#! /bin/bash

docker-compose up --force-recreate --no-deps --build matilda-discovery matilda-libsim-ki matilda-gateway matilda-state matilda-ui
# -d to let docker-container start in background
# matilda-lib-manager
