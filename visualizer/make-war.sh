#!/bin/bash
# make-war.sh $simNode $simHost $tournamentUrl
#
# Creates a .war file to deploy on some tomcat that interacts with a sim server.
# The sim location is defined by $simHost, it's name is defined by $simNode.
# The sim name is used for communication with the TournamentScheduler.
# The visualizer is reachable under http://tomcat_url:8080/visualizer/$simNode
# Assumes you have already done mvn clean compile.

if [ "$#" -ne "3" ]; then
  echo -e "\nThis script requires 3 arguments : \n"
  echo "- name of the sim server as defined in the TournamentScheduler"
  echo "- url or ip-address of the server (no http:// !)"
  echo "- url of the TournamentScheduler (http://server_url:8080/TournamentScheduler)"
  echo
  exit 1
fi

CMD="mvn war:war -DsimNode=$1 -DsimHost=$2 -DtournamentUrl=$3"
echo -e "\nRunning '$CMD'\n"
eval "$CMD"

NEWNAME="visualizer#$1.war"
echo -e "\nRenamming war file to $NEWNAME"
mv target/visualizer*.war $NEWNAME
echo
