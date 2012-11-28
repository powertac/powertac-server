#!/bin/bash
# deploy-local.sh $simNode $simHost $tournamentUrl
#
# Deploys to a local tomcat install. It will interact with the sim server
# The sim location is defined by $simHost, it's name is defined by $simNode.
# The sim name is used for communication with the TournamentScheduler.
# The visualizer is reachable under http://tomcat_url:8080/visualizer/$simNode

NODE="server_name"
HOST="server_url"
URL="http://localhost:8080/TournamentScheduler"

if [ "$#" -eq "0" ]; then
  echo "No arguments given. Using default values."
elif [ "$#" -eq "3" ]; then
  NODE=$1
  HOST=$2
  URL=$3
else
  echo "Incorrect number of arguments (need 3), using default values."
fi

CMD="mvn tomcat7:deploy -DsimNode=$NODE -DsimHost=$HOST -DtournamentUrl=$URL"
echo -e "\nRunning '$CMD'\n"
eval "$CMD"