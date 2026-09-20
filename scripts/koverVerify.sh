#!/bin/sh

chmod +x ../gradlew
echo $(ls -la ..)
echo $(whoami)
../gradlew koverVerify