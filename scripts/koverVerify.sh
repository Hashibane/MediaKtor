#!/bin/sh

echo $(ls -la ..)
echo $(whoami)
echo $(file ../gradlew)
../gradlew koverVerify