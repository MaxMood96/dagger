#!/bin/bash

set -ex

if [ $# -lt 2 ]; then
    echo "Usage: $0 <AGP_VERSION> <JAVA_VERSION> [KOTLIN_VERSION] [KSP_VERSION]"
    exit 1
fi
readonly AGP_VERSION_INPUT=$1
readonly JAVA_VERSION_INPUT=$2
readonly KOTLIN_VERSION_INPUT=$3
readonly KSP_VERSION_INPUT=$4

GRADLE_ARGS="--no-daemon --stacktrace --configuration-cache"
GRADLE_ARGS+=" -PagpVersion=$AGP_VERSION_INPUT"
GRADLE_ARGS+=" -PjavaVersion=$JAVA_VERSION_INPUT"
GRADLE_ARGS+="${KOTLIN_VERSION_INPUT:+ -PkotlinVersion=$KOTLIN_VERSION_INPUT}"
GRADLE_ARGS+="${KSP_VERSION_INPUT:+ -PkspVersion=$KSP_VERSION_INPUT}"

readonly JAVA_ANDROID_GRADLE_PROJECTS=(
    "javatests/artifacts/dagger-android/simple"
    "javatests/artifacts/dagger-android-ksp"
    "javatests/artifacts/hilt-android/simple"
    "javatests/artifacts/hilt-android/pluginMarker"
)
readonly KOTLIN_ANDROID_GRADLE_PROJECTS=(
    "javatests/artifacts/hilt-android/simpleKotlin"
)

for project in "${JAVA_ANDROID_GRADLE_PROJECTS[@]}"; do
    echo "Running gradle tests for $project with AGP $AGP_VERSION_INPUT"
    ./$project/gradlew -p $project assembleDebug $GRADLE_ARGS
    ./$project/gradlew -p $project testDebugUnitTest --continue $GRADLE_ARGS
done

for project in "${KOTLIN_ANDROID_GRADLE_PROJECTS[@]}"; do
    echo "Running gradle tests for $project with AGP $AGP_VERSION_INPUT"
    ./$project/gradlew -p $project assembleDebug $GRADLE_ARGS
    ./$project/gradlew -p $project testWithKaptDebugUnitTest --continue $GRADLE_ARGS
    ./$project/gradlew -p $project testWithKspDebugUnitTest --continue $GRADLE_ARGS
done
