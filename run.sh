#!/bin/bash
# Runs the executable
echo -e "Enter the machine IP: \c "
read ip
echo "Launching.."
java -Djava.security.policy=my.policy -Djava.rmi.server.hostname=$ip -jar executable/DAS.jar