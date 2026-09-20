#!/bin/sh

echo $(ls ..)
chmod +x koverVerify
../gradlew koverVerify