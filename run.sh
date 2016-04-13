#!/bin/bash
# Runs the executable
myip=$(cat ../ip.txt)
echo "Your IP is ${myip}"
echo -e "Enter the config file: \c "
read filename
echo "Launching.."
java -Djava.security.policy=my.policy -Djava.rmi.server.hostname=$myip -jar executable/DAS.jar $filename.config