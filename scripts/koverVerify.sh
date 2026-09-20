#!/bin/sh

echo $(ls -la ..)
chmod +x gradlew
../gradlew koverVerify