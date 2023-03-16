#!/bin/bash

export APPDYNAMICS_AGENT_ACCOUNT_ACCESS_KEY=xla10qs6ygz8
export APPDYNAMICS_AGENT_ACCOUNT_NAME=bb-pov
export APPDYNAMICS_AGENT_APPLICATION_NAME=ACME-FLIGHTS
export APPDYNAMICS_CONTROLLER_HOST_NAME=bb-pov.saas.appdynamics.com
export APPDYNAMICS_CONTROLLER_PORT=443
export APPDYNAMICS_CONTROLLER_SSL_ENABLED=true
export EVENT_ENDPOINT=https://analytics.api.appdynamics.com
export APPDYNAMICS_AGENT_GLOBAL_ACCOUNT_NAME=bb-pov_265c626f-ff25-47dc-be1f-4944ba8450b0
export EUM_HOST=pdx-col.eum-appdynamics.com
export EUM_HOST_SSL=pdx-col.eum-appdynamics.com
export EUM_KEY=AD-AAB-ABX-KCU

export API_SERVER=http://localhost:8080
export WEB_SERVER=http://localhost:8154

export ENABLE_BASE_LOAD=1

nodemon --inspect ./src/index.js
