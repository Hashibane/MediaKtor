#!/bin/sh

echo $(ls -la ..)
echo $(whoami)
../gradlew koverVerify