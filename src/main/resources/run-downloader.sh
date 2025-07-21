#!/bin/bash

APP_NAME="downloader"
JAR_NAME="${APP_NAME}-1.0-SNAPSHOT.jar"
JAVA_MIN_VERSION=11

# Function to check if Java is installed and meets the minimum version
check_java() {
    if ! command -v java &> /dev/null; then
        echo "Error: Java is not installed or not in PATH"
        exit 1
    fi

    JAVA_VERSION=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}' | cut -d. -f1)
    if [ "$JAVA_VERSION" -lt "$JAVA_MIN_VERSION" ]; then
        echo "Error: Java $JAVA_MIN_VERSION or higher is required, found Java $JAVA_VERSION"
        exit 1
    fi
}

# Function to run the JAR
run_jar() {
    # Assume JAR is in the same directory as the script
    SCRIPT_DIR=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)
    JAR_PATH="${SCRIPT_DIR}/${JAR_NAME}"

    if [ ! -f "$JAR_PATH" ]; then
        echo "Error: Could not find ${JAR_NAME} in ${SCRIPT_DIR}"
        exit 1
    fi

    # Build classpath including dependencies in the same directory
    CLASSPATH="$JAR_PATH"
    for dep in "${SCRIPT_DIR}"/*.jar; do
        if [ -f "$dep" ] && [ "$dep" != "$JAR_PATH" ]; then
            CLASSPATH="$CLASSPATH:$dep"
        fi
    done

    # Run the JAR with specified memory settings and pass arguments
    java -Xmx512m -cp "$CLASSPATH" org.example.downloader.Main "$@"
}

# Main execution
check_java
run_jar "$@"