#!/bin/bash
# run JPortUI .JAR as Posix compliant Bash shell script without deploying Ant, Maven, etc.
JAR_APP="JPortsUI.jar"

mvn clean compile package

cp target/jport-1.0-SNAPSHOT.jar $JAR_APP

if [ -e $JAR_APP ]; then
  echo "Built $(ls -o $JAR_APP)"
  java -jar $JAR_APP &
fi

# exit
