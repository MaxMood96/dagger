#!/bin/bash

set -ex

if [ $# -lt 1 ]; then
    echo "Usage: $0 <JAVA_VERSION> [KOTLIN_VERSION] [KSP_VERSION]"
    exit 1
fi
readonly JAVA_VERSION_INPUT=$1
readonly KOTLIN_VERSION_INPUT=$2
readonly KSP_VERSION_INPUT=$3

GRADLE_ARGS="--no-daemon --stacktrace"
GRADLE_ARGS+=" -PjavaVersion=$JAVA_VERSION_INPUT"
GRADLE_ARGS+="${KOTLIN_VERSION_INPUT:+ -PkotlinVersion=$KOTLIN_VERSION_INPUT}"
GRADLE_ARGS+="${KSP_VERSION_INPUT:+ -PkspVersion=$KSP_VERSION_INPUT}"

readonly GRADLE_PROJECTS=(
    "javatests/artifacts/dagger"
    "javatests/artifacts/dagger-ksp"
)
for project in "${GRADLE_PROJECTS[@]}"; do
    echo "Running gradle tests for $project"
    ./$project/gradlew -p $project build $GRADLE_ARGS
    ./$project/gradlew -p $project test  --continue $GRADLE_ARGS
done
