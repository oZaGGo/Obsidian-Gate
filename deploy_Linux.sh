#!/bin/bash

GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

while true; do
    echo -e "${GREEN}[SYSTEM] Starting Obsidian panel...${NC}"


    mvn clean package -DskipTests

    # Search for the generated JAR file in the target directory
    JAR_PATH=$(ls target/*.jar | head -n 1)

    # Deploy
    java -jar "$JAR_PATH"

    # Check the exit code of the Java application
    EXIT_CODE=$?

    if [ $EXIT_CODE -eq 10 ]; then
        echo -e "${YELLOW}[UPDATE] Updating app from git...${NC}"
        git pull origin main
        echo -e "${YELLOW}[UPDATE] Done applying changes. Deploying again...${NC}"
    elif [ $EXIT_CODE -eq 0 ]; then
        echo -e "${GREEN}[SYSTEM] Closing app normally...${NC}"
        break
    else
        echo -e "${YELLOW}[SYSTEM] Error (Code: $EXIT_CODE).${NC}"
        break
    fi
done